package com.example.recipeguide;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExtraInfoDialogFragment extends DialogFragment {

    // Интерфейс для передачи данных обратно в Activity
    public interface OnExtraInfoSavedListener {
        void onExtraInfoSaved(String cuisine, List<String> diet);
        //void onExtraInfoSaved(String cuisine, String diet, List<String> mainIngredients);
    }

    private OnExtraInfoSavedListener listener;
    private String otherTranslateText;
    SharedPreferences sharedPreferences;

    private Spinner spinnerCuisine;
    private Spinner spinnerDiet;
    private EditText etIngredient1;
    private EditText etIngredient2;
    private EditText etIngredient3;
    private Button btnCancel;
    private Button btnSave;
    private RadioButton lastCheckedRadioButton = null;
    private ColorStateList currentTint;
    private RadioButton rbItaly, rbJapan, rbChina, rbRussia, rbThailand, rbMexico, rbFrance, rbSpain, rbTurkey, rbGreece, rbOtherKitchen;

    private CheckBox cbMediterranean, cbVegetarianism, cbVeganism, cbLowCarb, cbGlutenFree, cbKetoDiet, cbDashDiet, cbPaleoDiet;
    private EditText etOtherCuisine;

    // Статический метод для создания экземпляра диалога
    public static ExtraInfoDialogFragment newInstance() {
        return new ExtraInfoDialogFragment();
    }

    // Метод для установки слушателя
    public void setOnExtraInfoSavedListener(OnExtraInfoSavedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_extra_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPreferences = requireActivity().getSharedPreferences("MODE", Context.MODE_PRIVATE);

        // Инициализация view
        initViews(view);

        // Настройка кнопок
        setupButtons();

        setupRadioButtons(view);
    }

    private void setupRadioButtons(View view) {
        RadioButton[] radioButtons = {
                view.findViewById(R.id.rb_italy),
                view.findViewById(R.id.rb_japan),
                view.findViewById(R.id.rb_china),
                view.findViewById(R.id.rb_russia),
                view.findViewById(R.id.rb_thailand),
                view.findViewById(R.id.rb_france),
                view.findViewById(R.id.rb_mexico),
                view.findViewById(R.id.rb_spain),
                view.findViewById(R.id.rb_turkey),
                view.findViewById(R.id.rb_greece),
                view.findViewById(R.id.rb_other_kitchen)
        };

        for (RadioButton radioButton : radioButtons) {
            radioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    // Снимаем выделение с предыдущей радио-кнопки
                    if (lastCheckedRadioButton != null && lastCheckedRadioButton != buttonView) {
                        lastCheckedRadioButton.setChecked(false);
                    }
                    lastCheckedRadioButton = (RadioButton) buttonView;

                    // Показываем EditText только для "Другая кухня"
                    EditText etOther = view.findViewById(R.id.et_other_kitchen);
                    if (buttonView.getId() == R.id.rb_other_kitchen) {
                        etOther.setVisibility(View.VISIBLE);
                    } else {
                        etOther.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    private void initViews(View view) {
        rbItaly = view.findViewById(R.id.rb_italy);
        rbJapan = view.findViewById(R.id.rb_japan);
        rbChina = view.findViewById(R.id.rb_china);
        rbRussia = view.findViewById(R.id.rb_russia);
        rbThailand = view.findViewById(R.id.rb_thailand);
        rbFrance = view.findViewById(R.id.rb_france);
        rbMexico = view.findViewById(R.id.rb_mexico);
        rbSpain = view.findViewById(R.id.rb_spain);
        rbTurkey = view.findViewById(R.id.rb_turkey);
        rbGreece = view.findViewById(R.id.rb_greece);
        rbOtherKitchen = view.findViewById(R.id.rb_other_kitchen);

        cbMediterranean = view.findViewById(R.id.cb_mediterranean);
        cbVegetarianism = view.findViewById(R.id.cb_vegetarianism);
        cbVeganism = view.findViewById(R.id.cb_veganism);
        cbLowCarb = view.findViewById(R.id.cb_low_carb);
        cbGlutenFree = view.findViewById(R.id.cb_gluten_free);
        cbKetoDiet = view.findViewById(R.id.cb_keto_diet);
        cbDashDiet = view.findViewById(R.id.cb_dash_diet);
        cbPaleoDiet = view.findViewById(R.id.cb_paleo_diet);

        etOtherCuisine = view.findViewById(R.id.et_other_kitchen);

        /*etIngredient1 = view.findViewById(R.id.et_ingredient_1);
        etIngredient2 = view.findViewById(R.id.et_ingredient_2);
        etIngredient3 = view.findViewById(R.id.et_ingredient_3);*/
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnSave = view.findViewById(R.id.btn_save);
    }


    private void setupButtons() {
        btnCancel.setOnClickListener(v -> dismiss());

        btnSave.setOnClickListener(v -> {
           /* // Проверка обязательных полей


            // Собираем данные
            //String cuisine = spinnerCuisine.getSelectedItem().toString();
            //String diet = spinnerDiet.getSelectedItem().toString();

            List<String> ingredients = new ArrayList<>();
            ingredients.add(ingredient1);

            String ingredient2 = etIngredient2.getText().toString().trim();
            if (!ingredient2.isEmpty()) {
                ingredients.add(ingredient2);
            }

            String ingredient3 = etIngredient3.getText().toString().trim();
            if (!ingredient3.isEmpty()) {
                ingredients.add(ingredient3);
            }*/

            if (validateET()) {
                if (listener != null) {
                    String cuisine = "";

                    String otherText = etOtherCuisine.getText().toString().trim();

                    if (rbOtherKitchen.isChecked() && !otherText.isEmpty()) {
                        if (sharedPreferences.getBoolean("language", false)) {
                            ExecutorService translationExecutor = Executors.newSingleThreadExecutor();

                            translationExecutor.execute(() -> {

                                TranslatorOptions options = new TranslatorOptions.Builder()
                                        .setSourceLanguage(TranslateLanguage.RUSSIAN)
                                        .setTargetLanguage(TranslateLanguage.ENGLISH)
                                        .build();

                                Translator translator = Translation.getClient(options);

                                translator.downloadModelIfNeeded()
                                        .addOnSuccessListener(unused -> {
                                            translator.translate(otherText)
                                                    .addOnSuccessListener(translatedText -> {
                                                        otherTranslateText = translatedText;
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e("MLKit", "Translation failed: " + e.getMessage());
                                                    });

                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("MLKit", "Model download failed: " + e.getMessage());
                                        });
                            });
                        }
                        else{
                            otherTranslateText = otherText;
                        }
                    }

                    if (rbItaly.isChecked()) cuisine = "Italian";
                    else if (rbJapan.isChecked()) cuisine = "Japanese";
                    else if (rbChina.isChecked()) cuisine = "Chinese";
                    else if (rbRussia.isChecked()) cuisine = "Russian";
                    else if (rbThailand.isChecked()) cuisine = "Thai";
                    else if (rbFrance.isChecked()) cuisine = "French";
                    else if (rbMexico.isChecked()) cuisine = "Mexican";
                    else if (rbSpain.isChecked()) cuisine = "Spanish";
                    else if (rbTurkey.isChecked()) cuisine = "Turkish";
                    else if (rbGreece.isChecked()) cuisine = "Greek";
                    else if (!otherTranslateText.isEmpty()) {
                        cuisine = otherTranslateText;
                    } else {
                        cuisine = "null";
                    }

                    List<String> selectedDiets = new ArrayList<>();

// Проверяем каждый CheckBox и добавляем текст в StringBuilder если выбран
                    if (cbMediterranean.isChecked()) {
                        selectedDiets.add("Mediterranean Diet");
                    }
                    if (cbVegetarianism.isChecked()) {
                        selectedDiets.add("Vegetarianism");
                    }
                    if (cbVeganism.isChecked()) {
                        selectedDiets.add("Veganism");
                    }
                    if (cbLowCarb.isChecked()) {
                        selectedDiets.add("Low-Carb Diet");
                    }
                    if (cbGlutenFree.isChecked()) {
                        selectedDiets.add("Gluten-Free Diet");
                    }
                    if (cbKetoDiet.isChecked()) {
                        selectedDiets.add("Keto Diet");
                    }
                    if (cbDashDiet.isChecked()) {
                        selectedDiets.add("DASH Diet");
                    }
                    if (cbPaleoDiet.isChecked()) {
                        selectedDiets.add("Paleo Diet");
                    }


                    if (selectedDiets.isEmpty()) {
                        selectedDiets.add("null");
                    }

                    listener.onExtraInfoSaved(cuisine, selectedDiets);
                }

                // Закрываем диалог
                dismiss();

            }
            // Передаем данные через слушатель

        });
    }

    public boolean validateET() {
        RadioButton otherRB = rbOtherKitchen;
        EditText otherET = etOtherCuisine;
        boolean hasOtherCuisineFilled = true;
        if (otherRB.isChecked()) {
            String otherText = otherET.getText().toString().trim();
            // Убираем пунктуацию и проверяем, остался ли текст
            String textWithoutPunctuation = otherText.replaceAll("[\\p{Punct}\\s]+", "");
            hasOtherCuisineFilled = !textWithoutPunctuation.isEmpty();

            if (!hasOtherCuisineFilled) {
                currentTint = ColorStateList.valueOf(Color.RED); // Сохраняем цвет ошибки
                otherET.setBackgroundTintList(currentTint);
                otherET.setHint(R.string.error_hint_other_cuisine);
                otherET.setHintTextColor(currentTint);
            } else {
                TypedValue typedValue = new TypedValue();
                requireActivity().getTheme().resolveAttribute(android.R.attr.colorControlActivated, typedValue, true);
                int colorPrimary = typedValue.data;
                otherET.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                otherET.setHint(getString(R.string.enter_other));
                otherET.setHintTextColor(ColorStateList.valueOf(colorPrimary));

            }
        } else {
            TypedValue typedValue = new TypedValue();
            requireActivity().getTheme().resolveAttribute(android.R.attr.colorControlActivated, typedValue, true);
            int colorPrimary = typedValue.data;
            otherET.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
            otherET.setHint(getString(R.string.enter_other));
        }

        return hasOtherCuisineFilled;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Настройка размеров диалога
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            // Добавляем отступы 100dp сверху и снизу (как вы хотели)
            int marginInPx = (int) (100 * getResources().getDisplayMetrics().density);

            // Применяем отступы к корневому view
            dialog.getWindow().getDecorView().setPadding(
                    0,           // left
                    marginInPx,  // top
                    0,           // right
                    marginInPx   // bottom
            );
        }
    }
}
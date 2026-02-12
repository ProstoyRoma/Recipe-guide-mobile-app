package com.example.recipeguide;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.gson.Gson;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import Data.DatabaseHandler;
import Model.QuestionnaireModel;

/**
 * QuestionnaireActivity
 * Сбор минимального профиля пользователя.
 * <p>
 * Требует:
 * - QuestionnaireModel (см. пример ранее)
 * - NetworkUtils.postJson(context, url, json, callback)
 * - strings.xml локализация для текстов
 */
public class QuestionnaireActivity extends AppCompatActivity {
    private static final String PREFS = "app_prefs";
    private static final String KEY_QUESTIONNAIRE = "questionnaire_completed";


    private CheckBox cbNuts, cbMilk, cbEggs, cbFish, cbWheat, cbSoy, cbPeanuts, cbShellfish, cbOtherAllergy, cbNoAllergy,
            cbItaly, cbJapan, cbChina, cbRussia, cbThailand, cbMexico, cbFrance, cbSpain, cbTurkey, cbGreece, cbOtherKitchen;
    private RadioGroup dietRadioGroup; // Если переделали на RadioGroup
    private ChipGroup categoryChipGroup;
    private Button btnSkip, btnFinish;
    private EditText otherAllergy, otherKitchen;
    private TextView errorAllergy, errorCategory, hintCategory;
    private ProgressBar uploadPB;
    private List<CheckBox> allergyCheckBoxes = new ArrayList<>();
    private DatabaseHandler db;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    SharedPreferences sharedPreferences;
    private ColorStateList currentTint;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire);

        db = new DatabaseHandler(this);
        mAuth = FirebaseAuth.getInstance();

        sharedPreferences = getSharedPreferences("MODE", MODE_PRIVATE);

        initViews();
        setupButtons();
        initAllergyCheckBox();
    }

    private void initViews() {
        cbNuts = findViewById(R.id.cb_nuts);
        cbMilk = findViewById(R.id.cb_milk);
        cbEggs = findViewById(R.id.cb_eggs);
        cbFish = findViewById(R.id.cb_fish);
        cbSoy = findViewById(R.id.cb_soy);
        cbWheat = findViewById(R.id.cb_wheat);
        cbPeanuts = findViewById(R.id.cb_peanuts);
        cbShellfish = findViewById(R.id.cb_shellfish);
        cbOtherAllergy = findViewById(R.id.cb_other);
        cbNoAllergy = findViewById(R.id.cb_no_allergy);

        allergyCheckBoxes.add(cbMilk);
        allergyCheckBoxes.add(cbEggs);
        allergyCheckBoxes.add(cbNuts);
        allergyCheckBoxes.add(cbPeanuts);
        allergyCheckBoxes.add(cbFish);
        allergyCheckBoxes.add(cbSoy);
        allergyCheckBoxes.add(cbWheat);
        allergyCheckBoxes.add(cbShellfish);
        allergyCheckBoxes.add(cbOtherAllergy); // Добавляем "Другое"
        allergyCheckBoxes.add(cbNoAllergy); // Добавляем "Нет аллергий"

        //dietRadioGroup = findViewById(R.id.diet_radio_group)

        /*cbItaly = findViewById(R.id.cb_italy);
        cbJapan = findViewById(R.id.cb_japan);
        cbChina = findViewById(R.id.cb_china);
        cbRussia = findViewById(R.id.cb_russia);
        cbThailand = findViewById(R.id.cb_thailand);
        cbFish = findViewById(R.id.cb_france);
        cbMexico = findViewById(R.id.cb_mexico);
        cbSpain = findViewById(R.id.cb_spain);
        cbTurkey = findViewById(R.id.cb_turkey);
        cbGreece = findViewById(R.id.cb_greece);
        cbOtherKitchen = findViewById(R.id.cb_other_kitchen);*/

        errorAllergy=findViewById(R.id.allergy_error);
        errorCategory=findViewById(R.id.category_error);
        hintCategory=findViewById(R.id.category_hint);

        otherAllergy = findViewById(R.id.et_other);
        //otherKitchen = findViewById(R.id.et_other_kitchen);

        categoryChipGroup = findViewById(R.id.allergy_chip_group);

        uploadPB=findViewById(R.id.uploadProgressBar);

        btnSkip = findViewById(R.id.btn_skip);
        btnFinish = findViewById(R.id.btn_finish);

    }

    private void initAllergyCheckBox() {
        // 1. Обработка чекбокса "Другое"
        cbOtherAllergy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Показываем/скрываем EditText в зависимости от состояния
                if (isChecked) {
                    otherAllergy.setVisibility(View.VISIBLE);
                    otherAllergy.requestFocus(); // Фокус на поле для ввода

                    // Если выбрано "Другое", сбрасываем "Нет аллергий"
                    cbNoAllergy.setChecked(false);
                } else {
                    otherAllergy.setVisibility(View.GONE);
                }
            }
        });

        // 2. Обработка чекбокса "Нет аллергий"
        cbNoAllergy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Сбрасываем ВСЕ остальные чекбоксы аллергенов
                    for (CheckBox checkBox : allergyCheckBoxes) {
                        if (checkBox != cbNoAllergy) { // Не сбрасываем сам cbNoAllergy
                            checkBox.setChecked(false);
                        }
                    }

                    // Скрываем поле "Другое", если оно было показано
                    if (cbOtherAllergy.isChecked()) {
                        cbOtherAllergy.setChecked(false); // Это автоматически скроет etOther
                    }
                }
            }
        });

        // 3. Обработка для всех остальных чекбоксов (чтобы сбрасывать "Нет аллергий")
        for (CheckBox checkBox : allergyCheckBoxes) {
            if (checkBox != cbNoAllergy && checkBox != cbOtherAllergy) {
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        // Если выбрали любой аллерген, сбрасываем "Нет аллергий"
                        if (isChecked) {
                            cbNoAllergy.setChecked(false);
                        }
                    }
                });
            }
        }
    }

    private void setupButtons() {
        btnSkip.setOnClickListener(v -> {
            // Нажал "Позже": сбрасываем флаг reorder, чтобы при следующем запуске снова показать Reorder
            Intent intent = new Intent(this, MainScreen.class);
            startActivity(intent);
            finish();
        });

        btnFinish.setOnClickListener(v -> {
            if(validateQuestionnaire()) {
                uploadPB.setVisibility(View.VISIBLE);
                saveQuestionnaireData();
            }

        });
    }

    private void saveQuestionnaireData() {
        // 1. Собираем аллергены в одну строку через запятую
        StringBuilder allergiesBuilder = new StringBuilder();

        // Основные аллергены
        if (cbMilk.isChecked()) allergiesBuilder.append("Milk,");
        if (cbEggs.isChecked()) allergiesBuilder.append("Eggs,");
        if (cbNuts.isChecked()) allergiesBuilder.append("Nuts,");
        if (cbPeanuts.isChecked()) allergiesBuilder.append("Peanuts,");
        if (cbFish.isChecked()) allergiesBuilder.append("Fish,");
        if (cbShellfish.isChecked()) allergiesBuilder.append("Shellfish,");
        if (cbSoy.isChecked()) allergiesBuilder.append("Soy,");
        if (cbWheat.isChecked()) allergiesBuilder.append("Wheat,");
        if (cbNoAllergy.isChecked()) allergiesBuilder.append("null");

        // Добавляем "Другое", если заполнено
        String otherAllergyText = otherAllergy.getText().toString().trim();
        String otherAllergyTextTranslate = "";
        if (cbOtherAllergy.isChecked() && !otherAllergyText.isEmpty()) {
            if (sharedPreferences.getBoolean("language", false)) {

                final String[] translatedResult = {""};

                ExecutorService translationExecutor = Executors.newSingleThreadExecutor();

                translationExecutor.execute(() -> {

                    TranslatorOptions options = new TranslatorOptions.Builder()
                            .setSourceLanguage(TranslateLanguage.RUSSIAN)
                            .setTargetLanguage(TranslateLanguage.ENGLISH)
                            .build();

                    Translator translator = Translation.getClient(options);

                    translator.downloadModelIfNeeded()
                            .addOnSuccessListener(unused -> {
                                translator.translate(otherAllergyText)
                                        .addOnSuccessListener(translatedText -> {
                                            translatedResult[0] = translatedText;
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("MLKit", "Translation failed: " + e.getMessage());
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("MLKit", "Model download failed: " + e.getMessage());
                            });
                });
                try {
                    //latch.await(10, TimeUnit.SECONDS);
                    otherAllergyTextTranslate = translatedResult[0];
                } catch (Exception e) {
                    Log.e("TRANSLATION", "Translation interrupted", e);
                    otherAllergyTextTranslate = otherAllergyText; // Используем оригинал
                }

                // Добавляем в список аллергенов
                if (!otherAllergyTextTranslate.isEmpty()) {
                    allergiesBuilder.append(otherAllergyTextTranslate).append(",");
                }

            }else{
                allergiesBuilder.append(otherAllergyText).append(",");
            }
        }

        // Убираем последнюю запятую
        String allergies = "";
        if (allergiesBuilder.length() > 0) {
            allergies = allergiesBuilder.substring(0, allergiesBuilder.length() - 1);
        }

        // 2. Собираем категории как цифры
        ArrayList<Integer> categoryNumbers = new ArrayList<>();

        // Маппинг категорий к цифрам (согласно вашему списку)
        Map<String, Integer> categoryMap = new HashMap<>();
        categoryMap.put(getString(R.string.category_appetizers), 0);
        categoryMap.put(getString(R.string.category_soup), 1);
        categoryMap.put(getString(R.string.category_salad), 2);
        categoryMap.put(getString(R.string.category_main_course), 3);
        categoryMap.put(getString(R.string.category_side_dish), 4);
        categoryMap.put(getString(R.string.category_dessert), 5);
        categoryMap.put(getString(R.string.category_drink), 6);
        categoryMap.put(getString(R.string.category_sauces_and_seasonings), 7);
        categoryMap.put(getString(R.string.category_baked_goods), 8);
        categoryMap.put(getString(R.string.category_snacks), 9); // У вас дублируется "Закуски" (0 и 9)

        // Проверяем выбранные чипсы категорий
        for (int i = 0; i < categoryChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) categoryChipGroup.getChildAt(i);
            if (chip.isChecked()) {
                String categoryName = chip.getText().toString();
                Integer categoryNumber = categoryMap.get(categoryName);
                if (categoryNumber != null) {
                    categoryNumbers.add(categoryNumber);
                }
            }
        }

        // 3. Преобразуем список категорий в строку через запятую
        StringBuilder categoriesBuilder = new StringBuilder();
        for (Integer num : categoryNumbers) {
            categoriesBuilder.append(num).append(",");
        }

        String categoriesString = "";
        if (categoriesBuilder.length() > 0) {
            categoriesString = categoriesBuilder.substring(0, categoriesBuilder.length() - 1);
        }

        // 4. Сохраняем в SQLite
        long dbResult = db.saveUserPreferences(
                User.username,
                allergies,
                categoriesString
        );

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // 5. Сохраняем в Firebase
            saveToFirebase(user, allergies, categoriesString);
        }
        // 6. Обновляем статический класс User
        User.updateFromQuestionnaire(allergies, categoryNumbers);

        // 7. Показываем результат и закрываем активность
        if (dbResult != -1) {
            /*SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            editor.putBoolean(KEY_QUESTIONNAIRE, true);
            editor.apply();*/

            db.getRecommendedRecipe(this, new RecommendedCallback() {
                @Override
                public void onSuccess() {
                    // Запускаем MainScreen сразу после успешной загрузки рекомендаций
                    runOnUiThread(() -> {
                        Toast.makeText(QuestionnaireActivity.this, "Для сохранения данных в облаке необходимо войти в аккаунт.", Toast.LENGTH_LONG).show();
                        uploadPB.setVisibility(View.GONE);

                        Intent intent = new Intent(QuestionnaireActivity.this, MainScreen.class);
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    // Даже при ошибке переходим на главный экран
                    runOnUiThread(() -> {
                        uploadPB.setVisibility(View.GONE);
                        Intent intent = new Intent(QuestionnaireActivity.this, MainScreen.class);
                        startActivity(intent);
                        finish();
                    });
                }
            });
        } else {
            uploadPB.setVisibility(View.GONE);
            Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
        }
    }
    // Метод для показа диалога

    private void saveToFirebase(FirebaseUser user, String allergies, String categoriesString) {
        try {
            database = FirebaseDatabase.getInstance();
            myRef = database.getReference("users");
            myRef.child(user.getUid()).child("allergies").setValue(allergies);
            myRef.child(user.getUid()).child("likeCategory").setValue(categoriesString);
            myRef.child(user.getUid()).child("date_time").setValue(String.valueOf(LocalDateTime.now()));
        } catch (Exception e) {
            Log.e("FIREBASE", "Firebase error", e);
        }
    }

    private boolean validateQuestionnaire() {
        // 1. Проверка аллергенов: хотя бы один CheckBox выбран или "Нет аллергий"
        boolean hasAllergySelected = false;
        boolean hasOtherAllergyFilled = true; // По умолчанию true, если "Другое" не выбрано

        // Проверяем основные аллергены
        hasAllergySelected = cbMilk.isChecked() || cbEggs.isChecked() || cbNuts.isChecked() ||
                cbPeanuts.isChecked() || cbFish.isChecked() || cbShellfish.isChecked() ||
                cbSoy.isChecked() || cbWheat.isChecked() || cbNoAllergy.isChecked();

        // Если выбран "Другое", проверяем заполненность EditText
        if (cbOtherAllergy.isChecked()) {
            String otherText = otherAllergy.getText().toString().trim();
            // Убираем пунктуацию и проверяем, остался ли текст
            String textWithoutPunctuation = otherText.replaceAll("[\\p{Punct}\\s]+", "");
            hasOtherAllergyFilled = !textWithoutPunctuation.isEmpty();

            if (!hasOtherAllergyFilled) {
                currentTint = ColorStateList.valueOf(Color.RED); // Сохраняем цвет ошибки
                otherAllergy.setBackgroundTintList(currentTint);

                errorAllergy.setVisibility(View.VISIBLE);
                errorAllergy.setText("Пожалуйста, укажите какой продукт хотите исключить");
            }else{
                TypedValue typedValue = new TypedValue();
                getTheme().resolveAttribute(android.R.attr.colorControlActivated, typedValue, true);
                int colorPrimary = typedValue.data;
                otherAllergy.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));

                errorAllergy.setVisibility(View.GONE);
                errorAllergy.setText("Пожалуйста, ответьте на вопрос");
            }
            hasAllergySelected = true; // "Другое" считается выбранным аллергеном
        }else{
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.colorControlActivated, typedValue, true);
            int colorPrimary = typedValue.data;
            otherAllergy.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));

            errorAllergy.setVisibility(View.GONE);
            errorAllergy.setText("Пожалуйста, ответьте на вопрос");
        }

        if (!hasAllergySelected) {
            errorAllergy.setVisibility(View.VISIBLE);
        }

        // 2. Проверка категорий: хотя бы один Chip выбран
        boolean hasCategorySelected = false;
        for (int i = 0; i < categoryChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) categoryChipGroup.getChildAt(i);
            if (chip.isChecked()) {
                hasCategorySelected = true;
                break;
            }
        }

        if (!hasCategorySelected) {
            errorCategory.setVisibility(View.VISIBLE);
            hintCategory.setVisibility(View.GONE);
        }else {
            errorCategory.setVisibility(View.GONE);
            hintCategory.setVisibility(View.VISIBLE);
        }

        return hasCategorySelected && hasAllergySelected && hasOtherAllergyFilled;
    }

}

package com.example.recipeguide;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// IngredientsFragmentForAddScreen.java
public class IngredientsFragmentForAddScreen extends Fragment {
    private LinearLayout container;
    private Button addRowBtn;

    // Список доступных ингредиентов и мапа "ингредиент → мера"
    private final List<String> ingredients = Arrays.asList(
            "Мука", "Сахар", "Молоко", "Яйцо", "Соль", "Перец"
    );
    private static final String MEASURE_TASTE = "по вкусу";

    private final Map<String, String> defaultMeasures = new HashMap<String, String>() {{
        put("Мука", "г");
        put("Сахар", "г");
        put("Молоко", "мл");
        put("Яйцо", "шт");
        put("Соль", "ч.л.");
    }};
    private final List<String> measures = Arrays.asList("г", "кг", "мл", "л", "шт", "ч.л.", "ст.л.", "по вкусу");
    private static final String KEY_ROWS = "ingredients_rows"; // список строк вида "name|qty|measure"

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ingredients_input, parent, false);

        container = view.findViewById(R.id.ingredients_container);
        addRowBtn = view.findViewById(R.id.btn_add_row);

        addRowBtn.setOnClickListener(v -> addIngredientRow());
        addIngredientRow();

        // Если есть сохранённое состояние — восстановим
        /*if (savedInstanceState != null && savedInstanceState.containsKey(KEY_ROWS)) {
            ArrayList<String> rows = savedInstanceState.getStringArrayList(KEY_ROWS);
            if (rows != null && !rows.isEmpty()) {
                // Добавляем строки в том же порядке, как были сохранены
                for (String row : rows) {
                    // формат "name|qty|measure" (возможно поля пустые)
                    String[] parts = row.split("\\|", -1);
                    String name = parts.length > 0 ? parts[0] : "";
                    String qty  = parts.length > 1 ? parts[1] : "";
                    String mes  = parts.length > 2 ? parts[2] : "";
                    addIngredientRow(name, qty, mes);
                }
            } else {
                // если список пуст — создать одну пустую строку
                addIngredientRow(null, null, null);
            }
        } else {
            // при первом создании фрагмента добавляем одну строку
            addIngredientRow(null, null, null);
        }*/
        return view;
    }
    /*@Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<String> rows = collectRowsData();
        outState.putStringArrayList(KEY_ROWS, rows);
    }*/


    private ArrayList<String> collectRowsData() {
        ArrayList<String> rows = new ArrayList<>();
        if (container == null) return rows;
        int count = container.getChildCount();
        for (int i = 0; i < count; i++) {
            View row = container.getChildAt(i);
            if (row == null) {
                rows.add(""); // на всякий случай
                continue;
            }
            AutoCompleteTextView ingInput = row.findViewById(R.id.actv_ingredient);
            EditText qtyInput = row.findViewById(R.id.et_quantity);
            AutoCompleteTextView mesInput = row.findViewById(R.id.actv_measure);

            String name = ingInput != null ? ingInput.getText().toString() : "";
            String qty = qtyInput != null ? qtyInput.getText().toString() : "";
            String mes = mesInput != null ? mesInput.getText().toString() : "";

            // объединяем с разделителем '|', используем пустые строки, если поле пустое
            rows.add(name + "|" + qty + "|" + mes);
        }
        return rows;
    }

    private void addIngredientRow() {
        View row = getLayoutInflater().inflate(
                R.layout.item_ingredient_row, container, false
        );

        // Получаем все view
        TextView indexTv = row.findViewById(R.id.tv_index);
        AutoCompleteTextView ingInput = row.findViewById(R.id.actv_ingredient);
        EditText qtyInput = row.findViewById(R.id.et_quantity);
        AutoCompleteTextView mesInput = row.findViewById(R.id.actv_measure);
        ImageButton removeBtn = row.findViewById(R.id.btn_remove_row); // если у вас есть кнопка удаления


        // Устанавливаем inputType так, как хочется (например numberDecimal чтобы открывалась цифровая клавиатура)
        qtyInput.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        qtyInput.setKeyListener(DigitsKeyListener.getInstance("0123456789.-–—, "));
// Фильтр: разрешаем цифры, точку, запятую, пробел и дефисы/тире (ASCII '-', en-dash '–', em-dash '—')
        InputFilter rangeFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                int cp = Character.codePointAt(source.toString(), i);
                if (!(Character.isDigit(cp)
                        || cp == '.'
                        || cp == ','
                        || cp == ' '
                        || cp == '-'
                        || cp == '–'    // en-dash
                        || cp == '—'))  // em-dash
                {
                    return ""; // запретить этот символ
                }
            }
            return null; // разрешить
        };

        qtyInput.setFilters(new InputFilter[]{ rangeFilter });
        // Устанавливаем номер строки
        int number = container.getChildCount() + 1;
        indexTv.setText(number + ".");

        // Адаптер для ингредиентов
        ArrayAdapter<String> ingAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                ingredients
        );
        ingInput.setAdapter(ingAdapter);

        // Адаптер для мер
        ArrayAdapter<String> mesAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                measures
        );
        mesInput.setAdapter(mesAdapter);

        /*if (namePrefill != null) ingInput.setText(namePrefill);
        if (qtyPrefill != null) qtyInput.setText(qtyPrefill);
        if (mesPrefill != null) mesInput.setText(mesPrefill);*/

        // helper: обновляет видимость/состояние поля quantity в зависимости от меры
        Runnable applyMeasureBehavior = () -> {
            String measureText = mesInput.getText() != null ? mesInput.getText().toString().trim() : "";
            boolean isTaste = MEASURE_TASTE.equalsIgnoreCase(measureText);
            if (isTaste) {
                // скрываем количество и очищаем его
                qtyInput.setText("");
                //qtyInput.setVisibility(View.GONE);
                qtyInput.setEnabled(false);
            } else {
                qtyInput.setVisibility(View.VISIBLE);
                qtyInput.setEnabled(true);
            }
        };

        // При выборе элемента из списка мер (AutoComplete) применяем поведение
        mesInput.setOnItemClickListener((parent, v, pos, id) -> applyMeasureBehavior.run());

        // При вводе текста вручную (потенциально пользователь вводит "По вкусу") — тоже отслеживаем
        mesInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                applyMeasureBehavior.run();
            }
        });

        // Если выбирается ингредиент — подставляем дефолтную меру и применяем поведение
        ingInput.setOnItemClickListener((parent, view, position, id) -> {
            String chosen = (String) parent.getItemAtPosition(position);
            String defM = defaultMeasures.get(chosen);
            if (defM != null) {
                mesInput.setText(defM);
            } else {
                mesInput.setText("");
            }
            applyMeasureBehavior.run();
        });

        // Применим поведение сразу при создании строки (например, если было prefill)
        applyMeasureBehavior.run();

        removeBtn.setOnClickListener(v -> {
            if (number > 1) {
                container.removeView(row);
                // обновляем номера строк
                refreshIndexes();
            }
        });
        container.addView(row);
        refreshIndexes();
    }

    private void refreshIndexes() {
        if (container == null) return;
        int count = container.getChildCount();
        for (int i = 0; i < count; i++) {
            View row = container.getChildAt(i);
            TextView indexTv = row.findViewById(R.id.tv_index);
            if (indexTv != null) indexTv.setText((i + 1) + ".");
        }
    }

    public boolean validateInputs() {
        if (container == null) return false;

        int count = container.getChildCount();
        for (int i = 0; i < count; i++) {
            View row = container.getChildAt(i);
            if (row == null) continue;

            AutoCompleteTextView ingInput = row.findViewById(R.id.actv_ingredient);
            EditText qtyInput = row.findViewById(R.id.et_quantity);
            AutoCompleteTextView mesInput = row.findViewById(R.id.actv_measure);

            /*String name = ingInput  != null ? ingInput.getText().toString().trim() : "";
            String qty  = qtyInput  != null ? qtyInput.getText().toString().trim() : "";
            String mes  = mesInput  != null ? mesInput.getText().toString().trim() : "";*/
            // Защитные проверки — если какой-то view отсутствует, логируем и считаем строку некорректной
            if (ingInput == null || qtyInput == null || mesInput == null) {
                Log.e("Ingredients", "validateInputs: missing view in row index=" + i
                        + " ing=" + (ingInput == null) + " qty=" + (qtyInput == null) + " mes=" + (mesInput == null));
                return false;
            }

            String name = safeText(ingInput);
            String qty = safeText(qtyInput);
            String mes = safeText(mesInput);

            boolean measureIsTaste = MEASURE_TASTE.equalsIgnoreCase(mes);
            // если строка частично заполнена — некорректно
            boolean allFilled;
            if (measureIsTaste) {
                // name и мера заполнены (либо всё пусто)
                allFilled = !name.isEmpty() && !mes.isEmpty();
            } else {
                allFilled = !name.isEmpty() && !qty.isEmpty() && !mes.isEmpty();
            }


            if (!allFilled) {
                return false;
            }
        }
        return true;
    }

    private String safeText(TextView tv) {
        CharSequence cs = tv.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    public String getIngredientsData() {
        StringBuilder sb = new StringBuilder();
        int count = container.getChildCount();
        int outIndex = 0;
        for (int i = 0; i < count; i++) {
            View row = container.getChildAt(i);
            if (row == null) continue;

            AutoCompleteTextView ingInput = row.findViewById(R.id.actv_ingredient);
            EditText qtyInput = row.findViewById(R.id.et_quantity);
            AutoCompleteTextView mesInput = row.findViewById(R.id.actv_measure);

            String name = ingInput != null ? ingInput.getText().toString().trim() : "";
            String qty = qtyInput != null ? qtyInput.getText().toString().trim() : "";
            String mes = mesInput != null ? mesInput.getText().toString().trim() : "";

            // Пропускаем строки без названия ингредиента
            if (name.isEmpty()) continue;

            outIndex++; // реальный порядковый номер только для непустых строк

            // Если количество или мера пусты — оставляем пустую часть (между | будет пусто)
            if (outIndex > 1) sb.append('\n');
            sb.append(outIndex).append(". ").append(name).append("|")
                    .append(qty).append("|").append(mes);
        }
        return sb.toString();
    }

    public void errorInputs() {
        if (container == null) return;

        // Цвет ошибки
        ColorStateList errTint = ColorStateList.valueOf(Color.RED);
        // Попытка получить стандартный tint из ресурсов; если нет — используем null
        ColorStateList normalTint = AppCompatResources.getColorStateList(requireContext(), R.color.black);

        int count = container.getChildCount();
        for (int i = 0; i < count; i++) {
            View row = container.getChildAt(i);
            if (row == null) continue;

            AutoCompleteTextView ingInput = row.findViewById(R.id.actv_ingredient);
            EditText qtyInput = row.findViewById(R.id.et_quantity);
            AutoCompleteTextView mesInput = row.findViewById(R.id.actv_measure);

            String name = ingInput != null ? ingInput.getText().toString().trim() : "";
            String qty = qtyInput != null ? qtyInput.getText().toString().trim() : "";
            String mes = mesInput != null ? mesInput.getText().toString().trim() : "";

            boolean measureIsTaste = MEASURE_TASTE.equalsIgnoreCase(mes);
            boolean allFilled;
            if (measureIsTaste) {
                allFilled = !name.isEmpty() && !mes.isEmpty();
            } else {
                allFilled = !name.isEmpty() && !qty.isEmpty() && !mes.isEmpty();
            }

            if (allFilled) {
                if (ingInput != null) setTintSafely(ingInput, normalTint);
                if (qtyInput != null) setTintSafely(qtyInput, normalTint);
                if (mesInput != null) setTintSafely(mesInput, normalTint);
            } else {
                if (ingInput != null) {
                    setTintSafely(ingInput, name.isEmpty() ? errTint : normalTint);
                }
                // если мера "по вкусу" — не подсвечиваем qty вообще, и скрываем его уже реализовано в addIngredientRow
                if (qtyInput != null) {
                    boolean qtyShouldBeHighlighted = !measureIsTaste && qty.isEmpty();
                    setTintSafely(qtyInput, qtyShouldBeHighlighted ? errTint : normalTint);
                }
                if (mesInput != null) {
                    setTintSafely(mesInput, mes.isEmpty() ? errTint : normalTint);
                }
            }
        }
    }

    public void goodInputs() {
        if (container == null) return;

        ColorStateList normalTint = AppCompatResources.getColorStateList(requireContext(), R.color.black);

        int count = container.getChildCount();
        for (int i = 0; i < count; i++) {
            View row = container.getChildAt(i);
            if (row == null) continue;
            AutoCompleteTextView ingInput = row.findViewById(R.id.actv_ingredient);
            EditText qtyInput = row.findViewById(R.id.et_quantity);
            AutoCompleteTextView mesInput = row.findViewById(R.id.actv_measure);

            if (ingInput != null) setTintSafely(ingInput, normalTint);
            if (qtyInput != null) setTintSafely(qtyInput, normalTint);
            if (mesInput != null) setTintSafely(mesInput, normalTint);
        }
    }

    private void setTintSafely(View v, ColorStateList tint) {
        if (v == null) return;
        try {
            // для EditText / AutoCompleteTextView используем setBackgroundTintList
            if (tint != null) {
                v.setBackgroundTintList(tint);
            } else {
                // Сброс tint: подставляем цвет из темы или null, чтобы вернуть дефолтный вид
                v.setBackgroundTintList(null);
                // Если нужно гарантированно восстановить ресурс из темы:
                // ColorStateList defaultTint = AppCompatResources.getColorStateList(requireContext(), R.color.some_default);
                // v.setBackgroundTintList(defaultTint);
            }
        } catch (Exception ignored) {
            // В некоторых кастомных View метод может отсутствовать; в таком случае можно пробовать v.setBackground(...)
        }
    }

}

/*
public class IngredientsFragmentForAddScreen extends Fragment {

    private EditText editTextIngredients;
    private ColorStateList currentTint;

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ingredients_input, container, false);

        editTextIngredients = view.findViewById(R.id.ingredients);
        if (currentTint != null) {
            editTextIngredients.setBackgroundTintList(currentTint);
        }
        return view;
    }

    public boolean validateInputs() {
        String ingredients = editTextIngredients.getText().toString().trim();
        return ingredients.isEmpty();
    }

    public String getIngredientsData() {
        return editTextIngredients.getText().toString().trim();
    }

    public void errorInputs() {
        currentTint = ColorStateList.valueOf(Color.RED); // Сохраняем цвет ошибки
        editTextIngredients.setBackgroundTintList(currentTint);
    }

    public void goodInputs() {
        currentTint = AppCompatResources.getColorStateList(getContext(), R.color.background_add_screen); // Ваш стандартный цвет
        editTextIngredients.setBackgroundTintList(currentTint);
    }


    }
*/

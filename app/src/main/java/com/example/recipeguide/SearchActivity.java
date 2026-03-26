package com.example.recipeguide;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import Data.DatabaseHandler;
import Model.Recipe;


public class SearchActivity extends AppCompatActivity {

    SearchView searchView;
    ListView listView;
    BaseAdActivity baseAdActivity;
    private ImageButton btnFilter;
    private FrameLayout filterContainer;
    private DishAdapter adapter;
    private DatabaseHandler databaseHelper;
    private String lastQuery = ""; // хранит последний текст в SearchView
    private FilterOptions currentFilters = new FilterOptions();
    private boolean isFilterVisible = false;
    private boolean isAddingItem = false;
    private boolean isSettingSelection = false;
    private boolean isFiltering = false;

    // Списки для выбора
    private String[] allCategories;
    private String[] allDiets;
    private String[] englishDiets;
    private String[] allCuisines;
    private String[] englishCuisines;
    // Элементы фильтра
    private Spinner spinnerDiet, spinnerCategory;
    private AutoCompleteTextView actvCuisine;
    private EditText etMaxTime;
    private Button btnClear, btnApply;
    private LinearLayout selectedCategoriesContainer;
    private LinearLayout selectedCuisinesContainer;
    private LinearLayout selectedDietsContainer;

    private ArrayAdapter<String> cuisineAutoCompleteAdapter, dietAdapter, categoryAdapter;
    private Map<String, String> cuisineMap;
    private Map<String, String> dietMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        /*baseAdActivity = new BaseAdActivity(
                this,
                R.id.main,
                R.id.ad_container_view,
                "demo-banner-yandex"
        );
        baseAdActivity.load();*/

        // Загружаем массивы из ресурсов
        allCategories = getResources().getStringArray(R.array.categories_array);
        allDiets = getResources().getStringArray(R.array.diets_array);
        englishDiets = getResources().getStringArray(R.array.diets_en);
        allCuisines = getResources().getStringArray(R.array.cuisines_array);
        englishCuisines = getResources().getStringArray(R.array.cuisines_en);

        initViews();
        setupSearchView();
        setupFilterButton();
        setupListView();

        /*listView.setAdapter(adapter); // Устанавливаем адаптер

        listView.setOnItemClickListener((parent, view, position, id) -> {
            // Получаем выбранное блюдо
            Recipe selectedDish = adapter.getItem(position);

            if (selectedDish != null) {
                if (lastQuery != null && lastQuery.trim().length() >= 3) {
                    // пример: insertEvent(String userId, String recipeId, String query)
                    // замените параметры на те, что нужны вашей реализации
                    databaseHelper.insertEvent(java.util.UUID.randomUUID().toString(),"search", selectedDish.getId(), System.currentTimeMillis());
                }else{
                    databaseHelper.insertEvent(java.util.UUID.randomUUID().toString(),"view",selectedDish.getId(),System.currentTimeMillis());

                }
                // Создаём Intent и передаём ID блюда
                Intent intent = new Intent(getApplicationContext(), recipe_example_activity.class);
                intent.putExtra("dish_id", selectedDish.getId()); // Передаём ID блюда
                startActivity(intent);
            }
        });*/
        /*searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                lastQuery = query != null ? query : "";
                adapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                lastQuery = newText != null ? newText : "";
                adapter.getFilter().filter(newText);
                return false;
            }
        });*/

    }

    private void initViews() {
        listView = findViewById(R.id.listView);
        searchView = findViewById(R.id.search_field);
        btnFilter = findViewById(R.id.btn_filter);
        filterContainer = findViewById(R.id.filter_container);

        databaseHelper = new DatabaseHandler(this);
        ArrayList<Recipe> dishes = databaseHelper.getAllRecipe();
        adapter = new DishAdapter(this, dishes);
        listView.setAdapter(adapter);
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                lastQuery = query != null ? query : "";
                if (isFiltering) {
                    applySearchAndFilters(() -> {});
                } else {
                    adapter.getFilter().filter(lastQuery);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                lastQuery = newText != null ? newText : "";
                if (isFiltering) {
                    applySearchAndFilters(() -> {});
                } else {
                    adapter.getFilter().filter(lastQuery);
                }
                return false;
            }
        });
    }

    private void setupFilterButton() {
        btnFilter.setOnClickListener(v -> {
            if (isFilterVisible) {
                hideFilterPanel();
            } else {
                showFilterPanel();
                isFiltering = true;
            }
        });
    }

    private void showFilterPanel() {
        if (spinnerCategory == null) {
            initFilterViews();
        }
        updateContainersVisibility();
        filterContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_down));
        filterContainer.setVisibility(View.VISIBLE);
        isFilterVisible = true;
    }

    private void hideFilterPanel() {
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slideUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Анимация началась
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Скрываем контейнер только после завершения анимации
                filterContainer.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Не используется
            }
        });

        filterContainer.startAnimation(slideUp);
        isFilterVisible = false;
    }

    private void initFilterViews() {
        View filterView = findViewById(R.id.filter_view);

        spinnerCategory = filterView.findViewById(R.id.spinner_category);
        spinnerDiet = filterView.findViewById(R.id.spinner_diet);
        actvCuisine = filterView.findViewById(R.id.actv_cuisine);
        etMaxTime = filterView.findViewById(R.id.et_max_time);
        btnApply = filterView.findViewById(R.id.btn_apply);
        btnClear = filterView.findViewById(R.id.btn_clear);

        selectedCategoriesContainer = filterView.findViewById(R.id.selected_categories_container);
        selectedCuisinesContainer = filterView.findViewById(R.id.selected_cuisines_container);
        selectedDietsContainer = filterView.findViewById(R.id.selected_diets_container);

        // Настройка спиннера для категорий
        categoryAdapter = new ArrayAdapter<String>(
                this,
                R.layout.spinner_item,
                R.id.spinner_item_tv,
                allCategories
        ) {
            @Override
            public boolean isEnabled(int position) {
                // запрещаем выбор заглушки
                return position != 0;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if (position == 0) {
                    // можно затемнить хинт, но оставляем основной цвет
                    tv.setTextColor(getResources().getColor(R.color.black));
                } else {
                    tv.setTextColor(getResources().getColor(R.color.black));
                }
                return view;
            }
        };

        categoryAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
        spinnerCategory.setSelection(0, false);  // показываем «Выбрать» без вызова слушателя
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0 || isAddingItem || isSettingSelection) {
                    return;
                }

                String selected = allCategories[position];
                if (!currentFilters.getSelectedCategories().contains(selected)) {
                    isAddingItem = true;
                    addSelectedItem(selectedCategoriesContainer, selected, "category");
                    currentFilters.addCategory(selected);
                    // Сбрасываем спиннер на первый элемент после добавления
                    resetSpinnerToFirst(spinnerCategory);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Создаем Map для быстрого поиска
        dietMap = new HashMap<>();
        for (int i = 0; i < allDiets.length; i++) {
            dietMap.put(allDiets[i], englishDiets[i]);
        }
        // Настройка спиннера для диет
        dietAdapter = new ArrayAdapter<String>(
                this,
                R.layout.spinner_item,
                R.id.spinner_item_tv,
                allDiets
        ) {
            @Override
            public boolean isEnabled(int position) {
                // запрещаем выбор заглушки
                return position != 0;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if (position == 0) {
                    // можно затемнить хинт, но оставляем основной цвет
                    tv.setTextColor(getResources().getColor(R.color.black));
                } else {
                    tv.setTextColor(getResources().getColor(R.color.black));
                }
                return view;
            }
        };
        dietAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerDiet.setAdapter(dietAdapter);
        spinnerDiet.setSelection(0, false);  // показываем «Выбрать» без вызова слушателя
        spinnerDiet.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0 || isAddingItem || isSettingSelection) {
                    return;
                }

                String selected = allDiets[position];
                String selectedEnglish = dietMap.get(selected);

                if (selectedEnglish != null && !currentFilters.getSelectedDiets().contains(selectedEnglish)) {
                    isAddingItem = true;
                    addSelectedItem(selectedDietsContainer, selected, "diet");
                    currentFilters.addDiet(selectedEnglish);
                    resetSpinnerToFirst(spinnerDiet);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Создаем Map для быстрого поиска
        cuisineMap = new HashMap<>();
        for (int i = 0; i < allCuisines.length; i++) {
            cuisineMap.put(allCuisines[i], englishCuisines[i]);
        }

        // Настройка AutoCompleteTextView для кухонь
        cuisineAutoCompleteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, allCuisines);
        actvCuisine.setAdapter(cuisineAutoCompleteAdapter);
        actvCuisine.setThreshold(1);
        actvCuisine.setOnItemClickListener((parent, view, position, id) -> {
            if (!isAddingItem) {
                String selected = parent.getItemAtPosition(position).toString();
                String selectedEnglish = cuisineMap.get(selected);

                if (selectedEnglish != null && !currentFilters.getSelectedCuisines().contains(selectedEnglish)) {
                    isAddingItem = true;
                    addSelectedItem(selectedCuisinesContainer, selected, "cuisine");
                    currentFilters.addCuisine(selectedEnglish);
                    actvCuisine.setText("");
                    isAddingItem = false;
                }
            }
        });

        // Кнопка "Применить"
        btnApply.setOnClickListener(v -> {
            btnApply.setEnabled(false);

            // Показываем индикатор загрузки (опционально)
            Toast.makeText(this, R.string.toast_applying_filters, Toast.LENGTH_SHORT).show();

            applySearchAndFilters(() -> {
                // Этот код выполнится после завершения фильтрации
                hideFilterPanel();
                Toast.makeText(this, R.string.toast_applied_filters, Toast.LENGTH_SHORT).show();
                btnApply.setEnabled(true);
            });
        });

        // Кнопка "Сбросить"
        btnClear.setOnClickListener(v -> {
            btnClear.setEnabled(false);

            clearAllFilters();
        });
    }

    private void resetSpinnerToFirst(Spinner spinner) {
        isSettingSelection = true;
        // Используем post() для отложенного выполнения, чтобы избежать конфликтов
        spinner.post(() -> {
            spinner.setSelection(0);
            isSettingSelection = false;
            isAddingItem = false;
        });
    }

    private void addSelectedItem(LinearLayout container, String text, String type) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(R.layout.item_selected_filter, container, false);

        TextView tvText = itemView.findViewById(R.id.tv_selected_text);
        ImageView ivRemove = itemView.findViewById(R.id.iv_remove);

        tvText.setText(text);

        ivRemove.setOnClickListener(v -> {
            container.removeView(itemView);
            updateContainersVisibility();

            // Удаляем из фильтров
            switch (type) {
                case "category":
                    currentFilters.removeCategory(text);
                    break;
                case "cuisine":
                    currentFilters.removeCuisine(cuisineMap.get(text));
                    break;
                case "diet":
                    currentFilters.removeDiet(dietMap.get(text));
                    break;
            }
            adapter.setFilters(currentFilters);
        });

        container.addView(itemView);
        container.setVisibility(View.VISIBLE);
        isAddingItem = false;
    }

    private void updateContainersVisibility() {
        selectedCategoriesContainer.setVisibility(selectedCategoriesContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE);
        selectedCuisinesContainer.setVisibility(selectedCuisinesContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE);
        selectedDietsContainer.setVisibility(selectedDietsContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void clearAllFilters() {
        currentFilters.clear();

        selectedCategoriesContainer.removeAllViews();
        selectedCuisinesContainer.removeAllViews();
        selectedDietsContainer.removeAllViews();

        updateContainersVisibility();
        etMaxTime.setText("");

        applySearchAndFilters(() ->{
            hideFilterPanel();
            Toast.makeText(this, R.string.toast_reset_filters, Toast.LENGTH_SHORT).show();
            // Включаем кнопку обратно
            btnClear.setEnabled(true);
        });
    }

    private void applySearchAndFilters(Runnable onComplete) {
        String maxTimeStr = etMaxTime.getText().toString().trim();
        if (!maxTimeStr.isEmpty()) {
            try {
                currentFilters.setMaxCookingTime(Integer.parseInt(maxTimeStr));
            } catch (NumberFormatException e) {
                currentFilters.setMaxCookingTime(null);
            }
        } else {
            currentFilters.setMaxCookingTime(null);
        }

        adapter.setFilters(currentFilters);
        adapter.getFilter().filter(lastQuery, new Filter.FilterListener() {
            @Override
            public void onFilterComplete(int count) {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    private void setupListView() {
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Recipe selectedDish = adapter.getItem(position);
            if (selectedDish != null) {
                if (lastQuery != null && lastQuery.trim().length() >= 3) {
                    databaseHelper.insertEvent(java.util.UUID.randomUUID().toString(), "search", selectedDish.getId(), System.currentTimeMillis());
                } else {
                    databaseHelper.insertEvent(java.util.UUID.randomUUID().toString(), "view", selectedDish.getId(), System.currentTimeMillis());
                }
                Intent intent = new Intent(getApplicationContext(), recipe_example_activity.class);
                intent.putExtra("dish_id", selectedDish.getId());
                startActivity(intent);
            }
        });
    }

    public void goAddScreen(View view) {
        Intent intent = new Intent(this, AddScreen.class);
        startActivity(intent);
    }

    public void goHome(View view) {
        Intent intent = new Intent(this, MainScreen.class);
        startActivity(intent);
    }

    public void goFavourites(View view) {
        Intent intent = new Intent(this, FavouritesScreen.class);
        startActivity(intent);
    }

    public void goOptions(View view) {
        Intent intent = new Intent(this, OptionsScreen.class);
        startActivity(intent);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("MODE", Context.MODE_PRIVATE);
        boolean russian = prefs.getBoolean("language", true);
        String langCode = russian ? "ru" : "en";
        Context context = LocaleHelper.setLocale(newBase, langCode);
        super.attachBaseContext(context);
    }
}
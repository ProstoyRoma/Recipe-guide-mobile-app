package com.example.recipeguide;


import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.util.Log;
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
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Data.DatabaseHandler;
import Model.Recipe;


public class SearchActivity extends AppCompatActivity {

    SearchView searchView;
    ListView listView;
    BaseAdActivity baseAdActivity;
    private ImageButton btnFilter, btnAI;
    private FrameLayout filterContainer;
    private ProgressBar progressBar;
    private DishAdapter adapter;
    private ArrayList<Recipe> allRecipes; // Храним все рецепты
    private boolean isAISearchActive = false; // Флаг активного ИИ-поиска
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
    private EditText etMaxTime, etIngredients;
    private Button btnClear, btnApply;
    private ImageButton btnMic;
    private LinearLayout selectedCategoriesContainer;
    private LinearLayout selectedCuisinesContainer;
    private LinearLayout selectedDietsContainer;

    private ArrayAdapter<String> cuisineAutoCompleteAdapter, dietAdapter, categoryAdapter;
    private Map<String, String> cuisineMap;
    private Map<String, String> dietMap;
    private static final int REQ_CODE_SPEECH = 1234;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_VOICE_INPUT = 100;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    private String cachedFileContent = null;

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

        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        initViews();
        setupSearchView();
        setupFilterButton();
        setupListView();

        setupAIButton();

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

        if (!isDialogShown()) {
            // Небольшая задержка для плавного появления
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                showAIIntroBottomSheet();
            }, 500);
        }
    }

    private void initViews() {
        listView = findViewById(R.id.listView);
        searchView = findViewById(R.id.search_field);
        btnFilter = findViewById(R.id.btn_filter);
        btnAI = findViewById(R.id.btn_ai);
        filterContainer = findViewById(R.id.filter_container);
        progressBar = findViewById(R.id.uploadProgressBar);

        databaseHelper = new DatabaseHandler(this);
        allRecipes = databaseHelper.getAllRecipe();
        adapter = new DishAdapter(this, allRecipes);
        listView.setAdapter(adapter);
    }

    private void setupSearchView() {
        searchView.setQueryHint(getString(R.string.search));

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(this, getClass())));

        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                lastQuery = query != null ? query : "";
                if (isFiltering) {
                    applySearchAndFilters(() -> {
                    });
                } else {
                    if (isAISearchActive && (lastQuery.isEmpty() || lastQuery.length() < 2)) {
                        resetToFullList();
                    }
                    adapter.getFilter().filter(lastQuery);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                lastQuery = newText != null ? newText : "";

                if (lastQuery.isEmpty()) {
                    resetToFullList();
                } else if (isAISearchActive) {
                    // Если активен ИИ-поиск и пользователь вводит текст, сбрасываем
                    resetToFullList();
                }

                if (isFiltering) {
                    applySearchAndFilters(() -> {
                    });
                } else {
                    adapter.getFilter().filter(lastQuery);
                }
                return false;
            }
        });
        searchView.setOnCloseListener(() -> {
            resetToFullList();
            return false;
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (query != null && !query.isEmpty()) {
                lastQuery = query;
                searchView.setQuery(query, false);
                if (isFiltering) {
                    applySearchAndFilters(() -> {
                    });
                } else {
                    adapter.getFilter().filter(query);
                }
            }
        }
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

    private void setupAIButton() {
        btnAI.setOnClickListener(v -> {
            String query = searchView.getQuery().toString().trim();

            if (query.isEmpty()) {
                Toast.makeText(this, R.string.toast_enter_reguest, Toast.LENGTH_SHORT).show();
                return;
            }
            progressBar.setVisibility(View.VISIBLE);

            // Показываем индикатор загрузки (опционально)
            Toast.makeText(this, R.string.toast_applying_ai, Toast.LENGTH_SHORT).show();

            if (!lastQuery.isEmpty()) {
                sendToAI(lastQuery);
            }
        });
    }
    private void resetToFullList() {
        if (isAISearchActive) {
            adapter.updateData(allRecipes);
            isAISearchActive = false;
        }
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
        etIngredients = filterView.findViewById(R.id.et_ingredients_filter);
        btnApply = filterView.findViewById(R.id.btn_apply);
        btnClear = filterView.findViewById(R.id.btn_clear);
        btnMic = filterView.findViewById(R.id.btn_mic);

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

        btnMic.setOnClickListener(v -> {
            if (checkAudioPermission()) {
                startVoiceInput();
            }
        });

        // Кнопка "Применить"
        btnApply.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            btnApply.setEnabled(false);

            // Показываем индикатор загрузки (опционально)
            Toast.makeText(this, R.string.toast_applying_filters, Toast.LENGTH_SHORT).show();

            applySearchAndFilters(() -> {
                // Этот код выполнится после завершения фильтрации
                hideFilterPanel();
                Toast.makeText(this, R.string.toast_applied_filters, Toast.LENGTH_SHORT).show();
                btnApply.setEnabled(true);
                progressBar.setVisibility(View.GONE);

            });
        });

        // Кнопка "Сбросить"
        btnClear.setOnClickListener(v -> {
            btnClear.setEnabled(false);

            clearAllFilters();
        });
    }



    private String getCachedFileContent() {
        if (cachedFileContent == null) {
            long start = System.currentTimeMillis();
            cachedFileContent = loadFileFromAssets(this, "output.toon");
            Log.d("AI_TIMING", "📁 Файл загружен в кэш за " + (System.currentTimeMillis() - start) + " мс");
        }
        return cachedFileContent;
    }
    private void sendToAI(String userMessage) {
        final long startTime = System.currentTimeMillis();
        Log.d("AI_TIMING", "🚀 Начало запроса: " + new java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(new java.util.Date(startTime)));

        String fileContent = getCachedFileContent(); // Кэшируем файл!
        String fullPrompt = getString(R.string.promt1) + fileContent +
                getString(R.string.promt2) + userMessage;

        GenerativeModelFutures model = MyApplication.getAiModel();
        Content prompt = new Content.Builder().addText(fullPrompt).build();

        // Используем потоковый режим
        Publisher<GenerateContentResponse> streamingResponse = model.generateContentStream(prompt);

        StringBuilder fullResponse = new StringBuilder();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        Log.d("AI_TIMING", "🟢 Ответ получен в: " + new java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(new java.util.Date(endTime)));
        Log.d("AI_TIMING", "⏱️ Общее время выполнения: " + duration + " мс (" + String.format("%.2f", duration / 1000.0) + " сек)");

        streamingResponse.subscribe(new Subscriber<GenerateContentResponse>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(GenerateContentResponse chunk) {
                String chunkText = chunk.getText();
                if (chunkText != null) {
                    fullResponse.append(chunkText);

                    // Логируем получение каждого чанка
                    long currentTime = System.currentTimeMillis();
                    Log.d("AI_TIMING", "📦 Получен чанк через " + (currentTime - startTime) + " мс, размер: " + chunkText.length());
                }
            }

            @Override
            public void onError(Throwable t) {
                Log.e("AI_TIMING", "❌ Ошибка: " + t.getMessage());
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SearchActivity.this, R.string.toast_fail_found_ai + t.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onComplete() {
                long endTime = System.currentTimeMillis();
                Log.d("AI_TIMING", "✅ Полный ответ получен за " + (endTime - startTime) + " мс");

                List<String> recipeIds = parseRecipeIds(fullResponse.toString());

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (recipeIds != null && !recipeIds.isEmpty()) {
                        displaySearchResults(recipeIds);
                    } else {
                        Toast.makeText(SearchActivity.this, R.string.toast_not_recipes, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

/*    private void sendToAI(String userMessage) {
        final long startTime = System.currentTimeMillis();
        Log.d("AI_TIMING", "🔵 sendToAI начал работу в: " + new java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(new java.util.Date(startTime)));
        Log.d("AI_TIMING", "📝 Запрос пользователя: " + userMessage);
        // Загружаем содержимое файла (один раз, можно закешировать)
        String fileContent = loadFileFromAssets(this, "output.toon");
        Log.d("AI_TIMING", "📁 Файл загружен, размер: " + fileContent.length() + " символов");

        // Формируем запрос
        String fullPrompt = getString(R.string.promt1) + fileContent +
                getString(R.string.promt2) + userMessage;
        Log.d("AI_TIMING", "📤 Промпт отправлен в Gemini API");
        Log.d("AI_TIMING", "📏 Длина промпта: " + fullPrompt.length() + " символов");

        GenerativeModelFutures model = MyApplication.getAiModel();
        Content prompt = new Content.Builder().addText(fullPrompt).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(prompt);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        Log.d("AI_TIMING", "🟢 Ответ получен в: " + new java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(new java.util.Date(endTime)));
        Log.d("AI_TIMING", "⏱️ Общее время выполнения: " + duration + " мс (" + String.format("%.2f", duration / 1000.0) + " сек)");

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String aiResponse = result.getText();
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                Log.d("AI_TIMING", "🟢 Ответ получен в: " + new java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(new java.util.Date(endTime)));
                Log.d("AI_TIMING", "⏱️ Общее время выполнения: " + duration + " мс (" + String.format("%.2f", duration / 1000.0) + " сек)");
                Log.d("AI_TIMING", "📥 Длина ответа: " + (aiResponse != null ? aiResponse.length() : 0) + " символов");
                Log.d("AI_TIMING", "📄 Ответ ИИ: " + (aiResponse != null ? aiResponse.substring(0, Math.min(aiResponse.length(), 200)) + "..." : "null"));

                // Парсим ответ и получаем список ID
                List<String> recipeIds = parseRecipeIds(aiResponse);
                Log.d("AI_TIMING", "🔍 Найдено ID рецептов: " + (recipeIds != null ? recipeIds.size() : 0));

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    //btnApplyAI.setEnabled(true);
                    //hideAIPanel();

                    if (recipeIds != null && !recipeIds.isEmpty()) {
                        // Показываем найденные рецепты
                        displaySearchResults(recipeIds);
                        Toast.makeText(SearchActivity.this,
                                getString(R.string.toast_search_ai1) + recipeIds.size() + getString(R.string.toast_search_ai2), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SearchActivity.this,
                                R.string.toast_not_found_ai_recipes, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                Log.e("AI_TIMING", "🔴 Ошибка в: " + new java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(new java.util.Date(endTime)));
                Log.e("AI_TIMING", "⏱️ Время до ошибки: " + duration + " мс (" + String.format("%.2f", duration / 1000.0) + " сек)");
                Log.e("AI_TIMING", "❌ Ошибка: " + t.getMessage(), t);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    //btnApplyAI.setEnabled(true);
                    Toast.makeText(SearchActivity.this,
                            getString(R.string.toast_fail_found_ai) + t.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }, executor);
    }*/

    private void showAIIntroBottomSheet() {
        View bottomSheetView = getLayoutInflater().inflate(R.layout.dialog_ai_intro, null);

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.setCancelable(false);

        TextView tvTitle = bottomSheetView.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = bottomSheetView.findViewById(R.id.tv_dialog_message);
        TextView tvWarning = bottomSheetView.findViewById(R.id.tv_dialog_warning);
        Button btnOk = bottomSheetView.findViewById(R.id.btn_ok);

        tvTitle.setText(R.string.AI_dialog_title);
        String message = getString(R.string.AI_dialog_message);

        String warning = getString(R.string.AI_dialog_warning);

        tvMessage.setText(message);
        tvWarning.setText(warning);
        btnOk.setOnClickListener(v -> {
            saveDialogShownPreference();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }
    private void saveDialogShownPreference() {
        SharedPreferences prefs = getSharedPreferences("AI_DIALOG", MODE_PRIVATE);
        prefs.edit().putBoolean("ai_intro_shown", true).apply();
    }

    private boolean isDialogShown() {
        SharedPreferences prefs = getSharedPreferences("AI_DIALOG", MODE_PRIVATE);
        return prefs.getBoolean("ai_intro_shown", false);
    }
    private List<String> parseRecipeIds(String aiResponse) {
        List<String> ids = new ArrayList<>();

        if (aiResponse == null || aiResponse.isEmpty()) {
            return ids;
        }

        try {
            // Убираем лишние символы и разбиваем по запятым
            String cleaned = aiResponse.trim();

            // Если ответ в формате JSON
            if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
                try {
                    org.json.JSONObject json = new org.json.JSONObject(cleaned);
                    if (json.has("recipe_ids")) {
                        org.json.JSONArray arr = json.getJSONArray("recipe_ids");
                        for (int i = 0; i < arr.length(); i++) {
                            String id = arr.getString(i);
                            if (id != null && !id.isEmpty()) {
                                ids.add(id);
                            }
                        }
                        return ids;
                    }
                } catch (Exception e) {
                    // Не JSON, пробуем другие форматы
                }
            }

            // Если ответ в формате [id1, id2, id3]
            if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                cleaned = cleaned.substring(1, cleaned.length() - 1);
            }

            // Разбиваем по запятым
            String[] parts = cleaned.split(",");
            for (String part : parts) {
                String id = part.trim()
                        .replaceAll("['\"\\[\\]]", "")  // Убираем кавычки и скобки
                        .trim();
                if (!id.isEmpty()) {
                    // Проверяем, что ID существует в базе
                    if (databaseHelper.myRecipeInSQLite(id)) {
                        ids.add(id);
                    }
                }
            }

        } catch (Exception e) {
            Log.e("AI_PARSE", "Error parsing AI response: " + e.getMessage());
        }

        return ids;
    }

    // Отображение найденных рецептов
    private void displaySearchResults(List<String> recipeIds) {
        ArrayList<Recipe> recipes = new ArrayList<>();

        for (String id : recipeIds) {
            Recipe recipe = databaseHelper.getRecipe(id);
            if (recipe != null) {
                recipes.add(recipe);
            }
        }

        if (recipes.isEmpty()) {
            Toast.makeText(this, R.string.toast_not_recipes, Toast.LENGTH_SHORT).show();
            return;
        }

        adapter.updateData(recipes);
        isAISearchActive = true;

        Toast.makeText(this,
                getString(R.string.toast_search_ai1) + recipes.size() + getString(R.string.toast_search_ai2),
                Toast.LENGTH_SHORT).show();

        // Прокручиваем к началу списка
        listView.smoothScrollToPosition(0);
        // Создаём новый адаптер с результатами
        DishAdapter resultAdapter = new DishAdapter(this, recipes);
        listView.setAdapter(resultAdapter);

        // Сохраняем результаты для последующего использования
        adapter = resultAdapter;
    }

    private void clearSearchResults() {
        // Возвращаем полный список рецептов
        ArrayList<Recipe> allRecipes = databaseHelper.getAllRecipe();
        adapter = new DishAdapter(this, allRecipes);
        listView.setAdapter(adapter);
    }

    public String loadFileFromAssets(Context context, String filename) {
        StringBuilder content = new StringBuilder();
        try {
            InputStream is = context.getAssets().open(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return content.toString();
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

        applySearchAndFilters(() -> {
            hideFilterPanel();
            Toast.makeText(this, R.string.toast_reset_filters, Toast.LENGTH_SHORT).show();
            // Включаем кнопку обратно
            btnClear.setEnabled(true);
        });
    }

    private void applySearchAndFilters(Runnable onComplete) {
        String maxTimeStr = etMaxTime.getText().toString().trim();
        String ingredientsStr = etIngredients.getText().toString().trim();
        if (!maxTimeStr.isEmpty()) {
            try {
                currentFilters.setMaxCookingTime(Integer.parseInt(maxTimeStr));
            } catch (NumberFormatException e) {
                currentFilters.setMaxCookingTime(null);
            }
        } else {
            currentFilters.setMaxCookingTime(null);
        }

        if (!ingredientsStr.isEmpty()) {
            try {
                currentFilters.setIngredients(ingredientsStr);
            } catch (NumberFormatException e) {
                currentFilters.setIngredients(null);
            }
        } else {
            currentFilters.setIngredients(null);
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

    private boolean checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
            return false;
        }
        return true;
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        SharedPreferences sharedPreferences = getSharedPreferences("MODE", MODE_PRIVATE);
        // Устанавливаем язык в зависимости от настроек приложения
        boolean isRussian = sharedPreferences.getBoolean("language", false);
        String language = isRussian ? "ru-RU" : "en-US";
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);

        if (isFilterVisible) {
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_promt));
        }else{
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_promt_ai));

        }
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        try {
            startActivityForResult(intent, REQUEST_VOICE_INPUT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.toast_voice_not_supported, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_VOICE_INPUT && resultCode == RESULT_OK) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                if (isFilterVisible) {
                    appendIngredient(spokenText);
                }
            }
        }
    }


    private void appendIngredient(String spokenText) {
        String currentText = etIngredients.getText().toString();

        // 1. Убираем всю пунктуацию и лишние пробелы
        String cleanedText = spokenText
                .replaceAll("[\\p{Punct}]", " ")  // Заменяем все знаки препинания на пробелы
                .replaceAll("\\s+", " ")          // Заменяем множественные пробелы на один
                .trim();                          // Убираем пробелы в начале и конце

        // 2. Разбиваем на слова
        String[] words = cleanedText.split(" ");

        // 3. Собираем слова через запятую
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (result.length() > 0) {
                    result.append(", ");
                }
                // Делаем первую букву заглавной
                String capitalized = word.substring(0, 1).toUpperCase() +
                        word.substring(1).toLowerCase();
                result.append(capitalized);
            }
        }

        String newIngredients = result.toString();

        if (newIngredients.isEmpty()) {
            Toast.makeText(this, R.string.toast_unable_recognize_ingredients, Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. Добавляем в EditText
        if (currentText.isEmpty()) {
            etIngredients.setText(newIngredients);
        } else {
            etIngredients.setText(currentText + ", " + newIngredients);
        }

        etIngredients.setSelection(etIngredients.getText().length());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceInput();
            } else {
                Toast.makeText(this, R.string.toast_without_permission, Toast.LENGTH_LONG).show();
            }
        }
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
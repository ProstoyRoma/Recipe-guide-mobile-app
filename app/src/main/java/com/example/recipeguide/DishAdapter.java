package com.example.recipeguide;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.example.recipeguide.Dish;
import com.example.recipeguide.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import Data.DatabaseHandler;
import Model.Recipe;
import Utils.FileUtils;
import Utils.VectorUtils;

public class DishAdapter extends ArrayAdapter<Recipe> {
    private List<Recipe> originalDishes; // Исходный список блюд
    private List<Recipe> filteredDishes; // Отфильтрованный список
    private Set<String> cachedCuisineRecipeIds = null; // Кэш для SQLite
    private Set<String> lastSelectedCuisines = null; // Кэшируем последние выбранные кухни
    private Filter dishFilter; // Фильтр для поиска
    private Context context;
    FirebaseDatabase database;
    DatabaseReference myRef;
    private DatabaseHandler databaseHelper;
    SharedPreferences sharedPreferences;
    private FilterOptions currentFilters;
    private float[] inputIngredientVector = null; // вектор введённых ингредиентов
    private List<String> inputIngredientParsed = null;
    private boolean hasInputVector = false;


    public DishAdapter(Context context, List<Recipe> dishes) {
        super(context, 0, dishes);
        this.context = context;
        this.originalDishes = new ArrayList<>(dishes); // Сохраняем оригинальный список
        this.filteredDishes = new ArrayList<>(dishes); // Создаём копию для фильтрации
        this.currentFilters = new FilterOptions();
        sharedPreferences = context.getSharedPreferences("MODE", Context.MODE_PRIVATE);
        databaseHelper = new DatabaseHandler(context);
    }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Recipe dish = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.layout_search_list, parent, false);
        }

        // Находим элементы интерфейса
        ImageView dishImageView = convertView.findViewById(R.id.recipe_image);
        TextView dishNameTextView = convertView.findViewById(R.id.recipe_name);
        TextView dishCookingTimeTextView = convertView.findViewById(R.id.recipe_cooking_time);

        // Устанавливаем данные
        if (dish != null) {
            if (sharedPreferences.getBoolean("language", false)) {
                dishNameTextView.setText(dish.getName());
            } else {
                dishNameTextView.setText(dish.getName_en());
            }


            String imagePath = dish.getImage();
            if (imagePath != null) {
                File imgFile = new File("/data/data/com.example.recipeguide/files/" + imagePath + ".jpg");
                if (imgFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    dishImageView.setImageBitmap(bitmap); // Устанавливаем изображение в ImageView
                } else {
                    Glide.with(context)
                            .load(imagePath)
                            .diskCacheStrategy(DiskCacheStrategy.ALL) // Загружаем из кеша, если интернета нет
                            .into(dishImageView);
                }
            } else {
                // Устанавливаем изображение-заглушку, если данных нет
                dishImageView.setImageResource(R.drawable.stub);
            }
            //dishCookingTimeTextView.setText("Время приготовления: \n" + dish.getRecipeCookingTime() + " мин");
            String cookingTimeText = context.getString(R.string.cooking_time_dishList, dish.getCookingTime());
            dishCookingTimeTextView.setText(cookingTimeText);
        }

        return convertView;
    }

    @Override
    public int getCount() {
        return filteredDishes.size();
    }

    @Nullable
    @Override
    public Recipe getItem(int position) {
        return filteredDishes.get(position);
    }

    public void setFilters(FilterOptions filters) {
        this.currentFilters = filters;
    }

    @Override
    public Filter getFilter() {
        if (dishFilter == null) {
            dishFilter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();

                    // В начале performFiltering(...)
                    if (currentFilters.getIngredients() != null) {
                        final String originalIng = currentFilters.getIngredients(); // Создаём final переменную
                        String ing = originalIng;

                        if (sharedPreferences.getBoolean("language", false)) {
                            // Используем CountDownLatch для ожидания
                            final CountDownLatch latch = new CountDownLatch(1);
                            final String[] translatedIngredient = {null};

                            TranslatorOptions options = new TranslatorOptions.Builder()
                                    .setSourceLanguage(TranslateLanguage.RUSSIAN)
                                    .setTargetLanguage(TranslateLanguage.ENGLISH)
                                    .build();

                            Translator translator = Translation.getClient(options);

                            translator.downloadModelIfNeeded()
                                    .addOnSuccessListener(unused -> {
                                        translator.translate(originalIng) // Используем originalIng
                                                .addOnSuccessListener(translatedText -> {
                                                    translatedIngredient[0] = translatedText;
                                                    latch.countDown();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e("MLKit", "Translation failed: " + e.getMessage());
                                                    translatedIngredient[0] = originalIng;
                                                    latch.countDown();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("MLKit", "Model download failed: " + e.getMessage());
                                        translatedIngredient[0] = originalIng;
                                        latch.countDown();
                                    });

                            try {
                                latch.await(3, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                Log.e("MLKit", "Translation interrupted", e);
                            }
                            ing = translatedIngredient[0] != null ? translatedIngredient[0] : originalIng;
                        }
                        /*try {
                            String modelAsset = "models/tfidf_word2vec_w2v.model";
                            String idfAsset = "models/tfidf_word2vec_tfidf_idf.joblib";
                            String metaAsset = "models/tfidf_word2vec_tfidf_meta.json";
                            String npzAsset = "models/word_vectors.npz";
                            String modelPath = FileUtils.copyAssetToFiles(context, modelAsset, "tfidf_word2vec_w2v.model");
                            String idfPath = FileUtils.copyAssetToFiles(context, idfAsset, "tfidf_word2vec_tfidf_idf.joblib");
                            String metaPath = FileUtils.copyAssetToFiles(context, metaAsset, "tfidf_word2vec_tfidf_meta.json");
                            String word2vecPath = FileUtils.copyAssetToFiles(context, npzAsset, "word_vectors.npz");

                            String startMsg = String.format("Assets copied. model=%s, idf=%s, meta=%s, npz=%s", modelPath, idfPath, metaPath, word2vecPath);
                            Log.i("LOG_TAG_PYTHON", startMsg);

                            // tfidf_prefix — путь без суффикса "_idf.joblib"
                            String tfidfPrefix = new File(context.getFilesDir(), "tfidf_word2vec_tfidf").getAbsolutePath();
                            // Вызов Python (Chaquopy) — аналогично вашему коду в AddScreen
                            Python py = Python.getInstance();
                            PyObject module = py.getModule("tfidf_android");
                            // word2vecPath и tfidfPrefix должны быть доступны в адаптере или переданы извне
                            PyObject pyResult = module.callAttr("parse_and_vectorize_to_search", word2vecPath, tfidfPrefix, ing);
                            String jsonStr = pyResult.toString();
                            JSONObject jo = new JSONObject(jsonStr);
                            JSONArray vecArr = jo.getJSONArray("vector");

                            if (vecArr != null && vecArr.length() > 0) {
                                inputIngredientVector = new float[vecArr.length()];
                                for (int i = 0; i < vecArr.length(); i++) {
                                    // JSONArray предоставляет getDouble; приводим к float безопасно
                                    try {
                                        double dv = vecArr.getDouble(i);
                                        inputIngredientVector[i] = (float) dv;
                                    } catch (JSONException je) {
                                        // Если элемент не число — ставим 0.0f
                                        inputIngredientVector[i] = 0.0f;
                                    }
                                }
                                hasInputVector = inputIngredientVector.length > 0;
                            } else {
                                // Пустой или отсутствующий вектор
                                inputIngredientVector = null;
                                hasInputVector = false;
                                Log.i("LOG_TAG_PYTHON", "No vector returned from parse_and_vectorize");
                            }
                        } catch (Exception e) {
                            Log.e("TFIDF", "Vectorize failed: " + e.getMessage());
                            hasInputVector = false;
                            inputIngredientVector = null;
                        }*/
                        try {

                            Python py = Python.getInstance();
                            PyObject module = py.getModule("tfidf_android");

                            // Вызываем функцию ingredient_parser_to_search
                            PyObject parsedResult = module.callAttr("ingredient_parser_to_search", ing);

                            // Преобразуем результат в Java-список
                            if (parsedResult != null) {
                                // Используем asList() для преобразования Python списка в Java список
                                List<PyObject> pyList = parsedResult.asList();

                                if (pyList != null && !pyList.isEmpty()) {
                                    inputIngredientParsed = new ArrayList<>();

                                    hasInputVector = true;
                                    for (PyObject item : pyList) {
                                        String ingredient = item.toString();
                                        inputIngredientParsed.add(ingredient);
                                        Log.d("TFIDF", "Ingredient: " + ingredient);
                                    }
                                } else {
                                    hasInputVector = false;
                                    inputIngredientParsed = null;
                                }
                            } else {
                                hasInputVector = false;
                                inputIngredientParsed = null;
                            }

                        } catch (Exception e) {
                            Log.e("TFIDF", "Vectorize failed: " + e.getMessage(), e);
                            hasInputVector = false;
                        }
                    }

                    String filterString = constraint.toString().toLowerCase().replaceAll("[\\p{Punct}]", "").trim();

                    List<Recipe> sourceList = new ArrayList<>(originalDishes);
                    List<Recipe> filteredList = new ArrayList<>();

                    for (Recipe dish : sourceList) {
                        if (!currentFilters.getSelectedCuisines().isEmpty() || !currentFilters.getSelectedCategories().isEmpty() ||
                                !currentFilters.getSelectedDiets().isEmpty() || currentFilters.getMaxCookingTime() != null || currentFilters.getIngredients() != null) {
                            if (applyAllFilters(dish, constraint)) {
                                filteredList.add(dish);
                            }
                        } else {
                            if (sharedPreferences.getBoolean("language", false)) {
                                if (dish.getName().toLowerCase().replaceAll("[\\p{Punct}]", "").contains(filterString)) {
                                    filteredList.add(dish);
                                }

                            } else {
                                if (dish.getName_en().toLowerCase().replaceAll("[\\p{Punct}]", "").contains(filterString)) {
                                    filteredList.add(dish);
                                }
                            }
                        }
                    }

                    if (hasInputVector) {
                        Collections.sort(filteredList, new Comparator<Recipe>() {
                            @Override
                            public int compare(Recipe r1, Recipe r2) {
                                return Double.compare(r2.getLastComparison(), r1.getLastComparison()); // по убыванию
                            }
                        });
                    }
                    results.values = filteredList;
                    results.count = filteredList.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredDishes.clear();
                    if (results.values != null) {
                        filteredDishes.addAll((List<Recipe>) results.values);
                    }
                    notifyDataSetChanged();
                }
            };
        }
        return dishFilter;
    }

    private boolean applyAllFilters(Recipe dish, CharSequence constraint) {
        boolean languageRussian = sharedPreferences.getBoolean("language", false);

        // 1. Поиск по тексту
        boolean matchesSearch = true;
        if (constraint != null && constraint.length() > 0) {
            String searchText = constraint.toString().toLowerCase().replaceAll("[\\p{Punct}]", "").trim();
            if (languageRussian) {
                matchesSearch = dish.getName().toLowerCase().replaceAll("[\\p{Punct}]", "").contains(searchText);
            } else {
                matchesSearch = dish.getName_en().toLowerCase().replaceAll("[\\p{Punct}]", "").contains(searchText);
            }
        }
        if (!matchesSearch) return false;

        // 2. Фильтр по категориям (множественный выбор)
        boolean matchesCategory = true;
        if (currentFilters.getSelectedCategories() != null && !currentFilters.getSelectedCategories().isEmpty()) {
            int dishCategoryId = dish.getCategory();
            String dishCategoryName = getCategoryNameById(dishCategoryId);
            matchesCategory = currentFilters.getSelectedCategories().contains(dishCategoryName);
        }
        if (!matchesCategory) return false;

        // 3. Фильтр по кухням (множественный выбор)
        boolean matchesCuisine = true;
        if (currentFilters.getSelectedCuisines() != null && !currentFilters.getSelectedCuisines().isEmpty()) {
            if (isNetworkAvailable()) {
                // Есть интернет - проверяем через Firebase
                matchesCuisine = checkCuisineInFirebase(dish.getId(), currentFilters.getSelectedCuisines());
            } else {
                // Нет интернета - проверяем через SQLite
                matchesCuisine = checkCuisineInSQLite(dish.getId(), currentFilters.getSelectedCuisines());
            }
        }
        if (!matchesCuisine) return false;

        // 4. Фильтр по диетам (множественный выбор)
        boolean matchesDiet = true;
        if (currentFilters.getSelectedDiets() != null && !currentFilters.getSelectedDiets().isEmpty()) {
            if (isNetworkAvailable()) {
                // Есть интернет - проверяем через Firebase
                matchesDiet = checkDietInFirebase(dish.getId(), currentFilters.getSelectedDiets());
            } else {
                // Нет интернета - проверяем через SQLite
                matchesDiet = checkDietInSQLite(dish.getId(), currentFilters.getSelectedDiets());
            }
        }
        if (!matchesDiet) return false;

        // 5. Фильтр по времени приготовления
        boolean matchesTime = true;
        if (currentFilters.getMaxCookingTime() != null) {
            matchesTime = dish.getCookingTime() <= currentFilters.getMaxCookingTime();
        }
        if (!matchesTime) return false;

        // 6. Фильтр по ингредиентам
        if (currentFilters.getIngredients() != null) {
            // Получаем byte[] из модели (dish.getIngredientVector() возвращает byte[])
            /*float[] recipeBytes = dish.getVectors();
            float[] recipeVec = recipeBytes;

// inputIngredientVector — float[] (вычислен и сохранён ранее в адаптере)
            if (hasInputVector && recipeVec != null && recipeVec.length > 0) {
                float sim = VectorUtils.cosineSimilarity(inputIngredientVector, recipeVec);
                dish.setLastSimilarity(sim); // lastSimilarity — float
                if (sim < 0.05) return false;
            } else {
                dish.setLastSimilarity(0.0f);
            }*/

            String recipeParsed = dish.getIngredient_parsed();
            if (hasInputVector && recipeParsed != null && inputIngredientParsed != null && !inputIngredientParsed.isEmpty()) {
                List<String> recipeIngredientList = parseRecipeIngredients(recipeParsed);

                if (recipeIngredientList == null || recipeIngredientList.isEmpty()) {
                    return false;
                }

                // Считаем количество совпадающих ингредиентов
                int matchCount = 0;
                for (String inputIngredient : inputIngredientParsed) {
                    for (String recipeIngredient : recipeIngredientList) {
                        // Проверяем, содержится ли ингредиент из запроса в ингредиентах рецепта
                        if (recipeIngredient.toLowerCase().contains(inputIngredient.toLowerCase()) ||
                                inputIngredient.toLowerCase().contains(recipeIngredient.toLowerCase())) {
                            matchCount++;
                            break; // Учитываем только одно совпадение на ингредиент
                        }
                    }
                }

                // Вычисляем процент совпадения
                float matchPercentage = (float) matchCount / recipeIngredientList.size() * 100;

                dish.setLastComparison(matchPercentage);
                // Возвращаем true если процент > 55%
                return matchCount > 0;

            } else {
                return false;
            }

        }
        return true;
    }

    private List<String> parseRecipeIngredients(String recipeParsed) {
        List<String> ingredients = new ArrayList<>();

        if (recipeParsed == null || recipeParsed.isEmpty()) {
            return ingredients;
        }

        try {
            // Убираем квадратные скобки
            String cleaned = recipeParsed.trim();
            if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                cleaned = cleaned.substring(1, cleaned.length() - 1);
            }

            // Разделяем по запятым
            String[] parts = cleaned.split(",");

            for (String part : parts) {
                String ingredient = part.trim();
                // Убираем кавычки
                ingredient = ingredient.replaceAll("^['\"]|['\"]$", "");
                if (!ingredient.isEmpty()) {
                    ingredients.add(ingredient);
                }
            }

            Log.d("TFIDF", "Recipe ingredients: " + ingredients.size());

        } catch (Exception e) {
            Log.e("TFIDF", "Error parsing recipe ingredients: " + e.getMessage());
        }

        return ingredients;
    }

    private String getCategoryNameById(int categoryId) {
        switch (categoryId) {
            case 0:
                return context.getString(R.string.category_appetizers);
            case 1:
                return context.getString(R.string.category_soup);
            case 2:
                return context.getString(R.string.category_salad);
            case 3:
                return context.getString(R.string.category_main_course);
            case 4:
                return context.getString(R.string.category_side_dish);
            case 5:
                return context.getString(R.string.category_dessert);
            case 6:
                return context.getString(R.string.category_drink);
            case 7:
                return context.getString(R.string.category_sauces_and_seasonings);
            case 8:
                return context.getString(R.string.category_baked_goods);
            case 9:
                return context.getString(R.string.category_snacks);
            default:
                return "";  // или обработать ошибку/неназначенную позицию
        }
    }

    // Проверка через Firebase (синхронно - для упрощения)
    private boolean checkCuisineInFirebase(String recipeId, Set<String> selectedCuisines) {
        // Используем синхронный запрос через CountDownLatch для упрощения
        final boolean[] result = {false};

        final CountDownLatch latch = new CountDownLatch(selectedCuisines.size());
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("indices").child("by_cuisine");

        for (String cuisine : selectedCuisines) {
            myRef.child(cuisine).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.hasChild(recipeId)) {
                        result[0] = true;
                    }
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e("DISH_CUISINE", "Failed to update version", error.toException());
                    latch.countDown();
                }
            });

            if (result[0]) break;
        }

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result[0];
    }

    private boolean checkCuisineInSQLite(String recipeId, Set<String> selectedCuisines) {
        // Если изменились выбранные кухни или кэш пуст - обновляем
        if (cachedCuisineRecipeIds == null || !selectedCuisines.equals(lastSelectedCuisines)) {
            cachedCuisineRecipeIds = databaseHelper.getAllRecipeIdsForCuisines(selectedCuisines);
            lastSelectedCuisines = new HashSet<>(selectedCuisines);
        }

        // Проверяем, есть ли recipeId в полученном списке
        return cachedCuisineRecipeIds != null && cachedCuisineRecipeIds.contains(recipeId);
    }

    private boolean checkDietInFirebase(String recipeId, Set<String> selectedDiets) {
        // Используем синхронный запрос через CountDownLatch для упрощения
        final boolean[] result = {false};

        final CountDownLatch latch = new CountDownLatch(selectedDiets.size());
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("indices").child("by_diet");

        for (String diet : selectedDiets) {
            myRef.child(diet).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.hasChild(recipeId)) {
                        result[0] = true;
                    }
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e("DISH_CUISINE", "Failed to update version", error.toException());
                    latch.countDown();
                }
            });

            if (result[0]) break;
        }

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return result[0];
    }

    private boolean checkDietInSQLite(String recipeId, Set<String> selectedDiets) {
        // Если изменились выбранные кухни или кэш пуст - обновляем
        if (cachedCuisineRecipeIds == null || !selectedDiets.equals(lastSelectedCuisines)) {
            cachedCuisineRecipeIds = databaseHelper.getAllRecipeIdsForDiets(selectedDiets);
            lastSelectedCuisines = new HashSet<>(selectedDiets);
        }

        // Проверяем, есть ли recipeId в полученном списке
        return cachedCuisineRecipeIds != null && cachedCuisineRecipeIds.contains(recipeId);
    }

    // Проверка интернета
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private float[] parseVectorString(String vecJson) {
        if (vecJson == null) return null;
        String s = vecJson.trim();
        if (s.startsWith("[")) s = s.substring(1);
        if (s.endsWith("]")) s = s.substring(0, s.length() - 1);
        if (s.trim().isEmpty()) return new float[0];
        String[] parts = s.split(",");
        float[] v = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                v[i] = Float.parseFloat(parts[i].trim());
            } catch (NumberFormatException e) {
                v[i] = 0.0f;
            }
        }
        return v;
    }


}

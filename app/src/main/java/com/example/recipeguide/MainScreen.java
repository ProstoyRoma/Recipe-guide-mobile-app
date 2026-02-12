package com.example.recipeguide;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cloudinary.android.MediaManager;
import com.google.firebase.FirebaseApp;
import com.yandex.mobile.ads.banner.BannerAdView;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import Data.DatabaseHandler;
import Model.Recipe;
import Model.Recommendation;

public class MainScreen extends AppCompatActivity {

    ListView listView;
    DishAdapter adapter;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    BaseAdActivity baseAdActivity;
    DatabaseHandler databaseHelper;
    boolean isNightMode, russianLanguage;
    private static boolean isCloudinaryInitialized = false;
    private final String userId = User.username;
    // интервал обновления рекомендаций (24 часа)
    private static final long UPDATE_INTERVAL_MS = TimeUnit.HOURS.toMillis(24);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /*// ресивер на случай, если WorkManager или внешняя логика пришлёт broadcast о новых рекомендациях
    private final BroadcastReceiver recReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DailyRecommendWorker.ACTION_RECOMMENDATIONS_UPDATED.equals(intent.getAction())) {
                String uid = intent.getStringExtra(DailyRecommendWorker.EXTRA_USER_ID);
                if (uid == null) uid = userId;
                // При получении broadcast'а просто перезапросим и обновим UI
                loadAndShowRecommendations(uid, false);
            }
        }
    };*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.main_screen);

        baseAdActivity = new BaseAdActivity(
                this,
                R.id.main,
                R.id.ad_container_view,
                "demo-banner-yandex"
        );
        baseAdActivity.load();

        initConfig();
        isNightMode = OptionsScreen.getCurrentTheme(this);

        if (isNightMode) {
            // Действия для тёмной темы
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            // Действия для светлой темы
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        listView = findViewById(R.id.listView);

        //RecommendationManager recommendationManager = new RecommendationManager(this);
        databaseHelper = new DatabaseHandler(this);
        ArrayList<Recipe> dishes = databaseHelper.getLastRecommendedRecipe(this);
        /*List<String> dish = recommendationManager.generateTop3ForUser(userId);
        JSONArray arrj = new JSONArray();
        for (String rid : dish) arrj.put(rid);

        long now = System.currentTimeMillis();
        databaseHelper.insertRecommendation(userId, arrj.toString(), now);
        String recJson = databaseHelper.getRecommendationJsonForUser(userId);

        ArrayList<Recipe> dishes = new ArrayList<>();

        if (recJson != null && !recJson.isEmpty()) {
            try {
                org.json.JSONArray arr = new org.json.JSONArray(recJson);
                for (int i = 0; i < arr.length() && dishes.size() < 3; i++) {
                    String recId = arr.getString(i);
                    Recipe row = databaseHelper.getRecipe(recId);
                    if (row != null) {
                        dishes.add(row);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        DishAdapter adapter = new DishAdapter(this, dishes);
        runOnUiThread(() -> listView.setAdapter(adapter));*/
        /*if(dishes == null || dishes.isEmpty()){
            dishes = databaseHelper.getRecommendedRecipe(this);
        }*/
        adapter = new DishAdapter(this, dishes);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            // Получаем выбранное блюдо
            Recipe selectedDish = adapter.getItem(position);

            if (selectedDish != null) {
                // Создаём Intent и передаём ID блюда
                Intent intent = new Intent(getApplicationContext(), recipe_example_activity.class);
                intent.putExtra("dish_id", selectedDish.getId()); // Передаём ID блюда
                startActivity(intent);
            }
        });

    }
    /*@Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(recReceiver); } catch (Exception ignored) {}
    }

    // --- основная логика проверки и загрузки ---

    private void checkUpdateAndLoad() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // получаем запись recommendations из БД
            RecommendationRow recRow = databaseHelper.getRecommendationEntry(userId); // должен вернуть null или объект
            long now = System.currentTimeMillis();

            boolean needRecompute = false;
            if (recRow == null) {
                // нет предыдущих рекомендаций -> нужно пересчитать
                needRecompute = true;
            } else {
                long age = now - recRow.generatedAt;
                if (age >= UPDATE_INTERVAL_MS) {
                    needRecompute = true;
                }
            }

            if (needRecompute) {
                // Пересчитываем рекомендации сейчас в фоне (можно заменить на WorkManager)
                recomputeRecommendationsAndShow();
            } else {
                // Время ещё не пришло: показываем существующие рекомендации (если есть)
                // или fallback, если их недостаточно
                ArrayList<Recipe> dishes = loadSavedRecommendationsOrFallback();
                postUpdateAdapter(dishes);
            }
        });
    }

    // Выполнить recompute рекомендаций (RecommendationManager) и показать результат
    private void recomputeRecommendationsAndShow() {
        // recompute в фоне
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                RecommendationManager manager = new RecommendationManager(getApplicationContext());
                // генерирует список id top3 и сохраняет куда-нибудь? у нас метод возвращает List<String>
                List<String> top3Ids = manager.generateTop3ForUser(userId);

                // Сохраняем в БД (JSON + timestamp)
                JSONArray arr = new JSONArray();
                if (top3Ids != null) {
                    for (String id : top3Ids) arr.put(id);
                }
                long now = System.currentTimeMillis();
                databaseHelper.insertRecommendation(userId, arr.toString(), now);

                // Теперь пытаемся получить объекты Recipe по id; если их <3 -> fallback
                ArrayList<Recipe> dishes = buildRecipeListFromJson(arr.toString());
                if (dishes.size() < 3) {
                    ArrayList<Recipe> fallback = databaseHelper.getRecommendedRecipe(this);
                    if (fallback != null && !fallback.isEmpty()) {
                        // убедимся, что итоговый список ровно 3 (или максимум доступных)
                        ArrayList<Recipe> result = new ArrayList<>();
                        int need = 3;
                        for (Recipe r : dishes) {
                            if (result.size() >= need) break;
                            result.add(r);
                        }
                        for (Recipe r : fallback) {
                            if (result.size() >= need) break;
                            // не добавляем дубликаты по id
                            boolean exists = false;
                            for (Recipe ex : result) { if (ex.getId() != null && ex.getId().equals(r.getId())) { exists = true; break; } }
                            if (!exists) result.add(r);
                        }
                        dishes = result;
                    }
                }

                // если всё ещё меньше 3 — достаём любые первые из БД
                if (dishes.size() < 3) {
                    List<DatabaseHandler.RecipeRow> all = databaseHelper.getAllRecipesWithVectors();
                    for (DatabaseHandler.RecipeRow rr : all) {
                        if (dishes.size() >= 3) break;
                        boolean exists = false;
                        for (Recipe ex : dishes) { if (ex.getId() != null && ex.getId().equals(rr.id)) { exists = true; break; } }
                        if (!exists) dishes.add(mapRecipeRowToRecipe(rr));
                    }
                }

                postUpdateAdapter(dishes);

            } catch (Exception e) {
                e.printStackTrace();
                // если ошибка при пересчёте — показать fallback
                ArrayList<Recipe> fallback = databaseHelper.getRecommendedRecipe(this);
                postUpdateAdapter(fallback != null ? fallback : new ArrayList<>());
            }
        });
    }

    // Попытка загрузить ранее сохранённые рекомендации; если их нет или меньше 3 — fallback getRecommendedRecipe
    private ArrayList<Recipe> loadSavedRecommendationsOrFallback() {
        ArrayList<Recipe> dishes = new ArrayList<>();
        try {
            Recommendation recRow = databaseHelper.getRecommendationEntry(userId);
            if (recRow != null && recRow.recipeIdsJson != null) {
                dishes = buildRecipeListFromJson(recRow.recipeIdsJson);
            }
        } catch (Exception ignored) {}

        if (dishes == null) dishes = new ArrayList<>();
        if (dishes.size() < 3) {
            ArrayList<Recipe> fallback = databaseHelper.getRecommendedRecipe(this);
            if (fallback != null && !fallback.isEmpty()) {
                // взять первые элементы из fallback, чтобы заполнить до 3
                ArrayList<Recipe> result = new ArrayList<>();
                for (Recipe r : dishes) if (result.size() < 3) result.add(r);
                for (Recipe r : fallback) {
                    if (result.size() >= 3) break;
                    boolean exists = false;
                    for (Recipe ex : result) { if (ex.getId() != null && ex.getId().equals(r.getId())) { exists = true; break; } }
                    if (!exists) result.add(r);
                }
                dishes = result;
            }
        }
        return dishes;
    }

    // Постинг обновления адаптера на UI-поток
    private void postUpdateAdapter(ArrayList<Recipe> dishes) {
        mainHandler.post(() -> {
            if (adapter == null) {
                adapter = new DishAdapter(MainScreen.this, dishes);
                listView.setAdapter(adapter);
            } else {
                adapter.updateData(dishes);
                adapter.notifyDataSetChanged();
            }
        });
    }

    // Создать ArrayList<Recipe> из JSON строки с id'шниками
    private ArrayList<Recipe> buildRecipeListFromJson(String json) {
        ArrayList<Recipe> out = new ArrayList<>();
        if (json == null || json.isEmpty()) return out;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length() && out.size() < 3; i++) {
                String rid = arr.getString(i);
                Recipe rr = databaseHelper.getRecipe(rid);
                if (rr != null) out.add(mapRecipeRowToRecipe(rr));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }*/

    /*private BroadcastReceiver recReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DailyRecommendWorker.ACTION_RECOMMENDATIONS_UPDATED.equals(intent.getAction())) {
                // повторяем ту же логику: перечитать recJson и обновить адаптер
                updateRecommendations();
            }
        }
    };*/

    /*private void updateRecommendations() {
        DatabaseHandler databaseHelper = new DatabaseHandler(this);
        // Попробуем получить сохранённые рекомендации (JSON array of ids)
        String userId = User.username; // или реальный user id
        String recJson = databaseHelper.getRecommendationJsonForUser(userId);

        ArrayList<Recipe> dishes = new ArrayList<>();

        if (recJson != null && !recJson.isEmpty()) {
            try {
                org.json.JSONArray arr = new org.json.JSONArray(recJson);
                for (int i = 0; i < arr.length() && dishes.size() < 3; i++) {
                    String recId = arr.getString(i);
                    Recipe row = databaseHelper.getRecipe(recId);
                    if (row != null) {
                        dishes.add(row);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

// Если рекомендаций нет или их меньше 3 — заполнить fallback первыми рецептами из БД
        if (dishes.size() < 3) {
            List<Recipe> all = databaseHelper.getRecommendedRecipe(this);
            int idx = 0;
            for (Recipe rr : all) {
                if (dishes.size() >= 3) break;
                // избежать дубликатов
                boolean exists = false;
                for (Recipe ex : dishes) {
                    if (ex.getId() != null && ex.getId().equals(rr.getId())) { exists = true; break; }
                }
                if (!exists) {
                    Recipe r = new Recipe();
                    r.setId(rr.getId());
                    r.setName(rr.getName() != null ? rr.getName() : "");
                    r.setName_en(rr.getName_en() != null ? rr.getName_en() : "");
                    r.setImage(rr.getImage());
                    r.setCookingTime(rr.getCookingTime());
                    dishes.add(r);
                }
                idx++;
            }
        }

// Убедимся, что в списке ровно 3 элемента (можно меньше, если БД маленькая)
        while (dishes.size() > 3) dishes.remove(dishes.size() - 1);
        SharedPreferences preferences = this.getSharedPreferences("RandomItems", this.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        long currentTime = System.currentTimeMillis();
        StringBuilder savedDishesBuilder = new StringBuilder();
        for (Recipe dish : dishes) {
            savedDishesBuilder.append(dish.getId()).append("|")
                    .append(dish.getName()).append("|")
                    .append(dish.getName_en()).append("|")
                    .append(dish.getImage()).append("|")
                    .append(dish.getCookingTime()).append(";");
        }
        editor.putLong("lastUpdateTime", currentTime);
        editor.putString("savedDishes", savedDishesBuilder.toString());
        editor.apply();

    }*/
    private void initConfig() {
        if (!isCloudinaryInitialized) { // Проверяем, была ли уже инициализация
            Map config = new HashMap();
            config.put("cloud_name", "dx7hf8znl");
            config.put("api_key", "182439927864613");
            config.put("api_secret", "U-dXjr3iiTkAM_SnAOu3C613_vE");

            MediaManager.init(this, config);
            isCloudinaryInitialized = true; // Устанавливаем флаг после инициализации
            Log.d("Cloudinary", "Cloudinary успешно инициализирован!");
        } else {
            Log.d("Cloudinary", "Cloudinary уже был инициализирован, повторное выполнение не требуется.");
        }
    }


    private String getSystemLanguage() {
        return Locale.getDefault().getLanguage(); // Возвращает "ru" для русского, "en" для английского
    }

    public void goHome(View view) {
        Intent intent = new Intent(this, MainScreen.class);
        startActivity(intent);
    }

    public void goAddScreen(View view) {
        Intent intent = new Intent(this, AddScreen.class);
        startActivity(intent);
    }

    public void goFavourites(View view) {
        Intent intent = new Intent(this, FavouritesScreen.class);
        startActivity(intent);
    }

    public void goToSearch(View view) {
        Intent intent = new Intent(this, SearchActivity.class);
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

    /*@Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(DailyRecommendWorker.ACTION_RECOMMENDATIONS_UPDATED);
        ContextCompat.registerReceiver(this, recReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);    }

    @Override
    protected void onResume() {
        super.onResume();
        updateRecommendations(); // перечитать из БД при возврате на экран
    }
    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(recReceiver);
        } catch (IllegalArgumentException ignored) {}
    }*/

}
package com.example.recipeguide;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.MobileAds;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.common.MlKit;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import Data.DatabaseHandler;
import Model.Recipe;
import Model.Tags;

public class SplashScreen extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    boolean russianLanguage;
    private DatabaseHandler databaseHandler;
    private DatabaseReference recipeRef;
    private FirebaseDatabase database;

    private static Translator enToRuTranslator, ruToEnTranslator;
    private static final String PREFS = "app_prefs";
    private static final String KEY_REORDER = "reorder_completed";
    private static final String KEY_ONBOARDING = "questionnaire_completed";
    private static final long SYNC_INTERVAL = 24 * 60 * 60 * 1000; // 24 часа
    private static final String TAG = "RecipeSync";
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.splash_activity);

        MobileAds.initialize(this, initializationStatus -> {
            // опционально: лог или флаг готовности SDK
            Log.d("AdMob", "Initialized");
        });

        FirebaseApp.initializeApp(this);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        //syncRecipesAndTagsIfNeeded();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.motionLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;

        });
        sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        final AtomicBoolean started = new AtomicBoolean(false);
        final long MIN_DISPLAY_MS = 2500L;
        final long splashStartTime = System.currentTimeMillis();
        databaseHandler = new DatabaseHandler(this);
        database = FirebaseDatabase.getInstance();

        //recipeRef = database.getReference();

        // Инициализируем компоненты для фоновой работы
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        /*Handler mainHandler = new Handler(Looper.getMainLooper());

        databaseHandler.getRecommendedRecipe(this, new RecommendedCallback() {
            @Override
            public void onSuccess() {
                // всегда запускаем на UI-потоке
                mainHandler.post(() -> {
                    if (started.get()) return;
                    long elapsed = System.currentTimeMillis() - splashStartTime;
                    long remaining = MIN_DISPLAY_MS - elapsed;
                    if (remaining <= 0) {
                        if (started.compareAndSet(false, true)) {
                            navigateToNextScreen();
                        }
                    } else {
                        mainHandler.postDelayed(() -> {
                            if (started.compareAndSet(false, true)) {
                                navigateToNextScreen();
                            }
                        }, remaining);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                // По желанию — аналогичная логика при ошибке (запустить приложение всё равно).
                mainHandler.post(() -> {
                    if (started.get()) return;
                    long elapsed = System.currentTimeMillis() - splashStartTime;
                    long remaining = MIN_DISPLAY_MS - elapsed;
                    if (remaining <= 0) {
                        if (started.compareAndSet(false, true)) {
                            navigateToNextScreen();
                        }
                    } else {
                        mainHandler.postDelayed(() -> {
                            if (started.compareAndSet(false, true)) {
                                navigateToNextScreen();
                            }
                        }, remaining);
                    }
                });
            }
        });*/


        // Запускаем синхронизацию в фоне
        startBackgroundSync();

        //uploadRecipesFirebase(this);

        loadUserData();
        applyLanguage();

        // Перевод с русского на английский
        ruToEnTranslator();

        // Перевод с английского на русский
        enToRuTranslator();

        // Запускаем логику сплеш-скрина
        startSplashLogic();


    }

    private void startBackgroundSync() {
        executorService.execute(() -> {
            try {
                syncRecipesAndTagsIfNeeded();
                Log.d(TAG, "Background sync completed");
            } catch (Exception e) {
                Log.e(TAG, "Background sync failed", e);
            }
        });
    }

    private void startSplashLogic() {
        final AtomicBoolean started = new AtomicBoolean(false);
        final long MIN_DISPLAY_MS = 2500L;
        final long splashStartTime = System.currentTimeMillis();

        databaseHandler.getRecommendedRecipe(this, new RecommendedCallback() {
            @Override
            public void onSuccess() {
                mainHandler.post(() -> {
                    if (started.get()) return;
                    long elapsed = System.currentTimeMillis() - splashStartTime;
                    long remaining = MIN_DISPLAY_MS - elapsed;

                    if (remaining <= 0) {
                        if (started.compareAndSet(false, true)) {
                            navigateToNextScreen();
                        }
                    } else {
                        mainHandler.postDelayed(() -> {
                            if (started.compareAndSet(false, true)) {
                                navigateToNextScreen();
                            }
                        }, remaining);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                mainHandler.post(() -> {
                    if (started.get()) return;
                    long elapsed = System.currentTimeMillis() - splashStartTime;
                    long remaining = MIN_DISPLAY_MS - elapsed;

                    if (remaining <= 0) {
                        if (started.compareAndSet(false, true)) {
                            navigateToNextScreen();
                        }
                    } else {
                        mainHandler.postDelayed(() -> {
                            if (started.compareAndSet(false, true)) {
                                navigateToNextScreen();
                            }
                        }, remaining);
                    }
                });
            }
        });
    }

    // Вынесенная функция запуска Activity
    private void navigateToNextScreen() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean reorderDone = prefs.getBoolean(KEY_REORDER, false);
        boolean onboardingDone = prefs.getBoolean(KEY_ONBOARDING, false);

        if (!reorderDone) {
            startActivity(new Intent(SplashScreen.this, ReorderActivity.class));
        } else if (!onboardingDone) {
            startActivity(new Intent(SplashScreen.this, QuestionnaireActivity.class));
        } else {
            startActivity(new Intent(SplashScreen.this, MainScreen.class));
        }
        finish();
    }

    public void syncRecipesAndTagsIfNeeded() {
        long lastSync = sharedPreferences.getLong("last_sync_time", 0);
        if (System.currentTimeMillis() - lastSync < SYNC_INTERVAL) {
            Log.d(TAG, "Sync not needed yet");
            return;
        }

        // Запускаем синхронизацию в фоне
        syncRecipes();
        syncTags();


        sharedPreferences.edit()
                .putLong("last_sync_time", System.currentTimeMillis())
                .apply();
    }

    private void syncRecipes() {
        // Получаем версию последней синхронизации
        int localVersion = sharedPreferences.getInt("sync_version_recipes", 0);
        //recipeRef = database.getReference("/recipes/metadata/recipes_version");
        // Запрашиваем обновления с Firebase
        recipeRef = FirebaseDatabase.getInstance().getReference("recipes_metadata");


        recipeRef
                //.child("recipes_version")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        try {

                            for (DataSnapshot child : snapshot.getChildren()) {
                                if (Objects.equals(child.getKey(), "recipes_version")) {
                                    int remoteVersion = child.getValue(Integer.class);
                                    if (remoteVersion > localVersion) {
                                        fetchUpdatesRecipes(localVersion, remoteVersion);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error in syncTags", e);
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Ошибка загрузки данных: " + error.getMessage());
                    }
                });
    }

    private void fetchUpdatesRecipes(int fromVersion, int toVersion) {
        recipeRef = FirebaseDatabase.getInstance().getReference("recipes");

        recipeRef
                .orderByChild("version")
                .startAt(fromVersion + 1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        try {
                            for (DataSnapshot recipeSnapshot : snapshot.getChildren()) {
                                Recipe recipe = new Recipe();
                                recipe.setId(recipeSnapshot.getKey());
                                recipe.setName(recipeSnapshot.child("name").getValue(String.class));
                                recipe.setName_en(recipeSnapshot.child("name_en").getValue(String.class));
                                recipe.setImage(recipeSnapshot.child("image").getValue(String.class));
                                recipe.setCookingTime(recipeSnapshot.child("cookingTime").getValue(Integer.class));
                                recipe.setCategory(recipeSnapshot.child("category").getValue(Integer.class));
                                recipe.setIngredient(recipeSnapshot.child("ingredient").getValue(String.class));
                                recipe.setIngredient_en(recipeSnapshot.child("ingredient_en").getValue(String.class));
                                recipe.setRecipe(recipeSnapshot.child("recipe").getValue(String.class));
                                recipe.setRecipe_en(recipeSnapshot.child("recipe_en").getValue(String.class));
                                recipe.setIngredient_parsed(recipeSnapshot.child("ingredients_parsed").getValue(String.class));
                                String ingVec = recipeSnapshot.child("ingredient_vectors").getValue(String.class);
                                recipe.setVectors(ingVec.getBytes(StandardCharsets.UTF_8));
                                databaseHandler.addRecipe(recipe);
                            }

                            // Обновляем версию синхронизации
                            sharedPreferences.edit()
                                    .putInt("sync_version_recipes", toVersion)
                                    .apply();
                        } catch (Exception e) {
                            Log.e("Firebase", "Ошибка сохранения рецептов: " + e.getMessage());
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Ошибка загрузки данных: " + error.getMessage());

                    }
                });


    }

    private void syncTags() {
        // Получаем версию последней синхронизации
        int localVersion = sharedPreferences.getInt("sync_version_tags", 0);

        recipeRef = FirebaseDatabase.getInstance().getReference("indices");

        // Запрашиваем обновления с Firebase
        recipeRef
                //.child("indices").child("metadata").child("tags_version")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        try {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                if (Objects.equals(child.getKey(), "metadata")) {
                                    int remoteVersion = child.child("tags_version").getValue(Integer.class);
                                    if (remoteVersion > localVersion) {
                                        fetchUpdatesTags(localVersion, remoteVersion);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error in syncTags", e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Ошибка загрузки данных: " + error.getMessage());
                    }
                });
    }

    private void fetchUpdatesTags(int fromVersion, int toVersion) {
        recipeRef = FirebaseDatabase.getInstance().getReference("indices");

        List<DatabaseReference> indexRefs = Arrays.asList(
                recipeRef.child("by_cuisine"),
                recipeRef.child("by_diet")
                // добавьте другие индексы по необходимости
        );

        for (DatabaseReference ref : indexRefs) {
            ref.orderByChild("version")
                    .startAt(fromVersion + 1)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            try {
                                // Сохраняем теги из этого индекса
                                for (DataSnapshot tagSnapshot : snapshot.getChildren()) {
                                    for (DataSnapshot recipeSnapshot : tagSnapshot.getChildren()) {
                                        if (ref == recipeRef.child("by_cuisine")) {
                                            databaseHandler.insertTags(new Tags(
                                                    UUID.randomUUID().toString(),
                                                    recipeSnapshot.getKey(),
                                                    "cuisine",
                                                    tagSnapshot.getKey()
                                            ));
                                        } else if (ref == recipeRef.child("by_diet")) {
                                            databaseHandler.insertTags(new Tags(
                                                    UUID.randomUUID().toString(),
                                                    recipeSnapshot.getKey(),
                                                    "diet",
                                                    tagSnapshot.getKey()
                                            ));
                                        }
                                    }
                                }


                                sharedPreferences.edit()
                                        .putInt("sync_version_tags", toVersion)
                                        .apply();



                            } catch (Exception e) {
                                Log.e("Firebase", "Ошибка сохранения тегов: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("Firebase", "Ошибка загрузки индекса: " + error.getMessage());
                        }
                    });
        }

    }

    private void uploadRecipesFirebase(Context context) {
        recipeRef = FirebaseDatabase.getInstance().getReference("recipes");

        recipeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DatabaseHandler dbHelper = new DatabaseHandler(context);
                Recipe recipe = new Recipe();

                for (DataSnapshot recipeSnapshot : snapshot.getChildren()) {
                    recipe.setId(recipeSnapshot.getKey());
                    recipe.setName(recipeSnapshot.child("name").getValue(String.class));
                    recipe.setName_en(recipeSnapshot.child("name_en").getValue(String.class));
                    recipe.setImage(recipeSnapshot.child("image").getValue(String.class));
                    if (recipeSnapshot.child("cookingTime").getValue(Integer.class) != null) {
                        recipe.setCookingTime(recipeSnapshot.child("cookingTime").getValue(Integer.class));

                    }
                    recipe.setIngredient(recipeSnapshot.child("ingredient").getValue(String.class));
                    recipe.setIngredient_en(recipeSnapshot.child("ingredient_en").getValue(String.class));
                    recipe.setRecipe(recipeSnapshot.child("recipe").getValue(String.class));
                    recipe.setRecipe_en(recipeSnapshot.child("recipe_en").getValue(String.class));
                    recipe.setIsFavorite(0);

                    dbHelper.insertOrUpdateRecipe(recipe);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Ошибка загрузки данных: " + error.getMessage());
            }
        });

    }

    private void loadUserData() {
        User.username = sharedPreferences.getString("username", User.username);
        User.userImage = sharedPreferences.getString("userImage", User.userImage);
        User.allergy = sharedPreferences.getString("userAllergy", User.allergy);
        User.diet = sharedPreferences.getString("userDiet", User.diet);
        User.likeCategory = sharedPreferences.getString("userLikeCategory", User.likeCategory);
        User.likeCuisine = sharedPreferences.getString("userLikeCuisine", User.likeCuisine);
        User.skillLevel = sharedPreferences.getString("userSkillLevel", User.skillLevel);
    }

    public static Translator enToRuTranslator() {
        if (enToRuTranslator == null) {
            TranslatorOptions enToRuOptions = new TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(TranslateLanguage.RUSSIAN)
                    .build();
            enToRuTranslator = Translation.getClient(enToRuOptions);
            enToRuTranslator.downloadModelIfNeeded();
        }
        return enToRuTranslator;
    }

    public static Translator ruToEnTranslator() {
        if (ruToEnTranslator == null) {
            TranslatorOptions ruToEnOptions = new TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.RUSSIAN)
                    .setTargetLanguage(TranslateLanguage.ENGLISH)
                    .build();
            ruToEnTranslator = Translation.getClient(ruToEnOptions);
            ruToEnTranslator.downloadModelIfNeeded();
        }
        return ruToEnTranslator;
    }


    private void applyLanguage() {
        SharedPreferences prefs = getSharedPreferences("MODE", MODE_PRIVATE);

        String languageCode;
        if (!prefs.contains("language")) {
            // Первый запуск
            Locale systemLocale = Locale.getDefault();
            String systemLang = systemLocale.getLanguage();

            boolean isRussian = "ru".equals(systemLang);
            languageCode = isRussian ? "ru" : "en";

            prefs.edit().putBoolean("language", isRussian).apply();
        } else {
            boolean useRussian = prefs.getBoolean("language", true);
            languageCode = useRussian ? "ru" : "en";
        }

        // Применяем локаль для текущей активности
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);

        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
    }
}
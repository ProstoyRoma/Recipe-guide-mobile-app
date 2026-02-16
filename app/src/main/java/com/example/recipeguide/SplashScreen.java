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

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import Data.DatabaseHandler;
import Model.Recipe;

public class SplashScreen extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    boolean russianLanguage;
    DatabaseHandler databaseHandler;
    private static Translator enToRuTranslator, ruToEnTranslator;
    private static final String PREFS = "app_prefs";
    private static final String KEY_REORDER = "reorder_completed";
    private static final String KEY_ONBOARDING = "onboarding_done";

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.motionLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;

        });
        applyLanguage();
        sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        User.username = sharedPreferences.getString("username", User.username);
        User.userImage = sharedPreferences.getString("userImage", User.userImage);
        User.allergy = sharedPreferences.getString("userAllergy", User.allergy);
        User.likeCategory = sharedPreferences.getString("userLikeCategory", User.likeCategory);

        final AtomicBoolean started = new AtomicBoolean(false);
        final long MIN_DISPLAY_MS = 2500L;
        final long splashStartTime = System.currentTimeMillis();
        databaseHandler = new DatabaseHandler(this);

        Handler mainHandler = new Handler(Looper.getMainLooper());

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
                            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                            boolean reorderDone = prefs.getBoolean(KEY_REORDER, false);
                            boolean onboardingDone = prefs.getBoolean(KEY_ONBOARDING, false);
                            if (!reorderDone) {
                                startActivity(new Intent(SplashScreen.this, ReorderActivity.class));
                                finish();
                            } else if (!onboardingDone) {
                                startActivity(new Intent(SplashScreen.this, QuestionnaireActivity.class));
                                finish();
                            } else {
                                startMainActivity(MainScreen.class);
                            }
                        }
                    } else {
                        mainHandler.postDelayed(() -> {
                            if (started.compareAndSet(false, true)) {
                                SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                                boolean reorderDone = prefs.getBoolean(KEY_REORDER, false);
                                boolean onboardingDone = prefs.getBoolean(KEY_ONBOARDING, false);
                                if (!reorderDone) {
                                    startActivity(new Intent(SplashScreen.this, ReorderActivity.class));
                                    finish();
                                } else if (!onboardingDone) {
                                    startActivity(new Intent(SplashScreen.this, QuestionnaireActivity.class));
                                    finish();
                                } else {
                                    startMainActivity(MainScreen.class);
                                }
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
                            startMainActivity(MainScreen.class);
                        }
                    } else {
                        mainHandler.postDelayed(() -> {
                            if (started.compareAndSet(false, true)) {
                                startMainActivity(MainScreen.class);
                            }
                        }, remaining);
                    }
                });
            }
        });


        uploadRecipesFirebase(this);


        /*if (!sharedPreferences.contains("language")) {
            // Определяем системный язык
            String systemLanguage = getSystemLanguage();

            // Если системный язык русский, устанавливаем русский, иначе — английский
            boolean defaultRussian = systemLanguage.equals("ru");
            editor.putBoolean("language", defaultRussian);
            editor.apply();

            russianLanguage = defaultRussian; // Применяем значение в переменную
        } else {
            russianLanguage = sharedPreferences.getBoolean("language", false); // Загружаем сохранённое значение
        }

        if (sharedPreferences.getBoolean("language", false)){
            setAppLocale("ru");
        }else{
            setAppLocale("en");
        }*/


        // Перевод с русского на английский
        ruToEnTranslator();
        // Перевод с английского на русский
        enToRuTranslator();


        /*TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sharedPreferences.getBoolean("language", false) ?
                        TranslateLanguage.RUSSIAN : TranslateLanguage.ENGLISH)
                .setTargetLanguage(sharedPreferences.getBoolean("language", false) ?
                        TranslateLanguage.ENGLISH : TranslateLanguage.RUSSIAN)
                .build();

        Translator translator = Translation.getClient(options);

        translator.downloadModelIfNeeded();*/

        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashScreen.this, MainScreen.class));
                finish();
            }
        }, 2500);*/

    }

    // Вынесенная функция запуска Activity
    private void startMainActivity(Class x) {
        Intent intent = new Intent(SplashScreen.this, x);
        startActivity(intent);
        finish();
    }

    private void uploadRecipesFirebase(Context context) {
        DatabaseReference recipeRef = FirebaseDatabase.getInstance().getReference("recipes");

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

    /*private void setAppLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

    }*/

    private String getSystemLanguage() {
        return Locale.getDefault().getLanguage(); // Возвращает "ru" для русского, "en" для английского
    }
    /*@Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("MODE", Context.MODE_PRIVATE);

        // Проверяем, существует ли ключ "language"
        if (!prefs.contains("language")) {
            // Первый запуск - устанавливаем язык системы
            String systemLang = getSystemLanguage();
            setAppLocale(systemLang);

            // Сохраняем выбор языка
            boolean isRussian = "ru".equals(systemLang);
            prefs.edit().putBoolean("language", isRussian).apply();
        }

        // Теперь всегда используем сохраненный язык
        boolean useRussian = prefs.getBoolean("language", true);
        String langToSet = useRussian ? "ru" : "en";

        // Применяем локаль
        Context context = setAppLocale(langToSet);
        super.attachBaseContext(context);
    }

    // Метод для применения локали
    private Context setAppLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        return getBaseContext().createConfigurationContext(config);
    }*/

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
}
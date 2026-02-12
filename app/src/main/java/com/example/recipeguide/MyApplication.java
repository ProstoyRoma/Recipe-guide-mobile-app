package com.example.recipeguide;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.Log;
import android.widget.Toast;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.chaquo.python.PyObject;
import org.json.JSONObject;
import org.json.JSONArray;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.yandex.mobile.ads.common.MobileAds;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import Utils.FileUtils;

public class MyApplication extends Application {
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    boolean russianLanguage;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        // Configure the user privacy data policy before init sdk
        MobileAds.initialize(this, () -> {
            Log.d("MyLog","Yandex Ads SDK initialized");
        });

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }


        SharedPreferences prefs = getSharedPreferences("MODE", MODE_PRIVATE);
        boolean russian = prefs.getBoolean("language", true); // true = ru
        String langCode = russian ? "ru" : "en";
        // Применяем к Application context
        LocaleHelper.setLocale(this, langCode);
        //scheduleDailyAt3AM(this);
        //scheduleEveryDay(this);
    }
    public void scheduleEveryDay(Context context) {
        long delayMillis = calculateDelayToNext(23, 15);
        long delayMinutes = TimeUnit.MILLISECONDS.toMinutes(delayMillis);
        long delayHours = TimeUnit.MILLISECONDS.toHours(delayMillis);
        // Констрейнты - можно настроить по необходимости
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                DailyRecommendWorker.class,
                //24, TimeUnit.HOURS)
                15, TimeUnit.MINUTES)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                //.setInitialDelay(5, TimeUnit.MINUTES)
                .setConstraints(new Constraints.Builder().build()) // убрал требование сети
                .addTag("daily_recommend_at_3")
                .build();

        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(
                        "daily_recommend_worker", // уникальное имя
                        ExistingPeriodicWorkPolicy.REPLACE,
                        request);


        Log.d("MyLog", "Scheduled daily recommendation work. initialDelayMs=" + delayMillis);
    }
    private long calculateDelayToNext(int targetHour, int targetMinute) {
        Calendar now = Calendar.getInstance(); // локальная таймзона устройства
        Calendar next = (Calendar) now.clone();
        next.set(Calendar.HOUR_OF_DAY, targetHour);
        next.set(Calendar.MINUTE, targetMinute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        if (!next.after(now)) {
            // если сейчас уже past target, сдвигаем на завтра
            next.add(Calendar.DAY_OF_YEAR, 1);
        }
        return next.getTimeInMillis() - now.getTimeInMillis();
    }

    // пример использования
    /*public void scheduleDailyAt3AM(Context context) {
        // вычисляем задержку до ближайших 03:00
        long delayMillis = calculateDelayToNext(3, 0);
        long delayMinutes = TimeUnit.MILLISECONDS.toMinutes(delayMillis);
        long delayHours = TimeUnit.MILLISECONDS.toHours(delayMillis);

        // Констрейнты — например требуем сеть, если нужно
        Constraints constraints = new Constraints.Builder()
                //.setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                DailyRecommendWorker.class,
                24, TimeUnit.HOURS) // повтор каждые ~24 часа
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS) // первый запуск в 3:00
                .setConstraints(constraints)
                .addTag("daily_recommend_at_3") // опционально
                .build();

        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(
                        "daily_recommend_worker", // уникальное имя
                        ExistingPeriodicWorkPolicy.REPLACE, // менять при повторном вызове при необходимости
                        request);
    }*/
    public static void setAppLocale(String languageCode, Context context) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);
        context.createConfigurationContext(config);

    }
    private String getSystemLanguage() {
        return Locale.getDefault().getLanguage(); // Возвращает "ru" для русского, "en" для английского
    }
}

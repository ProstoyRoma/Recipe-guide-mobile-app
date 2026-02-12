package com.example.recipeguide;

import androidx.work.Worker;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;
import android.content.SharedPreferences;


import org.json.JSONArray;

import java.util.List;

import Data.DatabaseHandler;
import Model.Recipe;

public class DailyRecommendWorker extends Worker {

    public static final String ACTION_RECOMMENDATIONS_UPDATED = "com.example.recipeguide.RECOMMENDATIONS_UPDATED";
    public static final String EXTRA_USER_ID = "userId";

    public DailyRecommendWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // single-user assumption; if multi-user, iterate users list
        String userId = User.username;
        Log.d("MyLog", "DailyRecommendWorker doWork start: " + System.currentTimeMillis());
        try {
            RecommendationManager manager = new RecommendationManager(getApplicationContext());
            List<String> top3 = manager.generateTop3ForUser(userId);

            // save to DB as JSON array (DatabaseHandler должен реализовать insertRecommendation)
            DatabaseHandler db = new DatabaseHandler(getApplicationContext());
            JSONArray arr = new JSONArray();
            for (String rid : top3) arr.put(rid);
            long now = System.currentTimeMillis();

            SharedPreferences prefs = getApplicationContext().getSharedPreferences("RandomItems", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("lastUpdateTime", now);
            editor.apply();

            StringBuilder savedDishesBuilder = new StringBuilder();
            for (String rid : top3) {
                Recipe r = db.getRecipe(rid);
                if (r != null) {
                    savedDishesBuilder.append(r.getId() != null ? r.getId() : "").append("|")
                            .append(r.getName() != null ? r.getName() : "").append("|")
                            .append(r.getName_en() != null ? r.getName_en() : "").append("|")
                            .append(r.getImage() != null ? r.getImage() : "").append("|")
                            .append(r.getCookingTime()).append(";");
                } else {
                    // если рецепта нет в БД — сохраняем только id
                    savedDishesBuilder.append(rid).append("||||;");
                }
            }
            editor.putString("savedDishes", savedDishesBuilder.toString());
            editor.apply();

            db.insertRecommendation(userId, arr.toString(), now); // implement in DatabaseHandler

            // notify UI via local broadcast
            /*Intent intent = new Intent(ACTION_RECOMMENDATIONS_UPDATED);
            intent.putExtra(EXTRA_USER_ID, userId);
            getApplicationContext().sendBroadcast(intent);*/
            Log.d("MyLog", "DailyRecommendWorker doWork finished");
            return Result.success();
        } catch (Exception ex) {
            ex.printStackTrace();
            // if transient error, retry; otherwise return failure
            Log.d("MyLog", "DailyRecommendWorker doWork exeption");
            return Result.retry();
        }

    }
}

package Data;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;


import android.util.Log;

import androidx.annotation.Nullable;
import androidx.work.ListenableWorker;

import com.example.recipeguide.Dish;
import com.example.recipeguide.RecommendationManager;
import com.example.recipeguide.RecommendedCallback;
import com.example.recipeguide.User;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import org.json.JSONArray;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import Model.Event;
import Model.Recipe;
import Utils.Util;

public class DatabaseHandler extends SQLiteAssetHelper {

    private static final String TAG = "TAG";
    private Context myContext;
    private SQLiteDatabase myDataBase;

    public DatabaseHandler(Context context) {
        super(context, Util.DATABASE_NAME, null, Util.DATABASE_VERSION);
        this.myContext = context;
        try {
            myDataBase = this.getWritableDatabase();
        } catch (SQLiteException e) {
            try {
                clearDB();
                myDataBase.close();
            } catch (Exception ex) {
                setForcedUpgrade();
            }

            Log.d(TAG, "MyDatabase: " + e);
        }
        copyAllImagesFromAssets(context);

    }

    public Cursor getAllData() {
        myDataBase = getReadableDatabase();
        return myDataBase.query(Util.TABLE_NAME, null, null, null,
                null, null, null);
    }

    public boolean myRecipeInSQLite(String id) {
        myDataBase = this.getWritableDatabase();

        // Проверяем, есть ли рецепт в SQLite
        Cursor cursor = myDataBase.query(Util.TABLE_NAME, null, Util.KEY_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();

        myDataBase.close();

        return exists;
    }

    public void insertOrUpdateRecipe(Recipe recipe) {
        myDataBase = this.getWritableDatabase();

        // Проверяем, есть ли рецепт в SQLite
        Cursor cursor = myDataBase.query(Util.TABLE_NAME, null, Util.KEY_ID + "=?", new String[]{String.valueOf(recipe.getId())}, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();

        if (!exists) {
            addRecipe(recipe);
        }
        myDataBase.close();
    }

    //Добавить рецепт
    public void addRecipe(Recipe recipe) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(Util.KEY_ID, recipe.getId());
        contentValues.put(Util.KEY_NAME_EN, recipe.getName_en());
        contentValues.put(Util.KEY_NAME, recipe.getName());
        contentValues.put(Util.KEY_INGREDIENT_EN, recipe.getIngredient_en());
        contentValues.put(Util.KEY_INGREDIENT, recipe.getIngredient());
        contentValues.put(Util.KEY_RECIPE_EN, recipe.getRecipe_en());
        contentValues.put(Util.KEY_RECIPE, recipe.getRecipe());
        contentValues.put(Util.KEY_IMAGE, recipe.getImage());
        contentValues.put(Util.KEY_COOKINGTIME, recipe.getCookingTime());
        contentValues.put(Util.KEY_CATEGORY, recipe.getCategory());
        contentValues.put(Util.KEY_ISFAVORITE, recipe.getIsFavorite());
        contentValues.put(Util.KEY_ISCOOKED, recipe.getIsCook());

        db.insert(Util.TABLE_NAME, null, contentValues);
        db.close();
    }

    public void insertEvent(String eventId, String userId, String eventType, String recipeId, long tsLocal) {
        ContentValues cv = new ContentValues();
        cv.put(Util.KEY_EVENT_ID, eventId);
        cv.put(Util.KEY_USER_ID, userId);
        cv.put(Util.KEY_EVENT_TYPE, eventType);
        cv.put(Util.KEY_RECIPE_ID, recipeId);
        cv.put(Util.KEY_TS_LOCAL, tsLocal);
        SQLiteDatabase db = this.getWritableDatabase();
        db.insertWithOnConflict(Util.TABLE_NAME_EVENT, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public List<Event> getRecentEvents(String userId, long sinceTs) {
        List<Event> out = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(Util.TABLE_NAME_EVENT, new String[]{Util.KEY_EVENT_ID, Util.KEY_USER_ID, Util.KEY_EVENT_TYPE, Util.KEY_RECIPE_ID, Util.KEY_TS_LOCAL},
                    "userId = ? AND tsLocal >= ?", new String[]{userId, String.valueOf(sinceTs)},
                    null, null, "tsLocal DESC");
            while (c != null && c.moveToNext()) {
                String eid = c.getString(c.getColumnIndexOrThrow(Util.KEY_EVENT_ID));
                String uid = c.getString(c.getColumnIndexOrThrow(Util.KEY_USER_ID));
                String et = c.getString(c.getColumnIndexOrThrow(Util.KEY_EVENT_TYPE));
                String rid = c.getString(c.getColumnIndexOrThrow(Util.KEY_RECIPE_ID));
                long ts = c.getLong(c.getColumnIndexOrThrow(Util.KEY_TS_LOCAL));
                out.add(new Event(eid, uid, et, rid, ts));
            }
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    //Найти рецерт по id(можно поменять вместо id другую переменную)
    public Recipe getRecipe(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(Util.TABLE_NAME, new String[]{Util.KEY_ID, Util.KEY_NAME_EN, Util.KEY_NAME, Util.KEY_INGREDIENT_EN, Util.KEY_INGREDIENT,
                        Util.KEY_RECIPE_EN, Util.KEY_RECIPE, Util.KEY_IMAGE, Util.KEY_COOKINGTIME, Util.KEY_CATEGORY, Util.KEY_ISFAVORITE, Util.KEY_INGREDIENT_PARSED, Util.KEY_VECTOR, Util.KEY_ISCOOKED}, Util.KEY_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        Recipe recipe = new Recipe(cursor.getString(0),
                cursor.getString(2), cursor.getString(1), cursor.getString(7),
                Integer.parseInt(cursor.getString(8)), cursor.getString(6),
                cursor.getString(5), cursor.getString(4), cursor.getString(3), Integer.parseInt(cursor.getString(10)), Integer.parseInt(cursor.getString(13)),
                Integer.parseInt(cursor.getString(9)), cursor.getString(11), cursor.getBlob(12));
        cursor.close();
        return recipe;
    }

    //Возвращает все рецепты
    public ArrayList<Recipe> getAllRecipe() {
        try {
            myDataBase = getReadableDatabase();
            if (myDataBase != null) {
                Log.d("DB_DEBUG", "Database opened successfully");
            } else {
                Log.e("DB_ERROR", "Database is null");
            }
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error opening database: " + e.getMessage());
        }
        ArrayList<Recipe> recipeList = new ArrayList<>();
        String selectAllRecipe = "SELECT " + Util.KEY_ID + ", " + Util.KEY_NAME_EN + ", " + Util.KEY_NAME + ", " + Util.KEY_IMAGE + ", " + Util.KEY_COOKINGTIME + ", "
                + Util.KEY_INGREDIENT_EN + ", " + Util.KEY_INGREDIENT + " FROM " + Util.TABLE_NAME;
        Cursor cursor = myDataBase.rawQuery(selectAllRecipe, null);
        if (cursor.moveToFirst()) {
            do {
                Recipe recipe = new Recipe();
                recipe.setId(cursor.getString(0));
                recipe.setName_en(cursor.getString(1));
                recipe.setName(cursor.getString(2));
                recipe.setImage(cursor.getString(3));
                recipe.setCookingTime(Integer.parseInt(cursor.getString(4)));
                recipe.setIngredient_en(cursor.getString(5));
                recipe.setIngredient(cursor.getString(6));

                recipeList.add(recipe);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return recipeList;
    }

    public List<Recipe> getAllRecipesWithVectors() {
        List<Recipe> out = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = this.getReadableDatabase();
            String[] cols = new String[]{
                    Util.KEY_ID, Util.KEY_NAME_EN, Util.KEY_NAME, Util.KEY_INGREDIENT_EN, Util.KEY_INGREDIENT,
                    Util.KEY_RECIPE_EN, Util.KEY_RECIPE, Util.KEY_IMAGE, Util.KEY_COOKINGTIME, Util.KEY_CATEGORY,
                    Util.KEY_ISFAVORITE, Util.KEY_INGREDIENT_PARSED, Util.KEY_VECTOR
            };
            c = db.query(Util.TABLE_NAME, cols, null, null, null, null, null);
            while (c != null && c.moveToNext()) {
                Recipe recipe = new Recipe();
                recipe.setId(c.getString(0));
                recipe.setName_en(c.getString(1));
                recipe.setName(c.getString(2));
                recipe.setIngredient_en(c.getString(3));
                recipe.setIngredient(c.getString(4));
                recipe.setRecipe_en(c.getString(5));
                recipe.setRecipe(c.getString(6));
                recipe.setImage(c.getString(7));
                recipe.setCookingTime(Integer.parseInt(c.getString(8)));
                recipe.setCategory(Integer.parseInt(c.getString(9)));
                recipe.setIsFavorite(Integer.parseInt(c.getString(10)));
                recipe.setIngredient_parsed(c.getString(11));
                recipe.setVectors(c.getBlob(12));
                out.add(recipe);
            }
        } finally {
            if (c != null) c.close();
            // don't close db (SQLiteAssetHelper manages)
        }
        return out;
    }

    public ArrayList<Recipe> getLastRecommendedRecipe(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("RandomItems", context.MODE_PRIVATE);

        String savedData = preferences.getString("savedDishes", null);
        if (savedData != null) {
            ArrayList<Recipe> savedDishes = new ArrayList<>();
            // Восстанавливаем сохранённые данные (формат: id|name|image|time, ...)
            String[] dishesArray = savedData.split(";");
            for (String dishData : dishesArray) {
                String[] dishFields = dishData.split("\\|");
                Recipe dish = new Recipe(
                        dishFields[0],  // id
                        dishFields[1],                   // recipeName
                        dishFields[2],
                        dishFields[3],                   // recipeImage
                        Integer.parseInt(dishFields[4])  // recipeCookingTime
                );
                savedDishes.add(dish);
            }
            return savedDishes;
        }
        return null;
    }

    public void getRecommendedRecipe(Context context, RecommendedCallback callback) {
        try {
            myDataBase = getReadableDatabase();
            if (myDataBase != null) {
                Log.d("DB_DEBUG", "Database opened successfully");
            } else {
                Log.e("DB_ERROR", "Database is null");
            }
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error opening database: " + e.getMessage());
        }
        SharedPreferences preferences = context.getSharedPreferences("RandomItems", context.MODE_PRIVATE);

        ArrayList<Recipe> dishList = new ArrayList<>();

        // Получаем время последнего обновления
        long lastUpdateTime = preferences.getLong("lastUpdateTime", 0);
        long currentTime = System.currentTimeMillis();

        if (lastUpdateTime != 0 && preferences.getString("savedDishes", null) != null) {
            if (currentTime - lastUpdateTime < 8640) {
                callback.onSuccess();
                /*// Если ещё не прошло 24 часа, возвращаем сохранённые данные
                String savedData = preferences.getString("savedDishes", null);
                if (savedData != null) {
                    ArrayList<Recipe> savedDishes = new ArrayList<>();
                    // Восстанавливаем сохранённые данные (формат: id|name|image|time, ...)
                    String[] dishesArray = savedData.split(";");
                    for (String dishData : dishesArray) {
                        String[] dishFields = dishData.split("\\|");
                        Recipe dish = new Recipe(
                                dishFields[0],  // id
                                dishFields[1],                   // recipeName
                                dishFields[2],
                                dishFields[3],                   // recipeImage
                                Integer.parseInt(dishFields[4])  // recipeCookingTime
                        );
                        savedDishes.add(dish);
                    }
                    return savedDishes;
                }*/
            }else{
                String userId = User.username;
                //Log.d("MyLog", "DailyRecommendWorker doWork start: " + System.currentTimeMillis());
                try {
                    RecommendationManager manager = new RecommendationManager(context);
                    List<String> top3 = manager.generateTop3ForUser(userId);

                    // save to DB as JSON array (DatabaseHandler должен реализовать insertRecommendation)
                    JSONArray arr = new JSONArray();
                    for (String rid : top3) arr.put(rid);
                    long now = System.currentTimeMillis();

                    SharedPreferences prefs = context.getSharedPreferences("RandomItems", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong("lastUpdateTime", now);
                    editor.apply();

                    StringBuilder savedDishesBuilder = new StringBuilder();
                    for (String rid : top3) {
                        Recipe r = getRecipe(rid);
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

                    insertRecommendation(userId, arr.toString(), now); // implement in DatabaseHandler

                    callback.onSuccess();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    // if transient error, retry; otherwise return failure
                    callback.onFailure(ex);
                }
            }
        }else {

            String selectAllRecipe = "SELECT " + Util.KEY_ID + ", " + Util.KEY_NAME_EN + ", " + Util.KEY_NAME + ", " + Util.KEY_IMAGE + ", " + Util.KEY_COOKINGTIME + " FROM " + Util.TABLE_NAME + " ORDER BY RANDOM() LIMIT 3";
            Cursor cursor = myDataBase.rawQuery(selectAllRecipe, null);
            if (cursor.moveToFirst()) {
                do {
                    Recipe dish = new Recipe();
                    dish.setId(cursor.getString(0));
                    dish.setName_en(cursor.getString(1));
                    dish.setName(cursor.getString(2));
                    dish.setImage(cursor.getString(3));
                    dish.setCookingTime(Integer.parseInt(cursor.getString(4)));

                    dishList.add(dish);
                } while (cursor.moveToNext());
            }
            cursor.close();

            SharedPreferences.Editor editor = preferences.edit();
            StringBuilder savedDishesBuilder = new StringBuilder();
            for (Recipe dish : dishList) {
                savedDishesBuilder.append(dish.getId()).append("|")
                        .append(dish.getName()).append("|")
                        .append(dish.getName_en()).append("|")
                        .append(dish.getImage()).append("|")
                        .append(dish.getCookingTime()).append(";");
            }
            editor.putLong("lastUpdateTime", currentTime);
            editor.putString("savedDishes", savedDishesBuilder.toString());
            editor.apply();
            callback.onSuccess();
        }

    }

    public void insertRecommendation(String userId, String recipeIdsJson, long generatedAt) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(Util.KEY_USER_ID, userId);
            cv.put(Util.KEY_RECIPE_IDS, recipeIdsJson);
            cv.put(Util.KEY_GENERATED_AT, generatedAt);
            db.beginTransaction();
            int updated = db.update(Util.TABLE_NAME_RECOMMENDATION, cv, Util.KEY_USER_ID + " = ?", new String[]{userId});

            // Если не обновлено ничего — вставляем новую запись
            if (updated == 0) {
                db.insert(Util.TABLE_NAME_RECOMMENDATION, null, cv);
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (db != null) {
                try {
                    db.endTransaction();
                    db.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public String getRecommendationJsonForUser(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(Util.TABLE_NAME_RECOMMENDATION, new String[]{Util.KEY_RECIPE_IDS}, "userId = ?", new String[]{userId}, null, null, null);
            if (c != null && c.moveToFirst()) {
                return c.getString(c.getColumnIndexOrThrow(Util.KEY_RECIPE_IDS));
            }
            return null;
        } finally {
            if (c != null) c.close();
        }
    }

    public ArrayList<Recipe> getFavoriteRecipe() {
        try {
            myDataBase = getReadableDatabase();
            if (myDataBase != null) {
                Log.d("DB_DEBUG", "Database opened successfully");
            } else {
                Log.e("DB_ERROR", "Database is null");
            }
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error opening database: " + e.getMessage());
        }
        ArrayList<Recipe> dishList = new ArrayList<>();
        String selectFavoriteRecipe = "SELECT " + Util.KEY_ID + ", " + Util.KEY_NAME_EN + ", " + Util.KEY_NAME + ", " + Util.KEY_IMAGE + ", " + Util.KEY_COOKINGTIME + ", " + Util.KEY_ISFAVORITE +
                " FROM " + Util.TABLE_NAME + " WHERE " + Util.KEY_ISFAVORITE + " = 1";
        Cursor cursor = myDataBase.rawQuery(selectFavoriteRecipe, null);
        if (cursor.moveToFirst()) {
            do {
                Recipe dish = new Recipe();
                dish.setId(cursor.getString(0));
                dish.setName_en(cursor.getString(1));
                dish.setName(cursor.getString(2));
                dish.setImage(cursor.getString(3));
                dish.setCookingTime(Integer.parseInt(cursor.getString(4)));

                dishList.add(dish);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return dishList;
    }

    public ArrayList<Recipe> getCookRecipe() {
        try {
            myDataBase = getReadableDatabase();
            if (myDataBase != null) {
                Log.d("DB_DEBUG", "Database opened successfully");
            } else {
                Log.e("DB_ERROR", "Database is null");
            }
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error opening database: " + e.getMessage());
        }
        ArrayList<Recipe> dishList = new ArrayList<>();
        String selectFavoriteRecipe = "SELECT " + Util.KEY_ID + ", " + Util.KEY_NAME_EN + ", " + Util.KEY_NAME + ", " + Util.KEY_IMAGE + ", " + Util.KEY_COOKINGTIME + ", " + Util.KEY_ISCOOKED +
                " FROM " + Util.TABLE_NAME + " WHERE " + Util.KEY_ISCOOKED + " = 1";
        Cursor cursor = myDataBase.rawQuery(selectFavoriteRecipe, null);
        if (cursor.moveToFirst()) {
            do {
                Recipe dish = new Recipe();
                dish.setId(cursor.getString(0));
                dish.setName_en(cursor.getString(1));
                dish.setName(cursor.getString(2));
                dish.setImage(cursor.getString(3));
                dish.setCookingTime(Integer.parseInt(cursor.getString(4)));

                dishList.add(dish);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return dishList;
    }

    public ArrayList<Recipe> getColdStartRecipe() {
        try {
            myDataBase = getReadableDatabase();
            if (myDataBase != null) {
                Log.d("DB_DEBUG", "Database opened successfully");
            } else {
                Log.e("DB_ERROR", "Database is null");
            }
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error opening database: " + e.getMessage());
        }
        ArrayList<Recipe> dishList = new ArrayList<>();
        String selectFavoriteRecipe = "SELECT " + Util.KEY_ID + ", " + Util.KEY_NAME_EN + ", " + Util.KEY_NAME + ", " + Util.KEY_IMAGE + ", " + Util.KEY_ISCOLDSTART +
                " FROM " + Util.TABLE_NAME + " WHERE " + Util.KEY_ISCOLDSTART + " = 1 LIMIT 10";
        Cursor cursor = myDataBase.rawQuery(selectFavoriteRecipe, null);
        if (cursor.moveToFirst()) {
            do {
                Recipe dish = new Recipe();
                dish.setId(cursor.getString(0));
                dish.setName_en(cursor.getString(1));
                dish.setName(cursor.getString(2));
                dish.setImage(cursor.getString(3));
                dish.setIsColdStart(Integer.parseInt(cursor.getString(4)));

                dishList.add(dish);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return dishList;
    }
    //Обновляет конкретный рецепт
    public int updateRecipe(Recipe recipe) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(Util.KEY_NAME, recipe.getName());
        contentValues.put(Util.KEY_IMAGE, recipe.getImage());
        contentValues.put(Util.KEY_COOKINGTIME, recipe.getCookingTime());
        contentValues.put(Util.KEY_RECIPE, recipe.getRecipe());
        contentValues.put(Util.KEY_INGREDIENT, recipe.getIngredient());
        contentValues.put(Util.KEY_ISFAVORITE, recipe.getIsFavorite());
        contentValues.put(Util.KEY_ISCOOKED, recipe.getIsCook());

        return db.update(Util.TABLE_NAME, contentValues, Util.KEY_ID + "=?", new String[]{String.valueOf(recipe.getId())});
    }

    //Удаляет конкретный рецепт
    public void deleteRecipe(Recipe recipe) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(Util.TABLE_NAME, Util.KEY_ID + "=?", new String[]{String.valueOf(recipe.getId())});
        db.close();
    }

    public void copyAllImagesFromAssets(Context context) {
        AssetManager assetManager = context.getAssets();

        try {
            // Получаем список всех файлов в папке assets
            String[] files = assetManager.list("image");
            if (files != null) {
                for (String fileName : files) {
                    // Проверяем, является ли файл изображением (например, по расширению)
                    //if (fileName.endsWith(".jpg") || fileName.endsWith(".png")) {
                    // Копируем файл
                    copyFileFromAssets(context, fileName);
                    //}
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyFileFromAssets(Context context, String fileName) {
        // Путь, куда будет скопирован файл
        String assetFilePath = "image/" + fileName;
        File outFile = new File(context.getFilesDir(), fileName);

        if (!outFile.exists()) {
            try (InputStream inputStream = context.getAssets().open(assetFilePath);
                 FileOutputStream outputStream = new FileOutputStream(outFile)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                outputStream.flush();
                System.out.println("Файл скопирован: " + outFile.getAbsolutePath());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Метод для сохранения данных анкеты
    public long saveUserPreferences(String username, String allergies, String categories) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Util.KEY_USER_ID, username);
        values.put(Util.KEY_ALLERGIES, allergies);
        values.put(Util.KEY_LIKECATEGORIES, categories);

        // Удаляем старые записи для этого пользователя (опционально)
        db.delete(Util.TABLE_NAME_USER, Util.KEY_USER_ID + " = ?", new String[]{username});

        // Вставляем новую запись
        long result = db.insert(Util.TABLE_NAME_USER, null, values);
        db.close();
        return result;
    }

    // Получить аллергены пользователя
    public String getAllergiesForUser(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        String allergies = null;

        Cursor cursor = db.query(
                Util.TABLE_NAME_USER,
                new String[]{Util.KEY_ALLERGIES},
                Util.KEY_USER_ID + " = ?",
                new String[]{username},
                null, null, Util.KEY_TIMESTAMP + " DESC", "1"
        );

        if (cursor != null && cursor.moveToFirst()) {
            allergies = cursor.getString(cursor.getColumnIndexOrThrow(Util.KEY_ALLERGIES));
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();

        return allergies;
    }

    public void closeDB() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen())
            db.close();
    }

    public void clearDB() {
        if (myDataBase != null && myDataBase.isOpen()) {
            myDataBase.close();
        }
        File file = new File(myContext.getDatabasePath(Util.DATABASE_NAME).getPath());
        SQLiteDatabase.deleteDatabase(file);
    }

}

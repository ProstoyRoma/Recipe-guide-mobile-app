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
import com.example.recipeguide.Dish;
import com.example.recipeguide.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import Data.DatabaseHandler;
import Model.Recipe;

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

                    String filterString = constraint.toString().toLowerCase().trim();

                    List<Recipe> sourceList = new ArrayList<>(originalDishes);
                    List<Recipe> filteredList = new ArrayList<>();

                    for (Recipe dish : sourceList) {
                        if (!currentFilters.getSelectedCuisines().isEmpty() || !currentFilters.getSelectedCategories().isEmpty() ||
                                !currentFilters.getSelectedDiets().isEmpty() || currentFilters.getMaxCookingTime() != null) {
                            if (applyAllFilters(dish, constraint)) {
                                filteredList.add(dish);
                            }
                        }else{
                            if(sharedPreferences.getBoolean("language", false)){
                                if (dish.getName().toLowerCase().contains(filterString) || dish.getIngredient().toLowerCase().contains(filterString)) {
                                    filteredList.add(dish);
                                }

                            }else{
                                if (dish.getName_en().toLowerCase().contains(filterString) || dish.getIngredient_en().toLowerCase().contains(filterString)) {
                                    filteredList.add(dish);
                                }
                            }
                        }
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
            String searchText = constraint.toString().toLowerCase().trim();
            if (languageRussian) {
                matchesSearch = dish.getName().toLowerCase().contains(searchText) ||
                        dish.getIngredient().toLowerCase().contains(searchText);
            } else {
                matchesSearch = dish.getName_en().toLowerCase().contains(searchText) ||
                        dish.getIngredient_en().toLowerCase().contains(searchText);
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

        // 5. Фильтр по времени приготовления
        boolean matchesTime = true;
        if (currentFilters.getMaxCookingTime() != null) {
            matchesTime = dish.getCookingTime() <= currentFilters.getMaxCookingTime();
        }

        return matchesSearch && matchesCategory && matchesCuisine && matchesDiet && matchesTime;
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

    }

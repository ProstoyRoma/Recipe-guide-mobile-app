package com.example.recipeguide;

import android.content.Context;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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

public class ProfileAdapter extends ArrayAdapter<Recipe>{


    private ArrayList<Recipe> originalDishes; // Исходный список блюд
    private ArrayList<Recipe> filteredDishes; // Отфильтрованный список
    private Set<String> cachedCuisineRecipeIds = null; // Кэш для SQLite
    private Set<String> lastSelectedCuisines = null; // Кэшируем последние выбранные кухни
    private Filter dishFilter; // Фильтр для поиска
    private Context context;
    FirebaseDatabase database;
    DatabaseReference myRef;
    private DatabaseHandler databaseHelper;
    SharedPreferences sharedPreferences;
    private FilterOptions currentFilters;
    private int check;
    private OnRecipeActionListener actionListener;

    public interface OnRecipeActionListener {
        void onEdit(Recipe recipe);
        void onDelete(Recipe recipe, ArrayList<Recipe> dishes);
    }
    public void setOnRecipeActionListener(OnRecipeActionListener listener) {
        this.actionListener = listener;
    }
    public ProfileAdapter(Context context, List<Recipe> dishes, int check) {
        super(context, 0, dishes);
        this.context = context;
        this.originalDishes = new ArrayList<>(dishes); // Сохраняем оригинальный список
        this.filteredDishes = new ArrayList<>(dishes); // Создаём копию для фильтрации
        this.currentFilters = new FilterOptions();
        sharedPreferences = context.getSharedPreferences("MODE", Context.MODE_PRIVATE);
        databaseHelper = new DatabaseHandler(context);
        this.check = check;
    }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Recipe dish = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.adapter_my_recipes, parent, false);
        }

        // Находим элементы интерфейса
        ImageView dishImageView = convertView.findViewById(R.id.recipe_image);
        TextView dishNameTextView = convertView.findViewById(R.id.recipe_name);
        TextView dishCookingTimeTextView = convertView.findViewById(R.id.recipe_cooking_time);
        Button deleteButton = convertView.findViewById(R.id.delete);
        Button editButton = convertView.findViewById(R.id.edit);

        if (check == 0) {
            deleteButton.setVisibility(View.GONE);
            editButton.setVisibility(View.GONE);
        } else {
            deleteButton.setVisibility(View.VISIBLE);
            editButton.setVisibility(View.VISIBLE);

            // Обработчик для кнопки редактирования
            editButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onEdit(dish);
                }
            });

            // Обработчик для кнопки удаления
            deleteButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onDelete(dish, originalDishes);
                }
            });
        }
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

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setFilters(FilterOptions filters) {
        this.currentFilters = filters;
    }

    public void updateData(List<Recipe> newData) {
        filteredDishes.clear();
        filteredDishes.addAll(newData);
        notifyDataSetChanged();
    }

    public void removeItem(Recipe recipe) {
        originalDishes.remove(recipe);
        filteredDishes.remove(recipe);
        notifyDataSetChanged();
    }



}

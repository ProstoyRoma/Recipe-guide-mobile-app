package com.example.recipeguide;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import Model.Recipe;

public class DishAdapter extends ArrayAdapter<Recipe> {
    private List<Recipe> originalDishes; // Исходный список блюд
    private List<Recipe> filteredDishes; // Отфильтрованный список
    private Filter dishFilter; // Фильтр для поиска
    private Context context;
    SharedPreferences sharedPreferences;
    public DishAdapter(Context context, List<Recipe> dishes) {
        super(context, 0, dishes);
        this.context = context;
        this.originalDishes = new ArrayList<>(dishes); // Сохраняем оригинальный список
        this.filteredDishes = new ArrayList<>(dishes); // Создаём копию для фильтрации
        sharedPreferences = context.getSharedPreferences("MODE", Context.MODE_PRIVATE);
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
            if(sharedPreferences.getBoolean("language", false)){
                dishNameTextView.setText(dish.getName());
            }else {
                dishNameTextView.setText(dish.getName_en());
            }


            String imagePath = dish.getImage();
            if (imagePath != null) {
                File imgFile = new File("/data/data/com.example.recipeguide/files/" + imagePath + ".jpg");
                if (imgFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    dishImageView.setImageBitmap(bitmap); // Устанавливаем изображение в ImageView
                }else {
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
    public Filter getFilter() {
        if (dishFilter == null) {
            dishFilter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();

                    if (constraint == null || constraint.length() == 0) {
                        // Если ничего не введено, возвращаем весь список
                        results.values = originalDishes;
                        results.count = originalDishes.size();
                    } else {
                        String filterString = constraint.toString().toLowerCase().trim();

                        // Отфильтровываем по названию блюда и ингредиентам
                        List<Recipe> filteredList = new ArrayList<>();
                        for (Recipe dish : originalDishes) {
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

                        results.values = filteredList;
                        results.count = filteredList.size();
                    }

                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredDishes.clear();
                    if (results.values != null) {
                        filteredDishes.addAll((List<Recipe>) results.values);
                    }
                    notifyDataSetChanged(); // Обновляем ListView
                }
            };
        }
        return dishFilter;
    }
}

package com.example.recipeguide;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.SearchView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

import Data.DatabaseHandler;
import Model.Recipe;


public class SearchActivity extends AppCompatActivity {

    SearchView searchView;
    ListView listView;
    BaseAdActivity baseAdActivity;
    private String lastQuery = ""; // хранит последний текст в SearchView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        baseAdActivity = new BaseAdActivity(
                this,
                R.id.main,
                R.id.ad_container_view,
                "demo-banner-yandex"
        );
        baseAdActivity.load();

        listView = findViewById(R.id.listView);
        searchView = findViewById(R.id.search_field);
        searchView.setQueryHint(getString(R.string.search));

        DatabaseHandler databaseHelper = new DatabaseHandler(this);
        ArrayList<Recipe> dishes = databaseHelper.getAllRecipe();
        DishAdapter adapter = new DishAdapter(this, dishes); // Создаём адаптер

        listView.setAdapter(adapter); // Устанавливаем адаптер

        listView.setOnItemClickListener((parent, view, position, id) -> {
            // Получаем выбранное блюдо
            Recipe selectedDish = adapter.getItem(position);

            if (selectedDish != null) {
                if (lastQuery != null && lastQuery.trim().length() >= 3) {
                    // пример: insertEvent(String userId, String recipeId, String query)
                    // замените параметры на те, что нужны вашей реализации
                    databaseHelper.insertEvent(java.util.UUID.randomUUID().toString(),User.username,"search", selectedDish.getId(), System.currentTimeMillis());
                }else{
                    databaseHelper.insertEvent(java.util.UUID.randomUUID().toString(),User.username,"view",selectedDish.getId(),System.currentTimeMillis());

                }
                // Создаём Intent и передаём ID блюда
                Intent intent = new Intent(getApplicationContext(), recipe_example_activity.class);
                intent.putExtra("dish_id", selectedDish.getId()); // Передаём ID блюда
                startActivity(intent);
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                lastQuery = query != null ? query : "";
                adapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                lastQuery = newText != null ? newText : "";
                adapter.getFilter().filter(newText);
                return false;
            }
        });

    }


    public void goAddScreen(View view){
        Intent intent = new Intent(this, AddScreen.class);
        startActivity(intent);
    }

    public void goHome(View view){
        Intent intent = new Intent(this, MainScreen.class);
        startActivity(intent);
    }

    public void goFavourites(View view){
        Intent intent = new Intent(this, FavouritesScreen.class);
        startActivity(intent);
    }

    public void goOptions(View view){
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
}
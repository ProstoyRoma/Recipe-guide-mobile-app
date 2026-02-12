package com.example.recipeguide;


import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import Data.DatabaseHandler;
import Model.Recipe;

public class ActivityProfile extends AppCompatActivity {

    private boolean isEdit, isAvatar = false;
    TextView user_name, my_recipe, my_cook_recipe;
    ListView user_recipes_list;
    ImageView user_avatar;
    ProgressBar uploadProgressBar;
    AppCompatButton logout_button, edit_profile_button;
    private FirebaseAuth mAuth;
    FirebaseDatabase database;
    DatabaseReference myRef;
    Uri imageUri;
    SharedPreferences sharedPreferences;
    SharedPreferences prefs ;
    SharedPreferences.Editor editor;
    BaseAdActivity baseAdActivity;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private View underline;


    private ArrayList<Recipe> dishList = new ArrayList<>();      // заполняйте данными
    private ArrayList <Recipe> cookList = new ArrayList<>();  // заполняйте данными

    private DishAdapter adapterMyRecipes;
    private DishAdapter adapterCook;
    private int activeTab = 0; // 0 = myRecipes, 1 = cooked

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        baseAdActivity = new BaseAdActivity(
                this,
                R.id.main,
                R.id.ad_container_view,
                "demo-banner-yandex"
        );
        baseAdActivity.load();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        prefs = getSharedPreferences("MODE", Context.MODE_PRIVATE);
        //sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE);
        editor = prefs.edit();

        mAuth = FirebaseAuth.getInstance();

        photoFromGallery();
        setupBackButtonHandler();

        uploadProgressBar = findViewById(R.id.uploadProgressBar);
        my_recipe = findViewById(R.id.tab_my_recipes);
        my_cook_recipe = findViewById(R.id.tab_cooked);
        user_recipes_list = findViewById(R.id.listView);
        underline = findViewById(R.id.tab_underline);
        user_name = findViewById(R.id.user_name);
        user_name.setText(User.username);
        user_avatar = findViewById(R.id.user_avatar);


        if(User.userImage != null && !User.userImage.equals("null")){
            loadAvatar(User.userImage);
        }else{
            user_avatar.setImageResource(R.drawable.avatar_icon);
        }


        ArrayList<String> cookDish = getCookRecipe();

        if(cookDish != null){

            DatabaseHandler databaseHandler = new DatabaseHandler(this);
            for(String dish: cookDish){
                databaseHandler.updateRecipe(databaseHandler.getRecipe(dish));
            }
            cookList = databaseHandler.getCookRecipe();

            /*adapter = new DishAdapter(this, dishList); // Создаём адаптер

            user_recipes_list.setAdapter(adapter); // Устанавливаем адаптер

            user_recipes_list.setOnItemClickListener((parent, view, position, id) -> {
                // Получаем выбранное блюдо
                Recipe selectedDish = adapter.getItem(position);

                if (selectedDish != null) {
                    // Создаём Intent и передаём ID блюда
                    Intent intent = new Intent(getApplicationContext(), recipe_example_activity.class);
                    intent.putExtra("dish_id", selectedDish.getId()); // Передаём ID блюда
                    startActivity(intent);
                }
            });*/
        }



        String dishJson = prefs.getString("dish_list", null);
        Gson gson = new Gson();

        ArrayList<String> dishes = new ArrayList<>();

        if (dishJson != null) {

            dishes = gson.fromJson(dishJson, new TypeToken<ArrayList<String>>(){}.getType());
            Log.d("ActivityProfile", getString(R.string.load_recipes) + dishes.size());
        }else{
            Bundle extras = getIntent().getExtras();
            if (extras != null && extras.containsKey("dish_list")) {
                dishes = getIntent().getStringArrayListExtra("dish_list");
                Log.d("ActivityProfile", getString(R.string.load_recipes_intent) + dishes.size());

                // 📌 Сохраняем `dishes` в `SharedPreferences` для последующих запусков
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("dish_list", gson.toJson(dishes));
                editor.apply();
            } else {
                Log.w("ActivityProfile", getString(R.string.no_dishes));
            }
        }

        if(dishes != null){

            DatabaseHandler databaseHandler = new DatabaseHandler(this);
            for(String dish: dishes){
                dishList.add(databaseHandler.getRecipe(dish));
            }
            /*adapter = new DishAdapter(this, dishList); // Создаём адаптер

            user_recipes_list.setAdapter(adapter); // Устанавливаем адаптер

            user_recipes_list.setOnItemClickListener((parent, view, position, id) -> {
                // Получаем выбранное блюдо
                Recipe selectedDish = adapter.getItem(position);

                if (selectedDish != null) {
                    // Создаём Intent и передаём ID блюда
                    Intent intent = new Intent(getApplicationContext(), recipe_example_activity.class);
                    intent.putExtra("dish_id", selectedDish.getId()); // Передаём ID блюда
                    startActivity(intent);
                }
            });*/
        }

        logout_button = findViewById(R.id.logout_button);
        logout_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("dish_list", null);
                editor.apply();

                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(ActivityProfile.this, MainScreen.class);
                startActivity(intent);
                finish();
            }
        });
        adapterMyRecipes = new DishAdapter(this, dishList);
        adapterCook = new DishAdapter(this, cookList);
        updateList(adapterMyRecipes);


        // Ожидаем, когда layout будет измерен, чтобы установить ширину и позицию underline
        final ViewTreeObserver vto = my_recipe.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                my_recipe.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                // Устанавливаем underline под первой вкладкой
                moveUnderlineToTab(0, false);
            }
        });

        my_recipe.setOnClickListener(v -> switchToTab(0));
        my_cook_recipe.setOnClickListener(v -> switchToTab(1));
        edit_profile_button = findViewById(R.id.edit_profile_button);

        // Гарантируем минимальную высоту (необязательно, но полезно)
        int minH = dpToPx(48);
        edit_profile_button.setMinHeight(minH);
        logout_button.setMinHeight(minH);

        // Выполняем код после прохождения layout, чтобы получить реальные высоты
        View root = ((View) edit_profile_button.getParent());
        root.post(() -> {
            int h1 = edit_profile_button.getHeight();
            int h2 = logout_button.getHeight();
            int max = Math.max(h1, h2);

            ViewGroup.LayoutParams lp1 = edit_profile_button.getLayoutParams();
            ViewGroup.LayoutParams lp2 = logout_button.getLayoutParams();

            lp1.height = max;
            lp2.height = max;

            edit_profile_button.setLayoutParams(lp1);
            logout_button.setLayoutParams(lp2);
        });
    }
    private void switchToTab(int tabIndex) {
        if (tabIndex == activeTab) return;
        activeTab = tabIndex;

        // меняем цвета заголовков (активный — тёмный, неактивный — серый)
        if (tabIndex == 0) {
            my_recipe.setTextColor(ContextCompat.getColor(this, R.color.black));
            my_cook_recipe.setTextColor(ContextCompat.getColor(this, R.color.text_time));
            updateList(adapterMyRecipes);
        } else {
            my_recipe.setTextColor(ContextCompat.getColor(this, R.color.text_time));
            my_cook_recipe.setTextColor(ContextCompat.getColor(this, R.color.black));
            updateList(adapterCook);
        }

        // анимируем underline
        moveUnderlineToTab(tabIndex, true);
    }

    private void updateList( DishAdapter adapter) {
        adapter.clear();
        user_recipes_list.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        user_recipes_list.setOnItemClickListener((parent, view, position, id) -> {
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

    private void moveUnderlineToTab(int tabIndex, boolean animate) {
        TextView target = (tabIndex == 0) ? my_recipe : my_cook_recipe;

        // вычисляем позицию относительно родителя
        int[] rootLoc = new int[2];
        View root = findViewById(R.id.root);
        root.getLocationOnScreen(rootLoc);

        int[] targetLoc = new int[2];
        target.getLocationOnScreen(targetLoc);

        // координата левого края target внутри родителя
        float targetLeft = targetLoc[0] - rootLoc[0];
        float targetWidth = target.getWidth();

        // задаём ширину underline = ширине target
        if (!animate) {
            ViewGroup.LayoutParams lp = underline.getLayoutParams();
            lp.width = (int) targetWidth;
            underline.setLayoutParams(lp);
            underline.setX(targetLeft);
        } else {
            // анимируем X и ширину одновременно
            // анимация X
            underline.animate().x(targetLeft).setDuration(220).setInterpolator(new DecelerateInterpolator()).start();

            // анимация ширины (через ValueAnimator)
            final int startW = underline.getWidth();
            final int endW = (int) targetWidth;
            ValueAnimator widthAnimator = ValueAnimator.ofInt(startW, endW);
            widthAnimator.setDuration(220);
            widthAnimator.setInterpolator(new DecelerateInterpolator());
            widthAnimator.addUpdateListener(animation -> {
                int w = (int) animation.getAnimatedValue();
                ViewGroup.LayoutParams lp = underline.getLayoutParams();
                lp.width = w;
                underline.setLayoutParams(lp);
            });
            widthAnimator.start();
        }
    }
    private ArrayList<String> getCookRecipe() {
        FirebaseUser user = mAuth.getCurrentUser();

        DatabaseHandler dbHelper = new DatabaseHandler(this);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("users").child(user.getUid()).child("isCook");

        ArrayList<String> cookList = new ArrayList<>();

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String recipeId = child.getKey();
                        if (recipeId == null) continue;

                        // 🔥 Проверяем, есть ли рецепт в SQLite
                        if (!dbHelper.myRecipeInSQLite(recipeId)) {
                            Recipe recipe = new Recipe();
                            recipe.setId(recipeId);
                            recipe.setName(child.child("name").getValue(String.class));
                            recipe.setName_en(child.child("name_en").getValue(String.class));
                            recipe.setImage(child.child("image").getValue(String.class));
                            recipe.setCookingTime(child.child("cookingTime").getValue(Integer.class));
                            recipe.setIngredient(child.child("ingredient").getValue(String.class));
                            recipe.setIngredient_en(child.child("ingredient_en").getValue(String.class));
                            recipe.setRecipe(child.child("recipe").getValue(String.class));
                            recipe.setRecipe_en(child.child("recipe_en").getValue(String.class));
                            recipe.setIsFavorite(1);

                            dbHelper.insertOrUpdateRecipe(recipe); // ✅ Сохраняем в SQLite
                        }
                        // Если значение узла boolean true или строка "true" — считаем, что рецепт есть в списке
                        Object val = child.getValue();
                        boolean enabled = false;
                        if (val instanceof Boolean) {
                            enabled = (Boolean) val;
                        } else if (val instanceof String) {
                            enabled = Boolean.parseBoolean((String) val);
                        } else if (val instanceof Long) {
                            enabled = ((Long) val) != 0L;
                        }

                        if (enabled) {
                            cookList.add(recipeId);
                        }
                    }

                } catch (Exception e) {

                    Log.e("Firebase", getString(R.string.error_loaded) );

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", getString(R.string.error_loaded) + error.getMessage());

            }
        });
        return cookList;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

/*    private ArrayList<Dish> getMyRecipe() {
        FirebaseUser user = mAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("users").child(user.getUid()).child("my_recipes");

        ArrayList<Dish> dishList = new ArrayList<>();
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Dish dish = new Dish();

                for (DataSnapshot recipeSnapshot : snapshot.getChildren()) {
                    dish.setId(recipeSnapshot.getKey());
                    dish.setRecipeName(recipeSnapshot.child("name").getValue(String.class));
                    dish.setRecipeNameEn(recipeSnapshot.child("name_en").getValue(String.class));
                    dish.setRecipeImage(recipeSnapshot.child("image").getValue(String.class));
                    dish.setRecipeCookingTime(recipeSnapshot.child("cookingTime").getValue(Integer.class));

                    dishList.add(dish);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Ошибка загрузки данных: " + error.getMessage());
            }
        });

        return dishList;
    }*/

    public void editProfile(View view) {
        if(!isEdit){
            isEdit = true;
            user_avatar.setEnabled(true);
            if(User.userImage == null || User.userImage.equals("null")){
                user_avatar.setImageResource(R.drawable.edit_avatar);
            }
            user_avatar.setOnClickListener(v -> openImageChooser());

            my_recipe.setVisibility(View.INVISIBLE);
            my_cook_recipe.setVisibility(View.INVISIBLE);
            underline.setVisibility(View.INVISIBLE);
            user_recipes_list.setVisibility(View.INVISIBLE);
            edit_profile_button.setText(getString(R.string.cancel));
            edit_profile_button.setTextColor(Color.RED);
            logout_button.setText(getString(R.string.save_button));
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.textColor, typedValue, true);
            int color = typedValue.data;
            logout_button.setTextColor(color);
            logout_button.setOnClickListener(v -> saveProfile());

            user_name.setBackgroundResource(R.drawable.edit_text_border); // Добавляем нижнюю линию
            user_name.setCursorVisible(true);
            user_name.setFocusable(true);
            user_name.setFocusableInTouchMode(true);
            user_name.requestFocus();




        }else {
            isEdit = false;
            user_avatar.setEnabled(false);
            if(User.userImage != null){
                loadAvatar(User.userImage);
            }else{
                user_avatar.setImageResource(R.drawable.avatar_icon);
            }
            my_recipe.setVisibility(View.VISIBLE);
            my_cook_recipe.setVisibility(View.VISIBLE);
            underline.setVisibility(View.VISIBLE);
            user_recipes_list.setVisibility(View.VISIBLE);
            edit_profile_button.setText(getString(R.string.edit_profile));
            logout_button.setTextColor(Color.RED);
            logout_button.setText(getString(R.string.log_out));
            logout_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    User.username = "Username";
                    User.userImage = null;
                    editor.putString("userImage", null);
                    editor.putString("username", User.username);
                    editor.apply();
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(ActivityProfile.this, MainScreen.class);
                    startActivity(intent);
                    finish();
                }
            });
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.textColor, typedValue, true);
            int color = typedValue.data;
            edit_profile_button.setTextColor(color);

            user_name.setText(User.username);
            user_name.setBackground(null); // Убираем нижнюю линию
            user_name.setCursorVisible(false);
            user_name.setFocusable(false);
            user_name.setFocusableInTouchMode(false);
        }
    }
    private void photoFromGallery() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        imageUri = result.getData().getData(); // Получаем URI выбранного изображения
                        Glide.with(this).load(imageUri).circleCrop().into(user_avatar);
                        isAvatar = true;
                    }
                }
        );

    }

    private void saveProfile() {

        if(!String.valueOf(user_name.getText()).equals(User.username)){
            saveUsernameToDatabase();
        }
        if(imageUri != null && !imageUri.equals(User.userImage)){
            saveCloudinary();
        }else {
            Intent intent = new Intent(ActivityProfile.this, ActivityProfile.class);
            startActivity(intent);
            finish();
        }
    }

    private void saveImageUrlToDatabase(String imageUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("users");
        myRef.child(user.getUid()).child("imageUrl").setValue(imageUrl.toString());

        User.userImage = imageUrl;
        editor.putString("userImage", User.userImage);
        editor.apply();
    }

    private void saveUsernameToDatabase() {
        FirebaseUser user = mAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("users");
        myRef.child(user.getUid()).child("username").setValue(user_name.getText().toString());

        User.username = user_name.getText().toString().trim();
        editor.putString("username", User.username);
        editor.apply();
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, getString(R.string.select_image)));
    }

    private void saveCloudinary(){
        if(isAvatar){
            MediaManager.get().upload(imageUri).option("folder", "avatar_icon").callback(new UploadCallback() {
                @Override
                public void onStart(String requestId) {
                    uploadProgressBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void onProgress(String requestId, long bytes, long totalBytes) {

                }

                @Override
                public void onSuccess(String requestId, Map resultData) {
                    uploadProgressBar.setVisibility(View.GONE);
                    String imageUrl = resultData.get("secure_url").toString(); // Ссылка на картинку
                    saveImageUrlToDatabase(imageUrl); // Сохранение в Firebase
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(ActivityProfile.this, ActivityProfile.class);
                            startActivity(intent);
                            finish();
                        }
                    }, 500);

                }

                @Override
                public void onError(String requestId, ErrorInfo error) {

                    uploadProgressBar.setVisibility(View.GONE);
                    Log.e("Cloudinary", getString(R.string.error_loaded) + error.getDescription());
                }

                @Override
                public void onReschedule(String requestId, ErrorInfo error) {

                }
            }).dispatch();
        }
    }

    private void loadAvatar(String url) {
        Glide.with(this)
                .load(url).circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Загружаем из кеша, если интернета нет
                .into(user_avatar);
    }
    private void setupBackButtonHandler() {
        // Устанавливаем обработчик для кнопки "Назад"
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Завершаем текущую Activity, возвращаемся на предыдущую
                finish();
            }
        });
    }
    public void goBack(View view) {
        finish();
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
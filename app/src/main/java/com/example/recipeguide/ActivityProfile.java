package com.example.recipeguide;


import android.animation.ValueAnimator;
import android.app.AlertDialog;
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
import java.nio.charset.StandardCharsets;
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
    DatabaseHandler databaseHandler;
    Uri imageUri;
    SharedPreferences prefs ;
    SharedPreferences.Editor editor;
    BaseAdActivity baseAdActivity;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private View underline;


    public ArrayList<Recipe> dishList = new ArrayList<>();      // заполняйте данными
    private ArrayList <Recipe> cookList = new ArrayList<>();  // заполняйте данными

    //private DishAdapter adapterMyRecipes;
    //private DishAdapter adapterCook;
    private ProfileAdapter adapterMyRecipes;
    private ProfileAdapter adapterCook;
    private int activeTab = 0; // 0 = myRecipes, 1 = cooked

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        /*baseAdActivity = new BaseAdActivity(
                this,
                R.id.main,
                R.id.ad_container_view,
                "demo-banner-yandex"
        );
        baseAdActivity.load();*/
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        databaseHandler = new DatabaseHandler(this);

        prefs = getSharedPreferences("MODE", Context.MODE_PRIVATE);
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


        cookList = databaseHandler.getCookRecipe();


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

            for(String dish: dishes){
                dishList.add(databaseHandler.getRecipe(dish));
            }
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

        //adapterMyRecipes = new DishAdapter(this, dishList);
        //adapterCook = new DishAdapter(this, cookList);
        adapterMyRecipes = new ProfileAdapter(this, dishList, 1);
        adapterCook = new ProfileAdapter(this, cookList, 0);
        // Устанавливаем слушатель для адаптера "Мои рецепты"
        adapterMyRecipes.setOnRecipeActionListener(new ProfileAdapter.OnRecipeActionListener() {
            @Override
            public void onEdit(Recipe recipe) {
                // Переход на экран редактирования
                Intent intent = new Intent(ActivityProfile.this, AddScreen.class);
                intent.putExtra("recipe_id", recipe.getId());
                intent.putExtra("is_edit", true);
                startActivity(intent);
            }

            @Override
            public void onDelete(Recipe recipe, ArrayList<Recipe> dishes) {
                // Показываем диалог подтверждения удаления
                showDeleteConfirmationDialog(recipe, dishes);
            }
        });

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
    private void showDeleteConfirmationDialog(Recipe recipe, ArrayList<Recipe> dishes) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_dialog)
                .setMessage(getString(R.string.delete_message)+ " \"" +
                        (prefs.getBoolean("language", false) ? recipe.getName() : recipe.getName_en()) +
                        "\"?")
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    deleteRecipe(recipe, dishes);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Удаляет рецепт из базы данных и обновляет список
     */
    private void deleteRecipe(Recipe recipe, ArrayList<Recipe> dishes) {

        // Удаляем из списка
        dishes.remove(recipe);

        // Обновляем адаптер, если он активен
        if (activeTab == 0) {
            adapterMyRecipes.updateData(dishes);
        }

        // Обновляем SharedPreferences
        Gson gson = new Gson();
        ArrayList<String> recipeIds = new ArrayList<>();
        for (Recipe r : dishes) {
            recipeIds.add(r.getId());
        }
        editor.putString("dish_list", gson.toJson(recipeIds));
        editor.apply();

        dishList = dishes;
        // Показываем уведомление об успешном удалении
        Toast.makeText(this, R.string.toast_recipe_deleted, Toast.LENGTH_SHORT).show();

        // Если есть интернет, удаляем из Firebase
        deleteFromDB(recipe.getId());

    }

    /**
     * Удаляет рецепт из Firebase
     */
    private void deleteFromDB(String recipeId) {
        databaseHandler.deleteRecipe(recipeId);

        FirebaseUser user = mAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("users");
        myRef.child(user.getUid()).child("my_recipes").child(recipeId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d("ActivityProfile", "Рецепт удалён из Firebase: " + recipeId);
                })
                .addOnFailureListener(e -> {
                    Log.e("ActivityProfile", "Ошибка удаления из Firebase", e);
                });



        myRef = database.getReference("recipes");
        myRef.child(recipeId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d("ActivityProfile", "Рецепт удалён из Firebase: " + recipeId);
                })
                .addOnFailureListener(e -> {
                    Log.e("ActivityProfile", "Ошибка удаления из Firebase", e);
                });

        DatabaseReference indicesRef = FirebaseDatabase.getInstance().getReference("indices");

        String cuisine = databaseHandler.getCuisineByRecipeId(recipeId);
        if (cuisine != null && !cuisine.isEmpty()) {
            indicesRef.child("by_cuisine").child(cuisine).child(recipeId).removeValue();
        }

        // Удаляем из индексов по диетам
        List<String> diets = databaseHandler.getDietsByRecipeId(recipeId);
        if (diets != null) {
            for (String diet : diets) {
                indicesRef.child("by_diet").child(diet).child(recipeId).removeValue();
            }
        }
    }
    private void updateList( ProfileAdapter adapter) {
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
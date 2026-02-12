package com.example.recipeguide;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.time.LocalDateTime;
import java.util.ArrayList;

import Data.DatabaseHandler;
import Model.Recipe;

public class LoginActivity extends AppCompatActivity {

    private boolean isLoginActivity = true;
    TextView auth_title;
    private FirebaseAuth mAuth;
    FirebaseDatabase database;
    DatabaseReference myRef;
    private TextInputLayout emailText, username, passwordText, confirm_password;
    private AppCompatButton login_button;
    private TextView toggleLoginSignUp;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emailText = findViewById(R.id.email);
        username = findViewById(R.id.username);
        passwordText = findViewById(R.id.password);
        confirm_password = findViewById(R.id.confirm_password);
        login_button = findViewById(R.id.login_button);
        toggleLoginSignUp = findViewById(R.id.toggleLoginSignUp);
        auth_title = findViewById(R.id.auth_title);

        mAuth = FirebaseAuth.getInstance();
        setupBackButtonHandler();

    }

    private boolean validaiteEmail() {
        String emailInput = emailText.getEditText().getText().toString().trim();
        if (emailInput.isEmpty()) {
            emailText.setError(getString(R.string.error_email_empty));
            return false;
        } else if (countAtSymbols(emailInput) != 1 || !emailInput.matches("^[\\w-\\.]+@[\\w-]+(\\.[\\w-]+)*\\.[a-z]{2,}$") || checkStringDoubleDots(emailInput) || checkStringSpace(emailInput)) {
            emailText.setError(getString(R.string.error_email));
            return false;
        } else {
            emailText.setError("");
            return true;
        }
    }

    private static int countAtSymbols(String input) {
        int count = 0;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '@') {
                count++;
            }
        }
        return count;
    }

    private boolean checkStringDoubleDots(String input) {
        return input.contains("..");
    }

    private boolean checkStringSpace(String input) {
        return input.contains(" ");
    }

    private boolean validaiteUsername() {
        String usernameInput = username.getEditText().getText().toString().trim();
        if (usernameInput.isEmpty()) {
            username.setError(getString(R.string.error_username));
            return false;
        } else {
            username.setError("");
            return true;
        }
    }

    private boolean validaitePassword() {
        String passwordInput = passwordText.getEditText().getText().toString().trim();
        if (passwordInput.isEmpty()) {
            passwordText.setError(getString(R.string.error_password_empty));
            return false;
        } else if (passwordInput.length() < 6) {
            passwordText.setError(getString(R.string.error_password));
            return false;
        } else {
            passwordText.setError("");
            return true;
        }
    }

    private boolean validaiteConfirmPassword() {
        String passwordInput = passwordText.getEditText().getText().toString().trim();
        String confirmPasswordInput = confirm_password.getEditText().getText().toString().trim();
        if (confirmPasswordInput.isEmpty()) {
            confirm_password.setError(getString(R.string.error_confirmpassw_empty));
            return false;
        } else if (confirmPasswordInput.length() < 6) {
            confirm_password.setError(getString(R.string.error_password));
            return false;
        } else if (!confirmPasswordInput.equals(passwordInput)) {
            confirm_password.setError(getString(R.string.errror_confirmpassw));
            return false;
        } else {
            confirm_password.setError("");
            return true;
        }
    }

    public void loginSignUp(View view) {
        String email = emailText.getEditText().getText().toString().trim();
        String password = passwordText.getEditText().getText().toString().trim();
        if (isLoginActivity) {
            if (validaiteEmail() & validaiteUsername() & validaitePassword() & validaiteConfirmPassword()) {
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    String usernameInput = username.getEditText().getText().toString().trim();
                                    Toast.makeText(LoginActivity.this, getString(R.string.user) + usernameInput + getString(R.string.success_signin), Toast.LENGTH_LONG).show();

                                    // Sign in success, update UI with the signed-in user's information
                                    Log.d("signup", "createUserWithEmail:success");
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    //updateUI(user);

                                    database = FirebaseDatabase.getInstance();
                                    myRef = database.getReference("users");
                                    myRef.child(user.getUid()).child("username").setValue(usernameInput);
                                    //myRef.child(user.getUid()).child("imageUrl").setValue(null);
                                    myRef.child(user.getUid()).child("date_time").setValue(String.valueOf(LocalDateTime.now()));

                                    User.username = username.getEditText().getText().toString().trim();
                                    User.userImage = null;

                                    sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE);
                                    editor = sharedPreferences.edit();
                                    editor.putString("username", User.username);
                                    editor.putString("userImage", User.userImage);
                                    editor.apply();

                                    Intent intent = new Intent(LoginActivity.this, ActivityProfile.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Log.w("signup", "createUserWithEmail:failure", task.getException());
                                    Toast.makeText(LoginActivity.this, getString(R.string.authentication_failed),
                                            Toast.LENGTH_SHORT).show();
                                    //updateUI(null);
                                }
                            }
                        });

            }

        } else {
            if (validaiteEmail() & validaitePassword()) {
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                Log.d("login", "signInWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                database = FirebaseDatabase.getInstance();
                                myRef = database.getReference("users");

                                myRef.child(user.getUid()).get().addOnCompleteListener(taskProfile -> {
                                    if (!taskProfile.isSuccessful()) {
                                        Log.e("firebase", getString(R.string.error_loaded), taskProfile.getException());
                                    } else {
                                        User.username = String.valueOf(taskProfile.getResult().child("username").getValue());
                                        User.userImage = String.valueOf(taskProfile.getResult().child("imageUrl").getValue());
                                        myRef.child(user.getUid()).child("date_time").setValue(String.valueOf(LocalDateTime.now()));

                                        sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE);
                                        editor = sharedPreferences.edit();
                                        editor.putString("username", User.username);
                                        editor.putString("userImage", User.userImage);
                                        editor.apply();

                                        getMyRecipe(LoginActivity.this, dishes -> {
                                            Intent intent = new Intent(LoginActivity.this, ActivityProfile.class);
                                            intent.putStringArrayListExtra("dish_list", dishes);
                                            startActivity(intent);
                                            finish();
                                        });
                                        Log.d("firebase", String.valueOf(taskProfile.getResult().getValue()));
                                    }
                                });

                                // 📌 Запрашиваем список рецептов через `Callback`

                            } else {
                                Log.w("login", "signInWithEmail:failure", task.getException());
                                Toast.makeText(LoginActivity.this, getString(R.string.authentication_failed), Toast.LENGTH_SHORT).show();
                            }
                        });
            }

        }
    }


    private void getMyRecipe(Context context, RecipeCallback callback) {
        /*FirebaseUser user = mAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("users").child(user.getUid()).child("my_recipes");
        ArrayList<Recipe> dishList = new ArrayList<>();

        myRef.addListenerForSingleValueEvent(new ValueEventListener() { // ✅ Используем `ListenerForSingleValueEvent`
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DatabaseHandler dbHelper = new DatabaseHandler(context);

                for (DataSnapshot recipeSnapshot : snapshot.getChildren()) {
                    String recipeId = recipeSnapshot.getKey();

                    // 🔥 Проверяем, есть ли рецепт в SQLite
                    if (!dbHelper.myRecipeInSQLite(recipeId)) {
                        Recipe recipe = new Recipe();
                        recipe.setId(recipeId);
                        recipe.setName(recipeSnapshot.child("name").getValue(String.class));
                        recipe.setName_en(recipeSnapshot.child("name_en").getValue(String.class));
                        recipe.setImage(recipeSnapshot.child("image").getValue(String.class));
                        recipe.setCookingTime(recipeSnapshot.child("cookingTime").getValue(Integer.class));
                        recipe.setIngredient(recipeSnapshot.child("ingredient").getValue(String.class));
                        recipe.setIngredient_en(recipeSnapshot.child("ingredient_en").getValue(String.class));
                        recipe.setRecipe(recipeSnapshot.child("recipe").getValue(String.class));
                        recipe.setRecipe_en(recipeSnapshot.child("recipe_en").getValue(String.class));
                        recipe.setIsFavorite(1);

                        dbHelper.insertOrUpdateRecipe(recipe); // ✅ Сохраняем в SQLite
                    }

                    // 📌 Добавляем рецепт в `dishList`
                    Recipe r = new Recipe();
                    r.setId(recipeId);
                    r.setRecipe(recipeSnapshot.child("name").getValue(String.class));
                    r.setRecipe_en(recipeSnapshot.child("name_en").getValue(String.class));
                    r.setImage(recipeSnapshot.child("image").getValue(String.class));
                    r.setCookingTime(recipeSnapshot.child("cookingTime").getValue(Integer.class));

                    if (!dishList.contains(r)) {
                        dishList.add(r);
                    }
                }

                SharedPreferences prefs = getSharedPreferences("MODE", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                Gson gson = new Gson();

                String dishJson = gson.toJson(dishList);
                editor.putString("dish_list", dishJson);
                editor.apply();
                callback.onRecipesLoaded(dishList); // ✅ Возвращаем данные через `Callback`
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", getString(R.string.error_loaded) + error.getMessage());
            }
        });*/

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {

            return;
        }

        DatabaseHandler dbHelper = new DatabaseHandler(context);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("users").child(user.getUid()).child("my_recipes");

        ArrayList<String> idList = new ArrayList<>();

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
                            idList.add(recipeId);
                        }
                    }

                    // Сохраняем список id в SharedPreferences как JSON (опционально)
                    SharedPreferences prefs = getSharedPreferences("MODE", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    Gson gson = new Gson();
                    String json = gson.toJson(idList);
                    editor.putString("dish_list", json);
                    editor.apply();

                    if (callback != null) callback.onRecipesLoaded(idList);
                } catch (Exception e) {
                    if (callback != null)
                        Log.e("Firebase", getString(R.string.error_loaded) );

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null)
                    Log.e("Firebase", getString(R.string.error_loaded) + error.getMessage());

            }
        });
    }


    public void toggleLoginSignUp(View view) {
        if (isLoginActivity) {
            isLoginActivity = false;
            username.setVisibility(View.GONE);
            confirm_password.setVisibility(View.GONE);
            auth_title.setText(getString(R.string.log_in));
            toggleLoginSignUp.setText(getString(R.string.sign_in));
            login_button.setText(R.string.enter_account);

        } else {
            isLoginActivity = true;
            username.setVisibility(View.VISIBLE);
            confirm_password.setVisibility(View.VISIBLE);
            auth_title.setText(getString(R.string.registration));
            toggleLoginSignUp.setText(getString(R.string.log_in));
            login_button.setText(getString(R.string.sign_in));
        }
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
package com.example.recipeguide;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Handler;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import org.json.JSONArray;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import Data.DatabaseHandler;
import Model.Recipe;
import Utils.FileUtils;


public class AddScreen extends AppCompatActivity {


    private static final int PICK_IMAGE_REQUEST = 1; // Код запроса для галереи
    // Размеры входного изображения, используемые при обучении модели: ширина = 169, высота = 274
    private static final int MODEL_INPUT_WIDTH = 274;
    private static final int MODEL_INPUT_HEIGHT = 169;
    // Порог реконструкционной ошибки – подбирается экспериментально (примерное значение)
    private static final float THRESHOLD = 0.009f;

    private Interpreter tflite;
    ProgressBar uploadProgressBar;
    private EditText recipeNameEditText;
    private EditText preparationTimeEditText;
    private Spinner spinner;
    private String recipeNameTranslate, ingredientDataTranslate, recipeDataTranslate;
    private String photoFileName; // Название сохранённого файла
    private ImageButton btnAddImage;
    private Bitmap selectedBitmap; // Для хранения выбранного изображения
    private IngredientsFragmentForAddScreen ingredientFragment = new IngredientsFragmentForAddScreen();
    private RecipeFragmentForAddScreen recipeFragment = new RecipeFragmentForAddScreen();
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private FirebaseAuth mAuth;
    FirebaseDatabase database;
    DatabaseReference myRef;
    Uri imageUri;
    SharedPreferences sharedPreferences;
    final String[] translated = new String[6];
    BaseAdActivity baseAdActivity;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_screen);

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.addScreen), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        baseAdActivity = new BaseAdActivity(
                this,
                R.id.addScreen,
                R.id.ad_container_view,
                "demo-banner-yandex"
        );
        baseAdActivity.load();

        /*List<String> categories = new ArrayList<>();
        categories.add("Выбрать");             // хинт
        categories.add("Закуски");
        categories.add("Суп");
        categories.add("Салат");
        categories.add("Основное блюдо");
        categories.add("Гарнир");
        categories.add("Десерты");
        categories.add("Напиток");
        categories.add("Соусы и приправы");
        categories.add("Выпечка");
        categories.add("Перекус");*/
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                R.layout.spinner_item,
                R.id.spinner_item_tv,
                Arrays.asList(getResources().getStringArray(R.array.categories_array))
        ) {
            @Override
            public boolean isEnabled(int position) {
                // запрещаем выбор заглушки
                return position != 0;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if (position == 0) {
                    // можно затемнить хинт, но оставляем основной цвет
                    tv.setTextColor(getResources().getColor(R.color.black));
                } else {
                    tv.setTextColor(getResources().getColor(R.color.black));
                }
                return view;
            }
        };

// 3) Привязываем адаптер и устанавливаем начальное состояние
        spinner = findViewById(R.id.spinner_category);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0, false);  // показываем «Выбрать» без вызова слушателя
// Сбрасываем красную обводку при первом касании
        spinner.setOnTouchListener((v, event) -> {
            spinner.setBackgroundResource(R.drawable.spinner_bg_with_arrow);
            return false;
        });
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                TextView tv = (TextView) view.findViewById(R.id.spinner_item_tv);
                if (tv == null) return;

                // Измеряем ширину текста
                float textWidth = tv.getPaint().measureText(tv.getText().toString());
                // Дополнительно учитываем отступы/паддинги
                int totalWidth = (int) (textWidth + tv.getPaddingLeft() + tv.getPaddingRight() + 100);

                // Устанавливаем новую ширину Spinner
                ViewGroup.LayoutParams lp = spinner.getLayoutParams();
                lp.width = totalWidth;
                spinner.setLayoutParams(lp);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        try {
            tflite = new Interpreter(loadModelFile(this, "final_autoencoder.tflite"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        DatabaseHandler databaseHelper = new DatabaseHandler(this);
        sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE);

        mAuth = FirebaseAuth.getInstance();

        setupBackButtonHandler();
        photoFromGallery();

        uploadProgressBar = findViewById(R.id.uploadProgressBar);
        recipeNameEditText = findViewById(R.id.recipe_name);
        preparationTimeEditText = findViewById(R.id.preparation_time);
        btnAddImage = findViewById(R.id.button_add_photo);
        btnAddImage.setOnClickListener(v -> openImageChooser());
        recipeNameEditText.setMovementMethod(new ScrollingMovementMethod());
        recipeNameEditText.post(() -> scrollingText(recipeNameEditText));

        Button buttonSave = findViewById(R.id.button_save);

        Button ingredientButton = findViewById(R.id.ingredient);
        Button recipeButton = findViewById(R.id.recipe);

        setNewFragment(recipeFragment, "recipe_fragment", false);
        setNewFragment(ingredientFragment, "ingredients_fragment", true);
        //getSupportFragmentManager().beginTransaction().add(recipeFragment, "recipeFragment").commit();
        ingredientButton.setBackgroundResource(R.drawable.rounded_button_focused);

        ingredientButton.setOnClickListener(v -> {
            ingredientButton.setBackgroundResource(R.drawable.rounded_button_focused);
            recipeButton.setBackgroundResource(R.drawable.rounded_button_default);
            setNewFragment(ingredientFragment, "ingredients_fragment", true);
        });

        recipeButton.setOnClickListener(v -> {
            recipeButton.setBackgroundResource(R.drawable.rounded_button_focused);
            ingredientButton.setBackgroundResource(R.drawable.rounded_button_default);
            setNewFragment(recipeFragment, "recipe_fragment", true);
        });

        buttonSave.setOnClickListener(v -> saveImageToInternalStorage(databaseHelper));

    }


    // Открытие галереи для выбора изображения
    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Выберите изображение"));
    }

    private void photoFromGallery() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        imageUri = result.getData().getData(); // Получаем URI выбранного изображения
                        loadImageFromUri(imageUri);
                    }
                }
        );
    }

    private void loadImageFromUri(Uri imageUri) {
        try {
            selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri); // Загружаем Bitmap
            btnAddImage.setImageBitmap(selectedBitmap); // Устанавливаем в ImageView
            btnAddImage.setBackgroundResource(android.R.color.transparent);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_load_image, Toast.LENGTH_SHORT).show();
        }
    }

    private String imageUrl() {
        final String[] imageUrl = new String[1];
        ExecutorService imageUploadExecutor = Executors.newSingleThreadExecutor();

        imageUploadExecutor.execute(() -> {
            MediaManager.get().upload(imageUri).option("folder", "recipe_image").callback(new UploadCallback() {
                @Override
                public void onStart(String requestId) {
                    uploadProgressBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void onProgress(String requestId, long bytes, long totalBytes) {

                }

                @Override
                public void onSuccess(String requestId, Map resultData) {
                    imageUrl[0] = resultData.get("secure_url").toString(); // Ссылка на картинку
                    uploadProgressBar.setVisibility(View.GONE);


                }

                @Override
                public void onError(String requestId, ErrorInfo error) {
                    Log.e("Cloudinary", "Ошибка загрузки: " + error.getDescription());
                }

                @Override
                public void onReschedule(String requestId, ErrorInfo error) {
                }
            }).dispatch();
        });
        return imageUrl[0].toString();
    }

    public void translateRecipeData(String recipeName, String ingredientsData, String recipeData,
                                    boolean isRussian, TranslationCallback callback) {
        ExecutorService translationExecutor = Executors.newSingleThreadExecutor();

        translationExecutor.execute(() -> {
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(isRussian ? TranslateLanguage.RUSSIAN : TranslateLanguage.ENGLISH)
                    .setTargetLanguage(isRussian ? TranslateLanguage.ENGLISH : TranslateLanguage.RUSSIAN)
                    .build();

            Translator translator = Translation.getClient(options);

            translator.downloadModelIfNeeded()
                    .addOnSuccessListener(unused -> {
                        List<String> translatedList = new ArrayList<>();
                        translatedList.add(recipeName);  // Оригинальное название
                        translatedList.add(ingredientsData);  // Оригинальные ингредиенты
                        translatedList.add(recipeData);  // Оригинальный рецепт

                        translator.translate(recipeName)
                                .addOnSuccessListener(translatedText -> translatedList.add(translatedText));

                        translator.translate(ingredientsData)
                                .addOnSuccessListener(translatedText -> translatedList.add(translatedText));

                        translator.translate(recipeData)
                                .addOnSuccessListener(translatedText -> translatedList.add(translatedText))
                                .addOnSuccessListener(unused2 -> {
                                    callback.onTranslationComplete(translatedList);
                                });
                    })
                    .addOnFailureListener(e -> Log.e("MLKit", getString(R.string.error_translate) + e.getMessage()));
        });
    }


    private void saveImageToInternalStorage(DatabaseHandler databaseHandler) {
        boolean isRussian = sharedPreferences.getBoolean("language", false);


        String recipeName = recipeNameEditText.getText().toString().trim();
        String ingredientsData = ingredientFragment != null ? ingredientFragment.getIngredientsData() : "";
        String recipeData = recipeFragment != null ? recipeFragment.getRecipeData() : "";
        translateRecipeData(recipeName, ingredientsData, recipeData, isRussian, translatedList -> {
            if (isRussian) {
                translated[0] = translatedList.get(0);
                translated[1] = translatedList.get(1);
                translated[2] = translatedList.get(2);
                translated[3] = translatedList.get(3);
                translated[4] = translatedList.get(4);
                translated[5] = translatedList.get(5);

            } else {
                translated[0] = translatedList.get(3);
                translated[1] = translatedList.get(4);
                translated[2] = translatedList.get(5);
                translated[3] = translatedList.get(0);
                translated[4] = translatedList.get(1);
                translated[5] = translatedList.get(2);
            }
            if (validateInputs()) {
                String recipeId = UUID.randomUUID().toString();
                if (validateImage()) {
                    saveData(databaseHandler, recipeId, translated);
                    saveDataFirebaseAllRecipe(recipeId, translated);
                    Toast.makeText(AddScreen.this, R.string.good_save, Toast.LENGTH_LONG).show();

                } else {
                    Toast.makeText(AddScreen.this, R.string.validation_no_image, Toast.LENGTH_LONG).show();
                }
            } else {
                // Show an error message to the user
                Toast.makeText(AddScreen.this, getString(R.string.validation), Toast.LENGTH_SHORT).show();
            }
        });

       /* new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (validateInputs()) {
                    String recipeId = UUID.randomUUID().toString();

            */
        /*translateRecipeData(recipeName, ingredientsData, recipeData, isRussian, translatedList -> {
                if (isRussian) {
                    translated[0] = translatedList.get(0);
                    translated[1] = translatedList.get(1);
                    translated[2] = translatedList.get(2);
                    translated[3] = translatedList.get(3);
                    translated[4] = translatedList.get(4);
                    translated[5] = translatedList.get(5);

                } else {
                    translated[0] = translatedList.get(3);
                    translated[1] = translatedList.get(4);
                    translated[2] = translatedList.get(5);
                    translated[3] = translatedList.get(0);
                    translated[4] = translatedList.get(1);
                    translated[5] = translatedList.get(2);
                }

                Log.d("Translation", "Оригинал: " + translated[0]);
            });*/
        /*
                    //if(isInternetAvailable(AddScreen.this)) {

                    //String imageUrl = imageUrl();



                    //showPublicRecipe(recipeId, translated); // ✅ Вызываем диалог в UI-потоке
                    //}


                    //}else{
                    //saveData(databaseHandler, recipeId); // ✅ Выполняется после диалога
                    //Toast.makeText(AddScreen.this, getString(R.string.recipe_save_toast), Toast.LENGTH_SHORT).show();
                    //}


                } else {
                    // Show an error message to the user
                    Toast.makeText(AddScreen.this, getString(R.string.validation), Toast.LENGTH_SHORT).show();
                }
            }
        }, 1000);
*/
    }

    /*private void showPublicRecipe(String recipeId, String[] translated) {
        ExecutorService imageUploadExecutor = Executors.newSingleThreadExecutor();

        //imageUploadExecutor.execute(() -> {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Публикация");
        builder.setMessage("Хотите опубликовать свой рецепт?");

        builder.setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
            saveDataFirebaseAllRecipe(recipeId, translated);
            //saveDataFirebaseMyRecipe(recipeId, translated);
            //dialog.dismiss();
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
            saveDataFirebaseMyRecipe(recipeId, translated);
            dialog.dismiss();
        });
        AlertDialog dialog = builder.create();
        *//*dialog.setOnDismissListener(d -> {
            saveData(databaseHandler, recipeId); // ✅ Выполняется после диалога
            Toast.makeText(AddScreen.this, getString(R.string.recipe_save_toast), Toast.LENGTH_SHORT).show();
        });*//*
        dialog.show();
        //});
    }*/

    private boolean validateImage() {
        float[][][][] inputImage = convertBitmapToFloatArray();

        // Создаём массив для результата инференса с размерами, соответствующими входным
        float[][][][] outputImage = new float[1][MODEL_INPUT_HEIGHT][MODEL_INPUT_WIDTH][3];

        // Выполняем инференс
        tflite.run(inputImage, outputImage);

        // Вычисляем среднеквадратичную ошибку (MSE) между входным и восстановленным изображениями
        float error = calculateMSE(inputImage, outputImage);
        Log.d("DishClassifier", "Reconstruction error: " + error);

        // Если ошибка реконструкции ниже порога, считаем, что изображение – блюдо
        if (error < THRESHOLD) {
            Log.d("DishClassifier", "Изображение: блюдо");
            return true;
        } else {
            Log.d("DishClassifier", "Изображение: не блюдо");
            return false;
        }
    }

    private void saveDataFirebaseMyRecipe(String recipeId, String[] translated) {

        String recipeName = recipeNameEditText.getText().toString().trim();
        String ingredientsData = ingredientFragment != null ? ingredientFragment.getIngredientsData() : "";
        String recipeData = recipeFragment != null ? recipeFragment.getRecipeData() : "";
        int preparationTime = Integer.parseInt(preparationTimeEditText.getText().toString().trim());

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE);
            database = FirebaseDatabase.getInstance();
            myRef = database.getReference("users");

            boolean isRussian = sharedPreferences.getBoolean("language", false);
            translateRecipeData(recipeName, ingredientsData, recipeData, isRussian, translatedList -> {
                if (isRussian) {
                    translated[0] = translatedList.get(0);
                    translated[1] = translatedList.get(1);
                    translated[2] = translatedList.get(2);
                    translated[3] = translatedList.get(3);
                    translated[4] = translatedList.get(4);
                    translated[5] = translatedList.get(5);

                } else {
                    translated[0] = translatedList.get(3);
                    translated[1] = translatedList.get(4);
                    translated[2] = translatedList.get(5);
                    translated[3] = translatedList.get(0);
                    translated[4] = translatedList.get(1);
                    translated[5] = translatedList.get(2);
                }

                Log.d("Translation", "Оригинал: " + translated[0]);
            });
            myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("name").setValue(translated[0]);
            myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("name_en").setValue(translated[3]);
            myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("cookingTime").setValue(preparationTime);
            //myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("image").setValue(imageUrl);
            myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("recipe").setValue(translated[2]);
            myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("recipe_en").setValue(translated[5]);
            myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("ingredient").setValue(translated[1]);
            myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("ingredient_en").setValue(translated[4]);
            myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("category").setValue(spinner.getSelectedItemPosition() - 1);
            myRef.child(user.getUid()).child("isFavorites").child(recipeId).setValue(true);


            // 🔹 ML Kit Перевод
        /*TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sharedPreferences.getBoolean("language", false) ?
                        TranslateLanguage.RUSSIAN : TranslateLanguage.ENGLISH)
                .setTargetLanguage(sharedPreferences.getBoolean("language", false) ?
                        TranslateLanguage.ENGLISH : TranslateLanguage.RUSSIAN)
                .build();

        Translator translator = Translation.getClient(options);
*/

            ExecutorService imageUploadExecutor = Executors.newSingleThreadExecutor();

            imageUploadExecutor.execute(() -> {
                MediaManager.get().upload(imageUri).option("folder", "recipe_image").callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {

                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {

                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = resultData.get("secure_url").toString(); // Ссылка на картинку
                        myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("image").setValue(imageUrl.toString());

                        //dish.setRecipeImage(imageUrl);

                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e("Cloudinary", "Ошибка загрузки: " + error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                    }
                }).dispatch();
            });

/*
            if (sharedPreferences.getBoolean("language", false)) {

                TranslatorOptions ruToEnOptions = new TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.RUSSIAN)
                        .setTargetLanguage(TranslateLanguage.ENGLISH)
                        .build();
                Translator translator = Translation.getClient(ruToEnOptions);
                translator.downloadModelIfNeeded()
                        .addOnSuccessListener(unused -> {
                            translator.translate(recipeName)
                                    .addOnSuccessListener(translatedText -> {
                                        myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("name_en").setValue(translatedText);
                                        //dish.setRecipeNameEn(translatedText);
                                    });

                            translator.translate(ingredientsData)
                                    .addOnSuccessListener(translatedText ->
                                            myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("ingredient_en").setValue(translatedText));

                            translator.translate(recipeData)
                                    .addOnSuccessListener(translatedText ->
                                            myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("recipe_en").setValue(translatedText));
                        })
                        .addOnFailureListener(e -> Log.e("MLKit", "Ошибка загрузки модели перевода: " + e.getMessage()));

                myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("name").setValue(recipeName);
                myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("recipe").setValue(recipeData);
                myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("ingredient").setValue(ingredientsData);

                //dish.setRecipeName(recipeName);
            } else {
                TranslatorOptions ruToEnOptions = new TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.ENGLISH)
                        .setTargetLanguage(TranslateLanguage.RUSSIAN)
                        .build();
                Translator translator = Translation.getClient(ruToEnOptions);
                translator.downloadModelIfNeeded()
                        .addOnSuccessListener(unused -> {
                            translator.translate(recipeName)
                                    .addOnSuccessListener(translatedText -> {
                                        myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("name").setValue(translatedText);
                                        //dish.setRecipeName(translatedText);

                                    });
                            translator.translate(ingredientsData)
                                    .addOnSuccessListener(translatedText ->
                                            myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("ingredient").setValue(translatedText));

                            translator.translate(recipeData)
                                    .addOnSuccessListener(translatedText ->
                                            myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("recipe").setValue(translatedText));
                        })
                        .addOnFailureListener(e -> Log.e("MLKit", "Ошибка загрузки модели перевода: " + e.getMessage()));

                myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("name_en").setValue(recipeName);
                myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("recipe_en").setValue(recipeData);
                myRef.child(user.getUid()).child("my_recipes").child(recipeId).child("ingredient_en").setValue(ingredientsData);

                //dish.setRecipeNameEn(recipeName);

            }*/



            /*SharedPreferences prefs = getSharedPreferences("MODE", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            Gson gson = new Gson();

            String dishJson = gson.toJson(dishList);
            editor.putString("dish_list", dishJson);
            editor.apply();
            if (!dishList.contains(dish)) {
                dishList.add(dish);
            }*/
        }


    }

    private void saveDataFirebaseAllRecipe(String recipeId, String[] translated) {

        ExecutorService dataUploadExecutor = Executors.newSingleThreadExecutor();
        int preparationTime = Integer.parseInt(preparationTimeEditText.getText().toString().trim());

        CountDownLatch latch = new CountDownLatch(1);
        FirebaseUser user = mAuth.getCurrentUser();

        mAuth = FirebaseAuth.getInstance();

        sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE);
        database = FirebaseDatabase.getInstance();

        myRef = database.getReference("recipes");

        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        File compressedFile = new File(getCacheDir(), "compressed_image.jpg");

        try (FileOutputStream fos = new FileOutputStream(compressedFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos); // ✅ Сжимаем изображение до 80%
        } catch (IOException e) {
            Log.e("ImageCompression", "Ошибка сжатия: " + e.getMessage());
            Toast.makeText(this, "Ошибка сжатия изображения", Toast.LENGTH_SHORT).show();

        }
        Uri compressedImageUri = Uri.fromFile(compressedFile);
        //Tasks.whenAllComplete().addOnSuccessListener(unused -> {
        ExecutorService imageUploadExecutor = Executors.newSingleThreadExecutor();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        imageUploadExecutor.execute(() -> {
            MediaManager.get().upload(compressedImageUri).option("folder", "recipe_image").callback(new UploadCallback() {
                @Override
                public void onStart(String requestId) {
                    uploadProgressBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void onProgress(String requestId, long bytes, long totalBytes) {

                }

                @Override
                public void onSuccess(String requestId, Map resultData) {
                    String LOG_TAG = "TFIDF";
                    executor.execute(() -> {
                        try {
                            // 1) скопировать артефакты из assets в filesDir
                            String modelAsset = "models/tfidf_word2vec_w2v.model";
                            String idfAsset = "models/tfidf_word2vec_tfidf_idf.joblib";
                            String metaAsset = "models/tfidf_word2vec_tfidf_meta.json";
                            String npzAsset = "models/word_vectors.npz";
                            String modelPath = FileUtils.copyAssetToFiles(AddScreen.this, modelAsset, "tfidf_word2vec_w2v.model");
                            String idfPath = FileUtils.copyAssetToFiles(AddScreen.this, idfAsset, "tfidf_word2vec_tfidf_idf.joblib");
                            String metaPath = FileUtils.copyAssetToFiles(AddScreen.this, metaAsset, "tfidf_word2vec_tfidf_meta.json");
                            String word2vecPath = FileUtils.copyAssetToFiles(AddScreen.this, npzAsset, "word_vectors.npz");

                            String startMsg = String.format("Assets copied. model=%s, idf=%s, meta=%s, npz=%s", modelPath, idfPath, metaPath, word2vecPath);
                            Log.i(LOG_TAG, startMsg);

                            // tfidf_prefix — путь без суффикса "_idf.joblib"
                            String tfidfPrefix = new File(getFilesDir(), "tfidf_word2vec_tfidf").getAbsolutePath();
                            // (joblib файл уже скопирован как tfidf_word2vec_tfidf_idf.joblib)
                            // 2) подготовить вход (вместо user_input — данные из Java)
                            // 3) вызвать Python функцию parse_and_vectorize
                            Python py = Python.getInstance();
                            PyObject module = py.getModule("tfidf_android"); // имя файла tfidf_android.py
                            PyObject pyResult = module.callAttr("parse_and_vectorize", word2vecPath, tfidfPrefix, translated[4]);
                            Log.i(LOG_TAG, "Python call finished in ms");
                            String jsonStr = pyResult.toString();
                            // 4) распарсить JSON
                            JSONObject jo = new JSONObject(jsonStr);
                            JSONArray parsedArr = jo.getJSONArray("parsed");
                            JSONArray tokensArr = jo.getJSONArray("tokens");

                            JSONArray vecArr = jo.getJSONArray("vector");
                            // распарсенные элементы из Python

                            // --- parsed как List<String> из JSON ---
                            List<String> parsed = new ArrayList<>();
                            for (int i = 0; i < parsedArr.length(); i++)
                                parsed.add(parsedArr.getString(i));

                            // Формируем строку вида ['a', 'b', 'c']
                            StringBuilder parsedSb = new StringBuilder();
                            parsedSb.append("[");
                            for (int i = 0; i < parsed.size(); i++) {
                                if (i > 0) parsedSb.append(", ");
                                // добавляем элемент в одинарных кавычках, экранируя одинарные кавычки внутри строки
                                String item = parsed.get(i).replace("'", "\\'");
                                parsedSb.append("'");
                                parsedSb.append(item);
                                parsedSb.append("'");
                            }
                            parsedSb.append("]");
                            String parsedString = parsedSb.toString(); // пример: ['chicken', 'acorn squash', ...]

                            // --- vector: из JSONArray в double[] и List<Double> ---
                            double[] vector = new double[vecArr.length()];
                            List<Double> vectorList = new ArrayList<>(vecArr.length());
                            for (int i = 0; i < vecArr.length(); i++) {
                                double v = vecArr.getDouble(i);
                                vector[i] = v;
                                vectorList.add(v);
                            }

                            // --- (опционально) строковое JSON-представление вектора, если нужно как текст ---
                            // но для RTDB достаточно vectorList; ниже показано, как получить строку "[0.1,0.2,...]"
                            StringBuilder vSb = new StringBuilder();
                            vSb.append("[");
                            for (int i = 0; i < vectorList.size(); i++) {
                                if (i > 0) vSb.append(",");
                                vSb.append(Double.toString(vectorList.get(i)));
                            }
                            vSb.append("]");
                            String vectorJsonString = vSb.toString();

                            myRef.child(recipeId).child("ingredients_parsed").setValue(parsedString);
                            myRef.child(recipeId).child("ingredient_vectors").setValue(vectorJsonString);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e("TFIDF", "Exception in executor: " + Log.getStackTraceString(e));
                            Log.d("TAG", e.getMessage());
                            runOnUiThread(() -> Toast.makeText(AddScreen.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });
                    String imageUrl = resultData.get("secure_url").toString(); // Ссылка на картинку
                    myRef.child(recipeId).child("image").setValue(imageUrl.toString());
                    myRef.child(recipeId).child("name").setValue(translated[0]);
                    myRef.child(recipeId).child("name_en").setValue(translated[3]);
                    myRef.child(recipeId).child("cookingTime").setValue(preparationTime);
                    //myRef.child(recipeId).child("image").setValue(imageUrl);
                    myRef.child(recipeId).child("recipe").setValue(translated[2]);
                    myRef.child(recipeId).child("recipe_en").setValue(translated[5]);
                    myRef.child(recipeId).child("ingredient").setValue(translated[1]);
                    myRef.child(recipeId).child("ingredient_en").setValue(translated[4]);
                    myRef.child(recipeId).child("category").setValue(spinner.getSelectedItemPosition() - 1);


                    uploadProgressBar.setVisibility(View.GONE);
                    //latch.countDown();


                }

                @Override
                public void onError(String requestId, ErrorInfo error) {
                    Log.e("Cloudinary", "Ошибка загрузки: " + error.getDescription());
                    //latch.countDown();
                }

                @Override
                public void onReschedule(String requestId, ErrorInfo error) {
                }
            }).dispatch();
        });
        //}).addOnFailureListener(e -> Log.e("Firebase", "Ошибка сохранения данных: " + e.getMessage()));

        // 🔹 Запускаем загрузку изображения в отдельном потоке
        /*dataUploadExecutor.execute(() -> {
            try {
                latch.await(); // 🔥 Задержка 2 секунды (может блокировать поток)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/


        //boolean isRussian = sharedPreferences.getBoolean("language", false);
        /*translateRecipeData(recipeName, ingredientsData, recipeData, isRussian, translatedList -> {
            if (isRussian) {
                translated[0] = translatedList.get(0);
                translated[1] = translatedList.get(1);
                translated[2] = translatedList.get(2);
                translated[3] = translatedList.get(3);
                translated[4] = translatedList.get(4);
                translated[5] = translatedList.get(5);

            } else {
                translated[0] = translatedList.get(3);
                translated[1] = translatedList.get(4);
                translated[2] = translatedList.get(5);
                translated[3] = translatedList.get(0);
                translated[4] = translatedList.get(1);
                translated[5] = translatedList.get(2);
            }*/


            /*Log.d("Translation", "Оригинал: " + translated[0]);
        });*/
        // 🔹 ML Kit Перевод
       /* TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sharedPreferences.getBoolean("language", false) ?
                        TranslateLanguage.RUSSIAN : TranslateLanguage.ENGLISH)
                .setTargetLanguage(sharedPreferences.getBoolean("language", false) ?
                        TranslateLanguage.ENGLISH : TranslateLanguage.RUSSIAN)
                .build();

        Translator translator = Translation.getClient(options);*/

        /*if (sharedPreferences.getBoolean("language", false)) {

            translationExecutor.execute(() -> {
                TranslatorOptions ruToEnOptions = new TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.RUSSIAN)
                        .setTargetLanguage(TranslateLanguage.ENGLISH)
                        .build();
                Translator translator = Translation.getClient(ruToEnOptions);
                translator.downloadModelIfNeeded()
                        .addOnSuccessListener(unused -> {
                            translator.translate(recipeName)
                                    .addOnSuccessListener(translatedText ->
                                            myRef.child(recipeId).child("name_en").setValue(translatedText));

                            translator.translate(ingredientsData)
                                    .addOnSuccessListener(translatedText ->
                                            myRef.child(recipeId).child("ingredient_en").setValue(translatedText));

                            translator.translate(recipeData)
                                    .addOnSuccessListener(translatedText ->
                                            myRef.child(recipeId).child("recipe_en").setValue(translatedText));
                        })
                        .addOnFailureListener(e -> Log.e("MLKit", "Ошибка загрузки модели перевода: " + e.getMessage()));

                myRef.child(recipeId).child("name").setValue(recipeName);
                myRef.child(recipeId).child("recipe").setValue(recipeData);
                myRef.child(recipeId).child("ingredient").setValue(ingredientsData);
            });
        } else {
            translationExecutor.execute(() -> {
                TranslatorOptions ruToEnOptions = new TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.ENGLISH)
                        .setTargetLanguage(TranslateLanguage.RUSSIAN)
                        .build();
                Translator translator = Translation.getClient(ruToEnOptions);
                translator.downloadModelIfNeeded()
                        .addOnSuccessListener(unused -> {
                            translator.translate(recipeName)
                                    .addOnSuccessListener(translatedText ->
                                            myRef.child(recipeId).child("name").setValue(translatedText));

                            translator.translate(ingredientsData)
                                    .addOnSuccessListener(translatedText ->
                                            myRef.child(recipeId).child("ingredient").setValue(translatedText));

                            translator.translate(recipeData)
                                    .addOnSuccessListener(translatedText ->
                                            myRef.child(recipeId).child("recipe").setValue(translatedText));
                        })
                        .addOnFailureListener(e -> Log.e("MLKit", "Ошибка загрузки модели перевода: " + e.getMessage()));

                myRef.child(recipeId).child("name_en").setValue(recipeName);
                myRef.child(recipeId).child("recipe_en").setValue(recipeData);
                myRef.child(recipeId).child("ingredient_en").setValue(ingredientsData);
            });
        }*/


        if (user != null) {
            database.getReference("users").child(user.getUid()).child("isFavorites").child(recipeId).setValue(true);
            database.getReference("users").child(user.getUid()).child("my_recipes").child(recipeId).setValue(true);
        }
        //});
    }

    private void saveData(DatabaseHandler databaseHandler, String recipeId, String[] translated) {

        sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE);

        String recipeName = recipeNameEditText.getText().toString().trim();
        String ingredientsData = ingredientFragment != null ? ingredientFragment.getIngredientsData() : "";
        String recipeData = recipeFragment != null ? recipeFragment.getRecipeData() : "";
        int preparationTime = Integer.parseInt(preparationTimeEditText.getText().toString().trim());

        boolean isRussian = sharedPreferences.getBoolean("language", false);

        // Генерация имени файла
        photoFileName = getFilesDir() + "/saved_image_" + System.currentTimeMillis() + ".png";

        // Сохранение изображения
        File file = new File(photoFileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos); // Сохраняем изображение

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.error_load_image), Toast.LENGTH_SHORT).show();
        }
        /*translateRecipeData(recipeName, ingredientsData, recipeData, isRussian, translatedList -> {
            if (isRussian) {
                translated[0] = translatedList.get(0);
                translated[1] = translatedList.get(1);
                translated[2] = translatedList.get(2);
                translated[3] = translatedList.get(3);
                translated[4] = translatedList.get(4);
                translated[5] = translatedList.get(5);

            } else {
                translated[0] = translatedList.get(3);
                translated[1] = translatedList.get(4);
                translated[2] = translatedList.get(5);
                translated[3] = translatedList.get(0);
                translated[4] = translatedList.get(1);
                translated[5] = translatedList.get(2);
            }*/

        Recipe dish = new Recipe();
        ArrayList<String> dishes = new ArrayList<>();

        SharedPreferences prefs = getSharedPreferences("MODE", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String dishJson = prefs.getString("dish_list", null);
        Gson gson = new Gson();

        if (dishJson != null) {

            dishes = gson.fromJson(dishJson, new TypeToken<ArrayList<String>>() {
            }.getType());
            Log.d("ActivityProfile", "Загружено рецептов: " + dishes.size());
        }

        /*dish.setName(translated[0]);
        dish.setName_en(translated[3]);
        dish.setImage(photoFileName);
        dish.setId(recipeId);
        dish.setCookingTime(preparationTime);
*/
        dishes.add(recipeId);

        dishJson = gson.toJson(dishes);
        editor.putString("dish_list", dishJson);
        editor.apply();
        Log.d("Translation", "Оригинал: " + translated[0]);

        databaseHandler.insertEvent(java.util.UUID.randomUUID().toString(), User.username, "create", recipeId, System.currentTimeMillis());
//ДОБАВИТЬ КАТЕГОРИИ!!!
        Recipe recipe = new Recipe(recipeId, translated[0], translated[3], photoFileName, preparationTime, translated[2], translated[5], translated[1], translated[4], 1, 0, spinner.getSelectedItemPosition() - 1, null, null);

        databaseHandler.addRecipe(recipe);
        //});
/*

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sharedPreferences.getBoolean("language", false) ?
                        TranslateLanguage.RUSSIAN : TranslateLanguage.ENGLISH)
                .setTargetLanguage(sharedPreferences.getBoolean("language", false) ?
                        TranslateLanguage.ENGLISH : TranslateLanguage.RUSSIAN)
                .build();
*/

       /* if (sharedPreferences.getBoolean("language", false)) {

            TranslatorOptions ruToEnOptions = new TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.RUSSIAN)
                    .setTargetLanguage(TranslateLanguage.ENGLISH)
                    .build();
            Translator translator = Translation.getClient(ruToEnOptions);
            translator.downloadModelIfNeeded()
                    .addOnSuccessListener(unused -> {
                        translator.translate(recipeName)
                                .addOnSuccessListener(translatedText ->
                                        recipeNameTranslate = translatedText);

                        translator.translate(ingredientsData)
                                .addOnSuccessListener(translatedText ->
                                        ingredientDataTranslate = translatedText);

                        translator.translate(recipeData)
                                .addOnSuccessListener(translatedText ->
                                        recipeDataTranslate = translatedText);
                    })
                    .addOnFailureListener(e -> Log.e("MLKit", "Ошибка загрузки модели перевода: " + e.getMessage()));

        } else {
            TranslatorOptions ruToEnOptions = new TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(TranslateLanguage.RUSSIAN)
                    .build();
            Translator translator = Translation.getClient(ruToEnOptions);
            translator.downloadModelIfNeeded()
                    .addOnSuccessListener(unused -> {
                        translator.translate(recipeName)
                                .addOnSuccessListener(translatedText ->
                                        recipeNameTranslate = translatedText);

                        translator.translate(ingredientsData)
                                .addOnSuccessListener(translatedText ->
                                        ingredientDataTranslate = translatedText);

                        translator.translate(recipeData)
                                .addOnSuccessListener(translatedText ->
                                        recipeDataTranslate = translatedText);
                    })
                    .addOnFailureListener(e -> Log.e("MLKit", "Ошибка загрузки модели перевода: " + e.getMessage()));

        }*/


        //Recipe recipe = new Recipe(recipeId, recipeName, photoFileName, preparationTime, recipeData, ingredientsData, 1);

    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Проверка имени рецепта
        if (recipeNameEditText.getText().toString().trim().isEmpty()) {
            recipeNameEditText.setBackgroundResource(R.drawable.error_background_with_border); // Красный контур
            isValid = false;
        } else {
            recipeNameEditText.setBackgroundResource(R.color.background_add_screen); // Сбрасываем ошибку
        }

        if (spinner.getSelectedItemPosition() == 0) {
            spinner.setBackgroundResource(R.drawable.spinner_bg_error); // Красная линия ввода текста
            isValid = false;
        } else {
            spinner.setBackgroundResource(R.drawable.spinner_bg_with_arrow); // Сбрасываем ошибку
        }
        // Проверка времени приготовления
        if (preparationTimeEditText.getText().toString().trim().isEmpty()) {
            preparationTimeEditText.setBackgroundResource(R.drawable.error_background_with_border); // Красная линия ввода текста
            isValid = false;
        } else {
            preparationTimeEditText.setBackgroundResource(R.color.background_add_screen); // Сбрасываем ошибку
        }

        // Проверка изображения
        if (selectedBitmap == null) { // Проверка наличия изображения
            btnAddImage.setBackgroundResource(R.drawable.error_underline);
            isValid = false;
        } else {
            btnAddImage.setBackgroundResource(android.R.color.transparent); // Сбрасываем ошибку
        }

        if (!ingredientFragment.validateInputs()) {
            ingredientFragment.errorInputs();
            isValid = false;
        } else {
            ingredientFragment.goodInputs();
        }

        if (recipeFragment.validateInputs()) {
            recipeFragment.errorInputs();
            isValid = false;
        } else {
            recipeFragment.goodInputs();
        }

        return isValid;
    }

    private MappedByteBuffer loadModelFile(Activity activity, String modelFilename) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Преобразует Bitmap в массив формата [1][MODEL_INPUT_HEIGHT][MODEL_INPUT_WIDTH][3].
     * Изображение масштабируется до 169x274 и нормализуется (значения пикселей от 0 до 1).
     */
    private float[][][][] convertBitmapToFloatArray() {
        // Выполняем обрезку изображения до центра с нужной шириной и высотой
        Bitmap cropped = cropBitmap(selectedBitmap, MODEL_INPUT_WIDTH, MODEL_INPUT_HEIGHT);
        float[][][][] inputArray = new float[1][MODEL_INPUT_HEIGHT][MODEL_INPUT_WIDTH][3];
        int[] pixels = new int[MODEL_INPUT_WIDTH * MODEL_INPUT_HEIGHT];
        cropped.getPixels(pixels, 0, MODEL_INPUT_WIDTH, 0, 0, MODEL_INPUT_WIDTH, MODEL_INPUT_HEIGHT);
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            // Извлекаем компоненты цвета и нормализуем (делим на 255.0)
            float r = ((pixel >> 16) & 0xFF) / 255.0f;
            float g = ((pixel >> 8) & 0xFF) / 255.0f;
            float b = (pixel & 0xFF) / 255.0f;
            int x = i % MODEL_INPUT_WIDTH;
            int y = i / MODEL_INPUT_WIDTH;
            inputArray[0][y][x][0] = r;
            inputArray[0][y][x][1] = g;
            inputArray[0][y][x][2] = b;
        }
        return inputArray;
    }

    /**
     * Выполняет центральную обрезку Bitmap до заданных размеров targetWidth x targetHeight.
     * Если исходное изображение меньше требуемого размера, оно сначала масштабируется.
     */
    private Bitmap cropBitmap(Bitmap bitmap, int targetWidth, int targetHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Если изображение меньше требуемых размеров, масштабируем его так, чтобы оно было не меньше целевого размера.
        if (width < targetWidth || height < targetHeight) {
            float scale = Math.max((float) targetWidth / width, (float) targetHeight / height);
            int scaledWidth = Math.round(scale * width);
            int scaledHeight = Math.round(scale * height);
            bitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
            width = bitmap.getWidth();
            height = bitmap.getHeight();
        }
        // Вычисляем координаты левого верхнего угла для центральной обрезки
        int x = (width - targetWidth) / 2;
        int y = (height - targetHeight) / 2;

        return Bitmap.createBitmap(bitmap, x, y, targetWidth, targetHeight);
    }

    /**
     * Вычисляет среднеквадратичную ошибку (MSE) между оригинальным и реконструированным изображениями.
     */
    private float calculateMSE(float[][][][] original, float[][][][] reconstructed) {
        float sum = 0;
        int count = 0;
        // Проходим по высоте и ширине масштабированного изображения
        for (int y = 0; y < MODEL_INPUT_HEIGHT; y++) {
            for (int x = 0; x < MODEL_INPUT_WIDTH; x++) {
                for (int c = 0; c < 3; c++) {
                    float diff = original[0][y][x][c] - reconstructed[0][y][x][c];
                    sum += diff * diff;
                    count++;
                }
            }
        }
        return sum / count;
    }

    private void scrollingText(EditText nameDish) {
        if (isTextOverflowing(nameDish)) {
            // Запускаем анимацию прокрутки вниз
            float fullHeight = nameDish.getLineCount() * nameDish.getLineHeight() - nameDish.getHeight();
            ObjectAnimator animatorDown = ObjectAnimator.ofInt(nameDish, "scrollY", 0, (int) fullHeight);
            animatorDown.setDuration(nameDish.getLineCount() * 600);

            // Анимация возврата вверх
            ObjectAnimator animatorUp = ObjectAnimator.ofInt(nameDish, "scrollY", (int) fullHeight, 0);
            animatorUp.setDuration(nameDish.getLineCount() * 600);

            // Последовательное выполнение анимаций
            animatorDown.start();
            animatorDown.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    animatorUp.start();
                }
            });
        }
    }

    // Метод для проверки, выходит ли текст за пределы TextView
    private boolean isTextOverflowing(EditText textView) {
        Rect bounds = new Rect();
        textView.getPaint().getTextBounds(textView.getText().toString(), 0, textView.getText().length(), bounds);
        int textHeight = textView.getLineCount() * textView.getLineHeight();
        return textHeight > textView.getHeight(); // Если высота текста больше TextView
    }

    private void setNewFragment(Fragment fragment, String tag, boolean showAfterAdd) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        // скрыть текущие видимые фрагменты
        for (Fragment f : fm.getFragments()) {
            if (f != null && f.isAdded() && !f.isHidden()) {
                ft.hide(f);
            }
        }

        Fragment existing = fm.findFragmentByTag(tag);
        if (existing == null) {
            // добавляем фрагмент в менеджер; если не хотим показывать — скрываем сразу
            ft.add(R.id.frame_layout_ingredients, fragment, tag);
            if (!showAfterAdd) ft.hide(fragment);
        } else {
            // если фрагмент уже добавлен — показать или скрыть в зависимости от флага
            if (showAfterAdd) ft.show(existing);
            else ft.hide(existing);
        }

        // не используйте addToBackStack, если не хотите, чтобы "скрытые" фрагменты
        // ломали логику назад; при необходимости можно добавить
        ft.commit();
    }

    public boolean isInternetAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            android.net.Network network = cm.getActiveNetwork();
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return false;
    }


    public void goAddScreen(View view) {
        Intent intent = new Intent(this, AddScreen.class);
        startActivity(intent);
    }

    public void goHome(View view) {
        Intent intent = new Intent(this, MainScreen.class);
        startActivity(intent);
    }

    public void goFavourites(View view) {
        Intent intent = new Intent(this, FavouritesScreen.class);
        startActivity(intent);
    }

    public void goOptions(View view) {
        Intent intent = new Intent(this, OptionsScreen.class);
        startActivity(intent);
    }

    public void goBack(View view) {
        finish();
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

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("MODE", Context.MODE_PRIVATE);
        boolean russian = prefs.getBoolean("language", true);
        String langCode = russian ? "ru" : "en";
        Context context = LocaleHelper.setLocale(newBase, langCode);
        super.attachBaseContext(context);
    }

}

package com.example.recipeguide;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;

import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;

import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import Data.DatabaseHandler;
import Model.Recipe;

public class recipe_example_activity extends AppCompatActivity {


    private Ingredient_Fragment_Example ingredientFragment = new Ingredient_Fragment_Example();
    private Recipe_Fragment_Example receptFragment = new Recipe_Fragment_Example();
    TypedValue typedValue = new TypedValue();
    private boolean isFavorite = false;
    private Recipe selectedDish;
    private String category;
    SharedPreferences sharedPreferences;
    private DatabaseHandler databaseHelper;
    private AdView mAdView;
    //private FrameLayout adContainerView;
    private BannerAdView mBannerAd;
    private View adContainerView;
    BaseAdActivity baseAdActivity;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_recipe_example);

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
        /*adContainerView = findViewById(R.id.ad_view_container);
        adView = new AdView(this);
        adView.setAdUnitId("ca-app-pub-3940256099942544/9214589741");
        // [START set_ad_size]
        // Request an anchored adaptive banner with a width of 360.
        adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, 360));
        // [END set_ad_size]

        // Replace ad container with new ad view.
        adContainerView.removeAllViews();
        adContainerView.addView(adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);*/

        /*// Находим контейнер и сам баннер через findViewById
        adContainerView = findViewById(R.id.main);
        mBannerAd = findViewById(R.id.ad_container_view);

        // Ждём, пока измерится ширина контейнера
        adContainerView.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        adContainerView.getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);
                        // Загрузка баннера с учётом размера
                        mBannerAd = loadBannerAd(getAdSize());
                    }
                });*/

        /*AdView adView = findViewById(R.id.banner_ad_view);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.setAdListener(new AdListener() {
            @Override public void onAdLoaded() { Log.d("AdMob","onAdLoaded"); }
            @Override public void onAdFailedToLoad(LoadAdError error) {
                Log.e("AdMob","onAdFailedToLoad: " + error.getCode() + " / " + error.getMessage());
            }
            @Override public void onAdOpened() { }
            @Override public void onAdClicked() { }
            @Override public void onAdClosed() { }
        });
        adView.loadAd(adRequest);*/

        sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE);

        setupBackButtonHandler();
        ImageButton saveFavoritesButton = findViewById(R.id.button_save_favorites);
        String dishId = getIntent().getStringExtra("dish_id");
        databaseHelper = new DatabaseHandler(this);

        saveFavoritesButton.setOnClickListener(view -> toggleFavoriteButton(dishId, saveFavoritesButton, databaseHelper));
        loadData(dishId, databaseHelper);

        TextView nameDish = findViewById(R.id.name_dish);
        nameDish.setMovementMethod(new ScrollingMovementMethod());
        nameDish.post(() -> scrollingText(nameDish));

        Button ingredientButton = findViewById(R.id.ingredient);
        Button recipeButton = findViewById(R.id.recipe);

        setNewFragment(ingredientFragment);
        ingredientButton.setBackgroundResource(R.drawable.rounded_button_focused);

        Recipe selectedDish = databaseHelper.getRecipe(dishId);
        ImageButton buttonShare = findViewById(R.id.button_share);
        buttonShare.setOnClickListener(v -> {
            try {
                createPdf(selectedDish);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });


        ingredientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                recipeButton.setBackgroundResource(R.drawable.rounded_button_default);

                // Устанавливаем фокус на текущую кнопку
                ingredientButton.setBackgroundResource(R.drawable.rounded_button_focused);
                setNewFragment(ingredientFragment);

            }
        });

        recipeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dishId != null) {
                    Recipe selectedDish = databaseHelper.getRecipe(dishId);
                    if(sharedPreferences.getBoolean("language", false)){
                        sendDishDataToFragment(receptFragment, selectedDish.getIngredient(), selectedDish.getRecipe(), selectedDish.getId());
                    }else {
                        sendDishDataToFragment(receptFragment, selectedDish.getIngredient_en(), selectedDish.getRecipe_en(),selectedDish.getId());
                    }
                }
                ingredientButton.setBackgroundResource(R.drawable.rounded_button_default);

                // Устанавливаем фокус на текущую кнопку
                recipeButton.setBackgroundResource(R.drawable.rounded_button_focused);
                setNewFragment(receptFragment);
            }
        });

    }


    @NonNull
    private BannerAdSize getAdSize() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        // высота экрана в dp
        int screenHeightDp = Math.round(displayMetrics.heightPixels / displayMetrics.density);

        // ширина контейнера в px
        int adWidthPx = adContainerView.getWidth();
        if (adWidthPx == 0) {
            // если ещё не измерили — берём полную ширину экрана
            adWidthPx = displayMetrics.widthPixels;
        }

        // конвертируем ширину в dp
        int adWidthDp = Math.round(adWidthPx / displayMetrics.density);
        // примерная максимальная высота баннера в dp
        int maxAdHeightDp = screenHeightDp / 17;

        return BannerAdSize.inlineSize(this, adWidthDp, maxAdHeightDp);
    }

    @NonNull
    private BannerAdView loadBannerAd(@NonNull final BannerAdSize adSize) {
        // Используем mBannerAd, найденный выше
        mBannerAd.setAdSize(adSize);
        mBannerAd.setAdUnitId("demo-banner-yandex");
        mBannerAd.setBannerAdEventListener(new BannerAdEventListener() {
            @Override
            public void onAdLoaded() {
                // защищаем от утечек, если Activity уже разрушена
                if (isDestroyed() && mBannerAd != null) {
                    mBannerAd.destroy();
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull final AdRequestError adRequestError) {
                // здесь можно логировать adRequestError.getDescription()
            }

            @Override public void onAdClicked()         { }
            @Override public void onLeftApplication()  { }
            @Override public void onReturnedToApplication() { }
            @Override public void onImpression(@Nullable ImpressionData impressionData) { }
        });

        final AdRequest adRequest = new AdRequest.Builder().build();
        mBannerAd.loadAd(adRequest);

        return mBannerAd;
    }

    @Override
    protected void onDestroy() {
        if (mBannerAd != null) {
            mBannerAd.destroy();
        }
        super.onDestroy();
    }
    private void loadData(String dishId, DatabaseHandler databaseHelper) {
        if (dishId != null) {
            // Здесь можно использовать dishId для загрузки данных из базы

            selectedDish = databaseHelper.getRecipe(dishId);

            // Логика отображения данных выбранного блюда
            if (selectedDish != null) {
                TextView dishName = findViewById(R.id.name_dish);
                ImageView dishImage = findViewById(R.id.image_dish);
                TextView dishCookingTime = findViewById(R.id.Cooking_time);
                ImageButton dishFavorite = findViewById(R.id.button_save_favorites);
                TextView dishCategory = findViewById(R.id.Category);

                String imagePath = selectedDish.getImage();
                if (imagePath != null) {
                    File imgFile = new File("/data/data/com.example.recipeguide/files/" + imagePath + ".jpg");
                    if (imgFile.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        dishImage.setImageBitmap(bitmap); // Устанавливаем изображение в ImageView
                    }else {
                        Glide.with(this)
                                .load(imagePath)
                                .diskCacheStrategy(DiskCacheStrategy.ALL) // Загружаем из кеша, если интернета нет
                                .into(dishImage);
                    }
                } else {
                    // Устанавливаем изображение-заглушку, если данных нет
                    dishImage.setImageResource(R.drawable.dumplings);
                }
                //dishCookingTime.setText("Время приготовления: " + selectedDish.getCookingTime() + " мин");
                /*String cookingTimeText = getString(R.string.cooking_time, selectedDish.getCookingTime());
                dishCookingTime.setText(cookingTimeText);*/
                /*int cookingTime = selectedDish.getCookingTime();
                String fullText = getString(R.string.cooking_time, cookingTime);
// fullText будет: "Время приготовления: 15 мин"

// Находим границы динамической части ("число + мин")
                String timePart = cookingTime + " мин";
                int start = fullText.indexOf(timePart);
                int end = start + timePart.length();

// Строим SpannableStringBuilder
                SpannableStringBuilder ssb = new SpannableStringBuilder(fullText);

// Делаем время + "мин" жирным
                ssb.setSpan(
                        new StyleSpan(Typeface.BOLD),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );*/

                int cookingTime = selectedDish.getCookingTime();
                String fullText = getString(R.string.cooking_time, cookingTime); // "Время приготовления: 15 мин"

                SpannableStringBuilder ssb = new SpannableStringBuilder(fullText);

// Попробуем найти число cookingTime в тексте (защищённо, с проверками)
                String numberStr = String.valueOf(cookingTime);
                int start = -1;
                int end = -1;

// Находим первое вхождение числа cookingTime
                int idx = fullText.indexOf(numberStr);
                if (idx >= 0) {
                    start = idx;
                    end = start + numberStr.length();

                    // Если сразу после числа идёт " мин" или "мин" — расширим диапазон
                    String after = fullText.substring(end);
                    // trimLeading пробелов для проверки
                    if (after.startsWith(" мин") || after.startsWith("мин") || after.startsWith(" мин")) { // учитываем NBSP
                        // найдём фактическую границу вхождения слова "мин"
                        int minIndexInAfter = -1;
                        if (after.startsWith(" мин")) minIndexInAfter = 1 + 3; // пробел + "мин" (3 символа)
                        else if (after.startsWith("мин")) minIndexInAfter = 3;
                        else if (after.startsWith("\u00A0мин")) minIndexInAfter = 1 + 3; // NBSP + "мин"

                        if (minIndexInAfter > 0) {
                            end = end + minIndexInAfter;
                        } else {
                            // альтернативно попробуем найти "мин" в оставшейся строке поближе
                            int foundMin = after.indexOf("мин");
                            if (foundMin >= 0) {
                                end = end + foundMin + 3;
                            }
                        }
                    } else {
                        // Попробуем найти слово "мин" в пределах ближайших 10 символов
                        int foundMin = fullText.indexOf("мин", end);
                        if (foundMin >= 0 && foundMin - end <= 5) {
                            end = foundMin + 3;
                        }
                    }
                }

// Финальная проверка индексов перед setSpan
                if (start >= 0 && end > start && end <= ssb.length()) {
                    ssb.setSpan(new StyleSpan(Typeface.BOLD),
                            start,
                            end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    Log.w("RecipeExample", "Не удалось применить span: start=" + start + " end=" + end + " fullLen=" + ssb.length());
                }




// Применяем в TextView
                dishCookingTime.setText(ssb);
                int position = selectedDish.getCategory();


                switch (position) {
                    case 0:
                        category = getString(R.string.category_appetizers);
                        break;
                    case 1:
                        category = getString(R.string.category_soup);
                        break;
                    case 2:
                        category = getString(R.string.category_salad);
                        break;
                    case 3:
                        category = getString(R.string.category_main_course);
                        break;
                    case 4:
                        category = getString(R.string.category_side_dish);
                        break;
                    case 5:
                        category = getString(R.string.category_dessert);
                        break;
                    case 6:
                        category = getString(R.string.category_drink);
                        break;
                    case 7:
                        category = getString(R.string.category_sauces_and_seasonings);
                        break;
                    case 8:
                        category = getString(R.string.category_baked_goods);
                        break;
                    case 9:
                        category = getString(R.string.category_snacks);
                        break;
                    default:
                        category = "";  // или обработать ошибку/неназначенную позицию
                        break;
                }
                String text = getString(R.string.category)
                        + " <b>" + category + "</b>";
                dishCategory.setText(
                        Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
                );
                if (selectedDish.getIsFavorite() == 0) {
                    getTheme().resolveAttribute(R.attr.buttonHeartIcon, typedValue, true);
                    dishFavorite.setImageResource(typedValue.resourceId);
                } else {
                    dishFavorite.setImageResource(R.drawable.button_heart_red);
                }


                if(sharedPreferences.getBoolean("language", false)){
                    dishName.setText(selectedDish.getName());
                    sendDishDataToFragment(ingredientFragment, selectedDish.getIngredient(), selectedDish.getRecipe(),selectedDish.getId());
                }else{
                    dishName.setText(selectedDish.getName_en());
                    sendDishDataToFragment(ingredientFragment, selectedDish.getIngredient_en(), selectedDish.getRecipe_en(),selectedDish.getId());
                }
            }
        }
    }

    private void setNewFragment(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.frame_layout, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    private void sendDishDataToFragment(Fragment fragment, String selectedIngredient, String selectedRecipe, String selectedId) {
        // Создаём объект Bundle для передачи данных
        Bundle bundle = new Bundle();
        bundle.putString("dish_ingredients", selectedIngredient); // Например, список ингредиентов
        bundle.putString("dish_recipe", selectedRecipe); // Например, текст рецепта
        bundle.putString("dish_id", selectedId); // Например, текст рецепта

        // Передаём Bundle во фрагмент
        fragment.setArguments(bundle);
    }

    private void scrollingText(TextView nameDish) {
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
            animatorDown.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    animatorUp.start();
                }
            });
        }
    }

    // Метод для проверки, выходит ли текст за пределы TextView
    private boolean isTextOverflowing(TextView textView) {
        Rect bounds = new Rect();
        textView.getPaint().getTextBounds(textView.getText().toString(), 0, textView.getText().length(), bounds);
        int textHeight = textView.getLineCount() * textView.getLineHeight();
        return textHeight > textView.getHeight(); // Если высота текста больше TextView
    }

    private void toggleFavoriteButton(String dishId, ImageButton saveFavoritesButton, DatabaseHandler databaseHandler) {
        if (isFavorite) {
            updateFavorite(dishId, databaseHandler, 0);

            // Получаем ресурс атрибута темы
            getTheme().resolveAttribute(R.attr.buttonHeartIcon, typedValue, true);
            // Устанавливаем ресурс в ImageButton
            saveFavoritesButton.setImageResource(typedValue.resourceId);
            isFavorite = false;
        } else {
            updateFavorite(dishId, databaseHandler, 1);
            // Устанавливаем "красное сердце"
            saveFavoritesButton.setImageResource(R.drawable.button_heart_red);
            isFavorite = true;
            databaseHelper.insertEvent(java.util.UUID.randomUUID().toString(),User.username,"favorite",dishId,System.currentTimeMillis());

        }
    }

    private void updateFavorite(String dishId, DatabaseHandler databaseHandler, int isFavorite) {
        if (dishId != null) {
            Recipe selectedDish = databaseHandler.getRecipe(dishId);
            if (selectedDish != null) {
                selectedDish.setIsFavorite(isFavorite);
                databaseHandler.updateRecipe(selectedDish);
            }
        }
    }

    public void toggleCookButton(DatabaseHandler databaseHandler, String id){
        databaseHandler.insertEvent(java.util.UUID.randomUUID().toString(),User.username,"cook", id,System.currentTimeMillis());
        Recipe r = databaseHandler.getRecipe(id);
        updateCook(r);
    }

    private void updateCook(Recipe r) {
        if (r.getId() != null) {
            if (r != null) {
                r.setIsCook(1);
                databaseHelper.updateRecipe(r);
            }
        }
    }
    private void createPdf(Recipe selectedDish) throws FileNotFoundException {
        try {
            boolean isRussian = sharedPreferences.getBoolean("language", false);

            String name, recipe, ingredient;
            if(isRussian){
                name = selectedDish.getName();
                recipe = selectedDish.getRecipe();
                ingredient = selectedDish.getIngredient();
            }else{
                name = selectedDish.getName_en();
                recipe = selectedDish.getRecipe_en();
                ingredient = selectedDish.getIngredient_en();
            }
            // Создаём временный PDF-файл
            File tempFile = new File(getCacheDir(), selectedDish.getName() + ".pdf");
            FileOutputStream fos = new FileOutputStream(tempFile);

            // Создаём документ iText
            PdfWriter writer = new PdfWriter(fos);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);
            String fontPath = "assets/times_new_roman.ttf"; // Если шрифт в папке assets
            PdfFont font = PdfFontFactory.createFont(fontPath);

            // Устанавливаем размер страницы и отступы
            pdfDocument.setDefaultPageSize(PageSize.A4);
            document.setMargins(20, 20, 20, 20);

            // Заголовок
            document.add(new Paragraph(name)
                    .setFont(font)
                    .setFontSize(25)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            String imagePath = selectedDish.getImage();
            Bitmap bitmap;
            if (imagePath != null) {
                File imgFile = new File("/data/data/com.example.recipeguide/files/" + imagePath + ".jpg");
                if (imgFile.exists()) {
                    // Конвертируем файл изображения в объект Bitmap
                    bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                } else {
                    // Загружаем изображение с помощью Glide
                    try {
                        bitmap = Glide.with(this)
                                .asBitmap()
                                .load(imagePath) // URL изображения, если оно хранится в Firebase/Cloudinary
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .submit()
                                .get();
                    } catch (Exception e) {
                        Log.e("Glide", "Ошибка загрузки изображения: " + e.getMessage());
                        return;
                    }
                }

// Конвертируем Bitmap в байты
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] bitmapData = stream.toByteArray();

// Создаём объект Image из байтов
                ImageData imageData = ImageDataFactory.create(bitmapData);
                Image image = new Image(imageData);

                image.setHorizontalAlignment(HorizontalAlignment.CENTER);
                image.setWidth(300);
                image.setHeight(200);

// Добавляем изображение в PDF
                document.add(image);
            } else {
                // Если изображения нет, добавляем заглушку
                document.add(new Paragraph(getString(R.string.error_load_image)));
            }
            // Время приготовления
            document.add(new Paragraph(getString(R.string.cooking_time, selectedDish.getCookingTime()))
                    .setFont(font)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));
            //Категория
            document.add(new Paragraph(getString(R.string.category)+" "+category)
                    .setFont(font)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));

            // Ингредиенты
            document.add(new Paragraph(getString(R.string.ingredients))
                    .setFont(font)
                    .setFontSize(20)
                    .setBold());
            document.add(new Paragraph(ingredient).setFontSize(20).setFont(font));

            // Рецепт
            document.add(new Paragraph("\n" + getString(R.string.recipe))
                    .setFont(font)
                    .setFontSize(20)
                    .setBold());
            document.add(new Paragraph(recipe).setFontSize(20).setFont(font));

            // Закрываем документ
            document.close();

            // Передаём файл для отправки
            sharePdf(tempFile);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_pdf, Toast.LENGTH_SHORT).show();
        }
    }

    private void sharePdf(File pdfFile) {
        Uri uri = FileProvider.getUriForFile(this, "com.example.recipeguide.fileprovider", pdfFile);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, "Share PDF via"));
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
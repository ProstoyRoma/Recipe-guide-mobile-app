package com.example.recipeguide;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import Data.DatabaseHandler;
import Model.Recipe;

public class ReorderActivity extends AppCompatActivity {

    private FrameLayout cardContainer;
    private TextView tvHint, tvLater;
    private Button btnDone;
    private ImageView overlayLike;
    private ImageView overlayDislike;
    private TextView tvThanks;

    private List<Recipe> allRecipes = new ArrayList<>();
    private int processedCount = 0;
    private final List<String> liked = new ArrayList<>();
    private final List<String> disliked = new ArrayList<>();
    private TextView tvCounter;
    private int currentIndex = 1; // 1-based индекс текущей карточки
    private static final int TOTAL_CARDS = 10;
    private static final int VISIBLE_CARDS = 2;
    private static final float SCALE_STEP = 0.04f;
    private static final float TRANSLATION_STEP_DP = 12f;
    private static final String PREFS = "app_prefs";
    private static final String KEY_REORDER = "reorder_completed";

    private int swipeThresholdPx;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reorder);

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fl_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        boolean isNightMode = OptionsScreen.getCurrentTheme(this);

        if (isNightMode) {
            // Действия для тёмной темы
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            // Действия для светлой темы
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        cardContainer = findViewById(R.id.card_container);
        tvHint = findViewById(R.id.tv_hint);
        tvLater = findViewById(R.id.tv_later);
        btnDone = findViewById(R.id.btn_done);
        overlayLike = findViewById(R.id.overlay_like);
        overlayDislike = findViewById(R.id.overlay_dislike);
        tvThanks = findViewById(R.id.tv_thanks);
        tvCounter = findViewById(R.id.tv_counter);
        updateCounter(); // покажет 1/10 или количество, если меньше 10

        swipeThresholdPx = (int) (getScreenWidth() * 0.25f);

        // Загрузите 10 рецептов из БД или API. Если больше — возьмите первые 10.
        DatabaseHandler db = new DatabaseHandler(this);
        List<Recipe> fromDb = db.getColdStartRecipe(); // ваш метод
        if (fromDb != null) {
            for (int i = 0; i < Math.min(TOTAL_CARDS, fromDb.size()); i++) {
                allRecipes.add(fromDb.get(i));
            }
        }

        populateInitialStack();

    }

    private void updateCounter() {
        // Если хотите показывать прогресс как "0/10" когда стек пуст — можно изменить логику
        int shown = Math.min(currentIndex, TOTAL_CARDS);
        if (processedCount >= TOTAL_CARDS) {
            tvCounter.setText(TOTAL_CARDS + "/" + TOTAL_CARDS);
            tvCounter.setVisibility(View.GONE); // или оставьте видимым, если нужно
        } else {
            tvCounter.setVisibility(View.VISIBLE);
            tvCounter.setText(shown + "/" + TOTAL_CARDS);
        }
    }

    private void populateInitialStack() {
        cardContainer.removeAllViews();
        processedCount = 0;
        // Добавляем первые TOTAL_CARDS карточек, но показываем только VISIBLE_CARDS визуально
        int toAdd = Math.min(TOTAL_CARDS, allRecipes.size());
        for (int i = toAdd - 1; i >= 0; i--) {
            addCardAtBottom(i);
        }
        // После добавления назначаем touch на верхнюю
        assignTopTouch();
        updateDoneVisibility();
        currentIndex = 1;
        updateCounter();
    }

    private void addCardAtBottom(int indexInAllRecipes) {
        Recipe r = allRecipes.get(indexInAllRecipes);
        LayoutInflater inflater = LayoutInflater.from(this);
        View card = inflater.inflate(R.layout.item_recipe_card, cardContainer, false);

        // Заполняем данные
        TextView title = card.findViewById(R.id.tv_recipe_title);
        ImageView image = card.findViewById(R.id.iv_recipe_image);
        SharedPreferences sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE);

        if (sharedPreferences.getBoolean("language", false)) {
            title.setText(r.getName() != null ? r.getName() : "");
        } else {
            title.setText(r.getName_en() != null ? r.getName_en() : "");
        }

        String imagePath = r.getImage();
        if (imagePath != null) {
            File imgFile = new File("/data/data/com.example.recipeguide/files/" + imagePath + ".jpg");
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                image.setImageBitmap(bitmap); // Устанавливаем изображение в ImageView
            } else {
                Glide.with(this)
                        .load(imagePath)
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Загружаем из кеша, если интернета нет
                        .into(image);
            }
        } else {
            // Устанавливаем изображение-заглушку, если данных нет
            image.setImageResource(R.drawable.dumplings);
        }

        // Сохраняем индекс в tag
        card.setTag(indexInAllRecipes);

        // Добавляем в начало контейнера, чтобы верхняя карточка была последней дочерней
        cardContainer.addView(card, 0);

        // Применяем визуальные уровни только для первых VISIBLE_CARDS
        int childCount = cardContainer.getChildCount();
        int levelFromTop = Math.max(0, Math.min(VISIBLE_CARDS - 1, VISIBLE_CARDS - childCount));
        // Но проще: пересчитаем уровни после добавления
        applyStackTransforms();
    }

    private void applyStackTransforms() {
        int childCount = cardContainer.getChildCount();
        // верхняя — последний элемент
        for (int i = 0; i < childCount; i++) {
            View v = cardContainer.getChildAt(i);
            int levelFromTop = childCount - 1 - i; // 0 = top
            if (levelFromTop >= VISIBLE_CARDS) {
                v.setVisibility(View.GONE);
            } else {
                v.setVisibility(View.VISIBLE);
                float scale = 1f - levelFromTop * SCALE_STEP;
                v.setScaleX(scale);
                v.setScaleY(scale);
                v.setTranslationY(dpToPx(levelFromTop * TRANSLATION_STEP_DP));
            }
        }
    }

    private void assignTopTouch() {
        int childCount = cardContainer.getChildCount();
        if (childCount == 0) return;
        View top = cardContainer.getChildAt(childCount - 1);
        Integer idx = (Integer) top.getTag();
        if (idx != null) { // currentIndex = idxInSequence + 1; // если allRecipes содержит только первые TOTAL_CARDS, можно:
            currentIndex = processedCount + 1; // следующая карточка для обработки
        }
        updateCounter();
        // Снимаем слушатели со всех карточек
        for (int i = 0; i < childCount; i++) cardContainer.getChildAt(i).setOnTouchListener(null);
        top.setOnTouchListener(new CardTouchListener(top));
    }

    private void updateDoneVisibility() {
        // processedCount — сколько карточек уже обработано (лайк/дизлайк)
        // TOTAL_CARDS — общее количество карточек для показа (10)
        // cardContainer — FrameLayout с дочерними карточками

        // Показываем кнопку, когда обработано TOTAL_CARDS карточек
        if (processedCount >= TOTAL_CARDS) {
            btnDone.setVisibility(View.VISIBLE);
            tvLater.setVisibility(View.GONE);
        } else {
            btnDone.setVisibility(View.GONE);
            tvLater.setVisibility(View.VISIBLE);
        }
    }

    public void toggleLater(View view) {
        Intent intent = new Intent(this, MainScreen.class);
        startActivity(intent);
        finish();
    }

    public void toggleDone(View view) {
        DatabaseHandler databaseHandler = new DatabaseHandler(this);
        for (String r : liked) {
            databaseHandler.insertEvent(java.util.UUID.randomUUID().toString(), User.username, "cook", r, System.currentTimeMillis());
        }
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean(KEY_REORDER, true);
        editor.apply();

        databaseHandler.getRecommendedRecipe(this, new RecommendedCallback() {
            @Override
            public void onSuccess() {
                // Запускаем MainScreen сразу после успешной загрузки рекомендаций
                runOnUiThread(() -> {
                    Intent intent = new Intent(ReorderActivity.this, MainScreen.class);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onFailure(Exception e) {
                // Даже при ошибке переходим на главный экран
                runOnUiThread(() -> {
                    Intent intent = new Intent(ReorderActivity.this, MainScreen.class);
                    startActivity(intent);
                    finish();
                });
            }
        });
    }




    private class CardTouchListener implements View.OnTouchListener {
        private final View view;
        private float downX, downY;
        private float startTX, startTY;

        CardTouchListener(View view) {
            this.view = view;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // overlay views
            ImageView ovLike = overlayLike;
            ImageView ovDislike = overlayDislike;

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX();
                    downY = event.getRawY();
                    startTX = view.getTranslationX();
                    startTY = view.getTranslationY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - downX;
                    float dy = event.getRawY() - downY;
                    view.setTranslationX(startTX + dx);
                    view.setTranslationY(startTY + dy);
                    view.setRotation((startTX + dx) / view.getWidth() * 20f);

                    // прогресс появления иконок: 0..1
                    float progress = Math.min(1f, Math.abs((startTX + dx) / swipeThresholdPx));

                    if (startTX + dx > 0) {
                        // тянем вправо — показываем overlayLike справа
                        ovLike.setVisibility(View.VISIBLE);
                        ovLike.setAlpha(progress);
                        ovDislike.setAlpha(0f);
                    } else {
                        // тянем влево — показываем overlayDislike слева
                        ovDislike.setVisibility(View.VISIBLE);
                        ovDislike.setAlpha(progress);
                        ovLike.setAlpha(0f);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    float totalDx = view.getTranslationX();
                    boolean swiped = Math.abs(totalDx) > swipeThresholdPx;
                    if (swiped) {
                        boolean toRight = totalDx > 0;
                        animateOffScreen(view, toRight);
                    } else {
                        // возврат в центр и скрытие overlay
                        view.animate().translationX(0).translationY(0).rotation(0).setDuration(150).start();
                        ovLike.animate().alpha(0f).setDuration(150).withEndAction(() -> ovLike.setVisibility(View.GONE)).start();
                        ovDislike.animate().alpha(0f).setDuration(150).withEndAction(() -> ovDislike.setVisibility(View.GONE)).start();
                    }
                    return true;
            }
            return false;
        }
    }


    private void animateOffScreen(View card, boolean toRight) {
        int screenW = getScreenWidth();
        float targetX = toRight ? screenW * 1.2f : -screenW * 1.2f;
        card.animate()
                .translationX(targetX)
                .rotation(toRight ? 30f : -30f)
                .setDuration(250)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        // Скрываем overlay
                        overlayLike.setAlpha(0f);
                        overlayLike.setVisibility(View.GONE);
                        overlayDislike.setAlpha(0f);
                        overlayDislike.setVisibility(View.GONE);

                        // Сохраняем результат
                        Integer idx = (Integer) card.getTag();
                        if (idx != null && idx >= 0 && idx < allRecipes.size()) {
                            Recipe r = allRecipes.get(idx);
                            if (toRight) liked.add(r.getId());
                            else disliked.add(r.getId());
                        }
                        // Удаляем карточку
                        cardContainer.removeView(card);
                        processedCount++;
                        currentIndex = processedCount + 1; // следующая карточка
                        updateCounter();
                        applyStackTransforms();
                        updateDoneVisibility(); // обновляем прогресс/кнопку

                        if (processedCount < TOTAL_CARDS && cardContainer.getChildCount() > 0) {
                            assignTopTouch();
                        } else {
                            // Все карточки обработаны — показываем благодарность
                            showFinishState();
                        }
                    }

                }).start();
    }

    private void showFinishState() {
        // Скрываем все overlay (на всякий случай)
        overlayLike.setVisibility(View.GONE);
        overlayDislike.setVisibility(View.GONE);

        // Убираем все карточки (если остались)
        cardContainer.removeAllViews();

        tvCounter.setVisibility(View.GONE);
        // Показываем текст благодарности
        tvThanks.setVisibility(View.VISIBLE);
        btnDone.setVisibility(View.VISIBLE);
    }


    private int getScreenWidth() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        if (wm != null) wm.getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
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

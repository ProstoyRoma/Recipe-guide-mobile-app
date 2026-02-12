package com.example.recipeguide;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.regex.Pattern;

import Data.DatabaseHandler;
import Model.Recipe;


public class Recipe_Fragment_Example extends Fragment {


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_PARAM3 = "param3";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private String mParam3;
    private Button cookedButton;
    private recipe_example_activity headActivity;
    private DatabaseHandler databaseHandler;
    private FirebaseAuth mAuth;
    FirebaseDatabase database;

    private Context appContext; // безопаснее хранить applicationContext

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        appContext = context.getApplicationContext(); // предотвращаем утечку Activity
    }

    public Recipe_Fragment_Example() {
        // Required empty public constructor
    }

    public static Recipe_Fragment_Example newInstance(String param1, String param2, String param3) {
        Recipe_Fragment_Example fragment = new Recipe_Fragment_Example();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        args.putString(ARG_PARAM3, param3);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
            mParam3 = getArguments().getString(ARG_PARAM3);
            databaseHandler = new DatabaseHandler(getContext());
            mAuth = FirebaseAuth.getInstance();

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_recipe__dumplings_, container, false);
        View view = inflater.inflate(R.layout.fragment_recipe_example, container, false);
        headActivity = new recipe_example_activity();
        cookedButton = view.findViewById(R.id.button_cook);

        // Получаем данные из аргументов
        Bundle bundle = getArguments();
        String id = bundle.getString("dish_id");

        Recipe r = databaseHandler.getRecipe(id);

        if (bundle != null) {

            String recipe = bundle != null ? bundle.getString("dish_recipe") : null;
            TextView recipeTextView = view.findViewById(R.id.recipe_dish);
            recipeTextView.setText(numberParagraphs(recipe));
        }
        if (r.getIsCook() == 0) {
            cookedButton.setOnClickListener(v -> {
                // немедленно блокируем кнопку
                v.setEnabled(false);
                v.setVisibility(View.GONE);
                //v.setEnabled(false);
                new Thread(() -> {
                    databaseHandler.insertEvent(java.util.UUID.randomUUID().toString(),
                            User.username, "cook", id, System.currentTimeMillis());

                    // обновляем состояние рецепта в БД (если у вас есть метод)
                    updateCook(r); // если updateCook делает DB-операции — убедитесь, что он безопасен в фоне

                    // обновляем UI на главном потоке: убрать кнопку и/или обновить элементы списка
                    r.setIsCook(1);


                }).start();
                Toast.makeText(getActivity(), R.string.cooked_message, Toast.LENGTH_SHORT).show();

                databaseHandler.insertEvent(java.util.UUID.randomUUID().toString(), User.username, "cook", id, System.currentTimeMillis());
                updateCook(r);
                //new Handler(Looper.getMainLooper()).postDelayed(() -> v.setEnabled(true), 10000);
            });
        } else {
            cookedButton.setVisibility(View.GONE);
            cookedButton.setOnClickListener(null);
        }
        return view;
    }

    private String numberParagraphs(@Nullable String raw) {
        if (raw == null) return "";

        // Нормализуем переводы строк (сохраняем одиночные переводы)
        String normalized = raw.replace("\r\n", "\n").replace("\r", "\n").trim();
        if (normalized.isEmpty()) return "";

        // Разбиваем по одиночным переводам строки — каждый элемент parts соответствует одной строке/абзацу
        String[] parts = normalized.split("\\n", -1);

        StringBuilder out = new StringBuilder();
        int idx = 1;
        // Регулярка для определения префикса "число. " с возможными ведущими пробелами
        Pattern numberedPrefix = Pattern.compile("^\\s*\\d+\\.\\s+.*", Pattern.DOTALL);

        for (String p : parts) {
            String paragraphRaw = p; // сохраняем исходный (чтобы корректно читать начало абзаца)
            String paragraphTrimmed = paragraphRaw.trim();
            if (paragraphTrimmed.isEmpty()) continue;

            if (out.length() > 0) out.append("\n\n");

            // Если строка уже начинается с "N. " — сохраняем её как есть (без повторной нумерации)
            if (numberedPrefix.matcher(paragraphRaw).matches()) {
                // добавляем исходный параграф (с сохранением пробелов внутри, но убираем ведущие/замыкающие переносы)
                out.append(paragraphRaw.trim());
            } else {
                // иначе добавляем номер и остальной текст (без лишних внешних пробелов)
                out.append(idx).append(".  ").append(paragraphTrimmed);
            }
            idx++;
        }
        return out.toString();

    }

    private void updateCook(Recipe r) {
        FirebaseUser user = mAuth.getCurrentUser();

        mAuth = FirebaseAuth.getInstance();

        database = FirebaseDatabase.getInstance();
        if (r.getId() != null) {
            if (r != null) {
                r.setIsCook(1);
                databaseHandler.updateRecipe(r);
            }
        }
        if (user != null) {
            database.getReference("users").child(user.getUid()).child("isCook").child(r.getId()).setValue(true);
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        appContext = null;
    }
}
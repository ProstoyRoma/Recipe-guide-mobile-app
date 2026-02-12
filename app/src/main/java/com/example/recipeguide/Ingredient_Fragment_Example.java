package com.example.recipeguide;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class Ingredient_Fragment_Example extends Fragment {



        // TODO: Rename parameter arguments, choose names that match
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private static final String ARG_PARAM1 = "param1";
        private static final String ARG_PARAM2 = "param2";

        // TODO: Rename and change types of parameters
        private String mParam1;
        private String mParam2;

        public Ingredient_Fragment_Example() {
            // Required empty public constructor
        }

        public static Ingredient_Fragment_Example newInstance(String param1, String param2) {
            Ingredient_Fragment_Example fragment = new Ingredient_Fragment_Example();
            Bundle args = new Bundle();
            args.putString(ARG_PARAM1, param1);
            args.putString(ARG_PARAM2, param2);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null) {
                mParam1 = getArguments().getString(ARG_PARAM1);
                mParam2 = getArguments().getString(ARG_PARAM2);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            //return inflater.inflate(R.layout.fragment_ingredient__dumplings_, container, false);
            View view = inflater.inflate(R.layout.fragment_ingredient_example, container, false);

            // Получаем данные из аргументов
            Bundle bundle = getArguments();
            if (bundle != null) {

                String ingredientsRaw = bundle != null ? bundle.getString("dish_ingredients") : null;
                TextView ingredientsTextView = view.findViewById(R.id.ingredients_dish);
                ingredientsTextView.setText(formatIngredientsForDisplay(ingredientsRaw));
            }

            return view;
        }
    private String formatIngredientsForDisplay(@Nullable String raw) {
        if (raw == null) return "";

        StringBuilder out = new StringBuilder();
        String[] lines = raw.split("\\r?\\n");
        for (String line : lines) {
            if (line == null) continue;
            line = line.trim();
            if (line.isEmpty()) continue;

            int dotIdx = line.indexOf('.');
            String prefix = "";
            String rest = line;
            if (dotIdx > 0) {
                prefix = line.substring(0, dotIdx + 1).trim(); // "1."
                rest = line.substring(dotIdx + 1).trim();      // "Название|qty|measure"
            }

            String[] parts = rest.split("\\|", -1);
            String name = parts.length > 0 ? parts[0].trim() : "";
            String qty  = parts.length > 1 ? parts[1].trim() : "";
            String unit = parts.length > 2 ? parts[2].trim() : "";

            if (name.isEmpty()) continue;

            // Капитализируем первую букву названия, сохраняя остальной текст
            name = capitalizeFirstLetter(name);

            String rightPart = "";
            if (!qty.isEmpty() && !unit.isEmpty()) {
                rightPart = qty + " " + unit;
            } else if (!qty.isEmpty()) {
                rightPart = qty;
            } else if (!unit.isEmpty()) {
                rightPart = unit;
            }

            if (out.length() > 0) out.append('\n');

            if (!rightPart.isEmpty()) {
                out.append(prefix.isEmpty() ? "" : (prefix + " "));
                out.append(name).append(" \u2014 ").append(rightPart);
            } else {
                out.append(prefix.isEmpty() ? "" : (prefix + " "));
                out.append(name);
            }
        }
        return out.toString();
    }

    private String capitalizeFirstLetter(String s) {
        if (s == null || s.isEmpty()) return s;
        int firstCodePoint = s.codePointAt(0);
        int firstCharLen = Character.charCount(firstCodePoint);
        String first = new String(Character.toChars(Character.toUpperCase(firstCodePoint)));
        if (s.length() == firstCharLen) return first;
        return first + s.substring(firstCharLen);
    }
}

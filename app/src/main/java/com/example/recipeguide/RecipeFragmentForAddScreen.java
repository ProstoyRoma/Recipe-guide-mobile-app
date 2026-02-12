package com.example.recipeguide;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

public class RecipeFragmentForAddScreen extends Fragment {

    private EditText editTextRecipe;
    private ColorStateList currentTint;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_recipe_input, container, false);

        //currentTint = AppCompatResources.getColorStateList(requireContext(), R.color.background_add_screen);
        editTextRecipe = view.findViewById(R.id.recipe);
        if (currentTint != null) {
            editTextRecipe.setBackgroundTintList(currentTint);
        }

        editTextRecipe.addTextChangedListener(new TextWatcher() {
            private boolean lock = false;
            private int lastSelection = 0;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                lastSelection = editTextRecipe.getSelectionStart();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (lock) return;
                lock = true;
                try {
                    String raw = s.toString().replace("\r\n", "\n").replace('\r', '\n');
                    String[] lines = raw.split("\n", -1);

                    StringBuilder rebuilt = new StringBuilder();
                    int oldPos = 0;          // позиция начала текущей старой строки в старом тексте
                    int newCursor = editTextRecipe.getSelectionStart();
                    int oldCursor = editTextRecipe.getSelectionStart();

                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i];
                        String expectedPrefix = (i + 1) + ". ";

                        // НЕ тримим — сохраняем пробелы, чтобы пользователь мог вводить их
                        String textAfter;
                        if (line.matches("^\\s*\\d+\\.\\s.*")) {
                            int dot = line.indexOf('.');
                            // берем всё, что после точки, сохраняем исходные пробелы после точки
                            textAfter = line.substring(dot + 1);
                            // удаляем только один ведущий пробел, если он есть, чтобы избежать двойного пробела
                            if (textAfter.startsWith(" ")) textAfter = textAfter.substring(1);
                        } else {
                            textAfter = line; // сохраняем как есть, включая пробелы
                        }

                        // перед добавлением префикса определим, попал ли курсор в эту старую строку и как сместить
                        int oldLineStart = oldPos;
                        int oldLineEnd = oldPos + line.length();
                        boolean cursorInOldLine = (oldCursor >= oldLineStart && oldCursor <= oldLineEnd);

                        // добавляем префикс + текст
                        int prefixStart = rebuilt.length();
                        rebuilt.append(expectedPrefix);
                        int prefixEnd = rebuilt.length();
                        rebuilt.append(textAfter);

                        // если курсор был в старой строке, вычисляем его новое положение:
                        if (cursorInOldLine) {
                            int offsetInOld = oldCursor - oldLineStart; // позиция внутри старой строки
                            // если old строка содержала префикс, нужно скорректировать offsetInOld
                            int oldPrefixLen = 0;
                            if (line.matches("^\\s*\\d+\\.\\s.*")) {
                                int dot = line.indexOf('.');
                                oldPrefixLen = dot + 2 <= line.length() ? dot + 2 : dot + 1; // "N. "
                            }
                            // позиция внутри textAfter (с учётом старого префикса)
                            int posInTextAfter = Math.max(0, offsetInOld - oldPrefixLen);
                            // новая позиция = текущ длине rebuilt до префикса + префикс длина + posInTextAfter
                            newCursor = prefixEnd + Math.max(0, Math.min(textAfter.length(), posInTextAfter));
                        }

                        // готовим смещения для следующей итерации
                        oldPos += line.length() + 1; // +1 для '\n' (split с -1 включает терминальный пустой)
                        if (i < lines.length - 1) rebuilt.append('\n');
                    }

                    // если весь rebuilt пустой (пользователь стер всё) — оставляем пустой EditText
                    String result = rebuilt.toString();
                    if (!result.equals(s.toString())) {
                        editTextRecipe.removeTextChangedListener(this);
                        editTextRecipe.setText(result);
                        // гарантируем границы курсора
                        int sel = Math.max(0, Math.min(result.length(), newCursor));
                        editTextRecipe.setSelection(sel);
                        editTextRecipe.addTextChangedListener(this);
                    } else {
                        // если текст не изменился, но курсор может быть внутри префикса — подвинем за префикс
                        ensureCursorOutsidePrefix(editTextRecipe);
                    }
                } finally {
                    lock = false;
                }

            }
        });

        return view;
    }
    /** Если курсор попал внутри префикса "N. ", перемещаем его в конец префикса */
    private void ensureCursorOutsidePrefix(EditText et) {
        int sel = et.getSelectionStart();
        String text = et.getText().toString().replace("\r\n", "\n").replace('\r', '\n');
        // Найти начало текущей строки
        int lineStart = text.lastIndexOf('\n', Math.max(0, sel - 1)) + 1;
        // определить текущный префикс (если есть)
        int i = lineStart;
        // прочитать цифры и точку
        while (i < text.length() && Character.isDigit(text.charAt(i))) i++;
        if (i < text.length() && i > lineStart && text.charAt(i) == '.') {
            int prefixEnd = i + 2 <= text.length() ? i + 2 : i + 1; // ". " длина 2; если нет пробела — просто за точку
            if (sel > lineStart && sel < prefixEnd) {
                et.setSelection(prefixEnd);
            }
        }
    }

    public boolean validateInputs() {
        String recipe = editTextRecipe == null ? "": editTextRecipe.getText().toString().trim();
        return recipe.isEmpty();
    }
    public String getRecipeData() {
        String data = editTextRecipe == null ? "": editTextRecipe.getText().toString().trim();
        return data;
    }

    public void errorInputs() {
        currentTint = ColorStateList.valueOf(Color.RED); // Сохраняем цвет ошибки
        editTextRecipe.setBackgroundTintList(currentTint);
        //setTintSafely(editTextRecipe, currentTint);
        //editTextRecipe.getBackground().setColorFilter(getResources().getColor(R.color.red),
           //     PorterDuff.Mode.SRC_ATOP);
        //editTextRecipe.setCompoundDrawableTintList(currentTint);

    }

    public void goodInputs() {
        currentTint = AppCompatResources.getColorStateList(getContext(), R.color.black); // Ваш стандартный цвет
        //editTextRecipe.setBackgroundTintList(currentTint);
        setTintSafely(editTextRecipe, currentTint);

    }
    private void setTintSafely(View v, ColorStateList tint) {
        if (v == null) return;
        try {
            // для EditText / AutoCompleteTextView используем setBackgroundTintList
            if (tint != null) {
                v.setBackgroundTintList(tint);
            } else {
                // Сброс tint: подставляем цвет из темы или null, чтобы вернуть дефолтный вид
                v.setBackgroundTintList(null);
                // Если нужно гарантированно восстановить ресурс из темы:
                // ColorStateList defaultTint = AppCompatResources.getColorStateList(requireContext(), R.color.some_default);
                // v.setBackgroundTintList(defaultTint);
            }
        } catch (Exception ignored) {
            // В некоторых кастомных View метод может отсутствовать; в таком случае можно пробовать v.setBackground(...)
        }
    }


}

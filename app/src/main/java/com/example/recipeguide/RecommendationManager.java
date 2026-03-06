package com.example.recipeguide;

import android.content.Context;
import android.util.Log;

import java.util.*;
import java.util.regex.Pattern;

import Data.DatabaseHandler;
import Model.Event;
import Model.Recipe;
import Model.Tags;
import Utils.VectorUtils;

public class RecommendationManager {
    private final DatabaseHandler db;
    private static final Map<String, Float> WEIGHTS;
    private static final float COSINE_WEIGHT = 0.65f;    // 65% - косинусное сходство
    private static final float TAGS_WEIGHT = 0.35f;      // 35% - теги

    // Веса внутри тегов (нормализованы, сумма = 1.0)
    private static final float DIET_WEIGHT = 0.45f;
    private static final float CUISINE_WEIGHT = 0.30f;
    private static final float CATEGORY_WEIGHT = 0.15f;
    private static final float SKILL_WEIGHT = 0.10f;

    static {
        Map<String, Float> m = new HashMap<>();
        m.put("cook", 3.0f);
        m.put("favorite", 2.0f);
        m.put("view", 0.5f);
        m.put("create", 1.5f);
        m.put("search", 1.0f);
        WEIGHTS = Collections.unmodifiableMap(m);
    }

    public RecommendationManager(Context ctx) {
        db = new DatabaseHandler(ctx);
    }

    public float calculateFinalScore(float cosineScore,
                                     Recipe r, List<Tags> tags, List<Integer> userCategories) {

        // 1. Косинусное сходство
        float normalizedCosine = (cosineScore + 1) / 2;

        // 2. Считаем совпадения по тегам
        float tagScore = getRecipeTags(r, tags, userCategories);

        // 3. Взвешенная сумма
        float finalScore = COSINE_WEIGHT * normalizedCosine + TAGS_WEIGHT * tagScore;

        return finalScore; // в диапазоне 0..1
    }
    public List<String> getUserAllergies() {
        String allergiesString = User.allergy;

        if (allergiesString == null || allergiesString.isEmpty() || allergiesString.equalsIgnoreCase("null")) {
            return null;
        }

        List<String> allergiesList = new ArrayList<>();
        // Разделяем по всем знакам препинания
        String[] allergiesArray = allergiesString.split("\\p{Punct}+");

        for (String allergy : allergiesArray) {
            String trimmed = allergy.trim();
            if (!trimmed.isEmpty()) {
                allergiesList.add(trimmed);
            }
        }

        return allergiesList.isEmpty() ? null : allergiesList;
    }

    // Метод для преобразования строки категорий в список Integer
    private List<Integer> getUserCategoriesFromString(String categoriesString) {
        List<Integer> categories = new ArrayList<>();

        if (categoriesString == null || categoriesString.trim().isEmpty()) {
            return categories; // возвращаем пустой список
        }

        try {
            // Разделяем по запятым с возможными пробелами
            String[] parts = categoriesString.split("\\s*,\\s*");

            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    // Парсим число
                    int category = Integer.parseInt(part.trim());
                    categories.add(category);
                }
            }
        } catch (NumberFormatException e) {
            Log.e("UserCategories", "Ошибка парсинга категорий: " + categoriesString, e);
        }

        return categories;
    }

    private float getRecipeTags(Recipe r, List<Tags> tags, List<Integer> userCategories) {
        float boost = 0f;

        List<Tags> tagsForRecipe = new ArrayList<>();
        for (Tags tag : tags) {
            if (tag.getRecipeId().equals(r.getId())) {
                tagsForRecipe.add(tag);
            }
        }

        //1. Категория
        if (userCategories != null && !userCategories.isEmpty()) {
            if (r != null && userCategories.contains(r.getCategory())) {
                boost += CATEGORY_WEIGHT;
            }
        }

        //2. Диета
        if(User.diet != null && !User.diet.isEmpty()){
            for (Tags tag : tagsForRecipe) {
                if ("diet".equals(tag.getKey()) && User.diet.equalsIgnoreCase(tag.getValue())) {
                    boost += DIET_WEIGHT;
                }
            }
        }

        //3.Любимая кухня
        if(User.likeCuisine != null && !User.likeCuisine.isEmpty()){
            for (Tags tag : tagsForRecipe) {
                if ("cuisine".equals(tag.getKey()) && User.likeCuisine.equalsIgnoreCase(tag.getValue())) {
                    boost += CUISINE_WEIGHT;
                }
            }
        }

        //4. Уровень навыков
        String text = r.getRecipe();
        int paragraphCount = 0;

        if (text != null) {
            // Убираем лишние пробелы в начале/конце
            text = text.trim();

            if (!text.isEmpty()) {
                // Разделяем по одному или нескольким переносам строк
                String[] paragraphs = text.split("\\r?\\n+|\\r");
                paragraphCount = paragraphs.length;
            }
        }

        if(User.skillLevel != null && !User.skillLevel.isEmpty()) {
            if ((User.skillLevel.equals("Beginner") && paragraphCount <= 5) ||(User.skillLevel.equals("Intermediate") && paragraphCount <= 15)){
                boost += SKILL_WEIGHT;
            }
        }

        return boost;
    }

    // lastDays: сколько дней истории учитываем
    public float[] buildUserVector(int lastDays) {
        long sinceTs = System.currentTimeMillis() - (long) lastDays * 24 * 3600 * 1000;
        List<Event> events = db.getRecentEvents(sinceTs);
        if (events == null || events.isEmpty()) return null;

        float[] sumVec = null;
        float totalW = 0f;

        // кеш для recipe vectors чтобы не дергать БД много раз
        Map<String, float[]> recipeVecCache = new HashMap<>();

        for (Event ev : events) {
            String rid = ev.recipeId;
            if (rid == null) continue;
            float[] rv = recipeVecCache.get(rid);
            if (rv == null && !recipeVecCache.containsKey(rid)) {
                Recipe rr = db.getRecipe(rid);
                if (rr != null && rr.getVectors() != null) {
                    rv = VectorUtils.bytesToFloats(rr.getVectors());
                } else rv = null;
                recipeVecCache.put(rid, rv);
            }
            if (rv == null) continue;
            float w = WEIGHTS.containsKey(ev.eventType) ? WEIGHTS.get(ev.eventType) : 0.5f;
            sumVec = VectorUtils.addScaled(sumVec, rv, w);
            totalW += w;
        }

        if (sumVec == null) return null;
        VectorUtils.scaleInPlace(sumVec, 1.0f / Math.max(1e-6f, totalW));
        return sumVec;
    }

    // candidatesK: сколько кандидатов брать из общего ранжирования, finalCount — 3
    public List<String> recommendTopK(float[] userVec, int candidatesK, int finalCount) {
        List<Recipe> all = db.getAllRecipesWithVectors();
        if (all == null || all.isEmpty()) return Collections.emptyList();

        List<Integer> userCategories = getUserCategoriesFromString(User.likeCategory);
        List<String> userAllergies = getUserAllergies();
        List<Tags> recipeTags = db.getRecipeTags();

        PriorityQueue<Map.Entry<String, Float>> pq = new PriorityQueue<>(Comparator.comparing(Map.Entry::getValue));
        Map<String, float[]> vecCache = new HashMap<>();
        Map<String, Recipe> recipeMap = new HashMap<>(); // Сохраняем объекты рецептов

        for (Recipe r : all) {
            if (r.getVectors() == null) continue;
            if (userAllergies != null && !userAllergies.isEmpty()) {
                String ingredients = r.getIngredient_en().toLowerCase().trim();

                boolean skipRecipe = false;
                for (String allergy : userAllergies) {
                    String normalizedAllergy = allergy.toLowerCase().trim();

                    // Более точная проверка (целое слово или часть слова)
                    if (ingredients.contains(normalizedAllergy)) {
                        // Дополнительная проверка, чтобы "орехи" не совпало с "ореховидный"
                        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(normalizedAllergy) + "\\b");
                        if (pattern.matcher(ingredients).find()) {
                            skipRecipe = true;
                            break;
                        }
                    }
                }

                if (skipRecipe) {
                    continue; // Пропускаем рецепт с аллергенами
                }
            }
            float[] rv = VectorUtils.bytesToFloats(r.getVectors());
            vecCache.put(r.getId(), rv);
            recipeMap.put(r.getId(), r);
            float score = VectorUtils.cosine(userVec, rv);
            float finalScore = calculateFinalScore(score, recipeMap.get(r.getId()), recipeTags, userCategories);
            if (pq.size() < candidatesK) {
                pq.offer(new AbstractMap.SimpleEntry<>(r.getId(), finalScore));
            } else if (finalScore > pq.peek().getValue()) {
                pq.poll();
                pq.offer(new AbstractMap.SimpleEntry<>(r.getId(), finalScore));
            }
        }

        // candidates sorted desc
        List<Map.Entry<String, Float>> candidates = new ArrayList<>();
        while (!pq.isEmpty()) candidates.add(pq.poll());
        Collections.reverse(candidates);

        // greedy diversity
        List<String> selected = new ArrayList<>();
        List<float[]> selectedVecs = new ArrayList<>();

        for (Map.Entry<String, Float> cand : candidates) {
            if (selected.size() >= finalCount) break;
            String rid = cand.getKey();
            float[] rv = vecCache.get(rid);
            boolean skip = false;
            for (float[] sv : selectedVecs) {
                if (VectorUtils.cosine(sv, rv) > 0.92f) {
                    skip = true;
                    break;
                }
            }
            if (!skip) {
                selected.add(rid);
                selectedVecs.add(rv);
            }
        }
        // если мало — дозабить сверху
        if (selected.size() < finalCount) {
            for (Map.Entry<String, Float> cand : candidates) {
                if (selected.size() >= finalCount) break;
                if (!selected.contains(cand.getKey())) selected.add(cand.getKey());
            }
        }
        return selected;
    }

    // полная операция: строим user vector и возвращаем top3
    public List<String> generateTop3ForUser() {
        float[] userVec = buildUserVector(90);
        if (userVec == null) {
            // fallback: первые три рецепта
            List<Recipe> all = db.getAllRecipesWithVectors();
            List<String> fallback = new ArrayList<>();
            for (int i = 0; i < Math.min(3, all.size()); i++) fallback.add(all.get(i).getId());
            return fallback;
        }
        return recommendTopK(userVec, 50, 3);
    }
}


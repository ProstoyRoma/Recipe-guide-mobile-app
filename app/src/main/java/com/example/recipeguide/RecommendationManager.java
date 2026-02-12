package com.example.recipeguide;

import android.content.Context;

import java.util.*;
import java.util.regex.Pattern;

import Data.DatabaseHandler;
import Model.Event;
import Model.Recipe;
import Utils.VectorUtils;

public class RecommendationManager {
    private final DatabaseHandler db;
    private static final Map<String, Float> WEIGHTS;
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
    public List<String> getUserAllergies() {
        String allergiesString = db.getAllergiesForUser(User.username);
        db.close();

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
    // lastDays: сколько дней истории учитываем
    public float[] buildUserVector(String userId, int lastDays) {
        long sinceTs = System.currentTimeMillis() - (long) lastDays * 24 * 3600 * 1000;
        List<Event> events = db.getRecentEvents(userId, sinceTs);
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

        List<String> userAllergies = getUserAllergies();
        PriorityQueue<Map.Entry<String, Float>> pq = new PriorityQueue<>(Comparator.comparing(Map.Entry::getValue));
        Map<String, float[]> vecCache = new HashMap<>();

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
            float score = VectorUtils.cosine(userVec, rv);
            if (pq.size() < candidatesK) {
                pq.offer(new AbstractMap.SimpleEntry<>(r.getId(), score));
            } else if (score > pq.peek().getValue()) {
                pq.poll();
                pq.offer(new AbstractMap.SimpleEntry<>(r.getId(), score));
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
                if (VectorUtils.cosine(sv, rv) > 0.92f) { skip = true; break; }
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
    public List<String> generateTop3ForUser(String userId) {
        float[] userVec = buildUserVector(userId, 90);
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


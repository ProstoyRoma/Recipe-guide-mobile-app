package Model;

import com.google.gson.Gson;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestionnaireModel implements Serializable {
    private List<String> allergens;
    private String diet;
    private int maxCookTimeMin;
    private String skillLevel;
    private List<String> favoriteCuisines;
    private Map<String, List<String>> ingredientPreferences; // keys: "liked", "disliked"
    private List<String> equipment;
    private String budgetPerServing;

    public QuestionnaireModel() {
        this.allergens = new ArrayList<>();
        this.diet = "";
        this.maxCookTimeMin = 30;
        this.skillLevel = "";
        this.favoriteCuisines = new ArrayList<>();
        this.ingredientPreferences = new HashMap<>();
        this.ingredientPreferences.put("liked", new ArrayList<>());
        this.ingredientPreferences.put("disliked", new ArrayList<>());
        this.equipment = new ArrayList<>();
        this.budgetPerServing = "medium";
    }

    // Getters and setters
    public List<String> getAllergens() { return allergens; }
    public void setAllergens(List<String> allergens) { this.allergens = allergens; }

    public String getDiet() { return diet; }
    public void setDiet(String diet) { this.diet = diet; }

    public int getMaxCookTimeMin() { return maxCookTimeMin; }
    public void setMaxCookTimeMin(int maxCookTimeMin) { this.maxCookTimeMin = maxCookTimeMin; }

    public String getSkillLevel() { return skillLevel; }
    public void setSkillLevel(String skillLevel) { this.skillLevel = skillLevel; }

    public List<String> getFavoriteCuisines() { return favoriteCuisines; }
    public void setFavoriteCuisines(List<String> favoriteCuisines) { this.favoriteCuisines = favoriteCuisines; }

    public Map<String, List<String>> getIngredientPreferences() { return ingredientPreferences; }
    public void setIngredientPreferences(Map<String, List<String>> ingredientPreferences) {
        this.ingredientPreferences = ingredientPreferences;
    }

    public List<String> getEquipment() { return equipment; }
    public void setEquipment(List<String> equipment) { this.equipment = equipment; }

    public String getBudgetPerServing() { return budgetPerServing; }
    public void setBudgetPerServing(String budgetPerServing) { this.budgetPerServing = budgetPerServing; }

    // Convenience helpers
    public void addAllergen(String allergen) {
        if (!this.allergens.contains(allergen)) this.allergens.add(allergen);
    }

    public void removeAllergen(String allergen) {
        this.allergens.remove(allergen);
    }

    public void addLikedIngredient(String ingredient) {
        this.ingredientPreferences.computeIfAbsent("liked", k -> new ArrayList<>()).add(ingredient);
    }

    public void addDislikedIngredient(String ingredient) {
        this.ingredientPreferences.computeIfAbsent("disliked", k -> new ArrayList<>()).add(ingredient);
    }

    // JSON serialization
    public String toJson() {
        return new Gson().toJson(this);
    }

    public static QuestionnaireModel fromJson(String json) {
        return new Gson().fromJson(json, QuestionnaireModel.class);
    }

    // Build payload wrapper (user_id, locale, meta) for server if needed
    public Map<String, Object> toPayload(String userId, String locale, String source) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("locale", locale);
        payload.put("onboarding_completed", true);
        payload.put("profile", this);

        Map<String, Object> meta = new HashMap<>();
        meta.put("created_at", Instant.now().toString());
        meta.put("source", source != null ? source : "onboarding_v1");
        payload.put("meta", meta);

        return payload;
    }
}

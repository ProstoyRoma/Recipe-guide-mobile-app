package com.example.recipeguide;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FilterOptions {
    private Set<String> selectedCategories;  // Множественный выбор категорий
    private Set<String> selectedCuisines;    // Множественный выбор кухонь
    private Set<String> selectedDiets;       // Множественный выбор диет
    private Integer maxCookingTime;
    private String ingredients;

    public FilterOptions() {
        this.selectedCategories = new HashSet<>();
        this.selectedCuisines = new HashSet<>();
        this.selectedDiets = new HashSet<>();
        this.maxCookingTime = null;
        this.ingredients = null;
    }

    public Set<String> getSelectedCategories() { return selectedCategories; }
    public void setSelectedCategories(Set<String> selectedCategories) { this.selectedCategories = selectedCategories; }
    public void addCategory(String category) { this.selectedCategories.add(category); }
    public void removeCategory(String category) { this.selectedCategories.remove(category); }

    public Set<String> getSelectedCuisines() { return selectedCuisines; }
    public void setSelectedCuisines(Set<String> selectedCuisines) { this.selectedCuisines = selectedCuisines; }
    public void addCuisine(String cuisine) { this.selectedCuisines.add(cuisine); }
    public void removeCuisine(String cuisine) { this.selectedCuisines.remove(cuisine); }

    public Set<String> getSelectedDiets() { return selectedDiets; }
    public void setSelectedDiets(Set<String> selectedDiets) { this.selectedDiets = selectedDiets; }
    public void addDiet(String diet) { this.selectedDiets.add(diet); }
    public void removeDiet(String diet) { this.selectedDiets.remove(diet); }

    public Integer getMaxCookingTime() { return maxCookingTime; }
    public void setMaxCookingTime(Integer maxCookingTime) { this.maxCookingTime = maxCookingTime; }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public boolean isEmpty() {
        return selectedCategories.isEmpty() &&
                selectedCuisines.isEmpty() &&
                selectedDiets.isEmpty() &&
                maxCookingTime == null;
    }

    public void clear() {
        selectedCategories.clear();
        selectedCuisines.clear();
        selectedDiets.clear();
        maxCookingTime = null;
    }
}
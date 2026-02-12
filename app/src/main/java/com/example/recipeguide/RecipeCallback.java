package com.example.recipeguide;

import java.util.ArrayList;

import Model.Recipe;

public interface RecipeCallback {
    void onRecipesLoaded(ArrayList<String> dishes);
    //void onError(Exception e);
}

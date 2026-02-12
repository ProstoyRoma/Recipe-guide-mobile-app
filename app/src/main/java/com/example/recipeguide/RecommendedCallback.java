package com.example.recipeguide;

public interface RecommendedCallback {
    void onSuccess();       // вызов при успешном завершении getRecommendedRecipe
    void onFailure(Exception e); // опционально — при ошибке
}

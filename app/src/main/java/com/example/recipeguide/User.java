package com.example.recipeguide;

import java.util.ArrayList;

public class User {
    public static String username = "Username";
    public static String userImage = null;
    public static String allergy = null;
    public static String diet = null;
    public static String likeCategory = null;
    public static String likeCuisine = null;
    public static String skillLevel = null;
    public User(){}

    public static void updateFromQuestionnaire(String allergies,String diet,String cuisine, String categories, String skillLevel) {
        User.allergy = allergies;
        User.diet = diet;
        User.likeCategory = categories;
        User.likeCuisine = cuisine;
        User.skillLevel = skillLevel;
    }

}

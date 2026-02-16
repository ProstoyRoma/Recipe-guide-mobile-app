package com.example.recipeguide;

import java.util.ArrayList;

public class User {
    public static String username = "Username";
    public static String userImage = null;
    public static String allergy = null;
    public static String likeCategory = null;
    public User(){}

    public static void updateFromQuestionnaire(String allergies, String categories) {
        User.allergy = allergies;
        User.likeCategory = categories;
    }

}

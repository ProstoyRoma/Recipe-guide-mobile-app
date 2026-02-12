package com.example.recipeguide;

import java.util.ArrayList;

public class User {
    public static String username = "Username";
    public static String userImage = null;
    public static String allergy = null;
    public static ArrayList <Integer> likeCategory = null;
    public User(){}

    public static void updateFromQuestionnaire(String allergies, ArrayList<Integer> categories) {
        User.allergy = allergies;
        User.likeCategory = categories;
    }

}

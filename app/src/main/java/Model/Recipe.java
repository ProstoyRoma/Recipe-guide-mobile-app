package Model;

public class Recipe {

    private String id;
    private String name;
    private String name_en;
    private String image;
    private int cookingTime;
    private String recipe;
    private String recipe_en;
    private String ingredient;
    private String ingredient_en;
    private int isFavorite;
    private int isCook;
    private int isColdStart;
    private int category;
    private String ingredient_parsed;
    private byte[] vector;


    public Recipe() {
    }
    public Recipe(String id, String name, String image, int cookingTime, String recipe, String ingredient, int isFavorite) {
        this.ingredient = ingredient;
        this.id = id;
        this.name = name;
        this.image = image;
        this.cookingTime = cookingTime;
        this.recipe = recipe;
        this.isFavorite = isFavorite;
    }
    public Recipe(String id, String name,String name_en, String image, int cookingTime, String recipe, String recipe_en,
                  String ingredient, String ingredient_en, int isFavorite, int isCook, int category, String ingredient_parsed, byte[] vector) {
        this.id = id;
        this.name = name;
        this.name_en = name_en;
        this.image = image;
        this.cookingTime = cookingTime;
        this.recipe = recipe;
        this.recipe_en = recipe_en;
        this.ingredient = ingredient;
        this.ingredient_en = ingredient_en;
        this.isFavorite = isFavorite;
        this.isCook = isCook;
        this.category = category;
        this.ingredient_parsed = ingredient_parsed;
        this.vector = vector;
    }

    public Recipe(String id, String name,String name_en, String image, int cookingTime){
        this.id = id;
        this.name = name;
        this.name_en = name_en;
        this.image = image;
        this.cookingTime = cookingTime;
    }
    public Recipe(String id, String name,String name_en, String image, int cookingTime, String ingredient, String ingredient_en){
        this.id = id;
        this.name = name;
        this.name_en = name_en;
        this.image = image;
        this.cookingTime = cookingTime;
        this.ingredient = ingredient;
        this.ingredient_en = ingredient_en;
    }
    public Recipe(String name, String image, int cookingTime, String recipe, String ingredient, int isFavorite) {
        this.name = name;
        this.image = image;
        this.cookingTime = cookingTime;
        this.recipe = recipe;
        this.ingredient = ingredient;
        this.isFavorite = isFavorite;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName_en() {
        return name_en;
    }

    public void setName_en(String name_en) {
        this.name_en = name_en;
    }

    public String getRecipe_en() {
        return recipe_en;
    }

    public void setRecipe_en(String recipe_en) {
        this.recipe_en = recipe_en;
    }

    public String getIngredient_en() {
        return ingredient_en;
    }

    public void setIngredient_en(String ingredient_en) {
        this.ingredient_en = ingredient_en;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getCookingTime() {
        return cookingTime;
    }

    public void setCookingTime(int cookingTime) {
        this.cookingTime = cookingTime;
    }

    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    public String getIngredient() {
        return ingredient;
    }

    public void setIngredient(String ingredient) {
        this.ingredient = ingredient;
    }

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    public int getIsFavorite() {
        return isFavorite;
    }

    public void setIsFavorite(int isFavorite) {
        this.isFavorite = isFavorite;
    }

    public int getIsCook() {
        return isCook;
    }

    public void setIsCook(int isCook) {
        this.isCook = isCook;
    }

    public int getIsColdStart() {
        return isColdStart;
    }

    public void setIsColdStart(int isColdStart) {
        this.isColdStart = isColdStart;
    }

    public byte[] getVectors() {
        return vector;
    }

    public void setVectors(byte[] vector) {
        this.vector = vector;
    }

    public String getIngredient_parsed() {
        return ingredient_parsed;
    }

    public void setIngredient_parsed(String ingredient_parsed) {
        this.ingredient_parsed = ingredient_parsed;
    }
}

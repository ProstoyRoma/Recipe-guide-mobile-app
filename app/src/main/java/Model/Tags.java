package Model;

public class Tags {

    public final String recipeId;
    public final String key;
    public final String value;
    public Tags(String recipeId, String key, String value){
        this.recipeId = recipeId;
        this.key = key;
        this.value = value;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}

package Model;

public class Tags {


    public final String tagId;
    public final String recipeId;
    public final String key;
    public final String value;
    public Tags(String tagId, String recipeId, String key, String value){
        this.tagId = tagId;
        this.recipeId = recipeId;
        this.key = key;
        this.value = value;
    }


    public String getTagId() {
        return tagId;
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

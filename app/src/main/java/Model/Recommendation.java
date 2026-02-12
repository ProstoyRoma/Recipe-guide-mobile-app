package Model;

public class Recommendation {
    public final String recipeIdsJson;
    public final long generatedAt;
    public Recommendation(String recipeIdsJson, long generatedAt) {
        this.recipeIdsJson = recipeIdsJson;
        this.generatedAt = generatedAt;
    }
}

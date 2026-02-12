package Model;

public class Event {
    public final String eventId;
    public final String userId;
    public final String eventType;
    public final String recipeId;
    public final long tsLocal;
    public Event(String eventId, String userId, String eventType, String recipeId, long tsLocal) {
        this.eventId = eventId; this.userId = userId; this.eventType = eventType; this.recipeId = recipeId; this.tsLocal = tsLocal;
    }
}

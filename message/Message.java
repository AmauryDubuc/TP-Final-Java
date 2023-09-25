package message;

import org.json.JSONException;
import org.json.JSONObject;

public class Message {
    private String type; // mot clef pour representer le type connexion envoyer deco etc
    private String content; // le contenu du message
    private String sender; // str qui represente l'auteur

    public Message(String type, String content, String sender) {
        this.type = type;
        this.content = content;
        this.sender = sender;
    }

    // Constructeur sans auteur pour les messages server
    public Message(String type, String content) {
        this.type = type;
        this.content = content;
        this.sender = null;
    }

    // getters et setters

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    // methode pour sérialiser l'objet en JSON
    public JsonObject toJson() {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("contenu", contenu);
        jsonObjectBuilder.add("expediteur", expéditeur);
        return jsonObjectBuilder.build();
    }

    // methode static pour désérialiser un objet JSON en objet java
    public static Message fromJson(JsonObject jsonObject) {
        String contenu = jsonObject.getString("contenu");
        String expéditeur = jsonObject.getString("expediteur");

        return new Message(contenu, expéditeur);
    }
}

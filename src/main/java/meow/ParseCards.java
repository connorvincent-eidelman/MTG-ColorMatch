package meow;

import com.google.gson.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ParseCards {

    public static void main(String[] args) throws IOException {
        String jsonFilePath = "unique-artwork.json";
        String json = new String(Files.readAllBytes(Paths.get(jsonFilePath)));

        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        JsonArray cardsArray = jsonObject.getAsJsonArray("data");

        // Example of iterating over the cards and extracting relevant fields
        for (JsonElement cardElement : cardsArray) {
            JsonObject card = cardElement.getAsJsonObject();
            String cardName = card.get("name").getAsString();
            String imageUrl = card.get("image_uris").getAsJsonObject().get("normal").getAsString();

            System.out.println("Card Name: " + cardName);
            System.out.println("Image URL: " + imageUrl);

            // Here you would call your RGB extraction code on the imageUrl
        }
    }
}


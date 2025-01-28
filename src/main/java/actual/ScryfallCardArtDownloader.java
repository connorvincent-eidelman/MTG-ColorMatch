package actual;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ScryfallCardArtDownloader {

    private static final String API_URL = "https://api.scryfall.com/cards/search?q=set%3Aall";
    private static final String IMAGE_DIR = "card_images/";

    public static void main(String[] args) {
        try {
            // Create the image directory if it doesn't exist
            File dir = new File(IMAGE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Track downloaded images to avoid duplicates
            Set<String> downloadedImages = new HashSet<>();

            // Start with the first page of results
            String apiUrl = API_URL;
            while (apiUrl != null) {
                // Send the GET request to the API
                String response = sendGetRequest(apiUrl);

                // Parse the JSON response
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();

                // Extract the card data
                JsonArray cards = jsonResponse.getAsJsonArray("data");

                for (int i = 0; i < cards.size(); i++) {
                    JsonObject card = cards.get(i).getAsJsonObject();

                    // Get card image URL (card face image)
                    JsonObject imageUris = card.getAsJsonObject("image_uris");
                    if (imageUris != null) {
                        String imageUrl = imageUris.get("normal").getAsString();

                        // Only download unique images
                        if (!downloadedImages.contains(imageUrl)) {
                            downloadedImages.add(imageUrl);
                            downloadImage(imageUrl);
                        }
                    }
                }

                // Check if there's a next page
                apiUrl = jsonResponse.has("next_page") ? jsonResponse.get("next_page").getAsString() : null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to send a GET request to the Scryfall API
    private static String sendGetRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        // Read the response
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    // Method to download an image from a URL
    private static void downloadImage(String imageUrl) {
        try {
            // Open connection to image URL
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // Get the file name from the URL (last part of the URL)
            String[] urlParts = imageUrl.split("/");
            String fileName = urlParts[urlParts.length - 1];

            // Create output file stream
            File outputFile = new File(IMAGE_DIR + fileName);
            InputStream in = connection.getInputStream();
            FileOutputStream out = new FileOutputStream(outputFile);

            // Read and write the image data
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            // Close streams
            in.close();
            out.close();
            System.out.println("Downloaded: " + fileName);
        } catch (IOException e) {
            System.err.println("Failed to download image: " + imageUrl);
            e.printStackTrace();
        }
    }
}

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ScryfallCardArtDownloader {

    private static final String API_URL = "https://api.scryfall.com/cards/search?q=new%3Aart+include%3Aextras&order=name&as=grid&unique=prints";
    private static final String IMAGE_DIR = "card_images/";
    private static final int THREAD_POOL_SIZE = 10; // For concurrent downloads
    private static final int MAX_RETRIES = 5;

    public static void main(String[] args) {
        try {
            File dir = new File(IMAGE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            Set<String> downloadedImages = new HashSet<>();
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

            String apiUrl = API_URL; // Start from the first page of cards
            int pageCount = 0;

            while (apiUrl != null) {
                pageCount++;
                System.out.println("Fetching page: " + pageCount);

                String response = sendGetRequest(apiUrl);
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                JsonArray cards = jsonResponse.getAsJsonArray("data");

                for (int i = 0; i < cards.size(); i++) {
                    JsonObject card = cards.get(i).getAsJsonObject();

                    // Check for single-faced card images
                    if (card.has("image_uris")) {
                        String imageUrl = card.getAsJsonObject("image_uris").get("art_crop").getAsString();
                        submitDownloadTask(executor, downloadedImages, imageUrl);
                    }

                    // Check for double-faced card images
                    if (card.has("card_faces")) {
                        JsonArray faces = card.getAsJsonArray("card_faces");
                        for (int j = 0; j < faces.size(); j++) {
                            JsonObject face = faces.get(j).getAsJsonObject();
                            if (face.has("image_uris")) {
                                String faceImageUrl = face.getAsJsonObject("image_uris").get("art_crop").getAsString();
                                submitDownloadTask(executor, downloadedImages, faceImageUrl);
                            }
                        }
                    }
                }

                // Get the next page URL from the API response
                apiUrl = jsonResponse.has("next_page") ? jsonResponse.get("next_page").getAsString() : null;
            }

            // Wait for all tasks to complete before shutting down
            executor.shutdown();
            while (!executor.isTerminated()) {
                Thread.sleep(1000); // Check every second if all tasks are finished
            }

            System.out.println("Download complete!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String sendGetRequest(String urlString) throws IOException {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                System.out.println("Sending request to: " + urlString);
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                // Add a small delay to avoid hitting the rate limit
                Thread.sleep(100); // 100ms delay between requests

                int responseCode = connection.getResponseCode();
                System.out.println("Response code: " + responseCode);
                if (responseCode == 429) { // Rate limit hit
                    System.out.println("Rate limit hit. Retrying in 2 seconds...");
                    Thread.sleep(2000);
                    attempts++;
                    continue;
                } else if (responseCode != 200) {
                    throw new IOException("Failed request: HTTP " + responseCode);
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return response.toString();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted during retry", e);
            }
        }
        throw new IOException("Failed to fetch after multiple attempts.");
    }

    private static void submitDownloadTask(ExecutorService executor, Set<String> downloadedImages, String imageUrl) {
        synchronized (downloadedImages) {
            if (!downloadedImages.contains(imageUrl)) {
                downloadedImages.add(imageUrl);
                executor.execute(() -> downloadImage(imageUrl));
            }
        }
    }

    private static void downloadImage(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // Sanitize file name (remove query parameters and special chars)
            String[] urlParts = imageUrl.split("/");
            String fileName = urlParts[urlParts.length - 1].split("\\?")[0].replaceAll("[^a-zA-Z0-9.-]", "_");

            File outputFile = new File(IMAGE_DIR + fileName);
            try (InputStream in = connection.getInputStream();
                    FileOutputStream out = new FileOutputStream(outputFile)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }

                System.out.println("Downloaded: " + fileName);
            }
        } catch (IOException e) {
            System.err.println("Failed to download image: " + imageUrl);
            e.printStackTrace();
        }
    }
}

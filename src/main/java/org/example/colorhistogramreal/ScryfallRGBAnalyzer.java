package org.example.colorhistogramreal;

import com.google.gson.*; // Ensure GSON is in classpath
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class ScryfallRGBAnalyzer {

    private static final String BULK_DATA_URL = "https://api.scryfall.com/bulk-data";
    private static final String OUTPUT_FILE = "rgb_histogram_data.txt";

    public static void main(String[] args) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE))) {
            // Step 1: Fetch bulk data metadata from Scryfall
            JsonObject bulkData = fetchJsonFromUrl(BULK_DATA_URL);
            String cardDataUrl = bulkData.getAsJsonArray("data").get(0).getAsJsonObject().get("download_uri").getAsString();

            // Step 2: Fetch card data JSON
            JsonArray cards = fetchJsonFromUrl(cardDataUrl).getAsJsonArray("data");

            // Step 3: Iterate through cards and analyze RGB data
            int processedCount = 0;
            for (JsonElement cardElement : cards) {
                JsonObject card = cardElement.getAsJsonObject();
                JsonObject imageUris = card.getAsJsonObject("image_uris");

                if (imageUris != null) {
                    String imageUrl = imageUris.get("normal").getAsString(); // Get the normal resolution URL
                    String cardName = card.get("name").getAsString();

                    // Fetch the image
                    BufferedImage image = fetchImageFromUrl(imageUrl);
                    if (image != null) {
                        // Analyze the RGB data
                        int[] redHistogram = new int[256];
                        int[] greenHistogram = new int[256];
                        int[] blueHistogram = new int[256];
                        analyzeRGB(image, redHistogram, greenHistogram, blueHistogram);

                        // Write histogram data to file
                        writeHistogramData(writer, cardName, redHistogram, greenHistogram, blueHistogram);
                        processedCount++;
                        System.out.println("Processed and saved: " + cardName);
                    }

                    // Handle rate limiting
                    Thread.sleep(100); // Increased delay to avoid overloading the server
                }
            }

            System.out.println("Processed " + processedCount + " card arts. Data saved to " + OUTPUT_FILE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper method to fetch JSON data from a URL
    private static JsonObject fetchJsonFromUrl(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        // Check HTTP status code
        int statusCode = connection.getResponseCode();
        if (statusCode != HttpURLConnection.HTTP_OK) {
            System.out.println("Error fetching data, status code: " + statusCode);
            throw new IOException("Failed to fetch data from Scryfall API. HTTP status: " + statusCode);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        // Print the raw JSON response for debugging
        System.out.println("API Response: " + response.toString());

        // Parse the response JSON
        return JsonParser.parseString(response.toString()).getAsJsonObject();
    }

    // Helper method to fetch an image from a URL
    private static BufferedImage fetchImageFromUrl(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            InputStream inputStream = connection.getInputStream();
            return ImageIO.read(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Helper method to analyze RGB data of an image
    private static void analyzeRGB(BufferedImage image, int[] redHistogram, int[] greenHistogram, int[] blueHistogram) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;

                redHistogram[red]++;
                greenHistogram[green]++;
                blueHistogram[blue]++;
            }
        }
    }

    // Helper method to write histogram data to a file
    private static void writeHistogramData(BufferedWriter writer, String cardName, int[] redHistogram, int[] greenHistogram, int[] blueHistogram) throws IOException {
        writer.write("Card: " + cardName + "\n");
        writer.write("Red Histogram: " + histogramToString(redHistogram) + "\n");
        writer.write("Green Histogram: " + histogramToString(greenHistogram) + "\n");
        writer.write("Blue Histogram: " + histogramToString(blueHistogram) + "\n");
        writer.write("\n"); // Add a blank line for readability
    }

    // Helper method to convert a histogram array to a string
    private static String histogramToString(int[] histogram) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < histogram.length; i++) {
            if (histogram[i] > 0) {
                sb.append(i).append(":").append(histogram[i]).append(" ");
            }
        }
        return sb.toString();
    }
}

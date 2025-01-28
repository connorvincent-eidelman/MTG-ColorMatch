package org.example.colorhistogramreal;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class ScryfallImageFetcher {
    public static BufferedImage fetchImageFromUrl(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            connection.setConnectTimeout(5000); // 5 seconds to establish connection
            connection.setReadTimeout(5000);    // 5 seconds to read data

            // Set a custom User-Agent
            connection.setRequestProperty("User-Agent", "MyApp/1.0");

            InputStream inputStream = connection.getInputStream();
            return ImageIO.read(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
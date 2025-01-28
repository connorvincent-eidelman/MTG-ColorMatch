package meow;

import java.io.*;
import java.net.URL;
import java.net.HttpURLConnection;

public class DownloadFile {

    public static void downloadFile(String fileUrl, String destinationPath) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(destinationPath)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String fileUrl = "https://data.scryfall.io/unique-artwork/unique-artwork-20250123100413.json";
        String destinationPath = "unique-artwork.json";

        downloadFile(fileUrl, destinationPath);
        System.out.println("File downloaded to: " + destinationPath);
    }
}

package actual;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Arrays;
import java.util.Iterator;
import com.google.gson.Gson;

public class ColorHistogramDatabase {

    private static final String DB_URL = "jdbc:sqlite:ColorHistogram.db";

    public static void main(String[] args) {
        String imageDirectoryPath = "/Users/connorv-e/colorhistogramreal/card_images"; // Image folder path

        try (Connection connection = DriverManager.getConnection(DB_URL)) {
            createDatabaseTable(connection);

            File imageDirectory = new File(imageDirectoryPath);
            if (imageDirectory.isDirectory()) {
                for (File file : imageDirectory.listFiles()) {
                    if (isImageFile(file)) {
                        System.out.println("Processing: " + file.getName());
                        BufferedImage image = safeReadImage(file);
                        if (image != null) {
                            HistogramData histogramData = calculateColorHistogram(image);
                            saveHistogramToDatabase(connection, file.getName(), histogramData);
                        } else {
                            System.err.println("Skipping unreadable image: " + file.getName());
                        }
                    }
                }
            } else {
                System.err.println("Provided path is not a directory.");
            }

            fetchAndDisplayData(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createDatabaseTable(Connection connection) throws SQLException {
        String createTableSQL = """
                CREATE TABLE IF NOT EXISTS ColorHistogram (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    filename TEXT NOT NULL,
                    red_histogram TEXT NOT NULL,
                    green_histogram TEXT NOT NULL,
                    blue_histogram TEXT NOT NULL
                );
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
        }
    }

    private static boolean isImageFile(File file) {
        String[] supportedExtensions = { ".jpg", ".jpeg", ".png", ".bmp", ".gif" };
        return Arrays.stream(supportedExtensions).anyMatch(file.getName().toLowerCase()::endsWith);
    }

    private static BufferedImage safeReadImage(File file) {
        try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                System.err.println("No ImageReader found for: " + file.getName());
                return null;
            }

            ImageReader reader = readers.next();
            reader.setInput(input);

            BufferedImage image = reader.read(0);
            reader.dispose(); // Cleanup
            return image;
        } catch (IOException e) {
            System.err.println("Error reading image: " + file.getName() + " -> " + e.getMessage());
            return null;
        }
    }

    private static HistogramData calculateColorHistogram(BufferedImage image) {
        int[] redHistogram = new int[256];
        int[] greenHistogram = new int[256];
        int[] blueHistogram = new int[256];

        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                redHistogram[(pixel >> 16) & 0xff]++;
                greenHistogram[(pixel >> 8) & 0xff]++;
                blueHistogram[pixel & 0xff]++;
            }
        }
        return new HistogramData(redHistogram, greenHistogram, blueHistogram);
    }

    private static void saveHistogramToDatabase(Connection connection, String fileName, HistogramData histogramData)
            throws SQLException {
        String insertSQL = """
                INSERT INTO ColorHistogram (filename, red_histogram, green_histogram, blue_histogram) VALUES (?, ?, ?, ?);
                """;
        Gson gson = new Gson();
        try (PreparedStatement stmt = connection.prepareStatement(insertSQL)) {
            stmt.setString(1, fileName.replaceAll("\\.\\w+$", ""));
            stmt.setString(2, gson.toJson(histogramData.redHistogram));
            stmt.setString(3, gson.toJson(histogramData.greenHistogram));
            stmt.setString(4, gson.toJson(histogramData.blueHistogram));
            stmt.executeUpdate();
        }
    }

    private static void fetchAndDisplayData(Connection connection) throws SQLException {
        String querySQL = "SELECT filename, red_histogram, green_histogram, blue_histogram FROM ColorHistogram;";
        try (PreparedStatement stmt = connection.prepareStatement(querySQL)) {
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                System.out.println("Filename: " + resultSet.getString("filename"));
                System.out.println("Red Histogram: " + resultSet.getString("red_histogram"));
                System.out.println("Green Histogram: " + resultSet.getString("green_histogram"));
                System.out.println("Blue Histogram: " + resultSet.getString("blue_histogram"));
                System.out.println();
            }
        }
    }

    // Class to hold histogram data
    private static class HistogramData {
        int[] redHistogram;
        int[] greenHistogram;
        int[] blueHistogram;

        public HistogramData(int[] redHistogram, int[] greenHistogram, int[] blueHistogram) {
            this.redHistogram = redHistogram;
            this.greenHistogram = greenHistogram;
            this.blueHistogram = blueHistogram;
        }
    }
}

package actual;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import javax.imageio.ImageIO;

import com.google.gson.Gson;

public class ColorHistogramDatabase {

    private static final String DB_URL = "jdbc:sqlite:ColorHistogram.db";

    @SuppressWarnings("CallToPrintStackTrace")
    public static void main(String[] args) {
        String imageDirectoryPath = "/Users/connorv-e/Desktop"; // Path to the directory with images

        try (Connection connection = DriverManager.getConnection(DB_URL)) {
            createDatabaseTable(connection);

            // Process images and store detailed histograms in the database
            File imageDirectory = new File(imageDirectoryPath);
            if (imageDirectory.isDirectory()) {
                for (File file : imageDirectory.listFiles()) {
                    if (isImageFile(file)) {
                        System.out.println("Processing: " + file.getName());
                        HistogramData histogramData = calculateColorHistogram(file);
                        saveHistogramToDatabase(connection, file.getName(), histogramData);
                    }
                }
            } else {
                System.err.println("Provided path is not a directory.");
            }

            // Retrieve and print the stored histograms from the database
            fetchAndDisplayData(connection);

        } catch (SQLException | IOException e) {
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
        String fileName = file.getName().toLowerCase();
        return Arrays.stream(supportedExtensions).anyMatch(fileName::endsWith);
    }

    private static HistogramData calculateColorHistogram(File imageFile) throws IOException {
        BufferedImage image = ImageIO.read(imageFile);

        int[] redHistogram = new int[256];
        int[] greenHistogram = new int[256];
        int[] blueHistogram = new int[256];

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
            stmt.setString(2, gson.toJson(histogramData.redHistogram)); // Convert histogram to JSON
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
                String filename = resultSet.getString("filename");
                String redHistogram = resultSet.getString("red_histogram");
                String greenHistogram = resultSet.getString("green_histogram");
                String blueHistogram = resultSet.getString("blue_histogram");

                System.out.println("Filename: " + filename);
                System.out.println("Red Histogram: " + redHistogram);
                System.out.println("Green Histogram: " + greenHistogram);
                System.out.println("Blue Histogram: " + blueHistogram);
                System.out.println();
            }
        }
    }

    // Data class to hold histogram arrays
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

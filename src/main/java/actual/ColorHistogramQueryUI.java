package actual;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ColorHistogramQueryUI extends Application {

    private static final String DB_URL = "jdbc:sqlite:ColorHistogram.db";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // UI Elements
        Label redLabel = new Label("Red:");
        Label greenLabel = new Label("Green:");
        Label blueLabel = new Label("Blue:");
        Label toleranceLabel = new Label("Tolerance:");

        // Text fields for RGB values
        TextField redTextField = createColorTextField();
        TextField greenTextField = createColorTextField();
        TextField blueTextField = createColorTextField();

        // Text field for tolerance
        TextField toleranceTextField = createColorTextField();

        // Label to display tolerance value (for user reference)
        Label toleranceValueLabel = new Label("Tolerance (0-255):");

        // Button to trigger the search
        Button searchButton = new Button("Find Closest Images");

        // Text Area to display results
        TextArea resultTextArea = new TextArea();
        resultTextArea.setEditable(false);
        resultTextArea.setPrefHeight(200);

        // Layout
        VBox root = new VBox(10);
        root.setPadding(new javafx.geometry.Insets(20));

        root.getChildren().addAll(redLabel, redTextField,
                greenLabel, greenTextField,
                blueLabel, blueTextField,
                toleranceLabel, toleranceTextField, toleranceValueLabel,
                searchButton,
                resultTextArea);

        // Action for Search Button
        searchButton.setOnAction(event -> {
            // Get RGB values from the text fields
            int red = parseColorInput(redTextField.getText());
            int green = parseColorInput(greenTextField.getText());
            int blue = parseColorInput(blueTextField.getText());
            int tolerance = parseColorInput(toleranceTextField.getText());

            // Ensure valid inputs
            if (red == -1 || green == -1 || blue == -1 || tolerance == -1) {
                resultTextArea.setText("Please enter valid RGB values (0-255) and tolerance.");
                return;
            }

            // Find closest images from database
            String result = findClosestImages(red, green, blue, tolerance);
            resultTextArea.setText(result);
        });

        // Set up the stage
        Scene scene = new Scene(root, 400, 400);
        primaryStage.setTitle("Color Histogram Search");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private TextField createColorTextField() {
        TextField textField = new TextField();
        textField.setPromptText("0-255");
        return textField;
    }

    // Method to parse the RGB values from text input
    private int parseColorInput(String input) {
        try {
            int value = Integer.parseInt(input);
            if (value < 0 || value > 255) {
                return -1; // Invalid value
            }
            return value;
        } catch (NumberFormatException e) {
            return -1; // Invalid input (not a number)
        }
    }

    // Method to query the database and find the closest images
    private String findClosestImages(int red, int green, int blue, int tolerance) {
        StringBuilder result = new StringBuilder();
        try (Connection connection = DriverManager.getConnection(DB_URL)) {
            Gson gson = new Gson();
            String querySQL = """
                    SELECT filename, red_histogram, green_histogram, blue_histogram
                    FROM ColorHistogram;
                    """;
            try (PreparedStatement stmt = connection.prepareStatement(querySQL)) {
                ResultSet resultSet = stmt.executeQuery();

                // List to store filenames and their scores
                List<ImageMatch> matches = new ArrayList<>();

                while (resultSet.next()) {
                    String filename = resultSet.getString("filename");
                    String redHistogramJson = resultSet.getString("red_histogram");
                    String greenHistogramJson = resultSet.getString("green_histogram");
                    String blueHistogramJson = resultSet.getString("blue_histogram");

                    int[] redHistogram = gson.fromJson(redHistogramJson, int[].class);
                    int[] greenHistogram = gson.fromJson(greenHistogramJson, int[].class);
                    int[] blueHistogram = gson.fromJson(blueHistogramJson, int[].class);

                    // Calculate match score
                    int matchScore = calculateMatchScore(red, green, blue, tolerance, redHistogram, greenHistogram,
                            blueHistogram);

                    if (matchScore > 0) {
                        matches.add(new ImageMatch(filename, matchScore));
                    }
                }

                // Sort matches by score (descending)
                matches.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

                // Build result string from sorted matches
                for (ImageMatch match : matches) {
                    result.append("Filename: ").append(match.getFilename())
                            .append(" | Match Score: ").append(match.getScore())
                            .append("\n");
                }

                // If no matches are found
                if (matches.isEmpty()) {
                    result.append("No matches found within the tolerance range.");
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
            result.append("Error querying the database.");
        }
        return result.toString();
    }

    // Method to calculate the match score based on the histograms
    private int calculateMatchScore(int red, int green, int blue, int tolerance,
            int[] redHistogram, int[] greenHistogram, int[] blueHistogram) {
        int matchScore = 0;

        // Loop through the histogram bins
        for (int i = 0; i < 256; i++) {
            if (Math.abs(i - red) <= tolerance && redHistogram[i] > 0)
                matchScore += redHistogram[i];
            if (Math.abs(i - green) <= tolerance && greenHistogram[i] > 0)
                matchScore += greenHistogram[i];
            if (Math.abs(i - blue) <= tolerance && blueHistogram[i] > 0)
                matchScore += blueHistogram[i];
        }

        return matchScore;
    }

    // ImageMatch class to store filename and score
    public static class ImageMatch {
        private String filename;
        private int score;

        public ImageMatch(String filename, int score) {
            this.filename = filename;
            this.score = score;
        }

        public String getFilename() {
            return filename;
        }

        public int getScore() {
            return score;
        }
    }
}

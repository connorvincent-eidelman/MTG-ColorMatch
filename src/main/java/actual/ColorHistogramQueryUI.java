package actual;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ColorHistogramQueryUI extends Application {

    private static final String DB_URL = "jdbc:sqlite:ColorHistogram.db";
    private List<String> extractedCardIds = new ArrayList<>();


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
        Label rangeLabel = new Label("Tolerance Range:");

        // Text fields for RGB values
        TextField redTextField = createColorTextField();
        TextField greenTextField = createColorTextField();
        TextField blueTextField = createColorTextField();

        // Text field for tolerance
        TextField toleranceTextField = createColorTextField();

        // ComboBox for selecting tolerance range
        ComboBox<String> rangeComboBox = new ComboBox<>();
        rangeComboBox.getItems().addAll("Downward Range", "Upward Range", "Both Ranges");
        rangeComboBox.setValue("Both Ranges"); // Default value

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
                toleranceLabel, toleranceTextField,
                rangeLabel, rangeComboBox,
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

            // Get the selected range type
            String rangeType = rangeComboBox.getValue();

            // Find closest images from the database
            String result = findClosestImages(red, green, blue, tolerance, rangeType);
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
     public void scanForCards() {
        extractedCardIds.clear(); // Reset list
        try (Connection connection = DriverManager.getConnection(DB_URL)) {
            String querySQL = "SELECT filename FROM ColorHistogram;";
            try (PreparedStatement stmt = connection.prepareStatement(querySQL)) {
                ResultSet resultSet = stmt.executeQuery();
                while (resultSet.next()) {
                    String filename = resultSet.getString("filename");
                    String cardId = extractCardIdFromFilename(filename);
                    if (cardId != null) {
                        extractedCardIds.add(cardId);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Returns extracted card IDs
    public List<String> getExtractedCardIds() {
        return extractedCardIds;
    }

    // Extracts a card ID from a filename (assumes format "12345.jpg")
    private String extractCardIdFromFilename(String filename) {
        Pattern pattern = Pattern.compile("(\\d+)\\.\\w+$");
        Matcher matcher = pattern.matcher(filename);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String findClosestImages(int red, int green, int blue, int tolerance, String rangeType) {
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
                    int matchScore = calculateMatchScore(red, green, blue, tolerance, rangeType, redHistogram,
                            greenHistogram, blueHistogram);

                    if (matchScore > 0) {
                        matches.add(new ImageMatch(filename, matchScore));
                    }
                }

                // Sort matches by score (descending)
                matches.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
                List<ImageMatch> topMatches = matches.stream().limit(100).collect(Collectors.toList());
                
                // Build result string from sorted matches
                for (ImageMatch match : topMatches) {
                    result.append("Filename: ").append(match.getFilename())
                            .append(" | Match Score: ").append(match.getScore())
                            .append("\n");
                }

                if (topMatches.isEmpty()) {
                    result.append("No matches found within the tolerance range.");
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
            result.append("Error querying the database.");
        }
        return result.toString();
    }

    private int calculateMatchScore(int red, int green, int blue, int tolerance, String rangeType,
            int[] redHistogram, int[] greenHistogram, int[] blueHistogram) {
        int matchScore = 0;

        for (int i = 0; i < 256; i++) {
            boolean isMatchRed = false, isMatchGreen = false, isMatchBlue = false;

            if ("Downward Range".equals(rangeType)) {
                if (i >= red - tolerance && i <= red && redHistogram[i] > 0)
                    isMatchRed = true;
                if (i >= green - tolerance && i <= green && greenHistogram[i] > 0)
                    isMatchGreen = true;
                if (i >= blue - tolerance && i <= blue && blueHistogram[i] > 0)
                    isMatchBlue = true;
            } else if ("Upward Range".equals(rangeType)) {
                if (i >= red && i <= red + tolerance && redHistogram[i] > 0)
                    isMatchRed = true;
                if (i >= green && i <= green + tolerance && greenHistogram[i] > 0)
                    isMatchGreen = true;
                if (i >= blue && i <= blue + tolerance && blueHistogram[i] > 0)
                    isMatchBlue = true;
            } else { // Both Ranges
                if (i >= red - tolerance && i <= red + tolerance && redHistogram[i] > 0)
                    isMatchRed = true;
                if (i >= green - tolerance && i <= green + tolerance && greenHistogram[i] > 0)
                    isMatchGreen = true;
                if (i >= blue - tolerance && i <= blue + tolerance && blueHistogram[i] > 0)
                    isMatchBlue = true;
            }

            if (isMatchRed)
                matchScore += redHistogram[i];
            if (isMatchGreen)
                matchScore += greenHistogram[i];
            if (isMatchBlue)
                matchScore += blueHistogram[i];
        }

        return matchScore;
    }

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


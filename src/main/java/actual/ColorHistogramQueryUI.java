package actual;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
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

        // Sliders for RGB values
        Slider redSlider = createColorSlider();
        Slider greenSlider = createColorSlider();
        Slider blueSlider = createColorSlider();

        // Slider for tolerance
        Slider toleranceSlider = new Slider(0, 255, 10);
        toleranceSlider.setBlockIncrement(1);

        // Label to display tolerance value
        Label toleranceValueLabel = new Label(String.format("%.0f", toleranceSlider.getValue()));
        toleranceSlider.valueProperty().addListener(
                (observable, oldValue, newValue) -> toleranceValueLabel.setText(String.format("%.0f", newValue)));

        // Button to trigger the search
        Button searchButton = new Button("Find Closest Images");

        // Text Area to display results
        TextArea resultTextArea = new TextArea();
        resultTextArea.setEditable(false);
        resultTextArea.setPrefHeight(200);

        // Layout
        VBox root = new VBox(10);
        root.setPadding(new javafx.geometry.Insets(20));

        root.getChildren().addAll(redLabel, redSlider,
                greenLabel, greenSlider,
                blueLabel, blueSlider,
                toleranceLabel, toleranceSlider, toleranceValueLabel,
                searchButton,
                resultTextArea);

        // Action for Search Button
        searchButton.setOnAction(event -> {
            int red = (int) redSlider.getValue();
            int green = (int) greenSlider.getValue();
            int blue = (int) blueSlider.getValue();
            int tolerance = (int) toleranceSlider.getValue();

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

    private Slider createColorSlider() {
        Slider slider = new Slider(0, 255, 128);
        slider.setBlockIncrement(1);
        slider.setMajorTickUnit(50);
        slider.setMinorTickCount(5);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        return slider;
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

                while (resultSet.next()) {
                    String filename = resultSet.getString("filename");
                    String redHistogramJson = resultSet.getString("red_histogram");
                    String greenHistogramJson = resultSet.getString("green_histogram");
                    String blueHistogramJson = resultSet.getString("blue_histogram");

                    int[] redHistogram = gson.fromJson(redHistogramJson, int[].class);
                    int[] greenHistogram = gson.fromJson(greenHistogramJson, int[].class);
                    int[] blueHistogram = gson.fromJson(blueHistogramJson, int[].class);

                    if (isColorMatch(red, green, blue, tolerance, redHistogram, greenHistogram, blueHistogram)) {
                        result.append("Filename: ").append(filename).append("\n");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    // Method to check if the color histogram is within tolerance range
    private boolean isColorMatch(int red, int green, int blue, int tolerance,
            int[] redHistogram, int[] greenHistogram, int[] blueHistogram) {
        // Check if the color histogram has a significant amount of pixels in the
        // tolerance range
        int matchCount = 0;
        for (int i = 0; i < 256; i++) {
            if (Math.abs(i - red) <= tolerance && redHistogram[i] > 0)
                matchCount++;
            if (Math.abs(i - green) <= tolerance && greenHistogram[i] > 0)
                matchCount++;
            if (Math.abs(i - blue) <= tolerance && blueHistogram[i] > 0)
                matchCount++;
        }

        // If the match count exceeds a certain threshold, consider it a match
        return matchCount > 100; // Arbitrary threshold, can be adjusted
    }
}

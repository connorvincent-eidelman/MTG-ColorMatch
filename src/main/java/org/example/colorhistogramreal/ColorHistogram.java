package org.example.colorhistogramreal;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ColorHistogram extends Application {
    public static void main(String[] args) {
        launch(args); // Launch the JavaFX application
    }

    @Override
    public void start(Stage stage) {
        try {
            // Load the image
            File imageFile = new File("/Users/connorv-e/Downloads/hard-techno-v-3.0-min/__ia_thumb.jpg");
            BufferedImage image = ImageIO.read(imageFile);

            // Initialize histograms for R, G, and B channels (256 bins each)
            int[] redHistogram = new int[256];
            int[] greenHistogram = new int[256];
            int[] blueHistogram = new int[256];

            // Get image dimensions
            int width = image.getWidth();
            int height = image.getHeight();

            // Iterate through all pixels
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    // Get the RGB value of the pixel
                    int pixel = image.getRGB(x, y);

                    // Extract the R, G, B components
                    int red = (pixel >> 16) & 0xff;
                    int green = (pixel >> 8) & 0xff;
                    int blue = pixel & 0xff;

                    // Increment the corresponding histogram bins
                    redHistogram[red]++;
                    greenHistogram[green]++;
                    blueHistogram[blue]++;
                }
            }

            // Create the X-Axis (pixel values 0-255)
            CategoryAxis xAxis = new CategoryAxis();
            xAxis.setLabel("Pixel Value");

            // Create the Y-Axis (frequency of pixel values)
            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("Frequency");

            // Create the bar chart
            BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
            barChart.setTitle("Color Histograms");

            // Create series for Red, Green, and Blue histograms
            XYChart.Series<String, Number> redSeries = new XYChart.Series<>();
            redSeries.setName("Red");
            for (int i = 0; i < 256; i++) {
                redSeries.getData().add(new XYChart.Data<>(String.valueOf(i), redHistogram[i]));
            }

            XYChart.Series<String, Number> greenSeries = new XYChart.Series<>();
            greenSeries.setName("Green");
            for (int i = 0; i < 256; i++) {
                greenSeries.getData().add(new XYChart.Data<>(String.valueOf(i), greenHistogram[i]));
            }

            XYChart.Series<String, Number> blueSeries = new XYChart.Series<>();
            blueSeries.setName("Blue");
            for (int i = 0; i < 256; i++) {
                blueSeries.getData().add(new XYChart.Data<>(String.valueOf(i), blueHistogram[i]));
            }

            // Add the series to the bar chart
            barChart.getData().addAll(redSeries, greenSeries, blueSeries);

            // Set up the scene and stage
            Scene scene = new Scene(barChart, 800, 600);
            stage.setTitle("Color Histogram");
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

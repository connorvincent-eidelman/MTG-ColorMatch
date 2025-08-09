package actual;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.json.JSONObject;

import com.google.gson.Gson;

public class ColorHistogramImageViewer {
    private static final String DB_URL = "jdbc:sqlite:ColorHistogram.db";
    private static List<CardData> displayedCards = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ColorHistogramImageViewer::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Color Histogram Image Viewer");
        frame.setSize(600, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        frame.add(panel);

        JLabel instructionLabel = new JLabel("Enter RGB values (0-255)");
        panel.add(instructionLabel);

        JTextField redField = new JTextField("255", 5);
        JTextField greenField = new JTextField("255", 5);
        JTextField blueField = new JTextField("255", 5);
        JTextField resultLimitField = new JTextField("100", 5);

        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("Red: "));
        inputPanel.add(redField);
        inputPanel.add(new JLabel("Green: "));
        inputPanel.add(greenField);
        inputPanel.add(new JLabel("Blue: "));
        inputPanel.add(blueField);
        inputPanel.add(new JLabel("Results: "));
        inputPanel.add(resultLimitField);

        panel.add(inputPanel);

        JButton searchButton = new JButton("Find Closest Images");
        panel.add(searchButton);
        JButton exportButton = new JButton("Export List");
        panel.add(exportButton);

        JPanel imagePanel = new JPanel();
        imagePanel.setLayout(new BoxLayout(imagePanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(imagePanel);
        scrollPane.setPreferredSize(new Dimension(550, 500));
        panel.add(scrollPane);

        searchButton.addActionListener((ActionEvent e) -> {
            int red = Integer.parseInt(redField.getText());
            int green = Integer.parseInt(greenField.getText());
            int blue = Integer.parseInt(blueField.getText());
            int resultLimit = Integer.parseInt(resultLimitField.getText());

            displayedCards = findClosestImages(red, green, blue, resultLimit);
            fetchImages(displayedCards, imagePanel);
        });

        exportButton.addActionListener((ActionEvent e) -> {
            if (displayedCards.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No cards to export!");
                return;
            }

            StringBuilder exportText = new StringBuilder();
            int count = 1;
            for (CardData card : displayedCards) {
                if (card.cardName != null && card.setCode != null && card.cardNumber != null) {
                    exportText.append(count).append(" ")
                            .append(card.cardName).append(" (")
                            .append(card.setCode.toUpperCase()).append(") ")
                            .append(card.cardNumber).append("\n");

                }
            }

            JTextArea textArea = new JTextArea(exportText.toString());
            textArea.setEditable(false);
            JScrollPane scrollPaneExport = new JScrollPane(textArea);
            scrollPaneExport.setPreferredSize(new Dimension(400, 300));
            JOptionPane.showMessageDialog(frame, scrollPaneExport, "Exported Cards", JOptionPane.INFORMATION_MESSAGE);
        });

        frame.setVisible(true);
    }

    private static List<CardData> findClosestImages(int red, int green, int blue, int resultLimit) {
        List<CardData> cards = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DB_URL)) {
            Gson gson = new Gson();
            String query = "SELECT filename, red_histogram, green_histogram, blue_histogram FROM ColorHistogram;";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                ResultSet resultSet = stmt.executeQuery();
                while (resultSet.next()) {
                    String filename = resultSet.getString("filename");
                    int[] redHist = gson.fromJson(resultSet.getString("red_histogram"), int[].class);
                    int[] greenHist = gson.fromJson(resultSet.getString("green_histogram"), int[].class);
                    int[] blueHist = gson.fromJson(resultSet.getString("blue_histogram"), int[].class);
                    int avgRed = calculateAverageColor(redHist);
                    int avgGreen = calculateAverageColor(greenHist);
                    int avgBlue = calculateAverageColor(blueHist);
                    int score = calculateMatchScore(red, green, blue, avgRed, avgGreen, avgBlue);
                    if (score > 0) {
                        cards.add(new CardData(filename, score));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        cards.sort(Comparator.comparingInt(CardData::getMatchScore).reversed());
        return cards.size() > resultLimit ? cards.subList(0, resultLimit) : cards;
    }

    private static int calculateAverageColor(int[] histogram) {
        int total = 0, sum = 0;
        for (int i = 0; i < histogram.length; i++) {
            total += histogram[i] * i;
            sum += histogram[i];
        }
        return sum == 0 ? 0 : total / sum;
    }

    private static int calculateMatchScore(int red, int green, int blue, int avgRed, int avgGreen, int avgBlue) {
        double redSimilarity = 1.0 - Math.abs(red - avgRed) / 255.0;
        double greenSimilarity = 1.0 - Math.abs(green - avgGreen) / 255.0;
        double blueSimilarity = 1.0 - Math.abs(blue - avgBlue) / 255.0;
        return (int) ((redSimilarity + greenSimilarity + blueSimilarity) / 3.0 * 100);
    }

    private static void fetchImages(List<CardData> cards, JPanel imagePanel) {
        imagePanel.removeAll(); // Clear previous results
        imagePanel.revalidate();
        imagePanel.repaint();

        new SwingWorker<Void, CardData>() {
            @Override
            protected void process(List<CardData> chunks) {
                for (CardData card : chunks) {
                    JPanel cardPanel = new JPanel();
                    cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
                    cardPanel.add(new JLabel("Card: " + card.cardName));
                    cardPanel.add(new JLabel("Set: " + card.setCode));
                    cardPanel.add(new JLabel("Number: " + card.cardNumber));
                    cardPanel.add(new JLabel("Score: " + card.matchScore));

                    if (card.cardImage != null) {
                        cardPanel.add(new JLabel(card.cardImage));
                    } else {
                        cardPanel.add(new JLabel("Image not available"));
                    }

                    imagePanel.add(cardPanel);
                }

                // Ensure UI updates dynamically
                imagePanel.revalidate();
                imagePanel.repaint();
            }

            @Override
            protected Void doInBackground() {
                for (CardData card : cards) {
                    try {
                        String apiUrl = "https://api.scryfall.com/cards/" + card.id;
                        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                        conn.setRequestMethod("GET");

                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null)
                            response.append(line);
                        reader.close();

                        JSONObject json = new JSONObject(response.toString());
                        card.cardName = json.optString("name", "Unknown Card");
                        card.setCode = json.optString("set", "Unknown Set");
                        card.cardNumber = json.optString("collector_number", "Unknown Number");

                        // Fetch image URL if available
                        if (json.has("image_uris")) {
                            String imageUrl = json.getJSONObject("image_uris").getString("normal");
                            card.cardImage = new ImageIcon(
                                    ImageIO.read(new URL(imageUrl)).getScaledInstance(200, 280, Image.SCALE_SMOOTH));
                        }

                    } catch (IOException ignored) {
                    }

                    publish(card); // Add card to UI dynamically
                }
                return null;
            }

            @Override
            protected void done() {
                imagePanel.revalidate();
                imagePanel.repaint();
            }

        }.execute();
    }

    private static class CardData {
        String id, cardName, setCode, cardNumber;
        int matchScore;
        ImageIcon cardImage;

        CardData(String id, int score) {
            this.id = id;
            this.matchScore = score;
        }

        int getMatchScore() {
            return matchScore;
        }
    }
}

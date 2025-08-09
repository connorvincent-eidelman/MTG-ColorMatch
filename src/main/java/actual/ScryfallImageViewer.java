package actual;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import org.json.JSONObject;

public class ScryfallImageViewer {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Scryfall Image Viewer");
        frame.setSize(500, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        frame.add(panel);
        placeComponents(panel);

        frame.setVisible(true);
    }

    private static void placeComponents(JPanel panel) {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("Enter formatted card IDs (one per line or comma-separated):");
        panel.add(label);

        JTextArea textArea = new JTextArea(5, 30);
        JScrollPane textScroll = new JScrollPane(textArea);
        panel.add(textScroll);

        JButton button = new JButton("Fetch Images");
        panel.add(button);

        // Scrollable panel for images
        JPanel imagePanel = new JPanel();
        imagePanel.setLayout(new BoxLayout(imagePanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(imagePanel);
        scrollPane.setPreferredSize(new Dimension(450, 400));
        panel.add(scrollPane);

        button.addActionListener((ActionEvent e) -> {
            String input = textArea.getText().trim();
            if (!input.isEmpty()) {
                List<CardData> cards = extractCardData(input);
                fetchImages(cards, imagePanel);
            }
        });
    }

    private static class CardData {
        String id;
        String matchScore;
        String cardName;
        ImageIcon cardImage;

        CardData(String id, String matchScore) {
            this.id = id;
            this.matchScore = matchScore;
        }
    }

    private static List<CardData> extractCardData(String input) {
        List<CardData> cards = new ArrayList<>();
        // Proper regex to ensure we only capture the ID and match score
        Pattern pattern = Pattern.compile("Filename: (.*?) \\| Match Score: ([0-9]+\\.?[0-9]*)");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String id = matcher.group(1).trim();
            String matchScore = matcher.group(2).trim(); // Ensures only the numeric score is captured
            cards.add(new CardData(id, matchScore));
        }
        return cards;
    }

    private static void fetchImages(List<CardData> cards, JPanel imagePanel) {
        imagePanel.removeAll(); // Clear previous images
        new SwingWorker<Void, CardData>() {
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
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();

                        JSONObject jsonResponse = new JSONObject(response.toString());

                        // Extract card name
                        if (jsonResponse.has("name")) {
                            card.cardName = jsonResponse.getString("name");
                        } else {
                            card.cardName = "Unknown Card";
                        }

                        // Extract image URL
                        if (jsonResponse.has("image_uris")) {
                            String imageUrl = jsonResponse.getJSONObject("image_uris").getString("normal");
                            Image image = ImageIO.read(new URL(imageUrl));
                            if (image != null) {
                                card.cardImage = new ImageIcon(image.getScaledInstance(200, 280, Image.SCALE_SMOOTH));
                            }
                        }
                    } catch (IOException ignored) {
                    }
                    publish(card);
                }
                return null;
            }

            @Override
            protected void process(List<CardData> chunks) {
                for (CardData card : chunks) {
                    JPanel cardPanel = new JPanel();
                    cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));

                    JLabel nameLabel = new JLabel("Card: " + card.cardName);
                    JLabel scoreLabel = new JLabel("Match Score: " + card.matchScore);
                    JLabel imageLabel = new JLabel();

                    if (card.cardImage != null) {
                        imageLabel.setIcon(card.cardImage);
                    } else {
                        imageLabel.setText("Image not found");
                    }

                    cardPanel.add(nameLabel);
                    cardPanel.add(scoreLabel);
                    cardPanel.add(imageLabel);
                    imagePanel.add(cardPanel);
                    imagePanel.add(Box.createRigidArea(new Dimension(0, 10))); // Space between cards
                }
                imagePanel.revalidate();
                imagePanel.repaint();
            }
        }.execute();
    }
}

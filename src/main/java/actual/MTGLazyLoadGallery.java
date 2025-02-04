package actual;

import java.util.List;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;

public class MTGLazyLoadGallery {

    private final TilePane tilePane;
    private final ColorHistogramQueryUI colorHistogramQueryUI;
    private final String SCRYFALL_IMAGE_URL = "https://api.scryfall.com/cards/%s?format=image";

    public MTGLazyLoadGallery(TilePane tilePane, ColorHistogramQueryUI queryUI) {
        this.tilePane = tilePane;
        this.colorHistogramQueryUI = queryUI;
    }

    // Loads images dynamically based on extracted card IDs
    public void loadImages() {
        colorHistogramQueryUI.scanForCards(); // Update card ID list
        List<String> cardIds = colorHistogramQueryUI.getExtractedCardIds();

        Platform.runLater(() -> {
            tilePane.getChildren().clear(); // Clear previous images
            for (String cardId : cardIds) {
                ImageView imageView = createLazyLoadedImageView(cardId);
                tilePane.getChildren().add(imageView);
            }
        });
    }

    // Creates an ImageView with lazy loading
    private ImageView createLazyLoadedImageView(String cardId) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(150);
        imageView.setFitHeight(210);
        imageView.setPreserveRatio(true);

        new Thread(() -> {
            try {
                String imageUrl = String.format(SCRYFALL_IMAGE_URL, cardId);
                Image image = new Image(imageUrl, true); // Load asynchronously
                Platform.runLater(() -> imageView.setImage(image));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        return imageView;
    }
}

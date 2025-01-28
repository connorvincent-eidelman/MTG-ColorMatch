package org.example.colorhistogramreal;

import java.awt.image.BufferedImage;

public class Main {
    public static void main(String[] args) {
        String imageUrl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/6/66b77eb8-d39c-4e84-b9db-e9ae7abdb6b7.jpg";

        BufferedImage image = ScryfallImageFetcher.fetchImageFromUrl(imageUrl);

        if (image != null) {
            System.out.println("Image fetched successfully! Dimensions: "
                    + image.getWidth() + "x" + image.getHeight());

            // You can now process the image, e.g., analyze RGB values, save it, etc.
        } else {
            System.out.println("Failed to fetch the image.");
        }
    }
}

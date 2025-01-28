package meow;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

public class RazzmatazzPixelFinder {

    private static final String IMAGE_DIRECTORY_PATH = "/Users/connorv-e/Desktop"; // Path to your images

    // Razzmatazz RGB value
    private static final int RAZZMATAZZ_RED = 227;
    private static final int RAZZMATAZZ_GREEN = 11;
    private static final int RAZZMATAZZ_BLUE = 92;

    // Tolerance for each channel (you can adjust this value)
    private static final int TOLERANCE = 30;

    public static void main(String[] args) {
        File imageDirectory = new File(IMAGE_DIRECTORY_PATH);

        // List to store image files with their razzmatazz pixel counts
        List<ImageWithRazzmatazzPixelCount> imageList = new ArrayList<>();

        if (imageDirectory.isDirectory()) {
            for (File file : imageDirectory.listFiles()) {
                if (isImageFile(file)) {
                    System.out.println("Processing: " + file.getName());
                    try {
                        int razzmatazzPixelCount = calculateRazzmatazzPixelCount(file);
                        imageList.add(new ImageWithRazzmatazzPixelCount(file.getName(), razzmatazzPixelCount));
                    } catch (IOException e) {
                        System.err.println("Failed to process image: " + file.getName());
                        e.printStackTrace();
                    }
                }
            }

            // Sort images based on razzmatazz pixel count (descending order)
            Collections.sort(imageList, (img1, img2) -> Integer.compare(img2.razzmatazzPixelCount, img1.razzmatazzPixelCount));

            // Print the top images with most razzmatazz-like pixels
            System.out.println("\nImages with the most razzmatazz-like pixels:");
            for (ImageWithRazzmatazzPixelCount image : imageList) {
                System.out.println(image.filename + " - Razzmatazz-like Pixel Count: " + image.razzmatazzPixelCount);
            }
        } else {
            System.err.println("The provided path is not a directory.");
        }
    }

    private static boolean isImageFile(File file) {
        String[] supportedExtensions = { ".jpg", ".jpeg", ".png", ".bmp", ".gif" };
        String fileName = file.getName().toLowerCase();
        for (String ext : supportedExtensions) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private static int calculateRazzmatazzPixelCount(File imageFile) throws IOException {
        BufferedImage image = ImageIO.read(imageFile);

        int razzmatazzPixelCount = 0;
        int totalPixels = 0;

        int width = image.getWidth();
        int height = image.getHeight();

        // Iterate through each pixel and check if it matches the razzmatazz color within tolerance
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;

                // Check if the pixel is within the specified RGB range around razzmatazz
                if (isWithinRazzmatazzRange(red, green, blue)) {
                    razzmatazzPixelCount++;
                }
                totalPixels++;
            }
        }

        return razzmatazzPixelCount;
    }

    private static boolean isWithinRazzmatazzRange(int red, int green, int blue) {
        // Check if each channel is within the defined tolerance of the razzmatazz color
        return (Math.abs(red - RAZZMATAZZ_RED) <= TOLERANCE) &&
               (Math.abs(green - RAZZMATAZZ_GREEN) <= TOLERANCE) &&
               (Math.abs(blue - RAZZMATAZZ_BLUE) <= TOLERANCE);
    }

    // Data class to hold image filename and the count of razzmatazz-like pixels
    private static class ImageWithRazzmatazzPixelCount {
        String filename;
        int razzmatazzPixelCount;

        public ImageWithRazzmatazzPixelCount(String filename, int razzmatazzPixelCount) {
            this.filename = filename;
            this.razzmatazzPixelCount = razzmatazzPixelCount;
        }
    }
}

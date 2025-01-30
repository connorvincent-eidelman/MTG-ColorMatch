package actual;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageResizer {

    public static void main(String[] args) {
        String imageDirectoryPath = "/Users/connorv-e/colorhistogramreal/card_images"; // Path to the image directory
        int targetWidth = 626;
        int targetHeight = 457;

        File imageDirectory = new File(imageDirectoryPath);

        if (imageDirectory.isDirectory()) {
            for (File file : imageDirectory.listFiles()) {
                if (isImageFile(file)) {
                    try {
                        BufferedImage originalImage = ImageIO.read(file);
                        int originalWidth = originalImage.getWidth();
                        int originalHeight = originalImage.getHeight();

                        // Only resize if the dimensions don't match the target
                        if (originalWidth != targetWidth || originalHeight != targetHeight) {
                            BufferedImage resizedImage = resizeImage(file, targetWidth, targetHeight);
                            saveResizedImage(resizedImage, file); // Overwrite original image
                            System.out.println("Resized and saved: " + file.getName());
                        } else {
                            System.out.println("Skipping (dimensions match): " + file.getName());
                        }

                    } catch (IOException e) {
                        System.err.println("Failed to process image: " + file.getName());
                        e.printStackTrace();
                    }
                }
            }
        } else {
            System.err.println("Provided path is not a directory.");
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

    private static BufferedImage resizeImage(File imageFile, int targetWidth, int targetHeight) throws IOException {
        BufferedImage originalImage = ImageIO.read(imageFile);

        // Resize the image using a scaled instance
        Image resizedImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);

        // Convert the resized image back to a BufferedImage
        BufferedImage bufferedResizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        bufferedResizedImage.getGraphics().drawImage(resizedImage, 0, 0, null);

        return bufferedResizedImage;
    }

    // Save resized image by overwriting the original file
    private static void saveResizedImage(BufferedImage resizedImage, File originalFile) throws IOException {
        // Get the file extension of the original image
        String fileExtension = getFileExtension(originalFile);

        // Save the resized image by overwriting the original image
        ImageIO.write(resizedImage, fileExtension, originalFile);

        System.out.println("Saved resized image (overwritten): " + originalFile.getName());
    }

    // Utility method to get the file extension of the image
    private static String getFileExtension(File file) {
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex > 0) ? fileName.substring(dotIndex + 1) : "jpg"; // Default to JPEG if no extension is found
    }
}

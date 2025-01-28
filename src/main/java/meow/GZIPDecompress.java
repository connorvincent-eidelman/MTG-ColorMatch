package meow;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class GZIPDecompress {

    public static void decompressGzipFile(String gzippedFilePath, String outputFilePath) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(gzippedFilePath);
             GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
             FileOutputStream fileOutputStream = new FileOutputStream(outputFilePath)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String gzippedFilePath = "unique-artwork.json.gz"; // Path to the gzipped file you downloaded
        String outputFilePath = "unique-artwork.json"; // Path to save the decompressed file

        decompressGzipFile(gzippedFilePath, outputFilePath);
        System.out.println("File decompressed to: " + outputFilePath);
    }
}

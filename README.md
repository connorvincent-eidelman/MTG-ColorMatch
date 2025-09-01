# MTG-ColorMatch

MTG-ColorMatch is a Java application for analyzing, storing, and visualizing color histograms of Magic: The Gathering card images. It supports downloading card art from Scryfall, calculating RGB histograms, storing results in a SQLite database, and searching for images by color similarity. The project includes both JavaFX and Swing user interfaces.

## Features

- **Download Card Art**: Fetches card images from Scryfall using their API.
- **Color Histogram Analysis**: Calculates RGB histograms for each image.
- **SQLite Database Storage**: Stores histogram data and filenames for fast querying.
- **Search UI**: Find images with similar color profiles using adjustable tolerance and range.
- **Image Gallery**: Lazy-load and display card images based on search results.
- **Export Functionality**: Export lists of matched cards for external use.

## Project Structure

- `src/main/java/actual/`: Core logic, database, image processing, and Swing UI.
- `src/main/java/org/example/MTG-ColorMatch/`: JavaFX UI and Scryfall API utilities.
- `src/main/resources/`: FXML files for JavaFX UI.
- `card_images/`: Folder for downloaded card images.
- `ColorHistogram.db`: SQLite database file.

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+
- Internet connection (for downloading images and API access)

### Build & Run

1. **Clone the repository**
   ```sh
   git clone <repo-url>
   cd MTG-ColorMatch
   ```

2. **Download dependencies and build**
   ```sh
   ./mvnw clean package
   ```

3. **Run the JavaFX application**
   ```sh
   ./mvnw javafx:run
   ```

4. **Run Swing utilities**
   ```sh
   java -m org.example.MTG-ColorMatch/actual.ColorHistogramDatabase
   java -m org.example.MTG-ColorMatch/actual.ScryfallCardArtDownloader
   java -m org.example.MTG-ColorMatch/actual.ColorHistogramImageViewer
   ```

## Usage

- **Download Images**: Run `ScryfallCardArtDownloader` to populate `card_images/`.
- **Analyze Images**: Run `ColorHistogramDatabase` to process images and store histograms.
- **Search & View**: Use `ColorHistogramQueryUI` (JavaFX) or `ColorHistogramImageViewer` (Swing) to search and view results.
- **Export**: Use the export button in the UI to save matched card lists.

## Dependencies

- JavaFX (controls, fxml, web, swing)
- ControlsFX, ValidatorFX, Ikonli, BootstrapFX, TilesFX
- Gson (JSON serialization)
- org.json (JSON parsing)
- SQLite JDBC
- TwelveMonkeys ImageIO (JPEG support)
- JUnit (testing)

## Configuration

- **Image Directory**: Change the path in source files if your images are stored elsewhere.
- **Database**: The SQLite database is created automatically in the project root.

## License

This project is licensed under the Apache License 2.0.

---

For questions or contributions, please open an issue or submit a

CREATE TABLE ColorHistogram (
                                id INTEGER PRIMARY KEY AUTOINCREMENT, -- Unique identifier for each row
                                filename TEXT NOT NULL,               -- The name of the image file
                                red_histogram TEXT NOT NULL,          -- JSON string representing the red channel histogram (256 bins)
                                green_histogram TEXT NOT NULL,        -- JSON string representing the green channel histogram (256 bins)
                                blue_histogram TEXT NOT NULL          -- JSON string representing the blue channel histogram (256 bins)
);

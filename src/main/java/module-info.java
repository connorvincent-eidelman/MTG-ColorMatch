module org.example.colorhistogramreal {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.net.http;
    requires java.sql;
    requires transitive javafx.graphics;

    requires com.google.gson;

    requires org.xerial.sqlitejdbc;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires java.desktop;

    opens org.example.colorhistogramreal to javafx.fxml;

    exports org.example.colorhistogramreal;
    exports actual;
}
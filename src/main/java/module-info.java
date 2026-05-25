module hu.javagpt {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires ollama4j;
    requires java.net.http;

    opens hu.javagpt to javafx.fxml, javafx.graphics;
    opens hu.javagpt.client.controller to javafx.fxml;
    opens hu.javagpt.model to com.google.gson;
    opens hu.javagpt.common to com.google.gson;

    exports hu.javagpt;
    exports hu.javagpt.client;
    exports hu.javagpt.client.controller;
    exports hu.javagpt.common;
    exports hu.javagpt.model;
    exports hu.javagpt.server;
}
package com.bidding.client.scene;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class AppNavigator {

    public static final double LOGIN_WIDTH = 1200;
    public static final double LOGIN_HEIGHT = 760;
    public static final double APP_WIDTH = 1280;
    public static final double APP_HEIGHT = 820;

    private AppNavigator() {
    }

    public static void openEntry(Stage stage, String resourcePath, String title) throws Exception {
        Parent root = FXMLLoader.load(AppNavigator.class.getResource(resourcePath));
        stage.setScene(new Scene(root, LOGIN_WIDTH, LOGIN_HEIGHT));
        stage.setTitle(title);
        stage.setResizable(true);
        stage.setMaximized(false);
        stage.centerOnScreen();
        stage.show();
    }

    public static void openPrimary(Stage stage, String resourcePath, String title) throws Exception {
        Parent root = FXMLLoader.load(AppNavigator.class.getResource(resourcePath));
        stage.setScene(new Scene(root, APP_WIDTH, APP_HEIGHT));
        stage.setTitle(title);
        stage.setResizable(true);
        stage.setMaximized(true);
        stage.show();
    }
}

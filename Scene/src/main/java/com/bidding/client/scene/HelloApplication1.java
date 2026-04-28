package com.bidding.client.scene;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.bidding.client.network.NetworkClient;

public class HelloApplication1 extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        NetworkClient.connect("127.0.0.1", 1234);


        FXMLLoader loader = new FXMLLoader(HelloApplication1.class.getResource("/com/bidding/client/scene/SceneBidder1.fxml"));
        Scene scene = new Scene(loader.load(), AppNavigator.LOGIN_WIDTH, AppNavigator.LOGIN_HEIGHT);

        stage.setTitle("Dang nhap he thong");
        stage.setResizable(true);
        stage.setMaximized(false);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}

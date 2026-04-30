package com.bidding.client.scene;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SceneAuctionManagementLoadTest {

    @Test
    void loadsSceneAuctionManagementFxml() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.startup(() -> {
            try {
                Parent root = FXMLLoader.load(AppNavigator.class.getResource(
                        "/com/bidding/client/scene/SceneAuctionManagement.fxml"));
                assertNotNull(root);
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                latch.countDown();
            }
        });

        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("Timed out while loading SceneAuctionManagement.fxml");
        }
        if (failure.get() != null) {
            throw new AssertionError("Failed to load SceneAuctionManagement.fxml", failure.get());
        }
    }
}

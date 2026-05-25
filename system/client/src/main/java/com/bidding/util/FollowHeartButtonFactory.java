package com.bidding.util;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/**
 * Vector heart buttons for auction follow / unfollow on dashboard cards.
 */
public final class FollowHeartButtonFactory {

    private static final String HEART_PATH =
            "M 12 20.2 C 12 20.2 4.2 14.5 4.2 9.1 C 4.2 6.4 6.3 4.3 9 4.3 C 10.4 4.3 11.5 5 12 5.8 C 12.5 5 13.6 4.3 15 4.3 C 17.7 4.3 19.8 6.4 19.8 9.1 C 19.8 14.5 12 20.2 12 20.2 Z";

    private FollowHeartButtonFactory() {
    }

    public static Button create(boolean following) {
        Button button = new Button();
        button.setText(null);
        button.getStyleClass().add(following ? "dashboard-favorite-button-active" : "dashboard-favorite-button");
        button.setGraphic(buildHeartGraphic(following));
        button.setUserData(following);
        updateTooltip(button, following);
        return button;
    }

    public static void setFollowing(Button button, boolean following) {
        button.setUserData(following);
        button.setGraphic(buildHeartGraphic(following));
        button.getStyleClass().removeAll("dashboard-favorite-button", "dashboard-favorite-button-active");
        button.getStyleClass().add(following ? "dashboard-favorite-button-active" : "dashboard-favorite-button");
        updateTooltip(button, following);
    }

    public static boolean isFollowing(Button button) {
        return Boolean.TRUE.equals(button.getUserData());
    }

    private static StackPane buildHeartGraphic(boolean filled) {
        SVGPath heart = new SVGPath();
        heart.setContent(HEART_PATH);
        heart.setScaleX(0.9);
        heart.setScaleY(0.9);
        if (filled) {
            heart.setFill(Color.web("#D2AA2A"));
            heart.setStroke(Color.web("#A67C00"));
            heart.setStrokeWidth(0.6);
        } else {
            heart.setFill(Color.TRANSPARENT);
            heart.setStroke(Color.web("#64748B"));
            heart.setStrokeWidth(1.3);
        }
        StackPane graphic = new StackPane(heart);
        graphic.setMinSize(22, 22);
        graphic.setMaxSize(22, 22);
        graphic.setAlignment(Pos.CENTER);
        return graphic;
    }

    private static void updateTooltip(Button button, boolean following) {
        button.setTooltip(new Tooltip(following
                ? "Bấm để hủy theo dõi"
                : "Theo dõi phiên đấu giá"));
    }
}

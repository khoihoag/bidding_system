package com.bidding.client.scene;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.util.Objects;
import java.util.function.Consumer;

public class NavBarController {

    @FXML private Button btnHome;
    @FXML private Button btnWatchlist;
    @FXML private Button btnProfile;
    @FXML private Button btnSearch;
    @FXML private TextField txtSearch;
    @FXML private Label lblClock;
    @FXML private Label lblBalance;
    @FXML private Label lblUserName;
    @FXML private Label lblNotificationBadge;
    @FXML private StackPane notificationBadgePane;
    @FXML private ImageView imgAvatar;
    @FXML private Label lblAvatarFallback;

    private Runnable onMenuAction;
    private Runnable onHomeAction;
    private Runnable onWatchlistAction;
    private Runnable onProfileAction;
    private Runnable onNotificationsAction;
    private Runnable onLogoutAction;
    private Runnable onAvatarAction;
    private Consumer<String> onSearchAction;

    @FXML
    private void handleMenu(ActionEvent event) {
        if (onMenuAction != null) onMenuAction.run();
    }

    @FXML
    private void handleHome(ActionEvent event) {
        if (onHomeAction != null) onHomeAction.run();
    }

    @FXML
    private void handleWatchlist(ActionEvent event) {
        if (onWatchlistAction != null) onWatchlistAction.run();
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        if (onProfileAction != null) onProfileAction.run();
    }

    @FXML
    private void handleNotifications(ActionEvent event) {
        if (onNotificationsAction != null) onNotificationsAction.run();
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        if (onSearchAction != null) onSearchAction.accept(getSearchText());
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        if (onLogoutAction != null) onLogoutAction.run();
    }

    @FXML
    private void handleAvatar(ActionEvent event) {
        if (onAvatarAction != null) onAvatarAction.run();
    }

    public void configure(String activeSection, String userName, String balance, int notificationCount, boolean searchVisible) {
        setActiveSection(activeSection);
        setUserName(userName);
        setBalanceText(balance);
        setNotificationCount(notificationCount);
        txtSearch.setVisible(searchVisible);
        txtSearch.setManaged(searchVisible);
        btnSearch.setVisible(searchVisible);
        btnSearch.setManaged(searchVisible);
    }

    public void setCallbacks(
            Runnable onMenuAction,
            Runnable onHomeAction,
            Runnable onWatchlistAction,
            Runnable onProfileAction,
            Runnable onNotificationsAction,
            Consumer<String> onSearchAction,
            Runnable onLogoutAction,
            Runnable onAvatarAction
    ) {
        this.onMenuAction = onMenuAction;
        this.onHomeAction = onHomeAction;
        this.onWatchlistAction = onWatchlistAction;
        this.onProfileAction = onProfileAction;
        this.onNotificationsAction = onNotificationsAction;
        this.onSearchAction = onSearchAction;
        this.onLogoutAction = onLogoutAction;
        this.onAvatarAction = onAvatarAction;
    }

    public void setActiveSection(String activeSection) {
        styleNavButton(btnHome, Objects.equals(activeSection, "home"));
        styleNavButton(btnWatchlist, Objects.equals(activeSection, "watchlist"));
        styleNavButton(btnProfile, Objects.equals(activeSection, "profile"));
    }

    public void setClockText(String clockText) {
        lblClock.setText(clockText);
    }

    public void setBalanceText(String balanceText) {
        lblBalance.setText(balanceText);
    }

    public void setUserName(String userName) {
        lblUserName.setText(userName);
    }

    public String getSearchText() {
        return txtSearch.getText() == null ? "" : txtSearch.getText().trim();
    }

    public void clearSearch() {
        txtSearch.clear();
    }

    public void setNotificationCount(int count) {
        boolean visible = count > 0;
        notificationBadgePane.setVisible(visible);
        notificationBadgePane.setManaged(visible);
        lblNotificationBadge.setText(String.valueOf(count));
    }

    public void setAvatarImage(Image image) {
        imgAvatar.setImage(image);
        boolean hasImage = image != null;
        lblAvatarFallback.setVisible(!hasImage);
        lblAvatarFallback.setManaged(!hasImage);
    }

    private void styleNavButton(Button button, boolean active) {
        button.setStyle(active
                ? "-fx-background-color: #e0f2fe; -fx-text-fill: #0f4c81; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 7 14; -fx-cursor: hand;"
                : "-fx-background-color: transparent; -fx-text-fill: #4b5563; -fx-background-radius: 20; -fx-padding: 7 14; -fx-cursor: hand;");
    }
}

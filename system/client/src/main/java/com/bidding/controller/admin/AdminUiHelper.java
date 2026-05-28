package com.bidding.controller.admin;

import com.bidding.util.JsonUtil;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Shared UI helpers for the admin panel.
 */
public final class AdminUiHelper {

    private AdminUiHelper() {
    }

    public static void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            styleAdminDialog(alert);
            alert.showAndWait();
        });
    }

    public static void styleAdminDialog(Dialog<?> dialog) {
        if (dialog == null) {
            return;
        }
        styleAdminDialog(dialog.getDialogPane());
    }

    public static void styleAdminDialog(DialogPane dialogPane) {
        if (dialogPane == null) {
            return;
        }
        try {
            String stylesheet = AdminUiHelper.class.getResource("/css/admin.css").toExternalForm();
            if (!dialogPane.getStylesheets().contains(stylesheet)) {
                dialogPane.getStylesheets().add(stylesheet);
            }
        } catch (Exception ignored) {
        }
        if (!dialogPane.getStyleClass().contains("admin-dialog-pane")) {
            dialogPane.getStyleClass().add("admin-dialog-pane");
        }
    }

    public static String formatTimestamp(String rawTimestamp) {
        if (rawTimestamp == null || rawTimestamp.isBlank()) {
            return "---";
        }
        try {
            return LocalDateTime.parse(rawTimestamp).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        } catch (Exception ignored) {
            return rawTimestamp;
        }
    }

    public static boolean getActiveValue(JsonObject obj) {
        if (obj.has("active") && !obj.get("active").isJsonNull()) {
            return obj.get("active").getAsBoolean();
        }
        if (obj.has("isActive") && !obj.get("isActive").isJsonNull()) {
            return obj.get("isActive").getAsBoolean();
        }
        return true;
    }

    public static String getString(JsonObject obj, String key) {
        return JsonUtil.getString(obj, key);
    }
}

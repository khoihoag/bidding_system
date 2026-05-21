package com.bidding.controller.auctiondetail;

import com.google.gson.JsonObject;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatting and JSON parsing helpers for auction detail.
 */
public final class AuctionDetailFormats {

    public static final DateTimeFormatter DISPLAY_TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static final NumberFormat CURRENCY_FMT =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private AuctionDetailFormats() {
    }

    public static String getStringSafe(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? json.get(key).getAsString()
                : "";
    }

    public static String formatTimestampForDisplay(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) {
            return LocalDateTime.now().format(DISPLAY_TIME_FMT);
        }
        try {
            LocalDateTime dt = LocalDateTime.parse(isoTimestamp, ISO_FMT);
            return dt.format(DISPLAY_TIME_FMT);
        } catch (Exception e) {
            return isoTimestamp.length() > 8
                    ? isoTimestamp.substring(11, 19)
                    : isoTimestamp;
        }
    }

    public static double parseAmount(String rawAmount) throws NumberFormatException {
        return Double.parseDouble(rawAmount.replace(",", "").replace(".", ""));
    }
}

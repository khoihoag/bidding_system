package com.bidding.util;

import com.google.gson.JsonObject;

/**
 * Shared JSON parsing helpers for client controllers.
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    public static JsonObject request(String action) {
        JsonObject request = new JsonObject();
        request.addProperty("action", action);
        return request;
    }

    public static String getString(JsonObject obj, String key) {
        return (obj != null && obj.has(key) && !obj.get(key).isJsonNull())
                ? obj.get(key).getAsString()
                : "";
    }

    public static String getString(JsonObject obj, String key, String defaultValue) {
        return (obj != null && obj.has(key) && !obj.get(key).isJsonNull())
                ? obj.get(key).getAsString()
                : defaultValue;
    }

    public static int getInt(JsonObject obj, String key, int defaultValue) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return defaultValue;
        }
        return obj.get(key).getAsInt();
    }

    public static boolean getBoolean(JsonObject obj, String key, boolean defaultValue) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return defaultValue;
        }
        if (obj.get(key).isJsonPrimitive() && obj.get(key).getAsJsonPrimitive().isBoolean()) {
            return obj.get(key).getAsBoolean();
        }
        return Boolean.parseBoolean(obj.get(key).getAsString());
    }

    public static String formatBidType(JsonObject obj, String key) {
        return getBoolean(obj, key, false) ? "AUTO" : "MANUAL";
    }
}

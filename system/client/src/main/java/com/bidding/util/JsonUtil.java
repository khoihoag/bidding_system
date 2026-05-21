package com.bidding.util;

import com.google.gson.JsonObject;

/**
 * Shared JSON parsing helpers for client controllers.
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    public static String getString(JsonObject obj, String key) {
        return (obj != null && obj.has(key) && !obj.get(key).isJsonNull())
                ? obj.get(key).getAsString()
                : "";
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

package com.bidding.server.utils;

import com.google.gson.JsonObject;

/**
 * Builds consistent JSON responses for the socket protocol.
 */
public final class ApiResponse {

    private ApiResponse() {
    }

    public static JsonObject success(String action) {
        JsonObject reply = new JsonObject();
        reply.addProperty("action", action);
        reply.addProperty("status", "SUCCESS");
        return reply;
    }

    public static JsonObject success(String action, String key, String value) {
        JsonObject reply = success(action);
        reply.addProperty(key, value);
        return reply;
    }

    public static JsonObject loginSuccess(String userId, String role, double balance) {
        JsonObject reply = new JsonObject();
        reply.addProperty("action", "LOGIN_REPLY");
        reply.addProperty("status", "SUCCESS");
        reply.addProperty("myId", userId);
        reply.addProperty("role", role);
        reply.addProperty("balance", balance);
        return reply;
    }
}

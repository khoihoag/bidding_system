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
        return loginSuccess(userId, role, 0, balance);
    }

    public static JsonObject loginSuccess(String userId, String role, int adminLevel, double balance) {
        JsonObject reply = new JsonObject();
        reply.addProperty("action", "LOGIN_REPLY");
        reply.addProperty("status", "SUCCESS");
        reply.addProperty("myId", userId);
        reply.addProperty("role", role);
        reply.addProperty("adminLevel", adminLevel);
        reply.addProperty("balance", balance);
        return reply;
    }

    public static JsonObject loginSuccess(String userId, String username, String fullName, String email,
                                          String role, int adminLevel, double balance) {
        JsonObject reply = loginSuccess(userId, role, adminLevel, balance);
        reply.addProperty("username", username);
        reply.addProperty("fullName", fullName);
        reply.addProperty("email", email);
        return reply;
    }
}

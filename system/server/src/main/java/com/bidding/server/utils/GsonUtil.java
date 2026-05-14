package com.bidding.server.utils;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GsonUtil {

    // Tạo sẵn 1 biến static dùng chung cho toàn Server
    private static final Gson customGson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                @Override
                public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                    return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                }
            })
            .registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
            })
            .create();

    // Hàm này để các class khác gọi ra lấy Gson xài
    public static Gson getInstance() {
        return customGson;
    }
}
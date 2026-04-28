package com.promoitotp.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class Json {

    private Json() {}

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public static String write(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    public static <T> T read(String src, Class<T> type) {
        try {
            return MAPPER.readValue(src, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON: " + e.getMessage());
        }
    }
}

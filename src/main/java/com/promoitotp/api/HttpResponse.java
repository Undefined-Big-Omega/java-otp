package com.promoitotp.api;

import com.promoitotp.util.Json;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpResponse {

    private HttpResponse() {}

    public static void ok(HttpExchange ex, Object body) throws IOException {
        send(ex, 200, body);
    }

    public static void created(HttpExchange ex, Object body) throws IOException {
        send(ex, 201, body);
    }

    public static void noContent(HttpExchange ex) throws IOException {
        ex.sendResponseHeaders(204, -1);
        ex.close();
    }

    public static void error(HttpExchange ex, int status, String message) throws IOException {
        send(ex, status, Map.of("error", message));
    }

    public static void send(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = Json.write(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    public static String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    public static String bearerToken(HttpExchange ex) {
        String h = ex.getRequestHeaders().getFirst("Authorization");
        return (h != null && h.startsWith("Bearer ")) ? h.substring(7) : null;
    }
}

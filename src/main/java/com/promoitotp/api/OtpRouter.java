package com.promoitotp.api;

import com.promoitotp.core.OtpService;
import com.promoitotp.util.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class OtpRouter implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(OtpRouter.class);

    private final OtpService otpService;

    public OtpRouter(OtpService otpService) {
        this.otpService = otpService;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        // claims установлены AccessFilter'ом до вызова этого хендлера
        Claims claims = (Claims) ex.getAttribute("claims");
        long userId = Long.parseLong(claims.getSubject());
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();

        try {
            if ("POST".equalsIgnoreCase(method) && path.endsWith("/generate")) {
                generate(ex, userId);
            } else if ("POST".equalsIgnoreCase(method) && path.endsWith("/validate")) {
                validate(ex, userId);
            } else {
                HttpResponse.error(ex, 404, "Маршрут не найден");
            }
        } catch (Exception e) {
            log.error("Ошибка в OtpRouter", e);
            HttpResponse.error(ex, 500, "Внутренняя ошибка сервера");
        }
    }

    private void generate(HttpExchange ex, long userId) throws IOException {
        try {
            var dto = Json.read(HttpResponse.readBody(ex), GenerateDto.class);
            if (dto.operationId() == null || dto.operationId().isBlank()) {
                HttpResponse.error(ex, 400, "operationId обязателен");
                return;
            }
            if (dto.channel() == null || dto.channel().isBlank()) {
                HttpResponse.error(ex, 400, "channel обязателен (EMAIL, SMS, TELEGRAM, FILE)");
                return;
            }
            String dest = dto.destination() != null ? dto.destination() : "";
            otpService.generate(userId, dto.operationId(), dto.channel(), dest);
            HttpResponse.ok(ex, Map.of("message", "OTP успешно отправлен"));
        } catch (IllegalArgumentException e) {
            HttpResponse.error(ex, 400, e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при генерации OTP", e);
            HttpResponse.error(ex, 500, "Ошибка при отправке OTP: " + e.getMessage());
        }
    }

    private void validate(HttpExchange ex, long userId) throws IOException {
        try {
            var dto = Json.read(HttpResponse.readBody(ex), ValidateDto.class);
            if (dto.operationId() == null || dto.operationId().isBlank()) {
                HttpResponse.error(ex, 400, "operationId обязателен");
                return;
            }
            if (dto.code() == null || dto.code().isBlank()) {
                HttpResponse.error(ex, 400, "code обязателен");
                return;
            }
            boolean valid = otpService.validate(userId, dto.operationId(), dto.code());
            if (valid) {
                HttpResponse.ok(ex, Map.of("valid", true, "message", "Код подтверждён"));
            } else {
                HttpResponse.send(ex, 400, Map.of("valid", false, "message", "Код неверен или истёк"));
            }
        } catch (IllegalArgumentException e) {
            HttpResponse.error(ex, 400, e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при валидации OTP", e);
            HttpResponse.error(ex, 500, "Внутренняя ошибка сервера");
        }
    }

    record GenerateDto(String operationId, String channel, String destination) {}
    record ValidateDto(String operationId, String code) {}
}

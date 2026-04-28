package com.promoitotp.api;

import com.promoitotp.core.UserService;
import com.promoitotp.dao.OtpSettingsDao;
import com.promoitotp.model.OtpSettings;
import com.promoitotp.util.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AdminRouter implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(AdminRouter.class);

    private final UserService   userService;
    private final OtpSettingsDao settingsDao;

    public AdminRouter(UserService userService, OtpSettingsDao settingsDao) {
        this.userService  = userService;
        this.settingsDao  = settingsDao;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path   = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();

        try {
            if ("PUT".equalsIgnoreCase(method) && path.endsWith("/config")) {
                updateSettings(ex);
            } else if ("GET".equalsIgnoreCase(method) && path.endsWith("/users")) {
                listUsers(ex);
            } else if ("DELETE".equalsIgnoreCase(method) && path.contains("/users/")) {
                deleteUser(ex, path);
            } else {
                HttpResponse.error(ex, 404, "Маршрут не найден");
            }
        } catch (Exception e) {
            log.error("Ошибка в AdminRouter", e);
            HttpResponse.error(ex, 500, "Внутренняя ошибка сервера");
        }
    }

    private void updateSettings(HttpExchange ex) throws IOException {
        try {
            var dto = Json.read(HttpResponse.readBody(ex), SettingsDto.class);
            if (dto.codeLength() == null || dto.ttlSeconds() == null) {
                HttpResponse.error(ex, 400, "Поля codeLength и ttlSeconds обязательны");
                return;
            }
            if (dto.codeLength() < 4 || dto.codeLength() > 10) {
                HttpResponse.error(ex, 400, "codeLength должен быть от 4 до 10");
                return;
            }
            if (dto.ttlSeconds() <= 0) {
                HttpResponse.error(ex, 400, "ttlSeconds должен быть положительным");
                return;
            }
            settingsDao.save(dto.codeLength(), dto.ttlSeconds());
            OtpSettings updated = settingsDao.load();
            HttpResponse.ok(ex, updated);
        } catch (IllegalArgumentException e) {
            HttpResponse.error(ex, 400, e.getMessage());
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void listUsers(HttpExchange ex) throws IOException {
        try {
            HttpResponse.ok(ex, userService.listUsers());
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void deleteUser(HttpExchange ex, String path) throws IOException {
        String[] parts = path.split("/");
        String idStr = parts[parts.length - 1];
        try {
            long id = Long.parseLong(idStr);
            boolean removed = userService.deleteUser(id);
            if (removed) {
                HttpResponse.noContent(ex);
            } else {
                HttpResponse.error(ex, 404, "Пользователь не найден или является администратором");
            }
        } catch (NumberFormatException e) {
            HttpResponse.error(ex, 400, "Некорректный id пользователя");
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    record SettingsDto(Integer codeLength, Integer ttlSeconds) {}
}

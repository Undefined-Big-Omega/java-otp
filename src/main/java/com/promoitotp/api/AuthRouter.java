package com.promoitotp.api;

import com.promoitotp.core.UserService;
import com.promoitotp.model.UserRecord;
import com.promoitotp.util.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class AuthRouter implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthRouter.class);

    private final UserService userService;

    public AuthRouter(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path   = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();

        try {
            if ("POST".equalsIgnoreCase(method) && path.endsWith("/register")) {
                register(ex);
            } else if ("POST".equalsIgnoreCase(method) && path.endsWith("/login")) {
                login(ex);
            } else {
                HttpResponse.error(ex, 404, "Маршрут не найден");
            }
        } catch (Exception e) {
            log.error("Ошибка в AuthRouter", e);
            HttpResponse.error(ex, 500, "Внутренняя ошибка сервера");
        }
    }

    private void register(HttpExchange ex) throws IOException {
        try {
            var req = Json.read(HttpResponse.readBody(ex), RegisterDto.class);
            UserRecord user = userService.register(req.login(), req.password(), req.role());
            HttpResponse.created(ex, Map.of(
                    "id",    user.getId(),
                    "login", user.getLogin(),
                    "role",  user.getRole()
            ));
        } catch (IllegalArgumentException e) {
            HttpResponse.error(ex, 400, e.getMessage());
        } catch (IllegalStateException e) {
            HttpResponse.error(ex, 409, e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка регистрации", e);
            HttpResponse.error(ex, 500, "Внутренняя ошибка сервера");
        }
    }

    private void login(HttpExchange ex) throws IOException {
        try {
            var req   = Json.read(HttpResponse.readBody(ex), LoginDto.class);
            String token = userService.authenticate(req.login(), req.password());
            HttpResponse.ok(ex, Map.of("token", token));
        } catch (IllegalArgumentException e) {
            HttpResponse.error(ex, 401, e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка аутентификации", e);
            HttpResponse.error(ex, 500, "Внутренняя ошибка сервера");
        }
    }

    record RegisterDto(String login, String password, String role) {}
    record LoginDto(String login, String password) {}
}

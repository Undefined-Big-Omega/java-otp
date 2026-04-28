package com.promoitotp.api;

import com.promoitotp.util.Tokens;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AccessFilter extends Filter {

    private static final Logger log = LoggerFactory.getLogger(AccessFilter.class);

    private final String requiredRole;

    public AccessFilter(String requiredRole) {
        this.requiredRole = requiredRole;
    }

    @Override
    public void doFilter(HttpExchange ex, Chain chain) throws IOException {
        String token = HttpResponse.bearerToken(ex);
        if (token == null) {
            HttpResponse.error(ex, 401, "Отсутствует заголовок Authorization");
            return;
        }
        try {
            Claims claims = Tokens.verify(token);
            String role   = claims.get("role", String.class);
            if (!requiredRole.equals(role)) {
                HttpResponse.error(ex, 403, "Недостаточно прав");
                return;
            }
            ex.setAttribute("claims", claims);
            chain.doFilter(ex);
        } catch (JwtException e) {
            log.warn("Неверный токен: {}", e.getMessage());
            HttpResponse.error(ex, 401, "Токен недействителен или истёк");
        }
    }

    @Override
    public String description() {
        return "Фильтр доступа по роли [" + requiredRole + "]";
    }
}

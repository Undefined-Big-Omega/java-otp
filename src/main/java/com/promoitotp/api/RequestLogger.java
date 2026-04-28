package com.promoitotp.api;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RequestLogger extends Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLogger.class);

    @Override
    public void doFilter(HttpExchange ex, Chain chain) throws IOException {
        long start = System.currentTimeMillis();
        chain.doFilter(ex);
        long ms = System.currentTimeMillis() - start;

        Claims claims = (Claims) ex.getAttribute("claims");
        String who    = claims != null ? claims.get("login", String.class) : "гость";
        String ip     = ex.getRemoteAddress().getAddress().getHostAddress();
        int    status = ex.getResponseCode();
        String line   = "%s %s %d | %s | ip=%s | %dms"
                .formatted(ex.getRequestMethod(), ex.getRequestURI().getPath(), status, who, ip, ms);

        if (status >= 500)      log.error(line);
        else if (status >= 400) log.warn(line);
        else                    log.info(line);
    }

    @Override
    public String description() { return "Логгер HTTP-запросов"; }
}

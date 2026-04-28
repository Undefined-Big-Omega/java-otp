package com.promoitotp;

import com.promoitotp.api.*;
import com.promoitotp.core.ExpiredOtpCleaner;
import com.promoitotp.core.OtpService;
import com.promoitotp.core.UserService;
import com.promoitotp.dao.ConnectionPool;
import com.promoitotp.dao.OtpDao;
import com.promoitotp.dao.OtpSettingsDao;
import com.promoitotp.dao.UserDao;
import com.promoitotp.notify.*;
import com.promoitotp.util.Props;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        Properties cfg = Props.load("app.properties");
        int port    = Props.getInt(cfg, "server.port",    8080);
        int threads = Props.getInt(cfg, "server.threads", 10);

        ConnectionPool.getInstance().initSchema();

        UserDao        userDao      = new UserDao();
        OtpDao         otpDao       = new OtpDao();
        OtpSettingsDao settingsDao  = new OtpSettingsDao();

        Map<OtpService.Channel, Sender> senders = new EnumMap<>(OtpService.Channel.class);
        senders.put(OtpService.Channel.EMAIL,    new EmailSender());
        senders.put(OtpService.Channel.SMS,      new SmsSender());
        senders.put(OtpService.Channel.TELEGRAM, new TelegramSender());
        senders.put(OtpService.Channel.FILE,     new FileSender());

        UserService userService = new UserService(userDao);
        OtpService  otpService  = new OtpService(otpDao, settingsDao, senders);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(threads));

        RequestLogger logger = new RequestLogger();

        HttpContext authCtx = server.createContext("/auth", new AuthRouter(userService));
        authCtx.getFilters().add(logger);

        HttpContext adminCtx = server.createContext("/admin", new AdminRouter(userService, settingsDao));
        adminCtx.getFilters().add(logger);
        adminCtx.getFilters().add(new AccessFilter("ADMIN"));

        HttpContext otpCtx = server.createContext("/otp", new OtpRouter(otpService));
        otpCtx.getFilters().add(logger);
        otpCtx.getFilters().add(new AccessFilter("USER"));

        ExpiredOtpCleaner cleaner = new ExpiredOtpCleaner(otpDao);
        cleaner.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Завершение работы...");
            cleaner.shutdown();
            server.stop(3);
        }));

        server.start();
        log.info("Promo OTP Service запущен на порту {}", port);
    }
}

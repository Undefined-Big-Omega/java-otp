package com.promoitotp.core;

import com.promoitotp.dao.OtpDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExpiredOtpCleaner {

    private static final Logger log = LoggerFactory.getLogger(ExpiredOtpCleaner.class);
    private static final int INTERVAL_SEC = 60;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "otp-cleaner");
        t.setDaemon(true);
        return t;
    });

    private final OtpDao otpDao;

    public ExpiredOtpCleaner(OtpDao otpDao) {
        this.otpDao = otpDao;
    }

    public void start() {
        executor.scheduleAtFixedRate(this::run, INTERVAL_SEC, INTERVAL_SEC, TimeUnit.SECONDS);
        log.info("Планировщик просрочки OTP запущен (каждые {}с)", INTERVAL_SEC);
    }

    public void shutdown() {
        executor.shutdownNow();
        log.info("Планировщик просрочки OTP остановлен");
    }

    private void run() {
        try {
            int count = otpDao.expireStale();
            if (count > 0) log.info("Помечено просроченными: {} OTP", count);
        } catch (Exception e) {
            log.error("Ошибка в задаче просрочки OTP", e);
        }
    }
}

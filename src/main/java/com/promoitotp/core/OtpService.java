package com.promoitotp.core;

import com.promoitotp.dao.OtpDao;
import com.promoitotp.dao.OtpSettingsDao;
import com.promoitotp.model.OtpEntry;
import com.promoitotp.model.OtpSettings;
import com.promoitotp.model.OtpStatus;
import com.promoitotp.notify.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.Map;

public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    public enum Channel { EMAIL, SMS, TELEGRAM, FILE }

    private final OtpDao otpDao;
    private final OtpSettingsDao settingsDao;
    private final Map<Channel, Sender> senders;

    public OtpService(OtpDao otpDao, OtpSettingsDao settingsDao, Map<Channel, Sender> senders) {
        this.otpDao      = otpDao;
        this.settingsDao = settingsDao;
        this.senders     = new EnumMap<>(senders);
    }

    public void generate(long userId, String operationId, String channelName, String destination)
            throws Exception {
        if (operationId == null || operationId.isBlank())
            throw new IllegalArgumentException("operationId обязателен");

        Channel channel;
        try {
            channel = Channel.valueOf(channelName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Неизвестный канал: " + channelName
                    + ". Доступные: EMAIL, SMS, TELEGRAM, FILE");
        }

        Sender sender = senders.get(channel);
        if (sender == null)
            throw new IllegalStateException("Канал не настроен: " + channel);

        OtpSettings settings = settingsDao.load();
        String code           = makeCode(settings.getCodeLength());
        OffsetDateTime now    = OffsetDateTime.now();

        OtpEntry entry = new OtpEntry();
        entry.setOperationId(operationId);
        entry.setCode(code);
        entry.setStatus(OtpStatus.ACTIVE);
        entry.setUserId(userId);
        entry.setCreatedAt(now);
        entry.setExpiresAt(now.plusSeconds(settings.getTtlSeconds()));

        otpDao.insert(entry);
        log.info("OTP создан: userId={} operationId={} channel={}", userId, operationId, channel);

        sender.send(destination, code, operationId);
    }

    public boolean validate(long userId, String operationId, String submittedCode) throws SQLException {
        if (operationId == null || operationId.isBlank())
            throw new IllegalArgumentException("operationId обязателен");
        if (submittedCode == null || submittedCode.isBlank())
            throw new IllegalArgumentException("code обязателен");

        OtpEntry entry = otpDao.findLatestActive(operationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Активный OTP для операции не найден"));

        if (OffsetDateTime.now().isAfter(entry.getExpiresAt())) {
            otpDao.setStatus(entry.getId(), OtpStatus.EXPIRED);
            log.info("OTP просрочен: operationId={} userId={}", operationId, userId);
            return false;
        }

        if (!submittedCode.equals(entry.getCode())) {
            log.info("OTP неверный код: operationId={} userId={}", operationId, userId);
            return false;
        }

        otpDao.setStatus(entry.getId(), OtpStatus.USED);
        log.info("OTP принят: operationId={} userId={}", operationId, userId);
        return true;
    }

    private String makeCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}

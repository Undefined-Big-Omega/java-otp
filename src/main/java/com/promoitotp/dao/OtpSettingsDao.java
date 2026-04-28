package com.promoitotp.dao;

import com.promoitotp.model.OtpSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class OtpSettingsDao {

    private static final Logger log = LoggerFactory.getLogger(OtpSettingsDao.class);
    private final ConnectionPool pool = ConnectionPool.getInstance();

    public OtpSettings load() throws SQLException {
        Connection conn = pool.acquire();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT code_length, ttl_seconds FROM otp_config WHERE id = 1");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new IllegalStateException("otp_config row missing");
            return new OtpSettings(rs.getInt("code_length"), rs.getInt("ttl_seconds"));
        } finally {
            pool.release(conn);
        }
    }

    public void save(int codeLength, int ttlSeconds) throws SQLException {
        Connection conn = pool.acquire();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE otp_config SET code_length = ?, ttl_seconds = ? WHERE id = 1")) {
            ps.setInt(1, codeLength);
            ps.setInt(2, ttlSeconds);
            ps.executeUpdate();
            log.info("OTP settings updated: length={} ttl={}s", codeLength, ttlSeconds);
        } finally {
            pool.release(conn);
        }
    }
}

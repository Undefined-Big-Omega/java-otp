package com.promoitotp.dao;

import com.promoitotp.model.OtpEntry;
import com.promoitotp.model.OtpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.Optional;

public class OtpDao {

    private static final Logger log = LoggerFactory.getLogger(OtpDao.class);
    private final ConnectionPool pool = ConnectionPool.getInstance();

    public OtpEntry insert(OtpEntry entry) throws SQLException {
        Connection conn = pool.acquire();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO otp_codes (operation_id, code, status, user_id, created_at, expires_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entry.getOperationId());
            ps.setString(2, entry.getCode());
            ps.setString(3, entry.getStatus().name());
            ps.setLong(4, entry.getUserId());
            ps.setObject(5, entry.getCreatedAt());
            ps.setObject(6, entry.getExpiresAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) entry.setId(keys.getLong(1));
            }
            return entry;
        } finally {
            pool.release(conn);
        }
    }

    public Optional<OtpEntry> findLatestActive(String operationId, long userId) throws SQLException {
        Connection conn = pool.acquire();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, operation_id, code, status, user_id, created_at, expires_at " +
                "FROM otp_codes WHERE operation_id = ? AND user_id = ? AND status = 'ACTIVE' " +
                "ORDER BY created_at DESC LIMIT 1")) {
            ps.setString(1, operationId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(toEntry(rs)) : Optional.empty();
            }
        } finally {
            pool.release(conn);
        }
    }

    public void setStatus(long id, OtpStatus status) throws SQLException {
        Connection conn = pool.acquire();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE otp_codes SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            ps.executeUpdate();
            log.info("OTP id={} -> {}", id, status);
        } finally {
            pool.release(conn);
        }
    }

    public int expireStale() throws SQLException {
        Connection conn = pool.acquire();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE otp_codes SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND expires_at < NOW()")) {
            return ps.executeUpdate();
        } finally {
            pool.release(conn);
        }
    }

    private OtpEntry toEntry(ResultSet rs) throws SQLException {
        OtpEntry e = new OtpEntry();
        e.setId(rs.getLong("id"));
        e.setOperationId(rs.getString("operation_id"));
        e.setCode(rs.getString("code"));
        e.setStatus(OtpStatus.valueOf(rs.getString("status")));
        e.setUserId(rs.getLong("user_id"));
        e.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        e.setExpiresAt(rs.getObject("expires_at", OffsetDateTime.class));
        return e;
    }
}

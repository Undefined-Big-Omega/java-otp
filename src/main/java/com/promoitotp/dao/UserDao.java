package com.promoitotp.dao;

import com.promoitotp.model.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDao.class);
    private final ConnectionPool pool = ConnectionPool.getInstance();

    public Optional<UserRecord> findByLogin(String login) throws SQLException {
        Connection conn = pool.acquire();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, login, password, role, created_at FROM users WHERE login = ?")) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(toUser(rs)) : Optional.empty();
            }
        } finally {
            pool.release(conn);
        }
    }

    public boolean hasAdmin() throws SQLException {
        Connection conn = pool.acquire();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM users WHERE role = 'ADMIN' LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next();
        } finally {
            pool.release(conn);
        }
    }

    public UserRecord insert(UserRecord u) throws SQLException {
        Connection conn = pool.acquire();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (login, password, role) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getLogin());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getRole());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) u.setId(keys.getLong(1));
            }
            log.info("Inserted user login={} role={}", u.getLogin(), u.getRole());
            return u;
        } finally {
            pool.release(conn);
        }
    }

    public List<UserRecord> listRegularUsers() throws SQLException {
        Connection conn = pool.acquire();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, login, role, created_at FROM users WHERE role = 'USER' ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<UserRecord> result = new ArrayList<>();
            while (rs.next()) {
                UserRecord u = new UserRecord();
                u.setId(rs.getLong("id"));
                u.setLogin(rs.getString("login"));
                u.setRole(rs.getString("role"));
                u.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
                result.add(u);
            }
            return result;
        } finally {
            pool.release(conn);
        }
    }

    public boolean removeById(long id) throws SQLException {
        Connection conn = pool.acquire();
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM users WHERE id = ? AND role = 'USER'")) {
            ps.setLong(1, id);
            boolean deleted = ps.executeUpdate() > 0;
            if (deleted) log.info("Removed user id={}", id);
            return deleted;
        } finally {
            pool.release(conn);
        }
    }

    private UserRecord toUser(ResultSet rs) throws SQLException {
        UserRecord u = new UserRecord();
        u.setId(rs.getLong("id"));
        u.setLogin(rs.getString("login"));
        u.setPasswordHash(rs.getString("password"));
        u.setRole(rs.getString("role"));
        u.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        return u;
    }
}

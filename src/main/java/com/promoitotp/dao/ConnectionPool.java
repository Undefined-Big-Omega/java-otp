package com.promoitotp.dao;

import com.promoitotp.util.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class ConnectionPool {

    private static final Logger log = LoggerFactory.getLogger(ConnectionPool.class);
    private static final ConnectionPool INSTANCE = new ConnectionPool();

    private final BlockingQueue<Connection> pool;
    private final List<Connection> allConnections;

    private ConnectionPool() {
        Properties p = Props.load("app.properties");
        String url   = Props.get(p, "db.url");
        String user  = Props.get(p, "db.user");
        String pass  = Props.get(p, "db.pass");
        int    size  = Props.getInt(p, "db.pool.size", 8);

        pool           = new ArrayBlockingQueue<>(size);
        allConnections = new ArrayList<>(size);

        try {
            for (int i = 0; i < size; i++) {
                Connection conn = DriverManager.getConnection(url, user, pass);
                pool.add(conn);
                allConnections.add(conn);
            }
            log.info("Connection pool ready ({} connections)", size);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot initialize connection pool", e);
        }
    }

    public static ConnectionPool getInstance() { return INSTANCE; }

    public Connection acquire() {
        try {
            return pool.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for DB connection", e);
        }
    }

    public void release(Connection conn) {
        if (conn != null) pool.offer(conn);
    }

    public void initSchema() {
        Connection conn = acquire();
        try {
            var is = getClass().getClassLoader().getResourceAsStream("schema.sql");
            if (is == null) throw new IllegalStateException("schema.sql not found");
            String sql = new String(is.readAllBytes());
            try (var stmt = conn.createStatement()) {
                for (String part : sql.split(";")) {
                    String s = part.strip();
                    if (!s.isEmpty()) stmt.execute(s);
                }
            }
            log.info("Schema applied");
        } catch (Exception e) {
            throw new IllegalStateException("Schema init failed", e);
        } finally {
            release(conn);
        }
    }
}

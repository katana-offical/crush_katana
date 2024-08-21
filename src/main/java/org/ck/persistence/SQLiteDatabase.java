package org.ck.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.function.Consumer;

public class SQLiteDatabase implements Database {

    private static final Logger log = LoggerFactory.getLogger(SQLiteDatabase.class);
    private static final String DB_URL = "jdbc:sqlite:ck.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            log.error("Failed to load SQLite JDBC driver", e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    @Override
    public void executeUpdate(String sql) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.executeUpdate();
            log.info("Executed update: {}", sql);

        } catch (SQLException e) {
            log.error("Failed to execute update: {}", sql, e);
        }
    }

    @Override
    public void executeQuery(String sql, Consumer<ResultSet> resultSetConsumer) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            resultSetConsumer.accept(rs);
            log.info("Executed query: {}", sql);

        } catch (SQLException e) {
            log.error("Failed to execute query: {}", sql, e);
        }
    }

    @Override
    public void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("Failed to close connection", e);
            }
        }
    }

    @Override
    public void closeStatement(PreparedStatement pstmt) {
        if (pstmt != null) {
            try {
                pstmt.close();
            } catch (SQLException e) {
                log.error("Failed to close PreparedStatement", e);
            }
        }
    }

    @Override
    public void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.error("Failed to close ResultSet", e);
            }
        }
    }
}

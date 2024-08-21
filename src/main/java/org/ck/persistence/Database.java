package org.ck.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

public interface Database extends Persistence {
    Connection getConnection() throws SQLException;
    void executeUpdate(String sql);
    void executeQuery(String sql, Consumer<ResultSet> resultSetConsumer);
    void closeConnection(Connection conn);
    void closeStatement(PreparedStatement pstmt);
    void closeResultSet(ResultSet rs);
}

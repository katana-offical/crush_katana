package org.ck;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class PersistenceTest extends TestCase {

    private static final Logger log = LoggerFactory.getLogger(PersistenceTest.class);

    public void testSqliteCreateTable() {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:ck.db");
            log.info("Opened database successfully");
            stmt = c.createStatement();
            String sql = "CREATE TABLE COMPANY " +
                    "(ID INT PRIMARY KEY     NOT NULL," +
                    " NAME           TEXT    NOT NULL, " +
                    " AGE            INT     NOT NULL, " +
                    " ADDRESS        CHAR(50), " +
                    " SALARY         REAL)";
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
        } catch (Exception e) {
            log.error(e.getClass().getName() + ": " + e.getMessage());
        }
        log.info("Table created successfully");
    }

    public void testSqliteInsert() {
        Connection c = null;
        Statement stmt = null;
        try {
            c = DriverManager.getConnection("jdbc:sqlite:ck.db");
            log.info("Opened database successfully");
            stmt = c.createStatement();
            String sql = "INSERT INTO COMPANY (ID,NAME,AGE,ADDRESS,SALARY) " +
                    "VALUES (1, 'John', 32, 'California', 20000.00 );";
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
        } catch (Exception e) {
            log.error(e.getClass().getName() + ": " + e.getMessage());
        }
        log.info("Values inserted successfully");
    }

    public void testSqliteSelect() {
        Connection c = null;
        Statement stmt = null;
        try {
            c = DriverManager.getConnection("jdbc:sqlite:ck.db");
            log.info("Opened database successfully");
            stmt = c.createStatement();
            String sql = "SELECT id, name, age, address, salary from COMPANY";
            ResultSet resultSet = stmt.executeQuery(sql);
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                int age = resultSet.getInt("age");
                String address = resultSet.getString("address");
                BigDecimal salary = resultSet.getBigDecimal("salary");
                log.info(id + "\t" + name + "\t" + age + "\t" + address + "\t" + salary.toString());
            }
            stmt.close();
            c.close();
        } catch (Exception e) {
            log.error(e.getClass().getName() + ": " + e.getMessage());
        }
    }
}

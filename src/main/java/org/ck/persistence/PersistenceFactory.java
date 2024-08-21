package org.ck.persistence;

public class PersistenceFactory {
    public static Database createDatabase(String dbType) {
        if ("sqlite".equalsIgnoreCase(dbType)) {
            return new SQLiteDatabase();
        }
        // 这里可以扩展其他数据库实现，例如 MySQLDatabase
        throw new IllegalArgumentException("Unsupported database type: " + dbType);
    }
}

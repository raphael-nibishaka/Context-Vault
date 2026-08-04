package database;

import config.AppPaths;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    private final String jdbcUrl;

    public ConnectionFactory() {
        this.jdbcUrl = "jdbc:sqlite:" + AppPaths.getDatabaseFile();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }
}

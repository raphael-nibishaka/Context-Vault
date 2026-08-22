package database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final ConnectionFactory connectionFactory;

    public DatabaseInitializer(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void initialize() {
        try (Connection connection = connectionFactory.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS contexts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        project_name TEXT,
                        project_path TEXT NOT NULL,
                        git_repo_path TEXT,
                        git_branch TEXT,
                        open_files TEXT,
                        note TEXT,
                        commands TEXT,
                        tags TEXT,
                        browser_urls TEXT,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS settings (
                        setting_key TEXT PRIMARY KEY,
                        setting_value TEXT NOT NULL
                    )
                    """);

            migrateContextsTable(connection);
            LOGGER.info("Database schema ready");
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to initialize database schema", exception);
        }
    }

    private void migrateContextsTable(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "contexts", "project_name", "TEXT");
        addColumnIfMissing(connection, "contexts", "git_repo_path", "TEXT");
        addColumnIfMissing(connection, "contexts", "open_files", "TEXT");
        addColumnIfMissing(connection, "contexts", "tags", "TEXT");
        addColumnIfMissing(connection, "contexts", "browser_urls", "TEXT");
    }

    private void addColumnIfMissing(Connection connection, String table, String column, String definition)
            throws SQLException {
        if (columnExists(connection, table, column)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (resultSet.next()) {
                if (column.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
}

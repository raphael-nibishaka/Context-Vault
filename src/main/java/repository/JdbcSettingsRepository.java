package repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import database.ConnectionFactory;
import models.AppSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcSettingsRepository implements SettingsRepository {
    private static final String SETTINGS_KEY = "application_settings";

    private final ConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper;

    public JdbcSettingsRepository(ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        this.connectionFactory = connectionFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public AppSettings load() {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, SETTINGS_KEY);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return objectMapper.readValue(resultSet.getString("setting_value"), AppSettings.class);
                }
                return AppSettings.defaults();
            }
        } catch (SQLException | JsonProcessingException exception) {
            throw new IllegalStateException("Unable to load application settings", exception);
        }
    }

    @Override
    public void save(AppSettings settings) {
        String sql = """
                INSERT INTO settings(setting_key, setting_value)
                VALUES(?, ?)
                ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, SETTINGS_KEY);
            statement.setString(2, objectMapper.writeValueAsString(settings));
            statement.executeUpdate();
        } catch (SQLException | JsonProcessingException exception) {
            throw new IllegalStateException("Unable to save application settings", exception);
        }
    }
}

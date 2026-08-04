package repository;

import database.ConnectionFactory;
import models.ContextEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcContextRepository implements ContextRepository {
    private final ConnectionFactory connectionFactory;

    public JdbcContextRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public List<ContextEntry> findAll() {
        String sql = """
                SELECT id, name, project_path, git_branch, note, commands, created_at, updated_at
                FROM contexts
                ORDER BY updated_at DESC
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<ContextEntry> results = new ArrayList<>();
            while (resultSet.next()) {
                results.add(map(resultSet));
            }
            return results;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load contexts", exception);
        }
    }

    @Override
    public List<ContextEntry> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }

        String sql = """
                SELECT id, name, project_path, git_branch, note, commands, created_at, updated_at
                FROM contexts
                WHERE lower(name) LIKE lower(?)
                   OR lower(project_path) LIKE lower(?)
                   OR lower(coalesce(git_branch, '')) LIKE lower(?)
                ORDER BY updated_at DESC
                """;

        String like = "%" + query.trim() + "%";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, like);
            statement.setString(2, like);
            statement.setString(3, like);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<ContextEntry> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to search contexts", exception);
        }
    }

    @Override
    public Optional<ContextEntry> findById(long id) {
        String sql = """
                SELECT id, name, project_path, git_branch, note, commands, created_at, updated_at
                FROM contexts
                WHERE id = ?
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load context by id", exception);
        }
    }

    @Override
    public ContextEntry save(ContextEntry contextEntry) {
        String sql = """
                INSERT INTO contexts(name, project_path, git_branch, note, commands, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        LocalDateTime now = LocalDateTime.now();
        contextEntry.setCreatedAt(now);
        contextEntry.setUpdatedAt(now);

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, contextEntry);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    contextEntry.setId(generatedKeys.getLong(1));
                }
            }
            return contextEntry;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to save context", exception);
        }
    }

    @Override
    public ContextEntry update(ContextEntry contextEntry) {
        String sql = """
                UPDATE contexts
                SET name = ?, project_path = ?, git_branch = ?, note = ?, commands = ?, created_at = ?, updated_at = ?
                WHERE id = ?
                """;

        contextEntry.setUpdatedAt(LocalDateTime.now());

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, contextEntry);
            statement.setLong(8, contextEntry.getId());
            statement.executeUpdate();
            return contextEntry;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update context", exception);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM contexts WHERE id = ?";

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to delete context", exception);
        }
    }

    private void bind(PreparedStatement statement, ContextEntry contextEntry) throws SQLException {
        statement.setString(1, contextEntry.getName());
        statement.setString(2, contextEntry.getProjectPath());
        statement.setString(3, contextEntry.getGitBranch());
        statement.setString(4, contextEntry.getNote());
        statement.setString(5, contextEntry.getCommands());
        statement.setString(6, toTimestamp(contextEntry.getCreatedAt()).toString());
        statement.setString(7, toTimestamp(contextEntry.getUpdatedAt()).toString());
    }

    private ContextEntry map(ResultSet resultSet) throws SQLException {
        return new ContextEntry(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("project_path"),
                resultSet.getString("git_branch"),
                resultSet.getString("note"),
                resultSet.getString("commands"),
                parseDateTime(resultSet.getString("created_at")),
                parseDateTime(resultSet.getString("updated_at"))
        );
    }

    private LocalDateTime parseDateTime(String value) {
        return Timestamp.valueOf(value).toLocalDateTime();
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return Timestamp.valueOf(value);
    }
}

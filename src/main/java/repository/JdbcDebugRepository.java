package repository;

import database.ConnectionFactory;
import models.DebugEntry;

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

public class JdbcDebugRepository implements DebugRepository {
    private static final String SELECT_COLUMNS = """
            id, error_message, stack_trace, error_type, project_name, project_path,
            source_file, solution, fix_command, related_context, tags, created_at, updated_at
            """;

    private final ConnectionFactory connectionFactory;

    public JdbcDebugRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public List<DebugEntry> findAll() {
        String sql = """
                SELECT %s
                FROM debug_entries
                ORDER BY updated_at DESC
                """.formatted(SELECT_COLUMNS);

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<DebugEntry> results = new ArrayList<>();
            while (resultSet.next()) {
                results.add(map(resultSet));
            }
            return results;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load debug entries", exception);
        }
    }

    @Override
    public List<DebugEntry> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }

        String sql = """
                SELECT %s
                FROM debug_entries
                WHERE lower(coalesce(error_message, '')) LIKE lower(?)
                   OR lower(coalesce(error_type, '')) LIKE lower(?)
                   OR lower(coalesce(stack_trace, '')) LIKE lower(?)
                   OR lower(coalesce(project_name, '')) LIKE lower(?)
                   OR lower(coalesce(source_file, '')) LIKE lower(?)
                   OR lower(coalesce(solution, '')) LIKE lower(?)
                   OR lower(coalesce(fix_command, '')) LIKE lower(?)
                   OR lower(coalesce(related_context, '')) LIKE lower(?)
                   OR lower(coalesce(tags, '')) LIKE lower(?)
                ORDER BY updated_at DESC
                """.formatted(SELECT_COLUMNS);

        String like = "%" + query.trim() + "%";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 1; index <= 9; index++) {
                statement.setString(index, like);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                List<DebugEntry> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to search debug entries", exception);
        }
    }

    @Override
    public Optional<DebugEntry> findById(long id) {
        String sql = """
                SELECT %s
                FROM debug_entries
                WHERE id = ?
                """.formatted(SELECT_COLUMNS);

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
            throw new IllegalStateException("Unable to load debug entry by id", exception);
        }
    }

    @Override
    public DebugEntry save(DebugEntry entry) {
        String sql = """
                INSERT INTO debug_entries(
                    error_message, stack_trace, error_type, project_name, project_path,
                    source_file, solution, fix_command, related_context, tags, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        LocalDateTime now = LocalDateTime.now();
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, entry);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    entry.setId(generatedKeys.getLong(1));
                }
            }
            return entry;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to save debug entry", exception);
        }
    }

    @Override
    public DebugEntry update(DebugEntry entry) {
        String sql = """
                UPDATE debug_entries
                SET error_message = ?, stack_trace = ?, error_type = ?, project_name = ?, project_path = ?,
                    source_file = ?, solution = ?, fix_command = ?, related_context = ?, tags = ?,
                    created_at = ?, updated_at = ?
                WHERE id = ?
                """;

        entry.setUpdatedAt(LocalDateTime.now());

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, entry);
            statement.setLong(13, entry.getId());
            statement.executeUpdate();
            return entry;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update debug entry", exception);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM debug_entries WHERE id = ?";

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to delete debug entry", exception);
        }
    }

    private void bind(PreparedStatement statement, DebugEntry entry) throws SQLException {
        statement.setString(1, entry.getErrorMessage());
        statement.setString(2, entry.getStackTrace());
        statement.setString(3, entry.getErrorType());
        statement.setString(4, entry.getProjectName());
        statement.setString(5, entry.getProjectPath());
        statement.setString(6, entry.getSourceFile());
        statement.setString(7, entry.getSolution());
        statement.setString(8, entry.getFixCommand());
        statement.setString(9, entry.getRelatedContext());
        statement.setString(10, entry.getTags());
        statement.setString(11, toTimestamp(entry.getCreatedAt()).toString());
        statement.setString(12, toTimestamp(entry.getUpdatedAt()).toString());
    }

    private DebugEntry map(ResultSet resultSet) throws SQLException {
        return new DebugEntry(
                resultSet.getLong("id"),
                resultSet.getString("error_message"),
                resultSet.getString("stack_trace"),
                resultSet.getString("error_type"),
                resultSet.getString("project_name"),
                resultSet.getString("project_path"),
                resultSet.getString("source_file"),
                resultSet.getString("solution"),
                resultSet.getString("fix_command"),
                resultSet.getString("related_context"),
                resultSet.getString("tags"),
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

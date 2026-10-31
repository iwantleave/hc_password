package org.example.db;

import org.example.model.PasswordEntry;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class EntryRepository {
    private static final String COLUMNS =
            "id, title, username, password, url, category, notes, created_at, updated_at";

    private final DatabaseManager database;

    public EntryRepository(DatabaseManager database) {
        this.database = database;
    }

    public List<PasswordEntry> search(String keyword, String category) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM entries WHERE 1 = 1");
        List<String> parameters = new ArrayList<>();

        String trimmedKeyword = keyword == null ? "" : keyword.trim();
        if (!trimmedKeyword.isEmpty()) {
            String like = "%" + escapeLike(trimmedKeyword) + "%";
            sql.append(" AND (title LIKE ? ESCAPE '\\'")
                    .append(" OR username LIKE ? ESCAPE '\\'")
                    .append(" OR url LIKE ? ESCAPE '\\'")
                    .append(" OR category LIKE ? ESCAPE '\\')");
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ?");
            parameters.add(category);
        }
        sql.append(" ORDER BY updated_at DESC, title COLLATE NOCASE ASC");

        try (PreparedStatement statement = database.connection().prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setString(i + 1, parameters.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<PasswordEntry> entries = new ArrayList<>();
                while (resultSet.next()) {
                    entries.add(mapEntry(resultSet));
                }
                return entries;
            }
        }
    }

    public long insert(PasswordEntry entry) throws SQLException {
        try (PreparedStatement statement = database.connection().prepareStatement("""
                INSERT INTO entries(title, username, password, url, category, notes, created_at, updated_at)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            bindEntry(statement, entry);
            statement.executeUpdate();
        }
        try (Statement statement = database.connection().createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT last_insert_rowid()")) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        }
    }

    public void update(PasswordEntry entry) throws SQLException {
        try (PreparedStatement statement = database.connection().prepareStatement("""
                UPDATE entries
                SET title = ?, username = ?, password = ?, url = ?, category = ?, notes = ?, updated_at = ?
                WHERE id = ?
                """)) {
            statement.setString(1, entry.getTitle());
            statement.setString(2, entry.getUsername());
            statement.setString(3, entry.getPassword());
            statement.setString(4, entry.getUrl());
            statement.setString(5, entry.getCategory());
            statement.setString(6, entry.getNotes());
            statement.setString(7, entry.getUpdatedAt());
            statement.setLong(8, entry.getId());
            statement.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (PreparedStatement statement = database.connection().prepareStatement(
                "DELETE FROM entries WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    public List<String> findCategories() throws SQLException {
        try (Statement statement = database.connection().createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT DISTINCT category FROM entries
                     WHERE TRIM(category) <> ''
                     ORDER BY category COLLATE NOCASE
                     """)) {
            List<String> categories = new ArrayList<>();
            while (resultSet.next()) {
                categories.add(resultSet.getString(1));
            }
            return categories;
        }
    }

    public List<StoredSecret> findAllSecrets() throws SQLException {
        try (Statement statement = database.connection().createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT id, password, notes FROM entries ORDER BY id")) {
            List<StoredSecret> secrets = new ArrayList<>();
            while (resultSet.next()) {
                secrets.add(new StoredSecret(
                        resultSet.getLong("id"),
                        resultSet.getString("password"),
                        resultSet.getString("notes")));
            }
            return secrets;
        }
    }

    public void updateSecrets(long id, String encryptedPassword, String encryptedNotes)
            throws SQLException {
        try (PreparedStatement statement = database.connection().prepareStatement(
                "UPDATE entries SET password = ?, notes = ? WHERE id = ?")) {
            statement.setString(1, encryptedPassword);
            statement.setString(2, encryptedNotes);
            statement.setLong(3, id);
            statement.executeUpdate();
        }
    }

    private void bindEntry(PreparedStatement statement, PasswordEntry entry)
            throws SQLException {
        statement.setString(1, entry.getTitle());
        statement.setString(2, entry.getUsername());
        statement.setString(3, entry.getPassword());
        statement.setString(4, entry.getUrl());
        statement.setString(5, entry.getCategory());
        statement.setString(6, entry.getNotes());
        statement.setString(7, entry.getCreatedAt());
        statement.setString(8, entry.getUpdatedAt());
    }

    private PasswordEntry mapEntry(ResultSet resultSet) throws SQLException {
        PasswordEntry entry = new PasswordEntry();
        entry.setId(resultSet.getLong("id"));
        entry.setTitle(resultSet.getString("title"));
        entry.setUsername(resultSet.getString("username"));
        entry.setPassword(resultSet.getString("password"));
        entry.setUrl(resultSet.getString("url"));
        entry.setCategory(resultSet.getString("category"));
        entry.setNotes(resultSet.getString("notes"));
        entry.setCreatedAt(resultSet.getString("created_at"));
        entry.setUpdatedAt(resultSet.getString("updated_at"));
        return entry;
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    public record StoredSecret(long id, String password, String notes) {
    }
}

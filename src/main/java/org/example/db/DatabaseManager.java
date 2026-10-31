package org.example.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager implements AutoCloseable {
    private final Path databasePath;
    private Connection connection;

    public DatabaseManager(Path databasePath) {
        this.databasePath = databasePath;
    }

    public synchronized void open() throws SQLException, IOException {
        if (connection != null) {
            return;
        }
        Path parent = databasePath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("未找到 SQLite JDBC 驱动", e);
        }

        String url = "jdbc:sqlite:" + databasePath.toAbsolutePath().toString().replace('\\', '/');
        connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        createSchema();
    }

    private void createSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS meta (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        username TEXT NOT NULL DEFAULT '',
                        password TEXT NOT NULL,
                        url TEXT NOT NULL DEFAULT '',
                        category TEXT NOT NULL DEFAULT '默认',
                        notes TEXT NOT NULL DEFAULT '',
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_entries_title ON entries(title)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_entries_updated ON entries(updated_at)");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR IGNORE INTO meta(key, value) VALUES('schema_version', '1')")) {
            statement.executeUpdate();
        }
    }

    public synchronized Connection connection() throws SQLException {
        if (connection == null) {
            throw new SQLException("数据库尚未打开");
        }
        return connection;
    }

    public synchronized String getMeta(String key) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(
                "SELECT value FROM meta WHERE key = ?")) {
            statement.setString(1, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    public synchronized void putMeta(String key, String value) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement("""
                INSERT INTO meta(key, value) VALUES(?, ?)
                ON CONFLICT(key) DO UPDATE SET value = excluded.value
                """)) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        }
    }

    public synchronized void inTransaction(SqlWork work) throws SQLException {
        Connection current = connection();
        boolean autoCommit = current.getAutoCommit();
        current.setAutoCommit(false);
        try {
            work.run();
            current.commit();
        } catch (Exception e) {
            try {
                current.rollback();
            } catch (SQLException rollbackError) {
                e.addSuppressed(rollbackError);
            }
            if (e instanceof SQLException sqlException) {
                throw sqlException;
            }
            throw new SQLException("数据库事务执行失败", e);
        } finally {
            current.setAutoCommit(autoCommit);
        }
    }

    @Override
    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // 关闭连接失败时无需额外处理
            }
            connection = null;
        }
    }

    @FunctionalInterface
    public interface SqlWork {
        void run() throws Exception;
    }
}

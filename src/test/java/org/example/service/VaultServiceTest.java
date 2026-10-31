package org.example.service;

import org.example.db.DatabaseManager;
import org.example.model.PasswordEntry;
import org.example.security.VaultException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultServiceTest {
    private static final char[] MASTER_PASSWORD = "correct-master-1".toCharArray();
    private static final char[] NEW_MASTER_PASSWORD = "another-master-2".toCharArray();

    @TempDir
    Path tempDirectory;

    private Path databasePath;
    private DatabaseManager database;
    private VaultService vault;

    @BeforeEach
    void setUp() throws Exception {
        databasePath = tempDirectory.resolve("vault.db");
        database = new DatabaseManager(databasePath);
        database.open();
        vault = new VaultService(database);
    }

    @AfterEach
    void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    @Test
    void supportsFullEntryLifecycleAndEncryptedStorage() throws Exception {
        assertFalse(vault.isInitialized());
        vault.initialize(MASTER_PASSWORD);
        assertTrue(vault.isInitialized());
        assertTrue(vault.isUnlocked());

        PasswordEntry entry = new PasswordEntry();
        entry.setTitle("GitHub");
        entry.setUsername("alice@example.com");
        entry.setPassword("P@ssw0rd-123");
        entry.setUrl("https://github.com");
        entry.setCategory("开发");
        entry.setNotes("恢复码：ABC-123");

        long id = vault.addEntry(entry);
        assertTrue(id > 0);

        assertStoredValueIsEncrypted("P@ssw0rd-123", "password");
        assertStoredValueIsEncrypted("恢复码：ABC-123", "notes");

        List<PasswordEntry> entries = vault.listEntries("", null);
        assertEquals(1, entries.size());
        PasswordEntry loaded = entries.get(0);
        assertEquals(id, loaded.getId());
        assertEquals("GitHub", loaded.getTitle());
        assertEquals("alice@example.com", loaded.getUsername());
        assertEquals("P@ssw0rd-123", loaded.getPassword());
        assertEquals("https://github.com", loaded.getUrl());
        assertEquals("开发", loaded.getCategory());
        assertEquals("恢复码：ABC-123", loaded.getNotes());

        assertEquals(1, vault.listEntries("github", null).size());
        assertEquals(1, vault.listEntries("alice", "开发").size());
        assertTrue(vault.listEntries("not-found", null).isEmpty());
        assertEquals(List.of("开发"), vault.getCategories());

        loaded.setTitle("GitHub Enterprise");
        loaded.setPassword("new-secret-456");
        loaded.setNotes("updated");
        vault.updateEntry(loaded);

        PasswordEntry updated = vault.listEntries("Enterprise", null).get(0);
        assertEquals("new-secret-456", updated.getPassword());
        assertEquals("updated", updated.getNotes());

        vault.deleteEntry(id);
        assertTrue(vault.listEntries("", null).isEmpty());
    }

    @Test
    void persistsDataAndValidatesMasterPassword() throws Exception {
        vault.initialize(MASTER_PASSWORD);
        PasswordEntry entry = new PasswordEntry();
        entry.setTitle("邮箱");
        entry.setUsername("me@example.com");
        entry.setPassword("mail-password");
        long id = vault.addEntry(entry);

        vault.lock();
        assertFalse(vault.isUnlocked());
        assertThrows(VaultException.class,
                () -> vault.unlock("wrong-password".toCharArray()));
        assertFalse(vault.isUnlocked());
        vault.unlock(MASTER_PASSWORD);
        assertEquals("mail-password", vault.listEntries("", null).get(0).getPassword());

        database.close();
        database = new DatabaseManager(databasePath);
        database.open();
        vault = new VaultService(database);

        assertTrue(vault.isInitialized());
        vault.unlock(MASTER_PASSWORD);
        PasswordEntry reloaded = vault.listEntries("", null).get(0);
        assertEquals(id, reloaded.getId());
        assertEquals("邮箱", reloaded.getTitle());
        assertEquals("mail-password", reloaded.getPassword());
    }

    @Test
    void changesMasterPasswordAndReEncryptsExistingEntries() throws Exception {
        vault.initialize(MASTER_PASSWORD);
        PasswordEntry entry = new PasswordEntry();
        entry.setTitle("银行");
        entry.setPassword("bank-password");
        entry.setNotes("账户备注");
        long id = vault.addEntry(entry);

        vault.changeMasterPassword(NEW_MASTER_PASSWORD);
        vault.lock();

        assertThrows(VaultException.class, () -> vault.unlock(MASTER_PASSWORD));
        vault.unlock(NEW_MASTER_PASSWORD);

        PasswordEntry reloaded = vault.listEntries("", null).get(0);
        assertEquals(id, reloaded.getId());
        assertEquals("bank-password", reloaded.getPassword());
        assertEquals("账户备注", reloaded.getNotes());

        database.close();
        database = new DatabaseManager(databasePath);
        database.open();
        vault = new VaultService(database);
        vault.unlock(NEW_MASTER_PASSWORD);
        assertEquals("bank-password", vault.listEntries("", null).get(0).getPassword());
    }

    @Test
    void rejectsOperationsWithoutUnlocking() throws Exception {
        vault.initialize(MASTER_PASSWORD);
        vault.lock();

        assertThrows(VaultException.class, () -> vault.listEntries("", null));
        assertThrows(VaultException.class, () -> {
            PasswordEntry entry = new PasswordEntry();
            entry.setTitle("test");
            vault.addEntry(entry);
        });
    }

    private void assertStoredValueIsEncrypted(String plaintext, String column) throws Exception {
        try (Statement statement = database.connection().createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT " + column + " FROM entries LIMIT 1")) {
            assertTrue(resultSet.next());
            String stored = resultSet.getString(1);
            assertNotEquals(plaintext, stored);
            assertFalse(stored.contains(plaintext));
        }
    }
}

package org.example.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchOptionsTest {
    @TempDir
    Path tempDirectory;

    @Test
    void usesUserHomeByDefault() {
        LaunchOptions options = LaunchOptions.parse(new String[0], tempDirectory);

        Path expected = Paths.get(System.getProperty("user.home"), ".hc_password")
                .toAbsolutePath().normalize();
        assertEquals(expected, options.dataDirectory());
        assertEquals(expected.resolve("vault.db"), options.databasePath());
        assertFalse(options.portable());
    }

    @Test
    void portableUsesDataDirectoryNextToApplication() {
        LaunchOptions options = LaunchOptions.parse(new String[]{"--portable"}, tempDirectory);

        Path expected = tempDirectory.resolve("data").toAbsolutePath().normalize();
        assertEquals(expected, options.dataDirectory());
        assertEquals(expected.resolve("vault.db"), options.databasePath());
        assertTrue(options.portable());
    }

    @Test
    void customDataDirectoryOverridesDefault() {
        Path customDirectory = tempDirectory.resolve("custom");

        LaunchOptions options = LaunchOptions.parse(
                new String[]{"--data-dir", customDirectory.toString()}, tempDirectory);

        assertEquals(customDirectory.toAbsolutePath().normalize(), options.dataDirectory());
        assertFalse(options.portable());
    }

    @Test
    void helpDoesNotRequireDataDirectory() {
        LaunchOptions options = LaunchOptions.parse(new String[]{"--help"}, tempDirectory);

        assertTrue(options.helpRequested());
        assertNull(options.dataDirectory());
    }

    @Test
    void rejectsPortableWithCustomDirectory() {
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse(
                new String[]{"--portable", "--data-dir", "custom"}, tempDirectory));
    }

    @Test
    void rejectsMissingDataDirectoryValue() {
        assertThrows(IllegalArgumentException.class,
                () -> LaunchOptions.parse(new String[]{"--data-dir"}, tempDirectory));
    }

    @Test
    void rejectsUnknownArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> LaunchOptions.parse(new String[]{"--unknown"}, tempDirectory));
    }
}

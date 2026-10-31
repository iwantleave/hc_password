package org.example.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataDirectoryTest {
    @TempDir
    Path tempDirectory;

    @Test
    void createsMissingDirectory() throws Exception {
        Path directory = tempDirectory.resolve("vault-data");

        DataDirectory.prepare(directory);

        assertTrue(Files.isDirectory(directory));
        assertTrue(Files.isWritable(directory));
    }

    @Test
    void rejectsPathThatIsNotDirectory() throws Exception {
        Path file = tempDirectory.resolve("not-a-directory");
        Files.writeString(file, "x");

        assertThrows(IOException.class, () -> DataDirectory.prepare(file));
    }
}

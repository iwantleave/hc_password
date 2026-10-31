package org.example.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DataDirectory {
    private DataDirectory() {
    }

    public static void prepare(Path directory) throws IOException {
        Path normalized = directory.toAbsolutePath().normalize();
        if (Files.exists(normalized) && !Files.isDirectory(normalized)) {
            throw new IOException("数据目录不是文件夹：" + normalized);
        }

        try {
            Files.createDirectories(normalized);
            Path probe = Files.createTempFile(normalized, ".hc-password-write-test-", ".tmp");
            Files.deleteIfExists(probe);
        } catch (IOException e) {
            throw new IOException("数据目录不可写：" + normalized, e);
        }

        Path database = normalized.resolve("vault.db");
        if (Files.exists(database) && !Files.isWritable(database)) {
            throw new IOException("数据库文件不可写：" + database);
        }
    }
}

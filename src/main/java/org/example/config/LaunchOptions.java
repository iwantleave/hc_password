package org.example.config;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;

public final class LaunchOptions {
    private static final String DEFAULT_DIRECTORY = ".hc_password";
    private static final String DATABASE_DIRECTORY = "data";

    private final Path dataDirectory;
    private final boolean portable;
    private final boolean helpRequested;

    private LaunchOptions(Path dataDirectory, boolean portable, boolean helpRequested) {
        this.dataDirectory = dataDirectory;
        this.portable = portable;
        this.helpRequested = helpRequested;
    }

    public static LaunchOptions parse(String[] args) {
        return parse(args, applicationDirectory());
    }

    public static LaunchOptions parse(String[] args, Path applicationDirectory) {
        Path customDirectory = null;
        boolean portable = false;
        boolean help = false;

        for (int index = 0; index < args.length; index++) {
            String argument = args[index];
            switch (argument) {
                case "--portable" -> {
                    if (portable) {
                        throw new IllegalArgumentException("参数 --portable 重复指定");
                    }
                    portable = true;
                }
                case "--data-dir" -> {
                    if (customDirectory != null) {
                        throw new IllegalArgumentException("参数 --data-dir 重复指定");
                    }
                    if (index + 1 >= args.length || args[index + 1].isBlank()) {
                        throw new IllegalArgumentException("参数 --data-dir 缺少目录路径");
                    }
                    customDirectory = Paths.get(args[++index]);
                }
                case "-h", "--help" -> help = true;
                default -> throw new IllegalArgumentException("未知参数：" + argument);
            }
        }

        if (portable && customDirectory != null) {
            throw new IllegalArgumentException("--portable 与 --data-dir 不能同时使用");
        }
        if (help) {
            return new LaunchOptions(null, false, true);
        }

        Path directory;
        if (portable) {
            directory = applicationDirectory.resolve(DATABASE_DIRECTORY);
        } else if (customDirectory != null) {
            directory = customDirectory;
        } else {
            directory = Paths.get(System.getProperty("user.home"), DEFAULT_DIRECTORY);
        }
        return new LaunchOptions(directory.toAbsolutePath().normalize(), portable, false);
    }

    public static Path applicationDirectory() {
        CodeSource codeSource = LaunchOptions.class.getProtectionDomain().getCodeSource();
        if (codeSource != null && codeSource.getLocation() != null) {
            try {
                Path location = Paths.get(codeSource.getLocation().toURI()).toAbsolutePath().normalize();
                if (Files.isRegularFile(location)) {
                    Path parent = location.getParent();
                    if (parent != null) {
                        return parent;
                    }
                }
            } catch (URISyntaxException | IllegalArgumentException ignored) {
            }
        }
        return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    public Path dataDirectory() {
        return dataDirectory;
    }

    public Path databasePath() {
        return dataDirectory == null ? null : dataDirectory.resolve("vault.db");
    }

    public boolean portable() {
        return portable;
    }

    public boolean helpRequested() {
        return helpRequested;
    }

    public static String usage() {
        return """
                用法：java -jar hc-password.jar [选项]

                选项：
                  --portable        将数据库保存到 jar 同级 data 目录
                  --data-dir <目录> 将数据库保存到指定目录
                  -h, --help        显示帮助

                默认数据库目录：%USERPROFILE%\\.hc_password
                """;
    }
}

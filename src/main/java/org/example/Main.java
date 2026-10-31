package org.example;

import org.example.config.DataDirectory;
import org.example.config.LaunchOptions;
import org.example.db.DatabaseManager;
import org.example.service.VaultService;
import org.example.ui.UiTheme;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.nio.file.Path;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> launch(args));
    }

    private static void launch(String[] args) {
        LaunchOptions options;
        try {
            UiTheme.apply();
            options = LaunchOptions.parse(args);
        } catch (Exception e) {
            showError("启动失败", e.getMessage() + "\n\n" + LaunchOptions.usage());
            System.exit(1);
            return;
        }

        if (options.helpRequested()) {
            System.out.print(LaunchOptions.usage());
            JOptionPane.showMessageDialog(null, LaunchOptions.usage(),
                    "HC 密码保险箱", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            DataDirectory.prepare(options.dataDirectory());
            if (options.portable() && !confirmPortableMode(options.dataDirectory())) {
                System.exit(0);
            }

            DatabaseManager database = new DatabaseManager(options.databasePath());
            database.open();

            VaultService vault = new VaultService(database);
            new AppController(database, vault).start();
        } catch (Exception e) {
            e.printStackTrace();
            showError("启动失败", "启动失败：" + e.getMessage());
            System.exit(1);
        }
    }

    private static boolean confirmPortableMode(Path dataDirectory) {
        Object[] choices = {"继续启动", "取消"};
        int choice = JOptionPane.showOptionDialog(null,
                "便携模式已启用。\n\n数据库目录：\n" + dataDirectory
                        + "\n\n请定期备份整个 data 目录。\n如果目录位于只读位置，启动将失败。",
                "HC 密码保险箱", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, choices, choices[0]);
        return choice == 0;
    }

    private static void showError(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }
}

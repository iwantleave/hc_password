package org.example;

import org.example.db.DatabaseManager;
import org.example.service.VaultService;
import org.example.ui.UiTheme;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::launch);
    }

    private static void launch() {
        try {
            UiTheme.apply();

            Path dataDirectory = Paths.get(System.getProperty("user.home"), ".hc_password");
            DatabaseManager database = new DatabaseManager(dataDirectory.resolve("vault.db"));
            database.open();

            VaultService vault = new VaultService(database);
            new AppController(database, vault).start();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "启动失败：" + e.getMessage(),
                    "HC 密码保险箱", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}

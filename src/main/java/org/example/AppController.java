package org.example;

import org.example.db.DatabaseManager;
import org.example.security.VaultException;
import org.example.service.VaultService;
import org.example.ui.LoginDialog;
import org.example.ui.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class AppController {
    private final DatabaseManager database;
    private final VaultService vault;
    private MainFrame frame;
    private boolean exiting;

    public AppController(DatabaseManager database, VaultService vault) {
        this.database = database;
        this.vault = vault;
    }

    public void start() {
        try {
            if (authenticate()) {
                showMainFrame();
            } else {
                exit();
            }
        } catch (VaultException e) {
            showFatalError(e.getMessage());
            exit();
        }
    }

    public void lock() {
        if (exiting) {
            return;
        }
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
        vault.lock();
        try {
            if (authenticate()) {
                showMainFrame();
            } else {
                exit();
            }
        } catch (VaultException e) {
            showFatalError(e.getMessage());
            exit();
        }
    }

    public void exit() {
        if (exiting) {
            return;
        }
        exiting = true;
        vault.lock();
        database.close();
        System.exit(0);
    }

    private boolean authenticate() throws VaultException {
        boolean setupMode = !vault.isInitialized();
        LoginDialog dialog = new LoginDialog(null, vault, setupMode);
        return dialog.showDialog();
    }

    private void showMainFrame() {
        SwingUtilities.invokeLater(() -> {
            frame = new MainFrame(vault, this::lock, this::exit);
            frame.setVisible(true);
        });
    }

    private void showFatalError(String message) {
        JOptionPane.showMessageDialog(null, message == null ? "发生未知错误" : message,
                "HC 密码保险箱", JOptionPane.ERROR_MESSAGE);
    }
}

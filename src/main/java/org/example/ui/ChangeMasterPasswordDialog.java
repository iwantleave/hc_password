package org.example.ui;

import org.example.security.VaultException;
import org.example.service.VaultService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.Arrays;

public final class ChangeMasterPasswordDialog extends JDialog {
    private final VaultService vault;
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JPasswordField confirmField = new JPasswordField(22);
    private final JCheckBox showPasswordCheckBox = new JCheckBox("显示密码");
    private final JLabel messageLabel = new JLabel(" ");
    private boolean changed;

    public ChangeMasterPasswordDialog(Window owner, VaultService vault) {
        super(owner, "修改主密码", ModalityType.APPLICATION_MODAL);
        this.vault = vault;
        buildUi();
    }

    public boolean showDialog() {
        setVisible(true);
        return changed;
    }

    private void buildUi() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(24, 30, 22, 30));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;

        JLabel titleLabel = new JLabel("修改主密码");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        constraints.gridy = 0;
        content.add(titleLabel, constraints);

        JLabel descriptionLabel = new JLabel("所有记录将使用新主密码重新加密。");
        descriptionLabel.setForeground(new Color(95, 99, 104));
        constraints.gridy = 1;
        constraints.insets = new Insets(6, 0, 20, 0);
        content.add(descriptionLabel, constraints);

        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 6, 12);
        constraints.gridy = 2;
        content.add(new JLabel("新主密码"), constraints);

        JPanel passwordPanel = new JPanel(new BorderLayout(8, 0));
        passwordField.setPreferredSize(new Dimension(280, 32));
        passwordPanel.add(passwordField, BorderLayout.CENTER);
        passwordPanel.add(showPasswordCheckBox, BorderLayout.EAST);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 12, 0);
        content.add(passwordPanel, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 6, 12);
        content.add(new JLabel("确认密码"), constraints);

        confirmField.setPreferredSize(new Dimension(280, 32));
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 12, 0);
        content.add(confirmField, constraints);

        JLabel warningLabel = new JLabel("修改后请牢记新主密码，遗忘后无法恢复数据。");
        warningLabel.setForeground(new Color(190, 70, 50));
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(0, 0, 6, 0);
        content.add(warningLabel, constraints);

        messageLabel.setForeground(new Color(190, 50, 50));
        constraints.gridy = 5;
        content.add(messageLabel, constraints);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        JButton cancelButton = new JButton("取消");
        JButton submitButton = new JButton("确认修改");
        submitButton.setPreferredSize(new Dimension(110, 32));
        cancelButton.setPreferredSize(new Dimension(84, 32));
        buttonPanel.add(cancelButton);
        buttonPanel.add(submitButton);
        constraints.gridy = 6;
        constraints.insets = new Insets(0, 0, 0, 0);
        content.add(buttonPanel, constraints);

        setContentPane(content);
        getRootPane().setDefaultButton(submitButton);
        getRootPane().registerKeyboardAction(
                event -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        showPasswordCheckBox.addActionListener(event -> {
            char echoChar = showPasswordCheckBox.isSelected() ? (char) 0 : '\u2022';
            passwordField.setEchoChar(echoChar);
            confirmField.setEchoChar(echoChar);
        });
        submitButton.addActionListener(event -> submit());
        cancelButton.addActionListener(event -> dispose());

        pack();
        setResizable(false);
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        passwordField.requestFocusInWindow();
    }

    private void submit() {
        char[] password = passwordField.getPassword();
        char[] confirm = confirmField.getPassword();
        try {
            if (password.length < 8) {
                showError("主密码至少需要 8 个字符");
                return;
            }
            if (!Arrays.equals(password, confirm)) {
                showError("两次输入的主密码不一致");
                confirmField.setText("");
                confirmField.requestFocusInWindow();
                return;
            }

            vault.changeMasterPassword(password);
            changed = true;
            JOptionPane.showMessageDialog(this, "主密码已修改。", "修改成功",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (VaultException e) {
            showError(e.getMessage());
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirm, '\0');
        }
    }

    private void showError(String message) {
        messageLabel.setText(message == null ? "修改失败" : message);
    }
}

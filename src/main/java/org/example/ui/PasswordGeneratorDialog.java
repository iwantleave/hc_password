package org.example.ui;

import org.example.security.PasswordGenerator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;

public final class PasswordGeneratorDialog extends JDialog {
    private final JSpinner lengthSpinner = new JSpinner(new SpinnerNumberModel(16, 8, 64, 1));
    private final JCheckBox lowerCaseCheckBox = new JCheckBox("小写字母 a-z", true);
    private final JCheckBox upperCaseCheckBox = new JCheckBox("大写字母 A-Z", true);
    private final JCheckBox digitsCheckBox = new JCheckBox("数字 0-9", true);
    private final JCheckBox symbolsCheckBox = new JCheckBox("特殊符号", true);
    private final JTextField passwordField = new JTextField();
    private final JLabel messageLabel = new JLabel(" ");
    private String generatedPassword;

    public PasswordGeneratorDialog(Window owner) {
        super(owner, "生成密码", ModalityType.APPLICATION_MODAL);
        buildUi();
        generate();
    }

    public static String showDialog(Component parent) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        PasswordGeneratorDialog dialog = new PasswordGeneratorDialog(owner);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return dialog.generatedPassword;
    }

    private void buildUi() {
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 18, 24));

        JPanel optionsPanel = new JPanel(new GridLayout(0, 2, 12, 8));
        optionsPanel.add(new JLabel("密码长度"));
        JPanel spinnerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        lengthSpinner.setPreferredSize(new Dimension(80, 30));
        spinnerPanel.add(lengthSpinner);
        optionsPanel.add(spinnerPanel);
        optionsPanel.add(lowerCaseCheckBox);
        optionsPanel.add(upperCaseCheckBox);
        optionsPanel.add(digitsCheckBox);
        optionsPanel.add(symbolsCheckBox);

        JPanel topPanel = new JPanel(new BorderLayout(0, 12));
        JLabel titleLabel = new JLabel("随机密码生成器");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(optionsPanel, BorderLayout.CENTER);

        passwordField.setEditable(false);
        passwordField.setFont(new Font("Consolas", Font.BOLD, 17));
        passwordField.setHorizontalAlignment(JTextField.CENTER);
        passwordField.setPreferredSize(new Dimension(360, 40));

        JPanel centerPanel = new JPanel(new BorderLayout(0, 6));
        centerPanel.add(passwordField, BorderLayout.CENTER);
        messageLabel.setForeground(new Color(190, 50, 50));
        centerPanel.add(messageLabel, BorderLayout.SOUTH);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton regenerateButton = new JButton("重新生成");
        JButton copyButton = new JButton("复制");
        JButton cancelButton = new JButton("取消");
        JButton useButton = new JButton("使用");
        useButton.setPreferredSize(new Dimension(84, 32));
        buttonPanel.add(regenerateButton);
        buttonPanel.add(copyButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(useButton);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 14));
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        content.add(mainPanel, BorderLayout.CENTER);
        setContentPane(content);

        getRootPane().setDefaultButton(useButton);
        getRootPane().registerKeyboardAction(
                event -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        regenerateButton.addActionListener(event -> generate());
        copyButton.addActionListener(event -> copyPassword());
        cancelButton.addActionListener(event -> {
            generatedPassword = null;
            dispose();
        });
        useButton.addActionListener(event -> {
            if (generatedPassword != null) {
                dispose();
            }
        });

        lowerCaseCheckBox.addActionListener(event -> generate());
        upperCaseCheckBox.addActionListener(event -> generate());
        digitsCheckBox.addActionListener(event -> generate());
        symbolsCheckBox.addActionListener(event -> generate());
        lengthSpinner.addChangeListener(event -> generate());

        pack();
        setMinimumSize(new Dimension(460, getHeight()));
        setResizable(false);
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void generate() {
        try {
            generatedPassword = PasswordGenerator.generate(
                    (Integer) lengthSpinner.getValue(),
                    lowerCaseCheckBox.isSelected(),
                    upperCaseCheckBox.isSelected(),
                    digitsCheckBox.isSelected(),
                    symbolsCheckBox.isSelected());
            passwordField.setText(generatedPassword);
            passwordField.setCaretPosition(0);
            messageLabel.setForeground(new Color(190, 50, 50));
            messageLabel.setText(" ");
        } catch (IllegalArgumentException e) {
            generatedPassword = null;
            passwordField.setText("");
            messageLabel.setForeground(new Color(190, 50, 50));
            messageLabel.setText(e.getMessage());
        }
    }

    private void copyPassword() {
        if (generatedPassword != null) {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(generatedPassword), null);
            messageLabel.setForeground(new Color(40, 130, 70));
            messageLabel.setText("已复制到剪贴板");
        }
    }
}

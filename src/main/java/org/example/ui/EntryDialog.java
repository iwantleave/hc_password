package org.example.ui;

import org.example.model.PasswordEntry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

public final class EntryDialog extends JDialog {
    private final PasswordEntry source;
    private final JTextField titleField = new JTextField();
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JTextField urlField = new JTextField();
    private final JComboBox<String> categoryComboBox = new JComboBox<>();
    private final JTextArea notesArea = new JTextArea(5, 30);
    private final JCheckBox showPasswordCheckBox = new JCheckBox("显示");
    private PasswordEntry result;

    public EntryDialog(Window owner, PasswordEntry source, List<String> categories) {
        super(owner, source == null ? "新增密码" : "编辑密码", ModalityType.APPLICATION_MODAL);
        this.source = source;
        buildUi(categories);
        fillFields();
    }

    public PasswordEntry showDialog() {
        setVisible(true);
        return result;
    }

    private void buildUi(List<String> categories) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 12, 12);
        constraints.anchor = GridBagConstraints.NORTHWEST;

        addFormRow(form, constraints, 0, "标题 *", titleField);

        addFormRow(form, constraints, 1, "用户名", usernameField);

        JPanel passwordPanel = new JPanel(new BorderLayout(8, 0));
        passwordField.setPreferredSize(new Dimension(240, 32));
        passwordPanel.add(passwordField, BorderLayout.CENTER);
        JPanel passwordActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton generateButton = new JButton("生成");
        passwordActions.add(showPasswordCheckBox);
        passwordActions.add(generateButton);
        passwordPanel.add(passwordActions, BorderLayout.EAST);
        addFormRow(form, constraints, 2, "密码", passwordPanel);

        addFormRow(form, constraints, 3, "网址", urlField);

        for (String category : new ArrayList<>(new LinkedHashSet<>(categories))) {
            categoryComboBox.addItem(category);
        }
        if (((javax.swing.DefaultComboBoxModel<String>) categoryComboBox.getModel())
                .getIndexOf("默认") < 0) {
            categoryComboBox.addItem("默认");
        }
        categoryComboBox.setEditable(true);
        categoryComboBox.setPreferredSize(new Dimension(360, 32));
        addFormRow(form, constraints, 4, "分类", categoryComboBox);

        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane notesScrollPane = new JScrollPane(notesArea);
        notesScrollPane.setPreferredSize(new Dimension(360, 110));
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 12);
        form.add(new JLabel("备注"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, 0);
        form.add(notesScrollPane, constraints);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 24, 18, 24));
        JButton cancelButton = new JButton("取消");
        JButton saveButton = new JButton("保存");
        saveButton.setPreferredSize(new Dimension(96, 32));
        cancelButton.setPreferredSize(new Dimension(84, 32));
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        JPanel content = new JPanel(new BorderLayout());
        content.add(form, BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);
        setContentPane(content);

        getRootPane().setDefaultButton(saveButton);
        getRootPane().registerKeyboardAction(
                event -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        showPasswordCheckBox.addActionListener(event -> passwordField.setEchoChar(
                showPasswordCheckBox.isSelected() ? (char) 0 : '\u2022'));
        generateButton.addActionListener(event -> {
            String generated = PasswordGeneratorDialog.showDialog(this);
            if (generated != null) {
                passwordField.setText(generated);
                passwordField.requestFocusInWindow();
            }
        });
        saveButton.addActionListener(event -> save());
        cancelButton.addActionListener(event -> dispose());

        setSize(560, 590);
        setMinimumSize(new Dimension(520, 520));
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        titleField.requestFocusInWindow();
    }

    private void addFormRow(JPanel form, GridBagConstraints constraints, int row,
                            String label, JComponent component) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0.0;
        constraints.weighty = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 12, 12);
        form.add(new JLabel(label), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 12, 0);
        form.add(component, constraints);

        if (component instanceof JTextField textField) {
            textField.setPreferredSize(new Dimension(360, 32));
        }
    }

    private void fillFields() {
        if (source == null) {
            categoryComboBox.setSelectedItem("默认");
            return;
        }
        titleField.setText(source.getTitle());
        usernameField.setText(source.getUsername());
        passwordField.setText(source.getPassword());
        urlField.setText(source.getUrl());
        categoryComboBox.setSelectedItem(source.getCategory());
        notesArea.setText(source.getNotes());
        notesArea.setCaretPosition(0);
    }

    private void save() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "标题不能为空。", "提示",
                    JOptionPane.WARNING_MESSAGE);
            titleField.requestFocusInWindow();
            return;
        }

        PasswordEntry entry = source == null ? new PasswordEntry() : source.copy();
        entry.setTitle(title);
        entry.setUsername(usernameField.getText().trim());
        entry.setUrl(urlField.getText().trim());

        char[] password = passwordField.getPassword();
        entry.setPassword(new String(password));
        Arrays.fill(password, '\0');

        Object categoryValue = categoryComboBox.getEditor().getItem();
        String category = categoryValue == null ? "" : categoryValue.toString().trim();
        entry.setCategory(category.isEmpty() ? "默认" : category);
        entry.setNotes(notesArea.getText());

        result = entry;
        dispose();
    }
}

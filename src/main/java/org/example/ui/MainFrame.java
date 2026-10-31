package org.example.ui;

import org.example.model.PasswordEntry;
import org.example.security.VaultException;
import org.example.service.VaultService;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class MainFrame extends JFrame {
    private static final Color MUTED_COLOR = new Color(100, 105, 110);
    private static final Color MESSAGE_COLOR = new Color(40, 130, 70);
    private static final Color ERROR_COLOR = new Color(190, 50, 50);

    private final VaultService vault;
    private final Runnable onLock;
    private final Runnable onExit;

    private final EntryTableModel tableModel = new EntryTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField();
    private final JComboBox<String> categoryFilter = new JComboBox<>();
    private final JLabel countLabel = new JLabel();
    private final JLabel messageLabel = new JLabel(" ");
    private final Timer searchTimer = new Timer(220, event -> reloadEntries());

    private Timer clipboardTimer;
    private String clipboardValue;
    private boolean updatingCategories;

    public MainFrame(VaultService vault, Runnable onLock, Runnable onExit) {
        this.vault = vault;
        this.onLock = onLock;
        this.onExit = onExit;

        searchTimer.setRepeats(false);
        configureWindow();
        setJMenuBar(createMenuBar());
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createStatusPanel(), BorderLayout.SOUTH);
        installShortcuts();

        refreshCategories();
        reloadEntries();
    }

    private void configureWindow() {
        setTitle("HC 密码保险箱");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(860, 560));
        setSize(1060, 680);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                requestExit();
            }
        });
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("文件");
        JMenuItem changePasswordItem = new JMenuItem("修改主密码...");
        changePasswordItem.addActionListener(event -> changeMasterPassword());
        fileMenu.add(changePasswordItem);
        fileMenu.addSeparator();

        JMenuItem lockItem = new JMenuItem("锁定保险箱");
        lockItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
        lockItem.addActionListener(event -> lockVault());
        fileMenu.add(lockItem);

        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(event -> requestExit());
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        JMenu helpMenu = new JMenu("帮助");
        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.addActionListener(event -> JOptionPane.showMessageDialog(this,
                "HC 密码保险箱\n\n本地 SQLite 存储，密码使用 AES-256-GCM 加密。\n"
                        + "主密码通过 PBKDF2 派生，不会保存在磁盘上。",
                "关于", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBorder(BorderFactory.createEmptyBorder(16, 18, 10, 18));

        JPanel titlePanel = new JPanel(new BorderLayout(0, 2));
        JLabel titleLabel = new JLabel("密码保险箱");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 24f));
        JLabel subtitleLabel = new JLabel("本地加密存储，安全保存账号与密码");
        subtitleLabel.setForeground(MUTED_COLOR);
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH);
        header.add(titlePanel, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(createActionButton("新增", "添加密码记录 (Ctrl+N)", event -> addEntry()));
        actions.add(createActionButton("编辑", "编辑选中的记录 (Ctrl+E)", event -> editEntry()));
        actions.add(createActionButton("删除", "删除选中的记录 (Delete)", event -> deleteEntry()));
        actions.add(createActionButton("复制密码", "复制选中记录的密码 (Ctrl+C)",
                event -> copySelectedPassword()));
        actions.add(createActionButton("锁定", "锁定保险箱 (Ctrl+L)", event -> lockVault()));
        header.add(actions, BorderLayout.EAST);

        JPanel filterBar = new JPanel(new BorderLayout(12, 0));
        filterBar.setBorder(BorderFactory.createEmptyBorder(0, 18, 8, 18));

        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.add(new JLabel("搜索"), BorderLayout.WEST);
        searchField.setToolTipText("搜索标题、用户名、网址或分类");
        searchPanel.add(searchField, BorderLayout.CENTER);
        filterBar.add(searchPanel, BorderLayout.CENTER);

        JPanel categoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        categoryPanel.add(new JLabel("分类"));
        categoryFilter.setPreferredSize(new Dimension(140, 30));
        categoryPanel.add(categoryFilter);
        JButton clearFilterButton = new JButton("清除筛选");
        clearFilterButton.setFocusable(false);
        clearFilterButton.addActionListener(event -> {
            searchField.setText("");
            if (categoryFilter.getItemCount() > 0) {
                categoryFilter.setSelectedIndex(0);
            }
        });
        categoryPanel.add(clearFilterButton);
        filterBar.add(categoryPanel, BorderLayout.EAST);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                searchTimer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                searchTimer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                searchTimer.restart();
            }
        });
        categoryFilter.addActionListener(event -> {
            if (!updatingCategories) {
                reloadEntries();
            }
        });

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(header, BorderLayout.NORTH);
        northPanel.add(filterBar, BorderLayout.SOUTH);
        return northPanel;
    }

    private JButton createActionButton(String text, String tooltip, ActionListener listener) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(92, 34));
        button.addActionListener(listener);
        return button;
    }

    private JPanel createTablePanel() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.setAutoCreateRowSorter(true);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(232, 234, 237));
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));

        int[] widths = {260, 180, 280, 110, 145};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateCountLabel();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                    int row = table.rowAtPoint(event.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        editEntry();
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent event) {
                showContextMenu(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                showContextMenu(event);
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0,
                new Color(225, 227, 230)));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void showContextMenu(MouseEvent event) {
        if (!event.isPopupTrigger()) {
            return;
        }
        int row = table.rowAtPoint(event.getPoint());
        if (row < 0) {
            return;
        }
        table.setRowSelectionInterval(row, row);

        JPopupMenu menu = new JPopupMenu();
        JMenuItem copyPasswordItem = new JMenuItem("复制密码");
        copyPasswordItem.addActionListener(action -> copySelectedPassword());
        JMenuItem copyUsernameItem = new JMenuItem("复制用户名");
        copyUsernameItem.addActionListener(action -> copySelectedUsername());
        JMenuItem openUrlItem = new JMenuItem("打开网址");
        openUrlItem.addActionListener(action -> openSelectedUrl());
        JMenuItem editItem = new JMenuItem("编辑");
        editItem.addActionListener(action -> editEntry());
        JMenuItem deleteItem = new JMenuItem("删除");
        deleteItem.addActionListener(action -> deleteEntry());

        menu.add(copyPasswordItem);
        menu.add(copyUsernameItem);
        menu.add(openUrlItem);
        menu.addSeparator();
        menu.add(editItem);
        menu.add(deleteItem);
        menu.show(table, event.getX(), event.getY());
    }

    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout(12, 0));
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(225, 227, 230)),
                BorderFactory.createEmptyBorder(7, 18, 9, 18)));

        countLabel.setForeground(MUTED_COLOR);
        messageLabel.setForeground(MESSAGE_COLOR);
        JLabel hintLabel = new JLabel("双击编辑 · Delete 删除 · Ctrl+F 搜索");
        hintLabel.setForeground(MUTED_COLOR);

        statusPanel.add(countLabel, BorderLayout.WEST);
        statusPanel.add(messageLabel, BorderLayout.CENTER);
        statusPanel.add(hintLabel, BorderLayout.EAST);
        return statusPanel;
    }

    private void installShortcuts() {
        JComponent root = getRootPane();
        int condition = JComponent.WHEN_IN_FOCUSED_WINDOW;
        root.getInputMap(condition).put(KeyStroke.getKeyStroke(KeyEvent.VK_N,
                InputEvent.CTRL_DOWN_MASK), "newEntry");
        root.getInputMap(condition).put(KeyStroke.getKeyStroke(KeyEvent.VK_E,
                InputEvent.CTRL_DOWN_MASK), "editEntry");
        root.getInputMap(condition).put(KeyStroke.getKeyStroke(KeyEvent.VK_F,
                InputEvent.CTRL_DOWN_MASK), "focusSearch");

        root.getActionMap().put("newEntry", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                addEntry();
            }
        });
        root.getActionMap().put("editEntry", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                editEntry();
            }
        });
        root.getActionMap().put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                searchField.requestFocusInWindow();
                searchField.selectAll();
            }
        });

        table.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteEntry");
        table.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "editEntry");
        table.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "copyPassword");
        table.getActionMap().put("deleteEntry", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                deleteEntry();
            }
        });
        table.getActionMap().put("editEntry", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                editEntry();
            }
        });
        table.getActionMap().put("copyPassword", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                copySelectedPassword();
            }
        });
    }

    private void refreshCategories() {
        List<String> categories = currentCategories();
        Object previousValue = categoryFilter.getSelectedItem();

        LinkedHashSet<String> allCategories = new LinkedHashSet<>();
        allCategories.add("默认");
        allCategories.addAll(categories);

        List<String> items = new ArrayList<>();
        items.add("全部分类");
        items.addAll(allCategories);

        updatingCategories = true;
        try {
            categoryFilter.setModel(new DefaultComboBoxModel<>(items.toArray(String[]::new)));
            if (previousValue != null && items.contains(previousValue.toString())) {
                categoryFilter.setSelectedItem(previousValue);
            } else {
                categoryFilter.setSelectedIndex(0);
            }
        } finally {
            updatingCategories = false;
        }
    }

    private List<String> currentCategories() {
        try {
            return vault.getCategories();
        } catch (VaultException e) {
            showError(e.getMessage());
            return List.of("默认");
        }
    }

    private void reloadEntries() {
        if (!vault.isUnlocked()) {
            return;
        }
        String category = categoryFilter.getSelectedIndex() > 0
                ? (String) categoryFilter.getSelectedItem() : null;
        try {
            List<PasswordEntry> entries = vault.listEntries(searchField.getText(), category);
            tableModel.setEntries(entries);
            table.clearSelection();
            updateCountLabel();
        } catch (VaultException e) {
            showError(e.getMessage());
        }
    }

    private void updateCountLabel() {
        int total = tableModel.getRowCount();
        PasswordEntry selected = selectedEntry();
        if (selected == null) {
            countLabel.setText("共 " + total + " 条记录");
        } else {
            countLabel.setText("共 " + total + " 条记录 · 已选择：" + selected.getTitle());
        }
    }

    private PasswordEntry selectedEntry() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return tableModel.getEntryAt(modelRow);
    }

    private void addEntry() {
        EntryDialog dialog = new EntryDialog(this, null, currentCategories());
        PasswordEntry entry = dialog.showDialog();
        if (entry == null) {
            return;
        }
        try {
            vault.addEntry(entry);
            refreshCategories();
            reloadEntries();
            showMessage("已保存“" + entry.getTitle() + "”");
        } catch (VaultException e) {
            showError(e.getMessage());
        }
    }

    private void editEntry() {
        PasswordEntry selected = selectedEntry();
        if (selected == null) {
            showMessage("请先选择一条记录");
            return;
        }
        EntryDialog dialog = new EntryDialog(this, selected.copy(), currentCategories());
        PasswordEntry edited = dialog.showDialog();
        if (edited == null) {
            return;
        }
        try {
            vault.updateEntry(edited);
            refreshCategories();
            reloadEntries();
            showMessage("已更新“" + edited.getTitle() + "”");
        } catch (VaultException e) {
            showError(e.getMessage());
        }
    }

    private void deleteEntry() {
        PasswordEntry selected = selectedEntry();
        if (selected == null) {
            showMessage("请先选择一条记录");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "确定删除“" + selected.getTitle() + "”吗？此操作无法撤销。",
                "删除密码记录", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            vault.deleteEntry(selected.getId());
            refreshCategories();
            reloadEntries();
            showMessage("已删除“" + selected.getTitle() + "”");
        } catch (VaultException e) {
            showError(e.getMessage());
        }
    }

    private void copySelectedPassword() {
        PasswordEntry selected = selectedEntry();
        if (selected == null) {
            showMessage("请先选择一条记录");
            return;
        }
        copyText(selected.getPassword(), "密码", true);
    }

    private void copySelectedUsername() {
        PasswordEntry selected = selectedEntry();
        if (selected == null) {
            showMessage("请先选择一条记录");
            return;
        }
        copyText(selected.getUsername(), "用户名", false);
    }

    private void openSelectedUrl() {
        PasswordEntry selected = selectedEntry();
        if (selected == null) {
            showMessage("请先选择一条记录");
            return;
        }
        String rawUrl = selected.getUrl() == null ? "" : selected.getUrl().trim();
        if (rawUrl.isEmpty()) {
            showMessage("该记录没有填写网址");
            return;
        }
        try {
            String normalized = rawUrl.matches("(?i)^[a-z][a-z0-9+.-]*://.*")
                    ? rawUrl : "https://" + rawUrl;
            if (!java.awt.Desktop.isDesktopSupported()
                    || !java.awt.Desktop.getDesktop().isSupported(
                    java.awt.Desktop.Action.BROWSE)) {
                showError("当前系统不支持打开浏览器");
                return;
            }
            java.awt.Desktop.getDesktop().browse(new URI(normalized));
        } catch (Exception e) {
            showError("无法打开网址：" + e.getMessage());
        }
    }

    private void copyText(String value, String label, boolean autoClear) {
        if (value == null || value.isEmpty()) {
            showMessage(label + "为空");
            return;
        }
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(value), null);
        clipboardValue = value;

        if (clipboardTimer != null) {
            clipboardTimer.stop();
            clipboardTimer = null;
        }
        if (autoClear) {
            clipboardTimer = new Timer(30_000, event -> {
                clearClipboardIfNeeded();
                showMessage("剪贴板中的密码已自动清除");
            });
            clipboardTimer.setRepeats(false);
            clipboardTimer.start();
            showMessage(label + "已复制，30 秒后自动清除");
        } else {
            showMessage(label + "已复制");
        }
    }

    private void clearClipboardIfNeeded() {
        if (clipboardValue == null) {
            return;
        }
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(null);
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String current = (String) contents.getTransferData(DataFlavor.stringFlavor);
                if (clipboardValue.equals(current)) {
                    clipboard.setContents(new StringSelection(""), null);
                }
            }
        } catch (Exception ignored) {
            // 剪贴板被其他程序占用时忽略
        } finally {
            clipboardValue = null;
        }
    }

    private void changeMasterPassword() {
        ChangeMasterPasswordDialog dialog = new ChangeMasterPasswordDialog(this, vault);
        if (dialog.showDialog()) {
            showMessage("主密码已修改");
        }
    }

    private void lockVault() {
        if (clipboardTimer != null) {
            clipboardTimer.stop();
        }
        clearClipboardIfNeeded();
        searchTimer.stop();
        tableModel.setEntries(List.of());
        dispose();
        onLock.run();
    }

    private void requestExit() {
        int choice = JOptionPane.showConfirmDialog(this, "确定要退出密码保险箱吗？",
                "退出", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        if (clipboardTimer != null) {
            clipboardTimer.stop();
        }
        clearClipboardIfNeeded();
        searchTimer.stop();
        dispose();
        onExit.run();
    }

    private void showMessage(String message) {
        messageLabel.setForeground(MESSAGE_COLOR);
        messageLabel.setText(message == null ? " " : message);
    }

    private void showError(String message) {
        messageLabel.setForeground(ERROR_COLOR);
        messageLabel.setText(message == null ? "操作失败" : message);
        JOptionPane.showMessageDialog(this, message == null ? "操作失败" : message,
                "错误", JOptionPane.ERROR_MESSAGE);
    }
}

package org.example.ui;

import org.example.model.PasswordEntry;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public final class EntryTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"标题", "用户名", "网址", "分类", "更新时间"};
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private List<PasswordEntry> entries = new ArrayList<>();

    public void setEntries(List<PasswordEntry> newEntries) {
        entries = new ArrayList<>(newEntries);
        fireTableDataChanged();
    }

    public PasswordEntry getEntryAt(int modelRow) {
        return entries.get(modelRow);
    }

    @Override
    public int getRowCount() {
        return entries.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        PasswordEntry entry = entries.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> entry.getTitle();
            case 1 -> entry.getUsername();
            case 2 -> entry.getUrl();
            case 3 -> entry.getCategory();
            case 4 -> formatTimestamp(entry.getUpdatedAt());
            default -> "";
        };
    }

    private static String formatTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .format(DISPLAY_FORMAT);
        } catch (DateTimeParseException e) {
            return value;
        }
    }
}

package org.example.ui;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public final class UiTheme {
    private UiTheme() {
    }

    public static void apply() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // 使用系统默认外观
        }

        FontUIResource font = new FontUIResource("Microsoft YaHei UI", Font.PLAIN, 13);
        List<Object> fontKeys = new ArrayList<>();
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key.toString().endsWith(".font")
                    && UIManager.get(key) instanceof FontUIResource) {
                fontKeys.add(key);
            }
        }
        for (Object key : fontKeys) {
            UIManager.put(key, font);
        }
        UIManager.put("Table.rowHeight", 28);
        UIManager.put("Table.showGrid", false);
        UIManager.put("ToolTip.font", new FontUIResource("Microsoft YaHei UI", Font.PLAIN, 12));
    }
}

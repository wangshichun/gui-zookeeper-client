package com.github.wangshichun.tools.zookeeper;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/**
 * Created by wangshichun on 2016/5/26.
 */
public class PanelOfHistory extends JDialog {
    java.util.List<String> list;

    PanelOfHistory(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
        setResizable(true);
//        setAlwaysOnTop(true);
        initPanel();

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                PanelOfHistory.this.setVisible(false);
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == 27) { // ESC key
                    PanelOfHistory.this.setVisible(false);
                }
            }
        });
    }

    private void initPanel() {
        list = readHistory();
        getContentPane().removeAll();
        setLayout(new FlowLayout(FlowLayout.LEFT));
        MouseAdapter cursorMouseAdapter = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            public void mouseExited(MouseEvent e) {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        };
        for (final String line : list) {
            final JLabel historyLabel = new JLabel(line);
            add(historyLabel);
            historyLabel.addMouseListener(cursorMouseAdapter);
            historyLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    for (StringSelectListener listener : stringSelectListenerList) {
                        if (!listener.select(historyLabel.getText()))
                            break;
                    }
                }
            });

            JLabel label = new JLabel("[");
            label.setForeground(Color.BLUE);
            add(label);

            label = new JLabel("  删  ");
            label.setForeground(Color.RED);
            label.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (list != null) {
                        list.remove(historyLabel.getText());
                        writeHistory();
                        initPanel();
                    }
                }
            });
            label.addMouseListener(cursorMouseAdapter);

            add(label);

            label = new JLabel("]");
            label.setForeground(Color.BLUE);
            add(label);


            newLine();
        }
    }
    void addNewHistoryRecord(String his) {
        if (list != null && !list.contains(his)) {
            list.add(his);
            writeHistory();
            initPanel();
        }
    }
    private void newLine() {
        JLabel label = new JLabel();
        label.setPreferredSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width, 0));
        getContentPane().add(label);
    }

    private java.util.List<String> readHistory() {
        java.util.List<String> list = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(getHistoryFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(line);
            }
            Collections.sort(list);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void writeHistory() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getHistoryFile()));) {
            for (String line : list) {
                writer.write(line);
                writer.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getHistoryFile() {
        String dir = System.getenv("APPDATA");
        if (dir == null || dir.isEmpty())
            dir = System.getProperty("user.home");
        if (dir == null || dir.isEmpty())
            dir = System.getProperty("user.dir");
        try {
            File directory = new File(dir, ".gui_zookeeper_client");
            if (!directory.exists())
                directory.mkdir();
            File file = new File(directory, "history.txt");
            if (!file.exists())
                file.createNewFile();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    void show(MouseEvent e) {
        setLocation(e.getXOnScreen(), e.getYOnScreen());
        setVisible(true);
    }

    private java.util.List<StringSelectListener> stringSelectListenerList = new LinkedList<>();
    void addStringSelectListener(StringSelectListener listener) {
        stringSelectListenerList.add(listener);
    }
    void removeStringSelectListener(StringSelectListener listener) {
        stringSelectListenerList.remove(listener);
    }

    interface StringSelectListener {
        boolean select(String text);
    }
}

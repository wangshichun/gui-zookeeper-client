package com.github.wangshichun.tools.zookeeper;

import com.google.common.base.Splitter;
import org.apache.zookeeper.data.Stat;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTML;
import java.awt.*;
import java.awt.event.*;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

/**
 * Created by wangshichun on 2016/5/15.
 */
public class Main extends JFrame {
    private ZKUtil zkUtil = new ZKUtil();
    private boolean isConnected = false;
    private String path = "/"; // 查看zk的哪个路径的数据
    private String pathFilter = ""; // 过滤子节点的表达式
    private java.util.List<String> children; // 当前路径下的子路径
    private Runnable setStatForPathRunnable; // 查看按钮调用
    private Runnable setPathRunnable; // 点击超链接的时候调用
    private Runnable setChildrenForPathRunnable; // 过滤子节点的时候调用

    private boolean isTreeView = true;
    private Runnable setPanelRunnable; // 勾选“是否树结构展示”的时候调用

    public static void main(String[] args) {
        final Main view = new Main();
        view.setVisible(true);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                view.zkUtil.shutdown();
            }
        }));
    }

    public Main() {
        setTitle("zookeeper工具");
        setSize(850, 750);
        setExtendedState(MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        JPanel headPanel = new JPanel();
        getContentPane().add(headPanel);
        setConstraints(2, 1, true, 1.0, null, layout, headPanel);
        initHeadPanel(headPanel);

        final JPanel subHeadPanel1 = new JPanel();
        getContentPane().add(subHeadPanel1);
        setConstraints(2, 1, true, 1.0, null, layout, subHeadPanel1);
        initSubHeadPanel1(subHeadPanel1);

        final JPanel subHeadPanel = new JPanel();
        getContentPane().add(subHeadPanel);
        setConstraints(1, 1, true, 1.0, null, layout, subHeadPanel);
        initSubHeadPanel2(subHeadPanel);

        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        editorPane.setBackground(Color.LIGHT_GRAY);
//        editorPane.setPreferredSize(new Dimension(500, 400));
//        editorPane.setText("<h1>aa</h1><h2>bb</h2><a href=\"111\" vv=\"x\">ddddd</a>/<a href=\"111\" vv=\"x\">这种</a>");
        final JScrollPane scrollPane = new JScrollPane(editorPane);
        getContentPane().add(scrollPane);
        setConstraints(1, 1, false, 0.7, 1.0, layout, scrollPane);
        initEditorPanel(editorPane);

        final JPanel sidePanel = new JPanel();
        getContentPane().add(sidePanel);
        sidePanel.setBorder(new LineBorder(Color.black, 1, true));
        setConstraints(1, 1, true, 0.3, 1.0, layout, sidePanel);
        initSidePanel(sidePanel);

        final PanelOfTree panelOfTree = new PanelOfTree(zkUtil);
        getContentPane().add(panelOfTree);
        setConstraints(2, 1, true, 1.0, 1.0, layout, panelOfTree);

        setPanelRunnable = new Runnable() {
            @Override
            public void run() {
                panelOfTree.setVisible(false);
                sidePanel.setVisible(false);
                scrollPane.setVisible(false);
                subHeadPanel.setVisible(false);
                subHeadPanel1.setVisible(false);
                if (isTreeView) {
                    panelOfTree.setVisible(true);
                    if (isConnected)
                        panelOfTree.whenOpenZookeeper();
                    else
                        panelOfTree.whenCloseZookeeper();
                } else {
                    sidePanel.setVisible(true);
                    scrollPane.setVisible(true);
                    subHeadPanel.setVisible(true);
                    subHeadPanel1.setVisible(true);
                }
            }
        };
        setPanelRunnable.run();
    }

    private void initSubHeadPanel2(JPanel subHeadPanel) {
        subHeadPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel("过滤子节点列表：");
        subHeadPanel.add(label);
        final JTextField filterTextField = new JTextField();
        filterTextField.setColumns(20);
        subHeadPanel.add(filterTextField);
        filterTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                pathFilter = filterTextField.getText().trim();
                setChildrenForPathRunnable.run();
            }
        });
        filterTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                pathFilter = filterTextField.getText().trim();
                setChildrenForPathRunnable.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                pathFilter = filterTextField.getText().trim();
                setChildrenForPathRunnable.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                pathFilter = filterTextField.getText().trim();
                setChildrenForPathRunnable.run();
            }
        });
    }

    private void initEditorPanel(final JEditorPane editorPane) {
        editorPane.setAutoscrolls(true);
        editorPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getSourceElement() instanceof AbstractDocument.AbstractElement == false)
                    return;
                if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED)
                    return;

                AttributeSet attributeSet = (AttributeSet) ((AbstractDocument.AbstractElement) e.getSourceElement()).getAttribute(HTML.Tag.A);
                Enumeration names = attributeSet.getAttributeNames();
                while (names.hasMoreElements()) {
                    Object name = names.nextElement();
                    Object value = attributeSet.getAttribute(name);
                    System.out.print(name + ": ");
                    System.out.println(value);
                    if ("path".equals(name) && value instanceof String) {
                        if (value.toString().startsWith("/")) {
                            path = value.toString().trim();
                            setPathRunnable.run();
                        }
                    }
                }
                System.out.println(e);
            }
        });

        setChildrenForPathRunnable = new Runnable() {
            @Override
            public void run() {
                if (children == null || children.isEmpty()) {
                    editorPane.setText("无子节点");
                    return;
                }
                editorPane.setContentType("text/html");
                StringBuilder builder = new StringBuilder();
                builder.append("<html><body>");
                boolean isDubbo = path.contains("/dubbo") && (path.endsWith("/providers") || path.endsWith("/consumers"));

                try {
                    for (String ch : children) {
                        if (ch.contains(pathFilter)) {
                            String value = ch;
                            if (isDubbo) {
                                value = URLDecoder.decode(ch, StandardCharsets.UTF_8.name());
                                value = join(Splitter.fixedLength(100).splitToList(value), "<br/>");
                            }
                            builder.append("\n<a path=\"").append(path).append(path.endsWith("/") ? "" : "/").append(ch).append("\"").append(" href=\"\">").append(value).append("</a><br/>").append(isDubbo ? "<br/>" : "");
                        }
                    }
                    builder.append("</body></html>");
                    System.out.println(builder.toString());
                    editorPane.setText(builder.toString());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private static String join(java.util.List list, String separator) {
        if (list == null || list.isEmpty())
            return "";
        StringBuilder builder = new StringBuilder();
        separator = null == separator || separator.isEmpty() ? "," : separator;
        for (Object obj : list) {
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(obj);
        }
        return builder.toString();
    }

    private void initSidePanel(final JPanel panel) {
        GridBagLayout gridBagLayout = new GridBagLayout();
        panel.setLayout(gridBagLayout);
        JLabel label = new JLabel("路径：");
        final JTextField textField = new JTextField();
        textField.setEditable(false);
        panel.add(label);
        panel.add(textField);
        setConstraints(2, 1, true, null, null, gridBagLayout, label);
        setConstraints(2, 1, true, null, null, gridBagLayout, textField);

        final JTextArea textForStat = new JTextArea();
        textForStat.setLineWrap(false);
        textForStat.setColumns(40);
        textForStat.setEditable(false);
        textForStat.setBackground(Color.lightGray);

        JPanel tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(tempPanel);
        tempPanel.add(textForStat);
        setConstraints(2, 1, true, 1.0, null, gridBagLayout, tempPanel);

        label = new JLabel("数据：");
        panel.add(label);
        setConstraints(2, 1, true, null, null, gridBagLayout, label);

        final JTextArea textForData = new JTextArea();
        textForData.setLineWrap(true);
        textForData.setColumns(40);
        textForData.setRows(10);
        JScrollPane jScrollPane = new JScrollPane(textForData);
        panel.add(jScrollPane);
        jScrollPane.setPreferredSize(new Dimension(450, 200));
        jScrollPane.setMaximumSize(new Dimension(450, 200));
        setConstraints(2, 1, true, 0.0, null, gridBagLayout, jScrollPane);

        // 新建按钮
        final JButton createButton = new JButton("新建(不带数据)");
        final JButton createWithDataButton = new JButton("新建(带数据)");
        panel.add(createButton);
        panel.add(createWithDataButton);
        setConstraints(1, 1, false, null, null, gridBagLayout, createButton);
        setConstraints(1, 1, true, null, null, gridBagLayout, createWithDataButton);
        createButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isZKConnected(isConnected))
                    return;
                if (!checkRootPath(path))
                    return;
                if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(null, String.format("确定要创建[%s]节点吗？", path)))
                    return;

                String status = zkUtil.create(path, null);
                JOptionPane.showMessageDialog(null, status);
            }
        });
        createWithDataButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isZKConnected(isConnected))
                    return;
                if (!checkRootPath(path))
                    return;
                String data = textForData.getText();
                if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(null, String.format("确定要创建[%s]节点，\n且将数据设置为【%s】(UTF-8)吗？", path, (data.length() > 20 ? data.substring(0, 20) + "...." : data))))
                    return;

                String status = zkUtil.create(path, data.getBytes(StandardCharsets.UTF_8));
                JOptionPane.showMessageDialog(null, status);
            }
        });


        // 修改、删除按钮
        final JButton deleteButton = new JButton("删除此节点");
        final JButton updateButton = new JButton("修改数据为如上文本框中的数据");
        panel.add(deleteButton);
        panel.add(updateButton);
        setConstraints(1, 1, false, null, null, gridBagLayout, deleteButton);
        setConstraints(1, 1, true, null, null, gridBagLayout, updateButton);
        deleteButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isZKConnected(isConnected))
                    return;
                if (!checkRootPath(path))
                    return;
                if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(null, String.format("确定要删除节点[%s]吗？", path)))
                    return;
                String msg = zkUtil.delete(path);
                JOptionPane.showMessageDialog(null, msg);
            }
        });
        updateButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isZKConnected(isConnected))
                    return;
                if (!checkRootPath(path))
                    return;
                String data = textForData.getText();
                if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(null, String.format("确定要修改节点[%s]\n的数据为\n【%s】(UTF-8)吗？", path, (data.length() > 20 ? data.substring(0, 20) + "...." : data))))
                    return;
                String msg = zkUtil.update(path, data.getBytes(StandardCharsets.UTF_8));
                JOptionPane.showMessageDialog(null, msg);
            }
        });


        createButton.setVisible(false);
        createWithDataButton.setVisible(false);
        updateButton.setVisible(false);
        deleteButton.setVisible(false);
        setStatForPathRunnable = new Runnable() {
            @Override
            public void run() {
                if (path.length() > 1 && path.endsWith("/"))
                    path = path.substring(0, path.length() - 1);

                textField.setText(path);
                textForStat.setText("");
                textForData.setText("");
                createButton.setVisible(false);
                createWithDataButton.setVisible(false);
                updateButton.setVisible(false);
                deleteButton.setVisible(false);
                children = null;
//                textField.setColumns(path.length() + 3);

                Stat stat = zkUtil.exists(path);
                if (null == stat) {
                    textForStat.setText("不存在");
                    createButton.setVisible(true);
                    createWithDataButton.setVisible(true);
                    setChildrenForPathRunnable.run();
                    return;
                }
                updateButton.setVisible(true);
                deleteButton.setVisible(true);
                StringBuilder builder = new StringBuilder();
                builder.append("Czxid: ").append(stat.getCzxid()).append("\n");
                builder.append("Mzxid: ").append(stat.getMzxid()).append("\n");
                builder.append("Ctime: ").append(stat.getCtime()).append("\n");
                builder.append("Mtime: ").append(stat.getMtime()).append("\n");
                builder.append("Version: ").append(stat.getVersion()).append("\n");
                builder.append("Cversion: ").append(stat.getCversion()).append("\n");
                builder.append("Aversion: ").append(stat.getAversion()).append("\n");
                builder.append("EphemeralOwner: ").append(stat.getEphemeralOwner()).append("\n");
                builder.append("DataLength: ").append(stat.getDataLength()).append("\n");
                builder.append("NumChildren: ").append(stat.getNumChildren()).append("\n");
                builder.append("Pzxid: ").append(stat.getPzxid()).append("\n");
                textForStat.setText(builder.toString());

                String data = zkUtil.getData(path);
                if (data != null)
                    textForData.setText(data);

                if (stat.getNumChildren() > 0) {
                    children = zkUtil.children(path);
                }
                setChildrenForPathRunnable.run();
            }
        };
    }

    private static boolean isDangDangDomain = "DANGDANG".equalsIgnoreCase(System.getenv("USERDOMAIN"));

    private void initHeadPanel(JPanel headPanel) {
        headPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel("zookeeper地址：");
        headPanel.add(label);

        final JTextField textField = new JTextField(isDangDangDomain ? "10.255.209.45:2181" : "host:port");
        textField.setColumns(13);
        headPanel.add(textField);

        final String textConnect = "连接";
        final String textUnConnect = "断开连接";
        final JButton button = new JButton(textConnect);
        headPanel.add(button);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (textConnect.equals(button.getText())) {
                    zkUtil.init(textField.getText(), null);
                    isConnected = true;
                    // 连接成功：
                    button.setText(textUnConnect);
                    textField.setEnabled(false);
                    setPanelRunnable.run();
                } else {
                    zkUtil.shutdown();
                    isConnected = false;
                    // 断开连接成功：
                    button.setText(textConnect);
                    textField.setEnabled(true);
                    setPanelRunnable.run();
                }
            }
        });

        final JCheckBox checkBox = new JCheckBox("是否树结构展示", true);
        headPanel.add(checkBox);
        checkBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                isTreeView = checkBox.isSelected();
                setPanelRunnable.run();
            }
        });
    }

    private void initSubHeadPanel1(JPanel headPanel) {
        headPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

//        JLabel label = new JLabel("              ");
//        headPanel.add(label);

        // 查看的路径chroot
        final JTextField chRootTextField = new JTextField("/");
        chRootTextField.setColumns(45);
        headPanel.add(chRootTextField);

        final JButton viewButton = new JButton("查看");
        headPanel.add(viewButton);
        viewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isZKConnected(isConnected))
                    return;
                setStatForPathRunnable.run();
            }
        });
        chRootTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                path = chRootTextField.getText();
                viewButton.doClick();
            }
        });
        chRootTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    path = chRootTextField.getText();
                    viewButton.doClick();
                }
            }
        });

        JButton upButton = new JButton("↑上级");
        headPanel.add(upButton);
        upButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String temp = path.trim();
                if (temp.length() == 1) {
                    JOptionPane.showMessageDialog(null, "已经是跟节点了");
                    return;
                }
                temp = temp.substring(0, temp.lastIndexOf("/"));
                if (temp.isEmpty())
                    temp = "/";
                chRootTextField.setText(temp);
                path = temp;
                viewButton.doClick();
            }
        });

        setPathRunnable = new Runnable() {
            @Override
            public void run() {
                chRootTextField.setText(path);
                viewButton.doClick();
            }
        };
    }

    static boolean checkRootPath(String path) {
        if ("/".equals(path)) {
            JOptionPane.showMessageDialog(null, "不可以操作跟节点");
            return false;
        }
        return true;
    }

    static boolean isZKConnected(boolean isConnected) {
        if (!isConnected) {
            JOptionPane.showMessageDialog(null, "请先连接zookeeper");
            return false;
        }
        return true;
    }

    static void setConstraints(Integer columns, Integer rows, boolean isLastColumn, Double width, Double height, GridBagLayout layout, Component component) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        // 是用来控制添加进的组件的显示位置
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        // 该方法是为了设置如果组件所在的区域比组件本身要大时的显示情况
        // NONE：不调整组件大小。
        // HORIZONTAL：加宽组件，使它在水平方向上填满其显示区域，但是不改变高度。
        // VERTICAL：加高组件，使它在垂直方向上填满其显示区域，但是不改变宽度。
        // BOTH：使组件完全填满其显示区域。
        if (isLastColumn)
            gridBagConstraints.gridwidth = 0; // 该方法是设置组件水平所占用的格子数，如果为0，就说明该组件是该行的最后一个
        else
            gridBagConstraints.gridwidth = columns;
        if (rows > 1)
            gridBagConstraints.gridheight = rows;
        if (width != null)
            gridBagConstraints.weightx = width; // 该方法设置组件水平的拉伸幅度，如果为0就说明不拉伸，不为0就随着窗口增大进行拉伸，0到1之间
        if (height != null)
            gridBagConstraints.weighty = height; // 该方法设置组件垂直的拉伸幅度，如果为0就说明不拉伸，不为0就随着窗口增大进行拉伸，0到1之间
        layout.setConstraints(component, gridBagConstraints);
    }
}

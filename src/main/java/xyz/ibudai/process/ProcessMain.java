package xyz.ibudai.process;

import xyz.ibudai.process.model.ProcessDetail;
import xyz.ibudai.process.util.ExceptionConvert;
import xyz.ibudai.process.util.ProcessUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProcessMain {

    private static final int frameWidth = 900;

    private static final int frameHeight = 600;

    private static final JFrame frame = new JFrame("Windows 进程管理");


    public static void main(String[] args) {
        try {
            draw();
        } catch (Exception e) {
            String message = ExceptionConvert.buildMsg(e);
            JOptionPane.showMessageDialog(frame, message, "Unexpect Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 绘制程序窗体页面
     */
    public static void draw() {
        // 创建 JFrame 窗口
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(frameWidth, frameHeight);

        // 端口输入；查询重置按钮
        JPanel inputPanel = new JPanel();
        JLabel portLabel = new JLabel("Port:");
        JTextField portText = new JTextField(20);
        JButton searchBt = new JButton("Search");
        JButton resetBt = new JButton("Reset");
        inputPanel.add(portLabel);
        inputPanel.add(portText);
        inputPanel.add(searchBt);
        inputPanel.add(resetBt);
        // 进程输入；删除按钮
        JLabel pIdLabel = new JLabel("PID:");
        JTextField pIdText = new JTextField(20);
        JButton killBt = new JButton("Kill");
        inputPanel.add(pIdLabel);
        inputPanel.add(pIdText);
        inputPanel.add(killBt);

        // Table 对象
        String[] heads = new String[]{"PID", "Protocol", "Inner Host", "Outer Host", "Status"};
        DefaultTableModel tableModel = new DefaultTableModel(heads, 0);
        JScrollPane tablePanel = drawTable(tableModel, portText, pIdText);

        // 搜索按钮事件监听
        searchTable(portText, searchBt, tableModel);
        // 重置按钮事件监听
        resetTable(resetBt, portText, pIdText, tableModel);
        // Kill按钮事件监听
        killProcess(portText, pIdText, killBt, resetBt);

        // 内容面板的内边距
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBorder(new EmptyBorder(20, 20, 20, 20));
        // 将 panel 添加到内容面板中
        contentPane.add(tablePanel, BorderLayout.CENTER);
        contentPane.add(inputPanel, BorderLayout.NORTH);

        frame.setContentPane(contentPane);
        // 设置面板位置未屏幕中央
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * 绘制 Table
     *
     * @param tableModel table 数据
     * @param portField  PORT 输入框
     * @param pIdField   PID 输入框
     * @return
     */
    private static JScrollPane drawTable(DefaultTableModel tableModel, JTextField portField, JTextField pIdField) {
        for (ProcessDetail detail : ProcessUtil.buildTableData()) {
            tableModel.addRow(
                    new Object[]{
                            detail.getPid(),
                            detail.getProtocol(),
                            detail.getInnerHost(),
                            detail.getOuterHost(),
                            detail.getStatus()
                    }
            );
        }

        // 创建 JTable
        JTable table = new JTable(tableModel) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setFont(new Font("Serif", Font.PLAIN, 17)); // 设置表格字体大小为 16px
        table.setRowHeight(20); // 设置行高，使得字体更适合

        // 添加表格选中行监听器
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    // PID
                    String pid = (String) tableModel.getValueAt(selectedRow, 0);
                    pIdField.setText(pid);
                    // Port
                    String port;
                    String innerHost = (String) tableModel.getValueAt(selectedRow, 2);
                    if (innerHost.startsWith("[")) {
                        port = innerHost.substring(innerHost.indexOf("]:") + 2);
                    } else {
                        port = innerHost.substring(innerHost.indexOf(":") + 1);
                    }
                    portField.setText(port);
                }
            }
        });

        return new JScrollPane(table);
    }

    /**
     * 根据输入端口号过滤进程
     *
     * @param textField  port input
     * @param searchBt   search button
     * @param tableModel table data
     */
    public static void searchTable(JTextField textField, JButton searchBt, DefaultTableModel tableModel) {
        // 添加按钮点击事件监听器
        searchBt.addActionListener(h -> {
            String input = textField.getText();
            if (Objects.isNull(input) || Objects.equals("", input)) {
                JOptionPane.showMessageDialog(frame, "请输入端口号", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            List<ProcessDetail> detailList = ProcessUtil.buildTableData();
            detailList = detailList.stream()
                    .filter(it -> {
                        String port;
                        String innerHost = it.getInnerHost();
                        if (innerHost.startsWith("[")) {
                            port = innerHost.substring(innerHost.indexOf("]:") + 2);
                        } else {
                            port = innerHost.substring(innerHost.indexOf(":") + 1);
                        }
                        return Objects.equals(input.trim(), port.trim());
                    })
                    .collect(Collectors.toList());
            if (detailList.isEmpty()) {
                String msg = String.format("未找到端口 {%s} 进程", input);
                JOptionPane.showMessageDialog(frame, msg, "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            for (ProcessDetail detail : detailList) {
                tableModel.setRowCount(0);
                tableModel.addRow(
                        new Object[]{
                                detail.getPid(),
                                detail.getProtocol(),
                                detail.getInnerHost(),
                                detail.getOuterHost(),
                                detail.getStatus()
                        }
                );
            }
            textField.setText("");
        });
    }

    /**
     * 重置表格数据
     *
     * @param resetBt    reset button
     * @param portField  port input
     * @param pidField   pid input
     * @param tableModel table data
     */
    public static void resetTable(JButton resetBt, JTextField portField, JTextField pidField, DefaultTableModel tableModel) {
        resetBt.addActionListener(h -> {
            tableModel.setRowCount(0);
            for (ProcessDetail detail : ProcessUtil.buildTableData()) {
                tableModel.addRow(
                        new Object[]{
                                detail.getPid(),
                                detail.getProtocol(),
                                detail.getInnerHost(),
                                detail.getOuterHost(),
                                detail.getStatus()
                        }
                );
            }
            portField.setText("");
            pidField.setText("");
        });
    }

    /**
     * Kill 所选进程
     *
     * @param portField port input
     * @param pidField  pid input
     * @param killBt    kill button
     * @param resetBt   reset button
     */
    public static void killProcess(JTextField portField, JTextField pidField, JButton killBt, JButton resetBt) {
        killBt.addActionListener(h -> {
            String text = pidField.getText();
            if (Objects.isNull(text) || Objects.equals("", text)) {
                JOptionPane.showMessageDialog(frame, "请输入 PID", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("taskkill", "-PID", text, "-F");
            try {
                processBuilder.start();
                resetBt.doClick();
                portField.setText("");
                pidField.setText("");
                JOptionPane.showMessageDialog(frame, "进程关闭成功", "Successfully", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}

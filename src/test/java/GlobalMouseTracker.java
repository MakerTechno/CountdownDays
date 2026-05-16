

import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinUser.*;
import com.sun.jna.win32.W32APIOptions;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 全局鼠标位置实时记录器（修正版）
 */
public class GlobalMouseTracker {
    
    private static final DateTimeFormatter formatter = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static PrintWriter logWriter = null;
    private static volatile boolean isRunning = true;
    private static POINT lastPoint = new POINT();
    
    // 使用扩展的User32接口
    private static final User32Extended USER32 = User32Extended.INSTANCE;
    
    public static void main(String[] args) {
        // 创建日志文件
        try {
            String logFileName = "mouse_log_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
            logWriter = new PrintWriter(new FileWriter(logFileName, true));
            logWriter.println("Timestamp,X,Y,EventType,WindowTitle,WindowClass");
            System.out.println("日志文件创建成功: " + logFileName);
        } catch (IOException e) {
            System.err.println("无法创建日志文件: " + e.getMessage());
            return;
        }
        
        // 启动GUI
        SwingUtilities.invokeLater(() -> createAndShowGUI());
        
        // 启动鼠标追踪线程
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        executor.scheduleAtFixedRate(() -> trackMousePosition(), 0, 20, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(() -> trackMouseButtons(), 0, 10, TimeUnit.MILLISECONDS);
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isRunning = false;
            executor.shutdown();
            if (logWriter != null) {
                logWriter.close();
            }
            System.out.println("鼠标追踪器已安全停止");
        }));
    }
    
    /**
     * 追踪鼠标位置变化
     */
    private static void trackMousePosition() {
        if (!isRunning) return;
        
        try {
            POINT currentPoint = new POINT();
            boolean success = USER32.GetCursorPos(currentPoint);
            
            if (success) {
                // 检查鼠标是否移动（可以设置最小移动阈值以避免微小抖动）
                if (currentPoint.x != lastPoint.x || currentPoint.y != lastPoint.y) {
                    lastPoint.x = currentPoint.x;
                    lastPoint.y = currentPoint.y;
                    
                    String timestamp = LocalDateTime.now().format(formatter);
                    WindowInfo windowInfo = getWindowInfo(currentPoint);
                    
                    String logEntry = String.format("%s,%d,%d,MOVE,%s,%s",
                        timestamp,
                        currentPoint.x,
                        currentPoint.y,
                        windowInfo.title.replace(",", ";"),
                        windowInfo.className);
                    
                    // 写入日志文件
                    if (logWriter != null) {
                        synchronized (logWriter) {
                            logWriter.println(logEntry);
                            logWriter.flush();
                        }
                    }
                    
                    // 控制台输出（可选的调试信息）
                    System.out.printf("[%s] 位置: (%d, %d) - 窗口: %s%n",
                        timestamp, currentPoint.x, currentPoint.y, windowInfo.title);
                }
            }
        } catch (Exception e) {
            System.err.println("追踪鼠标位置时出错: " + e.getMessage());
        }
    }
    
    /**
     * 追踪鼠标按钮状态
     */
    private static boolean leftPressed = false;
    private static boolean rightPressed = false;
    private static boolean middlePressed = false;
    
    private static void trackMouseButtons() {
        if (!isRunning) return;
        
        try {
            // 获取异步按键状态
            short rightState = USER32.GetAsyncKeyState(WinUser.WH_MOUSE);

            boolean currentRight = (rightState & 0x8000) != 0;

            
            if (currentRight != rightPressed) {
                logButtonEvent(currentRight ? "RIGHT_DOWN" : "RIGHT_UP", "右键");
                rightPressed = currentRight;
            }
            
        } catch (Exception e) {
            // 静默处理按键状态获取错误
        }
    }
    
    /**
     * 记录鼠标按钮事件
     */
    private static void logButtonEvent(String eventType, String buttonName) {
        try {
            POINT point = new POINT();
            if (USER32.GetCursorPos(point)) {
                String timestamp = LocalDateTime.now().format(formatter);
                WindowInfo windowInfo = getWindowInfo(point);
                
                String logEntry = String.format("%s,%d,%d,%s,%s,%s",
                    timestamp,
                    point.x,
                    point.y,
                    eventType,
                    windowInfo.title.replace(",", ";"),
                    windowInfo.className);
                
                if (logWriter != null) {
                    synchronized (logWriter) {
                        logWriter.println(logEntry);
                        logWriter.flush();
                    }
                }
                
                System.out.printf("[%s] %s事件在 (%d, %d)%n",
                    timestamp, buttonName, point.x, point.y);
            }
        } catch (Exception e) {
            System.err.println("记录按钮事件时出错: " + e.getMessage());
        }
    }
    
    /**
     * 窗口信息类
     */
    private static class WindowInfo {
        String title;
        String className;
        
        WindowInfo(String title, String className) {
            this.title = title;
            this.className = className;
        }
    }
    
    /**
     * 获取鼠标所在位置的窗口信息
     */
    private static WindowInfo getWindowInfo(POINT point) {
        String title = "";
        String className = "";
        
        try {
            // 使用WindowFromPoint获取窗口句柄
            HWND hwnd = USER32.WindowFromPoint(point);
            
            if (hwnd != null) {
                // 获取窗口标题
                char[] windowText = new char[512];
                USER32.GetWindowText(hwnd, windowText, 512);
                title = Native.toString(windowText);
                
                // 获取窗口类名
                char[] classText = new char[256];
                USER32.GetClassName(hwnd, classText, 256);
                className = Native.toString(classText);
                
                // 如果窗口没有标题，尝试获取父窗口的标题
                if (title.isEmpty()) {
                    HWND parentHwnd = USER32.GetAncestor(hwnd, User32Extended.GA_ROOT);
                    if (parentHwnd != null && !parentHwnd.equals(hwnd)) {
                        USER32.GetWindowText(parentHwnd, windowText, 512);
                        title = Native.toString(windowText);
                    }
                }
            }
        } catch (Exception e) {
            // 静默处理窗口信息获取失败
            title = "无法获取窗口信息";
        }
        
        return new WindowInfo(title, className);
    }
    
    /**
     * 创建并显示GUI界面
     */
    private static void createAndShowGUI() {
        try {
            // 设置系统外观
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // 使用默认外观
        }
        
        JFrame frame = new JFrame("全局鼠标位置记录器");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout(5, 5));
        
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 标题标签
        JLabel titleLabel = new JLabel("实时鼠标信息", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // 信息显示面板
        JPanel infoPanel = new JPanel(new GridLayout(6, 1, 5, 5));
        infoPanel.setBorder(BorderFactory.createTitledBorder("当前状态"));
        
        JLabel positionLabel = new JLabel("位置: (0, 0)");
        JLabel windowLabel = new JLabel("窗口: ");
        JLabel classLabel = new JLabel("类名: ");
        JLabel leftButtonLabel = new JLabel("左键: 释放");
        JLabel rightButtonLabel = new JLabel("右键: 释放");
        JLabel middleButtonLabel = new JLabel("中键: 释放");
        
        // 设置字体
        Font infoFont = new Font("Consolas", Font.PLAIN, 14);
        positionLabel.setFont(infoFont);
        windowLabel.setFont(infoFont);
        classLabel.setFont(infoFont);
        leftButtonLabel.setFont(infoFont);
        rightButtonLabel.setFont(infoFont);
        middleButtonLabel.setFont(infoFont);
        
        infoPanel.add(positionLabel);
        infoPanel.add(windowLabel);
        infoPanel.add(classLabel);
        infoPanel.add(leftButtonLabel);
        infoPanel.add(rightButtonLabel);
        infoPanel.add(middleButtonLabel);
        
        mainPanel.add(infoPanel, BorderLayout.CENTER);
        
        // 控制面板
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("控制"));
        
        JButton toggleButton = new JButton("暂停记录");
        JButton clearButton = new JButton("清空显示");
        JButton exitButton = new JButton("退出程序");
        
        toggleButton.addActionListener(e -> {
            isRunning = !isRunning;
            toggleButton.setText(isRunning ? "暂停记录" : "继续记录");
        });
        
        clearButton.addActionListener(e -> {
            positionLabel.setText("位置: (0, 0)");
            windowLabel.setText("窗口: ");
            classLabel.setText("类名: ");
        });
        
        exitButton.addActionListener(e -> {
            isRunning = false;
            if (logWriter != null) {
                logWriter.close();
            }
            System.exit(0);
        });
        
        controlPanel.add(toggleButton);
        controlPanel.add(clearButton);
        controlPanel.add(exitButton);
        
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
        
        // 状态栏
        JLabel statusLabel = new JLabel("状态: 记录中 | 日志文件: mouse_log_*.csv");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        frame.add(mainPanel, BorderLayout.CENTER);
        frame.add(statusLabel, BorderLayout.SOUTH);
        
        // 定时更新GUI显示
        Timer updateTimer = new Timer(100, e -> {
            POINT point = new POINT();
            if (USER32.GetCursorPos(point)) {
                positionLabel.setText(String.format("位置: (%d, %d)", point.x, point.y));
                
                WindowInfo windowInfo = getWindowInfo(point);
                windowLabel.setText("窗口: " + windowInfo.title);
                classLabel.setText("类名: " + windowInfo.className);
                
                leftButtonLabel.setText("左键: " + (leftPressed ? "按下" : "释放"));
                rightButtonLabel.setText("右键: " + (rightPressed ? "按下" : "释放"));
                middleButtonLabel.setText("中键: " + (middlePressed ? "按下" : "释放"));
            }
        });
        updateTimer.start();
        
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
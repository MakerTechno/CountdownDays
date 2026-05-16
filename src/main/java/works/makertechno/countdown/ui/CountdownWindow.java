package works.makertechno.countdown.ui;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import works.makertechno.countdown.component.ComponentManager;
import works.makertechno.countdown.config.AppConfig;
import works.makertechno.countdown.empower.DesktopIntegration;
import works.makertechno.countdown.empower.MouseTracker;
import works.makertechno.countdown.i18n.I18n;
import works.makertechno.countdown.model.CountdownData;
import works.makertechno.countdown.render.DefaultTextRenderer;
import works.makertechno.countdown.render.TextRenderer;
import works.makertechno.countdown.render.WindowFilter;
import works.makertechno.countdown.util.AutoStartManager;
import works.makertechno.countdown.util.TimeFormatter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CountdownWindow extends JFrame {
    private static final User32 USER32 = User32.INSTANCE;
    private static final Logger LOG = LoggerFactory.getLogger(CountdownWindow.class);

    private final Image icon = Toolkit.getDefaultToolkit().getImage(
            getClass().getResource("/icon.png")
    );

    private final I18n i18n;
    private final JPanel displayPanel;
    private final MouseTracker tracker = new MouseTracker();
    private CountdownData data;
    private String displayName;   // 用户设置的显示名称
    private Color textColor;
    private Path transPath;
    private boolean showYearsMonthsDays;
    private Map<String, String> currentVariables;
    private String displayText = "";
    // 拖动相关
    private int dragStartXOnScreen, dragStartYOnScreen;
    private int dragStartWindowX, dragStartWindowY;
    private TextRenderer textRenderer = TextRenderer.defaultRenderer();
    private WindowFilter windowFilter = WindowFilter.none();
    // 是否桌面模式
    private boolean isDesktopMode = false;

    // 配置缓存
    private AppConfig.ConfigData cachedConfig;
    // 保存防抖定时器
    private Timer saveDebounceTimer;
    // 组件管理器
    private final ComponentManager componentManager;

    public CountdownWindow(I18n i18n, CountdownData data, Color textColorIn, Path transPath, boolean showYearsMonthsDays) {
        this.i18n = i18n;
        this.data = data;
        this.textColor = textColorIn;
        this.transPath = transPath;
        this.showYearsMonthsDays = showYearsMonthsDays;
        this.currentVariables = new LinkedHashMap<>(i18n.getDefaultVariables());

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 1));
        setAlwaysOnTop(true);

        // 先设置默认大小，然后再加载配置（避免加载时出现0x0）
        setSize(900, 130);
        setLocationRelativeTo(null);
        // 加载并缓存配置
        cachedConfig = AppConfig.loadConfig();

        // 设置窗口位置
        setupWindowPosition();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(icon);

        // 添加组件监听器，自动保存窗口位置（带防抖）
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                saveWindowPositionWithDebounce();
            }
        });

        displayPanel = configureDisplayPanel();

        configureDrag();
        configureTray();
        configureRightClick();

        // 初始化组件管理器
        componentManager = new ComponentManager();

        // 从配置恢复自定义组件路径
        if (cachedConfig != null) {
            componentManager.setTextRendererZipPath(cachedConfig.getCustomTextRendererPath());
            componentManager.setWindowFilterZipPath(cachedConfig.getCustomWindowFilterPath());
            componentManager.restoreFromConfig();

            // 如果有自定义组件，替换当前的渲染器
            if (componentManager.hasCustomTextRenderer()) {
                this.textRenderer = componentManager.getTextRenderer();
                LOG.info("已加载自定义 TextRenderer");
            }
            if (componentManager.hasCustomWindowFilter()) {
                this.windowFilter = componentManager.getWindowFilter();
                LOG.info("已加载自定义 WindowFilter");
            }
        }

        startTimer();
        tracker.start(this);

        LOG.info("倒计时窗口已创建，文字颜色：{}，变量数：{}，显示模式：{}",
                textColor, currentVariables.size(), showYearsMonthsDays ? "年月日" : "总天数");
    }

    /**
     * 设置窗口位置（从配置加载或使用默认值）
     */
    private void setupWindowPosition() {
        if (cachedConfig != null) {
            Point position = cachedConfig.getWindowPosition();
            if (position != null) {
                // 直接使用保存的位置，不进行屏幕边界限制
                setLocation(position.x, position.y);
                LOG.info("加载窗口位置: x={}, y={}", position.x, position.y);
                return;
            }
        }
        // 使用默认位置，居中显示
        setLocationRelativeTo(null);
        LOG.info("使用默认窗口位置");
    }

    /**
     * 带防抖的窗口位置保存（避免频繁写入文件）
     */
    private void saveWindowPositionWithDebounce() {
        // 桌面模式下不保存位置
        if (isDesktopMode) return;

        Point position = getLocation();

        // 更新缓存
        if (cachedConfig == null) {
            cachedConfig = new AppConfig.ConfigData();
        }
        cachedConfig.setWindowPosition(position);

        // 使用防抖：延迟500ms保存，避免频繁写入
        if (saveDebounceTimer != null && saveDebounceTimer.isRunning()) {
            saveDebounceTimer.restart();
        } else {
            saveDebounceTimer = new Timer(500, e -> {
                AppConfig.saveConfig(cachedConfig);
                LOG.debug("保存窗口位置: x={}, y={}", position.x, position.y);
                saveDebounceTimer.stop();
            });
            saveDebounceTimer.setRepeats(false);
            saveDebounceTimer.start();
        }
    }

    /**
     * 立即保存窗口位置（用于关闭窗口时）
     */
    private void saveWindowPositionImmediately() {
        // 桌面模式下不保存位置
        if (isDesktopMode) return;

        Point position = getLocation();

        if (cachedConfig == null) {
            cachedConfig = new AppConfig.ConfigData();
        }
        cachedConfig.setWindowPosition(position);
        AppConfig.saveConfig(cachedConfig);
        LOG.info("立即保存窗口位置: x={}, y={}", position.x, position.y);

        // 停止防抖定时器
        if (saveDebounceTimer != null && saveDebounceTimer.isRunning()) {
            saveDebounceTimer.stop();
        }
    }

    private @NotNull JPanel configureDisplayPanel() {
        final JPanel displayPanel;
        displayPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                textRenderer.render(g2d, displayText, getBounds(), textColor);
                g2d.dispose();
            }
        };
        displayPanel.setOpaque(false);
        setContentPane(displayPanel);
        return displayPanel;
    }

    // 提供公开方法供后续配置
    public void setTextRenderer(TextRenderer renderer) {
        this.textRenderer = renderer;
        displayPanel.repaint();
    }

    public void setWindowFilter(WindowFilter filter) {
        this.windowFilter = filter;
        repaint();
    }

    // 在 paint 方法中应用滤镜
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g.create();
        windowFilter.apply(g2d, getWidth(), getHeight(), this, tracker);
        g2d.dispose();
    }
    // ==================== 拖动 ====================

    private void configureDrag() {
        MouseAdapter dragAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // 桌面模式下不允许拖动
                if (isDesktopMode) return;

                if (SwingUtilities.isLeftMouseButton(e)) {
                    // 记录拖动开始时的窗口位置和鼠标在屏幕上的位置
                    dragStartWindowX = getX();
                    dragStartWindowY = getY();
                    dragStartXOnScreen = e.getXOnScreen();
                    dragStartYOnScreen = e.getYOnScreen();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // 桌面模式下不允许拖动
                if (isDesktopMode) return;

                if (SwingUtilities.isLeftMouseButton(e)) {
                    // 计算鼠标移动的偏移量
                    int deltaX = e.getXOnScreen() - dragStartXOnScreen;
                    int deltaY = e.getYOnScreen() - dragStartYOnScreen;
                    // 设置新位置 = 原始窗口位置 + 鼠标移动偏移量
                    setLocation(dragStartWindowX + deltaX, dragStartWindowY + deltaY);
                }
            }
        };
        displayPanel.addMouseListener(dragAdapter);
        displayPanel.addMouseMotionListener(dragAdapter);
    }

    private void configureTray() {
        if (!SystemTray.isSupported()) return;

        TrayIcon trayIcon = new TrayIcon(
                icon,
                "Countdown Days"
        );
        trayIcon.setImageAutoSize(true);

    // 双击事件：退出桌面模式（恢复普通窗口）
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (isDesktopMode) {
                        // 退出桌面模式
                        exitDesktopMode();
                    } else {
                        // 普通模式下双击不做特殊处理，或者也可以隐藏/显示
                        setVisible(false);
                        dispose();
                        setVisible(true);
                        LOG.info("已恢复窗口化");
                    }
                }
            }
        });

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            LOG.error("Failed to add tray icon.", e);
        }
    }

    // ==================== 右键菜单 ====================

    private void configureRightClick() {
        displayPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) showPopupMenu(e);
            }
        });
    }

    private void showPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 80), 1));

    // 桌面模式下不应该能弹出菜单（因为鼠标穿透），但以防万一，桌面模式下不显示菜单
    if (isDesktopMode) {
        return;
    }

        JMenuItem settingsItem = createFlatMenuItem(i18n.get("settings"));
        settingsItem.addActionListener(ev -> openSettings());

    JMenuItem desktopItem = createFlatMenuItem(i18n.get("settings.desktop_mode"));
    desktopItem.addActionListener(ev -> enterDesktopMode());

        JMenuItem closeItem = createFlatMenuItem(i18n.get("close"));
        closeItem.addActionListener(ev -> {
            saveWindowPositionImmediately();  // 关闭前立即保存窗口位置
            dispose();
            System.exit(0);
        });

        menu.add(settingsItem);
    menu.add(desktopItem);
        menu.add(closeItem);
        menu.show(displayPanel, e.getX(), e.getY());
    }

    private JMenuItem createFlatMenuItem(String text) {
        JMenuItem item = new JMenuItem(text);
        item.setOpaque(true);
        item.setBackground(new Color(50, 50, 50));
        item.setForeground(new Color(220, 220, 220));
        item.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        item.setFont(new Font("SansSerif", Font.PLAIN, 14));
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                item.setBackground(new Color(70, 70, 70));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                item.setBackground(new Color(50, 50, 50));
            }
        });
        return item;
    }

    // ==================== 设置对话框 ====================

    private void openSettings() {
        // 获取当前开机自启动状态
        boolean currentAutoStart = cachedConfig != null && cachedConfig.isAutoStart() || AutoStartManager.isAutoStartEnabled();;

        SettingsDialog dialog = new SettingsDialog(
                this, i18n, data, textColor, transPath,
                new LinkedHashMap<>(currentVariables),
                showYearsMonthsDays,
                currentAutoStart,
                this::applyAutoStartSetting,
            componentManager,  // 传入组件管理器
                result -> {
                    // 更新数据
                    data = new CountdownData(result.eventName(), result.targetTime());
                    textColor = result.textColor();
                    showYearsMonthsDays = result.showYearsMonthsDays();

                    // 处理翻译文件
                    boolean resetLanguage = result.resetLanguage();
                    if (resetLanguage) {
                        // 恢复内置语言：重新加载内置翻译
                        transPath = null;
                        // 重新创建 i18n 实例或重新加载内置翻译
                        // 方案1：重新加载内置翻译
                        i18n.reloadBuiltin(); // 需要在 I18n 中添加此方法
                        currentVariables = new LinkedHashMap<>(i18n.getDefaultVariables());
                        LOG.info("已恢复内置语言");
                    } else if (result.transPath() != null && !result.transPath().equals(transPath)) {
                        transPath = result.transPath();
                        boolean loaded = i18n.load(transPath);
                        if (!loaded) {
                            JOptionPane.showMessageDialog(this,
                                    i18n.get("settings.language_load_fail"),
                                    i18n.get("settings.language"), JOptionPane.ERROR_MESSAGE);
                            transPath = null; // 回退到内置
                        }
                    }

                    // 保存配置到文件（不包含 autoStart，因为已经单独保存了）
                    if (cachedConfig == null) {
                        cachedConfig = new AppConfig.ConfigData();
                    }
                    // 更新配置数据（不覆盖窗口位置）
                    cachedConfig.setEventName(result.eventName());
                    cachedConfig.setTargetTime(result.targetTime());
                    cachedConfig.setTextColorR(result.textColor().getRed());
                    cachedConfig.setTextColorG(result.textColor().getGreen());
                    cachedConfig.setTextColorB(result.textColor().getBlue());
                    cachedConfig.setTextColorA(result.textColor().getAlpha());
                    cachedConfig.setTransPath(result.transPath() != null ? result.transPath().toString() : null);
                    cachedConfig.setShowYearsMonthsDays(result.showYearsMonthsDays());
                    // 保留当前窗口位置
                    cachedConfig.setWindowPosition(getLocation());
                    AppConfig.saveConfig(cachedConfig);

                    // 保存自定义组件路径到配置
                    if (cachedConfig != null) {
                        cachedConfig.setCustomTextRendererPath(componentManager.getTextRendererZipPath());
                        cachedConfig.setCustomWindowFilterPath(componentManager.getWindowFilterZipPath());
                        AppConfig.saveConfig(cachedConfig);
                    }

                    // 如果有自定义 TextRenderer，应用它
                    if (componentManager.hasCustomTextRenderer()) {
                        setTextRenderer(componentManager.getTextRenderer());
                    } else {
                        // 恢复默认
                        setTextRenderer(new DefaultTextRenderer());
                    }

                    // 如果有自定义 WindowFilter，应用它
                    if (componentManager.hasCustomWindowFilter()) {
                        setWindowFilter(componentManager.getWindowFilter());
                    } else {
                        setWindowFilter(WindowFilter.none());
                    }
                    // 刷新显示
                    updateDisplay();
                    LOG.info("设置已应用并保存：颜色={}, 目标时间={}, 显示模式={}",
                            textColor, result.targetTime(), result.showYearsMonthsDays() ? "年月日" : "总天数");
                });
        dialog.setVisible(true);
    }

    /**
     * 应用开机自启动设置
 * @param enabled 是否启用
 * @return 是否设置成功
     */
private boolean applyAutoStartSetting(boolean enabled) {
    // 获取当前 jar 路径，传入 parent 和 i18n 用于弹窗提示
    String jarPath = AutoStartManager.getCurrentJarPath(this, i18n);

        if (enabled && jarPath == null) {
        // 已经在 getCurrentJarPath 中弹窗提示了，这里直接返回 false
        return false;
        }

        if (enabled) {
            // 弹出确认对话框
            String message = i18n.get("settings.auto_start_confirm").replace("{0}", jarPath);
            int result = JOptionPane.showConfirmDialog(this, message,
                    i18n.get("settings.auto_start"), JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) {
            // 用户取消，返回 false 表示未成功
            return false;
            }
        }

    // 调用管理器，传入 this 作为父窗口，传入 i18n
        boolean success = AutoStartManager.setAutoStart(enabled, jarPath, this, i18n);
        if (success) {
            LOG.info("开机自启动设置已{}", enabled ? "启用" : "禁用");
            // 更新缓存中的状态
            if (cachedConfig != null) {
                cachedConfig.setAutoStart(enabled);
            AppConfig.saveConfig(cachedConfig);
            }
        return true;
        } else {
            LOG.warn("开机自启动设置失败");
        return false;
        }
    }


    // ==================== 定时刷新 ====================

    private void startTimer() {
        Timer timer = new Timer(1, e -> updateDisplay());
        timer.setInitialDelay(10);
        timer.start();
    }

    private void updateDisplay() {
        LocalDateTime now = LocalDateTime.now();
        String remaining = TimeFormatter.formatRemaining(now, data.targetDateTime(), i18n, showYearsMonthsDays);
        Map<String, String> vars = new HashMap<>();
        vars.put("EventName", data.userDataText());
        vars.put("Time", remaining);
        displayText = i18n.get("countdown.text", vars);
        displayPanel.repaint();
    }

    // ============== Native ===============

    /**
     * 通过窗口标题查找 HWND
     */
    private WinDef.HWND getHWNDByTitle() {
        String title = getTitle();
        if (title == null || title.isEmpty()) {
            title = "CountdownWindow_" + System.currentTimeMillis();
            setTitle(title);
        }
        return USER32.FindWindow(null, title);
    }

    /**
     * 通过枚举窗口查找本进程的窗口
     */
    private WinDef.HWND getHWNDByEnum() {
        final int pid = (int) ProcessHandle.current().pid();
        final String targetTitle = getTitle();
        final WinDef.HWND[] result = {null};

        USER32.EnumWindows((hwnd, data) -> {
            // 检查进程ID
            IntByReference windowPid = new IntByReference();
            USER32.GetWindowThreadProcessId(hwnd, windowPid);

            if (windowPid.getValue() == pid) {
                // 获取窗口标题
                char[] titleChars = new char[256];
                USER32.GetWindowText(hwnd, titleChars, 256);
                String windowTitle = new String(titleChars).trim();

                if (windowTitle.equals(targetTitle)) {
                    // 检查是否有父窗口（顶层窗口没有父窗口）
                    WinDef.HWND parent = USER32.GetParent(hwnd);
                    if (parent == null || parent.getPointer() == Pointer.NULL) {
                        result[0] = hwnd;
                        return false;
                    }
                }
            }
            return true;
        }, null);

        return result[0];
    }

    /**
     * 获取窗口句柄
     */
    private WinDef.HWND getHWND() {
        // 方法1：通过标题直接查找
        WinDef.HWND hwnd = getHWNDByTitle();

        // 方法2：通过枚举查找
        if (hwnd == null || hwnd.getPointer() == Pointer.NULL) {
            hwnd = getHWNDByEnum();
        }

        return hwnd;
    }

    /**
     * 初始化桌面模式
     */
    private void initDesktopMode() {
        // 标记为桌面模式
        isDesktopMode = true;

        // 1. 设置唯一标题
        String uniqueTitle = "CountdownWidget_" + System.currentTimeMillis();
        setTitle(uniqueTitle);
        // 2. 显示窗口
        setVisible(true);


        // 3. 等待窗口完全创建
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        // 4. 获取窗口句柄
        WinDef.HWND hwnd = getHWND();

        if (hwnd == null || hwnd.getPointer() == Pointer.NULL) {
            LOG.error("无法获取窗口句柄");
            return;
        }

        LOG.info("成功获取窗口句柄: {}", hwnd.getPointer());


        // 6. 嵌入桌面
        DesktopIntegration.embedToDesktop(hwnd, getX(), getY(), getWidth(), getHeight());
    }
    /**
     * 进入桌面模式
     */
    public void enterDesktopMode() {
        // 保存当前位置用于恢复
        int x = getX();
        int y = getY();

        isDesktopMode = true;
        // 保存桌面模式状态到配置
        if (cachedConfig != null) {
            cachedConfig.setDesktopMode(true);
            AppConfig.saveConfig(cachedConfig);
        }
        // 重建窗口进入桌面模式
        setVisible(false);
        dispose();
        initDesktopMode();
        setLocation(x, y);  // 恢复位置
        setVisible(true);
    }

    /**
     * 退出桌面模式
     */
    private void exitDesktopMode() {
        // 保存当前位置用于恢复
        int x = getX();
        int y = getY();

        isDesktopMode = false;
        // 保存桌面模式状态到配置
        if (cachedConfig != null) {
            cachedConfig.setDesktopMode(false);
            AppConfig.saveConfig(cachedConfig);
        }
        // 重建窗口退出桌面模式
        setVisible(false);
        dispose();
        // 重新创建普通窗口
        setVisible(true);
        setLocation(x, y);  // 恢复位置
    }
}
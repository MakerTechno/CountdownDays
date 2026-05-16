package works.makertechno.countdown.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import works.makertechno.countdown.i18n.I18n;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 开机自启动管理器
 * 使用 VBScript 实现完全静默启动（无任何窗口闪烁）
 */
public class AutoStartManager {

    private static final Logger LOG = LoggerFactory.getLogger(AutoStartManager.class);

    // Windows 启动文件夹路径
    private static final String STARTUP_FOLDER = System.getProperty("user.home") + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";
    private static final String SCRIPT_NAME = "CountdownApp_startup.vbs";

    /**
     * 检查是否已设置开机自启动
     */
    public static boolean isAutoStartEnabled() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            return false;
        }

        Path startupScript = Paths.get(STARTUP_FOLDER, SCRIPT_NAME);
        boolean exists = Files.exists(startupScript);
        LOG.debug("开机自启动状态: {}", exists);
        return exists;
    }

    /**
     * 设置开机自启动
     */
    public static boolean setAutoStart(boolean enabled, String jarPath, java.awt.Component parent, I18n i18n) {
        String os = System.getProperty("os.name").toLowerCase();

        if (!os.contains("win")) {
            JOptionPane.showMessageDialog(parent,
                i18n.get("settings.auto_start_unsupported"),
                i18n.get("settings.auto_start"), JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        if (enabled) {
            return generateStartupScript(jarPath, parent, i18n);
        } else {
            return removeStartupScript(parent, i18n);
        }
    }

    /**
     * 生成 VBScript 启动脚本（完全静默）
     */
    private static boolean generateStartupScript(String jarPath, java.awt.Component parent, I18n i18n) {
        try {
            // 修复路径格式
            if (jarPath.startsWith("/") && jarPath.contains(":/")) {
                jarPath = jarPath.substring(1);
            }

            File jarFile = new File(jarPath);
            if (!jarFile.exists() || !jarPath.endsWith(".jar")) {
                String userDir = System.getProperty("user.dir");
                Path fallbackPath = Paths.get(userDir, "countdown-app.jar");
                if (Files.exists(fallbackPath)) {
                    jarPath = fallbackPath.toString();
                    jarFile = new File(jarPath);
                } else {
                    JOptionPane.showMessageDialog(parent,
                        i18n.get("settings.auto_start_no_jar"),
                        i18n.get("settings.auto_start"), JOptionPane.ERROR_MESSAGE);
                    LOG.error("无效的 jar 路径: {}", jarPath);
                    return false;
                }
            }

            // 获取 Java 路径
            String javaHome = System.getProperty("java.home");
            String javawPath = javaHome + "\\bin\\javaw.exe";

            Path startupScriptPath = Paths.get(STARTUP_FOLDER, SCRIPT_NAME);

        // VBScript 内容 - 使用 Chr(34) 代替双引号，避免转义问题
            String vbsContent =
                "WScript.Sleep 5000\n" +
                "Dim shell\n" +
                "Set shell = CreateObject(\"WScript.Shell\")\n" +
                "Dim cmd\n" +
                "cmd = Chr(34) + \"" + javawPath + "\" + Chr(34) + \" -jar \" + Chr(34) + \"" + jarPath + "\" + Chr(34)\n" +
                "shell.Run cmd, 0, False\n";

            Files.writeString(startupScriptPath, vbsContent);
            LOG.info("已生成 VBScript 启动脚本: {}", startupScriptPath);

            JOptionPane.showMessageDialog(parent,
                "开机自启动已启用！\n\n" +
                    "重启电脑后，程序将在延迟5秒后自动启动，无任何窗口闪烁。",
                i18n.get("settings.auto_start"), JOptionPane.INFORMATION_MESSAGE);

            return true;

        } catch (IOException e) {
            LOG.error("生成启动脚本失败", e);
            JOptionPane.showMessageDialog(parent,
                "生成启动脚本失败：" + e.getMessage(),
                i18n.get("settings.auto_start"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * 删除启动脚本
     */
    private static boolean removeStartupScript(java.awt.Component parent, I18n i18n) {
        try {
            Path startupScriptPath = Paths.get(STARTUP_FOLDER, SCRIPT_NAME);

            if (Files.exists(startupScriptPath)) {
                Files.delete(startupScriptPath);
                LOG.info("已删除启动脚本: {}", startupScriptPath);
            }

            JOptionPane.showMessageDialog(parent,
                "开机自启动已禁用！",
                i18n.get("settings.auto_start"), JOptionPane.INFORMATION_MESSAGE);

            return true;

        } catch (IOException e) {
            LOG.error("删除启动脚本失败", e);
            JOptionPane.showMessageDialog(parent,
                "删除启动脚本失败：" + e.getMessage(),
                i18n.get("settings.auto_start"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * 获取当前运行的 jar 路径
     */
    public static String getCurrentJarPath(java.awt.Component parent, I18n i18n) {
        try {
            String path = AutoStartManager.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()
                .getPath();

            if (path.startsWith("/") && path.contains(":/")) {
                path = path.substring(1);
            }

            if (path.contains("%20")) {
                path = path.replace("%20", " ");
            }

            if (path.endsWith(".jar")) {
                return path;
            }

            String userDir = System.getProperty("user.dir");
            Path fallbackPath = Paths.get(userDir, "countdown-app.jar");
            if (Files.exists(fallbackPath)) {
                return fallbackPath.toString();
            }

            JOptionPane.showMessageDialog(parent,
                i18n.get("settings.auto_start_ide_mode"),
                i18n.get("settings.auto_start"), JOptionPane.WARNING_MESSAGE);
            return null;

        } catch (Exception e) {
            LOG.error("获取当前 jar 路径失败", e);
            JOptionPane.showMessageDialog(parent,
                i18n.get("settings.auto_start_path_error") + "\n" + e.getMessage(),
                i18n.get("settings.auto_start"), JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
}
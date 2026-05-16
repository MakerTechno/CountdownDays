package works.makertechno.countdown.util;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import works.makertechno.countdown.i18n.I18n;

import javax.swing.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 开机自启动管理器
 * 使用 Windows 注册表实现（无需管理员权限）
 */
public class AutoStartManager {

    private static final Logger LOG = LoggerFactory.getLogger(AutoStartManager.class);

    // 注册表路径（当前用户，不需要管理员权限）
    private static final String REGISTRY_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String REGISTRY_VALUE = "CountdownApp";

    /**
     * 检查是否已设置开机自启动（通过注册表）
     */
    public static boolean isAutoStartEnabled() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            return false;
        }

        try {
            return Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, REGISTRY_KEY, REGISTRY_VALUE);
        } catch (Exception e) {
            LOG.warn("检查注册表失败", e);
            return false;
        }
    }

    /**
     * 设置开机自启动
     * @param enabled true=启用，false=禁用
     * @param jarPath 当前运行的 jar 包路径（启用时需要）
     * @param parent 父窗口，用于显示弹窗
     * @param i18n 国际化实例
     * @return 是否操作成功
     */
    public static boolean setAutoStart(boolean enabled, String jarPath, java.awt.Component parent, I18n i18n) {
        String os = System.getProperty("os.name").toLowerCase();

        if (!os.contains("win")) {
            JOptionPane.showMessageDialog(parent,
                    i18n.get("settings.auto_start_unsupported"),
                    i18n.get("settings.auto_start"), JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        try {
            if (enabled) {
                return enableAutoStart(jarPath, parent, i18n);
            } else {
                return disableAutoStart(parent, i18n);
            }
        } catch (Exception e) {
            LOG.error("设置开机自启动失败", e);
            JOptionPane.showMessageDialog(parent,
                    "设置失败：" + e.getMessage(),
                    i18n.get("settings.auto_start"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * 启用开机自启动（写入注册表）
     */
    private static boolean enableAutoStart(String jarPath, java.awt.Component parent, I18n i18n) {
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

        // 写入注册表：java -jar "路径"
        String command = "java -jar \"" + jarPath + "\"";
        Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, REGISTRY_KEY, REGISTRY_VALUE, command);

        LOG.info("已写入注册表: {} = {}", REGISTRY_KEY + "\\" + REGISTRY_VALUE, command);
        JOptionPane.showMessageDialog(parent,
                i18n.get("settings.auto_start_success"),
                i18n.get("settings.auto_start"), JOptionPane.INFORMATION_MESSAGE);
        return true;
    }

    /**
     * 禁用开机自启动（删除注册表项）
     */
    private static boolean disableAutoStart(java.awt.Component parent, I18n i18n) {
        if (Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, REGISTRY_KEY, REGISTRY_VALUE)) {
            Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, REGISTRY_KEY, REGISTRY_VALUE);
            LOG.info("已删除注册表项: {}", REGISTRY_KEY + "\\" + REGISTRY_VALUE);
        }

        JOptionPane.showMessageDialog(parent,
                i18n.get("settings.auto_start_disabled"),
                i18n.get("settings.auto_start"), JOptionPane.INFORMATION_MESSAGE);
        return true;
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
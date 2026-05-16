package works.makertechno.countdown;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import works.makertechno.countdown.config.AppConfig;
import works.makertechno.countdown.config.AppConfig.ConfigData;
import works.makertechno.countdown.i18n.I18n;
import works.makertechno.countdown.model.CountdownData;
import works.makertechno.countdown.render.DefaultTextRenderer;
import works.makertechno.countdown.render.MouseFollowingGorse;
import works.makertechno.countdown.ui.CountdownWindow;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class CountdownApp {

    private static final Logger LOG = LoggerFactory.getLogger(CountdownApp.class);

    private static final DateTimeFormatter[] FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
    };

    public static void main(String[] args) {
        LOG.info("倒计时工具启动");

        I18n i18n = new I18n();

        // 尝试从配置文件加载
        ConfigData savedConfig = AppConfig.loadConfig();

        CountdownData data;
        Color textColor;
        Path transPath = null;
        boolean showYearsMonthsDays;

        // 命令行参数优先级最高
        if (args.length >= 2) {
            showYearsMonthsDays = false;
            data = parseFromArgs(args[0], args[1]);
            textColor = new Color(255, 255, 255, 230);
            LOG.info("使用命令行参数");
        } else if (savedConfig != null) {
            // 使用保存的配置
            String eventName = savedConfig.getEventName();
            LocalDateTime targetTime = savedConfig.getTargetTime();

            // 兼容旧配置文件：如果 eventName 为 null 或空，使用默认值
            if (eventName == null || eventName.trim().isEmpty()) {
                eventName = "2026高考";
                LOG.warn("配置中的 eventName 为空，使用默认值: {}", eventName);
            }

            // 如果 targetTime 为 null，使用默认值
            if (targetTime == null) {
                targetTime = LocalDateTime.of(2026, 6, 7, 9, 0, 0);
                LOG.warn("配置中的 targetTime 为空，使用默认值");
            }

            data = new CountdownData(eventName, targetTime);
            textColor = savedConfig.getTextColor();
            showYearsMonthsDays = savedConfig.isShowYearsMonthsDays();
            if (savedConfig.getTransPath() != null && !savedConfig.getTransPath().isEmpty()) {
                transPath = Path.of(savedConfig.getTransPath());
                boolean loaded = i18n.load(transPath);
                if (!loaded) {
                    JOptionPane.showMessageDialog(null,
                            i18n.get("settings.language_load_fail"),
                            i18n.get("settings.language"), JOptionPane.ERROR_MESSAGE);
                    transPath = null; // 回退到内置
                }
            }
            LOG.info("使用已保存的配置：事件={}, 目标时间={}, 颜色={}, 显示模式={}",
                    eventName, targetTime, textColor, showYearsMonthsDays ? "年月日" : "总天数");
        } else {
            showYearsMonthsDays = false;
            // 默认配置
            LOG.warn("无保存配置，使用默认值：2026年高考");
            data = new CountdownData("2026高考", LocalDateTime.of(2026, 6, 7, 9, 0, 0));
            textColor = new Color(255, 255, 255, 230);
        }

        final Color finalTextColor = textColor;
        final Path finalTransPath = transPath;

        final boolean finalDesktopMode = savedConfig != null && savedConfig.isDesktopMode();  // 获取桌面模式状态

        SwingUtilities.invokeLater(() -> {
            CountdownWindow window = new CountdownWindow(i18n, data, finalTextColor, finalTransPath,
                    showYearsMonthsDays);
            window.setTextRenderer(new DefaultTextRenderer());
            window.setWindowFilter(new MouseFollowingGorse());

            // 如果上次是桌面模式，自动进入
            if (finalDesktopMode) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignore) {}
                window.enterDesktopMode();
            } else {
                window.setVisible(true);
            }
        });
    }

    private static CountdownData parseFromArgs(String userDataText, String timeStr) {
        // 确保 userDataText 不为 null 或空
        if (userDataText == null || userDataText.trim().isEmpty()) {
            userDataText = "命令行事件";
            LOG.warn("命令行事件名称为空，使用默认值");
        }

        LocalDateTime targetTime = null;
        for (DateTimeFormatter fmt : FORMATTERS) {
            try {
                targetTime = LocalDateTime.parse(timeStr, fmt);
                break;
            } catch (DateTimeParseException ignored) {
            }
        }

        if (targetTime == null) {
            LOG.error("无法解析时间字符串：{}，使用默认值", timeStr);
            targetTime = LocalDateTime.of(2026, 6, 7, 9, 0, 0);
        }

        if (targetTime.isBefore(LocalDateTime.now())) {
            LOG.warn("目标时间 {} 已过期，将显示全零倒计时", targetTime);
        }

        return new CountdownData(userDataText, targetTime);
    }
}
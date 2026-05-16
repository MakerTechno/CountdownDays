package works.makertechno.countdown.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * Windows: C:\Users\你的用户名\MakerTechnoApps\Countdown\config.json
 * macOS: /Users/你的用户名/MakerTechnoApps/Countdown/config.json
 * Linux: /home/你的用户名/MakerTechnoApps/Countdown/config.json
 */
public class AppConfig {

    private static final Logger LOG = LoggerFactory.getLogger(AppConfig.class);
    private static final String BASE_DIR = "MakerTechnoApps";
    private static final String APP_DIR = "Countdown";
    private static final String CONFIG_FILE = "config.json";

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);  // 忽略未知字段

    private static Path getConfigPath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, BASE_DIR, APP_DIR, CONFIG_FILE);
    }

    private static Path getConfigDir() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, BASE_DIR, APP_DIR);
    }

    public static void saveConfig(ConfigData config) {
        try {
            Path configDir = getConfigDir();
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            Path configPath = getConfigPath();
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(configPath.toFile(), config);

            LOG.info("配置已保存至: {}", configPath);
        } catch (IOException e) {
            LOG.error("保存配置文件失败", e);
        }
    }

    public static ConfigData loadConfig() {
        Path configPath = getConfigPath();
        if (!Files.exists(configPath)) {
            LOG.info("配置文件不存在，使用默认配置");
            return null;
        }

        try {
            ConfigData config = objectMapper.readValue(configPath.toFile(), ConfigData.class);
            LOG.info("配置已加载: {}", configPath);
            return config;
        } catch (IOException e) {
            LOG.error("加载配置文件失败", e);
            // 如果加载失败，删除损坏的配置文件
            try {
                LOG.warn("删除损坏的配置文件: {}", configPath);
                Files.deleteIfExists(configPath);
            } catch (IOException ex) {
                LOG.error("删除损坏配置文件失败", ex);
            }
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static class ConfigData {
        private String eventName;
        private LocalDateTime targetTime;
        private int textColorR = 255;
        private int textColorG = 255;
        private int textColorB = 255;
        private int textColorA = 230;
        private String transPath;
        private boolean showYearsMonthsDays = true;
        // 窗口位置
        private int windowX = -999;
        private int windowY = -999;
        // 是否桌面模式
        private boolean desktopMode = false;
        // 是否开机自启动
        private boolean autoStart = false;
        // 自定义组件路径
        private String customTextRendererPath;
        private String customWindowFilterPath;

        // 无参构造函数（Jackson 需要）
        public ConfigData() {
        }

        // 全参构造函数（带注解，用于反序列化）
        @JsonCreator
        public ConfigData(
                @JsonProperty("eventName") String eventName,
                @JsonProperty("targetTime") LocalDateTime targetTime,
                @JsonProperty("textColorR") int textColorR,
                @JsonProperty("textColorG") int textColorG,
                @JsonProperty("textColorB") int textColorB,
                @JsonProperty("textColorA") int textColorA,
                @JsonProperty("transPath") String transPath,
                @JsonProperty("showYearsMonthsDays") boolean showYearsMonthsDays,
                @JsonProperty("windowX") int windowX,
                @JsonProperty("windowY") int windowY,
                @JsonProperty("desktopMode") boolean desktopMode,
                @JsonProperty("autoStart") boolean autoStart,
                @JsonProperty("customTextRendererPath") String customTextRendererPath,
                @JsonProperty("customWindowFilterPath") String customWindowFilterPath) {
            this.eventName = eventName;
            this.targetTime = targetTime;
            this.textColorR = textColorR;
            this.textColorG = textColorG;
            this.textColorB = textColorB;
            this.textColorA = textColorA;
            this.transPath = transPath;
            this.showYearsMonthsDays = showYearsMonthsDays;
            this.windowX = windowX;
            this.windowY = windowY;
            this.desktopMode = desktopMode;
            this.autoStart = autoStart;
            this.customTextRendererPath = customTextRendererPath;
            this.customWindowFilterPath = customWindowFilterPath;
        }

        // 便捷构造函数
        public ConfigData(String eventName, LocalDateTime targetTime, Color textColor, String transPath, boolean showYearsMonthsDays) {
            this.eventName = eventName;
            this.targetTime = targetTime;
            if (textColor != null) {
                this.textColorR = textColor.getRed();
                this.textColorG = textColor.getGreen();
                this.textColorB = textColor.getBlue();
                this.textColorA = textColor.getAlpha();
            }
            this.transPath = transPath;
            this.showYearsMonthsDays = showYearsMonthsDays;
        }

        // Getters and Setters
        public String getEventName() {
            return eventName;
        }

        public void setEventName(String eventName) {
            this.eventName = eventName;
        }

        public LocalDateTime getTargetTime() {
            return targetTime;
        }

        public void setTargetTime(LocalDateTime targetTime) {
            this.targetTime = targetTime;
        }

        public int getTextColorR() {
            return textColorR;
        }

        public void setTextColorR(int textColorR) {
            this.textColorR = textColorR;
        }

        public int getTextColorG() {
            return textColorG;
        }

        public void setTextColorG(int textColorG) {
            this.textColorG = textColorG;
        }

        public int getTextColorB() {
            return textColorB;
        }

        public void setTextColorB(int textColorB) {
            this.textColorB = textColorB;
        }

        public int getTextColorA() {
            return textColorA;
        }

        public void setTextColorA(int textColorA) {
            this.textColorA = textColorA;
        }

        public Color getTextColor() {
            return new Color(textColorR, textColorG, textColorB, textColorA);
        }

        public String getTransPath() {
            return transPath;
        }

        public void setTransPath(String transPath) {
            this.transPath = transPath;
        }

        public boolean isShowYearsMonthsDays() {
            return showYearsMonthsDays;
        }

        public void setShowYearsMonthsDays(boolean showYearsMonthsDays) {
            this.showYearsMonthsDays = showYearsMonthsDays;
        }

        // 窗口位置相关的 getter/setter
        public int getWindowX() {
            return windowX;
        }

        public void setWindowX(int windowX) {
            this.windowX = windowX;
        }

        public int getWindowY() {
            return windowY;
        }

        public void setWindowY(int windowY) {
            this.windowY = windowY;
        }
        public String getCustomTextRendererPath() { return customTextRendererPath; }
        public void setCustomTextRendererPath(String customTextRendererPath) { this.customTextRendererPath = customTextRendererPath; }

        public String getCustomWindowFilterPath() { return customWindowFilterPath; }
        public void setCustomWindowFilterPath(String customWindowFilterPath) { this.customWindowFilterPath = customWindowFilterPath; }
        // 桌面模式
        public boolean isDesktopMode() {
            return desktopMode;
        }

        public void setDesktopMode(boolean desktopMode) {
            this.desktopMode = desktopMode;
        }

        // 开机自启动
        public boolean isAutoStart() {
            return autoStart;
        }

        public void setAutoStart(boolean autoStart) {
            this.autoStart = autoStart;
        }

        /**
         * 获取窗口位置 Point 对象
         */
        @JsonIgnore
        public Point getWindowPosition() {
            if (windowX == -999 && windowY == -999) return null;
            return new Point(windowX, windowY);
        }

        /**
         * 设置窗口位置
         */
        @JsonIgnore
        public void setWindowPosition(Point position) {
            if (position != null) {
                this.windowX = position.x;
                this.windowY = position.y;
            }
        }
    }
}
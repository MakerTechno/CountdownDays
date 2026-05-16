package works.makertechno.countdown.i18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class I18n {

    private static final Logger LOG = LoggerFactory.getLogger(I18n.class);
    private static final Pattern VAR_DECL_PATTERN = Pattern.compile("^\\$\\$(\\w+)\\s*=\\s*'([^']*)'\\s*$");
    private static final Pattern VAR_REF_PATTERN = Pattern.compile("\\{\\$(\\w+)\\}");

    private final Map<String, String> translations = new HashMap<>();
    private final Map<String, String> defaultVariables = new HashMap<>();

    public I18n() {
        loadBuiltinZh();
    }

    private void loadBuiltinZh() {
        // 主界面翻译
        translations.put("countdown.text", "离{$EventName}仅剩{$Time}");
        translations.put("settings", "设置");
        translations.put("close", "关闭");

        // 设置对话框翻译
        translations.put("settings.title", "设置");
        translations.put("settings.variables", "内建变量");
        translations.put("settings.display_name", "显示名称");
        translations.put("settings.time", "时间设置");
        translations.put("settings.color", "颜色设置");
        translations.put("settings.language", "语言设置");
        translations.put("settings.reset_language", "恢复默认");
        translations.put("settings.builtin", "（内置）");
        translations.put("settings.language_load_fail", "加载翻译文件失败，已回退到内置语言。");
        translations.put("settings.change_color", "更改颜色...");
        translations.put("settings.select_file", "选择文件...");
        translations.put("settings.time_label", "选择目标时间");
        translations.put("settings.year", "年");
        translations.put("settings.month", "月");
        translations.put("settings.day", "日");
        translations.put("settings.hour", "时");
        translations.put("settings.minute", "分");
        translations.put("settings.second", "秒");
        translations.put("settings.confirm", "确认");
        translations.put("settings.cancel", "取消");

        // 颜色选择器翻译
        translations.put("settings.red", "红");
        translations.put("settings.green", "绿");
        translations.put("settings.blue", "蓝");
        translations.put("settings.alpha", "透明度");
        translations.put("settings.hex", "HEX");
        translations.put("settings.pick_color", "选取颜色");

        // 提示信息翻译
        translations.put("settings.time_must_future", "目标时间必须在未来");
        translations.put("settings.invalid_hex", "无效的HEX值");
        translations.put("settings.filter_trans", "翻译文件 (*.trans)");

        // 显示模式翻译
        translations.put("settings.display_mode", "显示模式");
        translations.put("settings.show_ymd", "显示年月日（零值省略）");
        translations.put("settings.show_total_days", "显示总天数（始终显示为天）");

        // 桌面模式翻译
        translations.put("settings.desktop_mode", "桌面模式");

        // 开机自启动翻译
        translations.put("settings.auto_start", "开机自启动");
        translations.put("settings.auto_start_tip", "开机自动启动本程序");
        translations.put("settings.auto_start_confirm", "是否设置开机自启动？\n当前 jar 包位置：\n{0}\n\n如果移动了 jar 包位置，需要重新设置。");
        translations.put("settings.auto_start_need_admin", "已生成安装脚本，请右键以管理员身份运行。\\n脚本位置：\\n{0}");

        // 开机自启动相关提示
        translations.put("settings.auto_start_unsupported", "开机自启动功能目前仅支持 Windows 系统。");
        translations.put("settings.auto_start_no_jar", "无法设置开机自启动：未找到可执行的 jar 文件。\n请确保程序以 jar 包形式运行。");
        translations.put("settings.auto_start_fail", "设置开机自启动失败，请检查是否有写入权限。");
        translations.put("settings.auto_start_disable_fail", "取消开机自启动失败，请手动删除启动文件夹中的脚本。");
        translations.put("settings.auto_start_success", "开机自启动已启用。");
        translations.put("settings.auto_start_disabled", "开机自启动已禁用。");
        translations.put("settings.auto_start_ide_mode", "当前在 IDE 调试模式下运行，无法设置开机自启动。\n请先打包为 jar 文件后再运行。");
        translations.put("settings.auto_start_path_error", "获取程序路径失败。");

        // 时间格式翻译
        translations.put("time.year", "{$value}年");
        translations.put("time.month", "{$value}月");
        translations.put("time.day", "{$value}日");
        translations.put("time.hms", "{$hour}时{$minute}分{$second}秒");
        translations.put("time.total", "{$day}天{$hour}小时{$minute}分钟{$second}秒");

        // 自定义组件翻译
        translations.put("component.title", "自定义渲染组件");
        translations.put("component.text_renderer", "文本渲染器");
        translations.put("component.window_filter", "窗口滤镜");
        translations.put("component.load", "加载 ZIP");
        translations.put("component.unload", "卸载");
        translations.put("component.status", "状态");
        translations.put("component.none", "无");
        translations.put("component.load_success", "组件加载成功，完成设置并确定后生效。");
        translations.put("component.load_fail", "组件加载失败");
        translations.put("component.unload_confirm", "确定要卸载当前组件吗？");
        translations.put("component.unload_success", "组件已卸载");

        LOG.info("已加载内置中文翻译，共 {} 条", translations.size());
    }

    public boolean load(Path path) {
        if (!Files.exists(path)) {
            LOG.warn("翻译文件不存在: {}", path);
            return false;
        }
        try {
            Map<String, String> loaded = new HashMap<>();
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                    Matcher varMatcher = VAR_DECL_PATTERN.matcher(trimmed);
                    if (varMatcher.matches()) {
                        defaultVariables.put(varMatcher.group(1), varMatcher.group(2));
                        continue;
                    }

                    int eqIdx = trimmed.indexOf('=');
                    if (eqIdx > 0) {
                        String key = trimmed.substring(0, eqIdx).trim();
                        String value = trimmed.substring(eqIdx + 1).trim();
                        loaded.put(key, value);
                    }
                }
            }
            translations.putAll(loaded);
            LOG.info("已加载翻译文件：{}，共 {} 条", path, loaded.size());
            return true;
        } catch (IOException e) {
            LOG.error("加载翻译文件失败", e);
            return false;
        }
    }

    /**
     * 重新加载内置中文翻译（用于恢复默认）
     */
    public void reloadBuiltin() {
        translations.clear();
        defaultVariables.clear();
        loadBuiltinZh();
        LOG.info("已恢复内置中文翻译");
    }

    public String get(String key) {
        return translations.getOrDefault(key, "!" + key + "!");
    }

    public String get(String key, Map<String, String> variables) {
        String template = get(key);
        Matcher m = VAR_REF_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String varName = m.group(1);
            String value = variables != null && variables.containsKey(varName)
                    ? variables.get(varName)
                    : defaultVariables.getOrDefault(varName, "");
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public void setDefaultVariable(String name, String value) {
        defaultVariables.put(name, value);
    }

    /**
     * 删除指定内建变量
     */
    public void removeDefaultVariable(String name) {
        defaultVariables.remove(name);
    }

    public Map<String, String> getDefaultVariables() {
        return new HashMap<>(defaultVariables);
    }

    /**
     * 清空并批量设置所有内建变量
     */
    public void setDefaultVariables(Map<String, String> vars) {
        defaultVariables.clear();
        if (vars != null) {
            defaultVariables.putAll(vars);
        }
    }
}

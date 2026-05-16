package works.makertechno.countdown.util;

import works.makertechno.countdown.i18n.I18n;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;

public final class TimeFormatter {

    private TimeFormatter() {
    }

    /**
     * 格式化剩余时间（使用指定的 I18n 实例）
     *
     * @param now                 当前时间
     * @param target              目标时间
     * @param i18n                翻译实例
     * @param showYearsMonthsDays 是否显示年月日（false 则全部转为日时分秒）
     * @return 格式化后的时间字符串
     */
    public static String formatRemaining(LocalDateTime now, LocalDateTime target, I18n i18n, boolean showYearsMonthsDays) {
        if (!now.isBefore(target)) {
            return getLocalizedZeroTime(i18n);
        }

        if (showYearsMonthsDays) {
            // 原有的年月日时分秒格式
            return formatWithYearsMonthsDays(now, target, i18n);
        } else {
            // 全部转为日时分秒格式
            return formatAsTotalDays(now, target, i18n);
        }
    }

    /**
     * 原有格式：年月日时分秒（零值不显示）
     */
    private static String formatWithYearsMonthsDays(LocalDateTime now, LocalDateTime target, I18n i18n) {
        Period period = Period.between(now.toLocalDate(), target.toLocalDate());
        int years = period.getYears();
        int months = period.getMonths();
        int days = period.getDays();

        LocalDateTime adjusted = now.plusYears(years).plusMonths(months).plusDays(days);
        long totalSec = Duration.between(adjusted, target).getSeconds();

        if (totalSec < 0) {
            if (days > 0) {
                days--;
            } else if (months > 0) {
                months--;
                days += now.toLocalDate().lengthOfMonth();
            } else if (years > 0) {
                years--;
                months += 11;
            }
            adjusted = now.plusYears(years).plusMonths(months).plusDays(days);
            totalSec = Math.max(0, Duration.between(adjusted, target).getSeconds());
        }

        int hours = (int) (totalSec / 3600);
        int minutes = (int) ((totalSec % 3600) / 60);
        int seconds = (int) (totalSec % 60);

        // 使用 i18n 构建字符串
        StringBuilder sb = new StringBuilder();
        Map<String, String> vars = new HashMap<>();

        if (years > 0) {
            vars.put("value", String.valueOf(years));
            sb.append(i18n.get("time.year", vars));
        }
        if (months > 0) {
            vars.put("value", String.valueOf(months));
            sb.append(i18n.get("time.month", vars));
        }
        if (days > 0 || (years == 0 && months == 0)) {
            vars.put("value", String.valueOf(days));
            sb.append(i18n.get("time.day", vars));
        }

        vars.put("hour", String.format("%02d", hours));
        vars.put("minute", String.format("%02d", minutes));
        vars.put("second", String.format("%02d", seconds));
        sb.append(i18n.get("time.hms", vars));

        return sb.toString();
    }

    /**
     * 总天数格式：全部转为日时分秒
     */
    private static String formatAsTotalDays(LocalDateTime now, LocalDateTime target, I18n i18n) {
        Duration duration = Duration.between(now, target);
        long totalSeconds = duration.getSeconds();

        if (totalSeconds < 0) {
            totalSeconds = 0;
        }

        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        Map<String, String> vars = new HashMap<>();
        vars.put("day", String.valueOf(days));
        vars.put("hour", String.format("%02d", hours));
        vars.put("minute", String.format("%02d", minutes));
        vars.put("second", String.format("%02d", seconds));

        return i18n.get("time.total", vars);
    }

    /**
     * 获取本地化的零时间显示
     */
    private static String getLocalizedZeroTime(I18n i18n) {
        Map<String, String> vars = new HashMap<>();
        vars.put("hour", "00");
        vars.put("minute", "00");
        vars.put("second", "00");
        return i18n.get("time.hms", vars);
    }

    // ========== 保留旧方法用于兼容 ==========

    /**
     * @deprecated 使用 formatRemaining(now, target, i18n, showYearsMonthsDays) 代替
     */
    @Deprecated
    public static String formatRemaining(LocalDateTime now, LocalDateTime target) {
        return formatRemaining(now, target, null, true);
    }
}
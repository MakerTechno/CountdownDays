package works.makertechno.countdown.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 倒计时数据载体。
 *
 * @param userDataText   用户自定义文本，如 "新年"、"生日"
 * @param targetDateTime 目标日期时间
 */
public record CountdownData(String userDataText, LocalDateTime targetDateTime) {

    public CountdownData {
        Objects.requireNonNull(userDataText, "userDataText 不能为 null");
        Objects.requireNonNull(targetDateTime, "targetDateTime 不能为 null");
    }
}

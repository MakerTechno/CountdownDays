package works.makertechno.countdown.render;

import works.makertechno.countdown.empower.MouseTracker;

import javax.swing.*;
import java.awt.*;

/**
 * 窗口级滤镜接口，在每帧绘制后对整体画面做后处理（如毛玻璃、发光、色彩偏移等）。
 */
@FunctionalInterface
public interface WindowFilter {
    static WindowFilter none() {
        return ((g, width, height, target, tracker) -> {
        });
    }

    /**
     * 对窗口内容应用滤镜。
     *
     * @param g       窗口的 Graphics 对象
     * @param width   窗口宽度
     * @param height  窗口高度
     * @param target  正在渲染的JFrame，可获取部分信息
     * @param tracker 鼠标追踪器，可获取鼠标位置信息
     */
    void apply(Graphics2D g, int width, int height, JFrame target, MouseTracker tracker);

}

package works.makertechno.countdown.render;

import java.awt.*;

/**
 * 文本渲染器接口，后续可实现自定义字体、颜色、阴影、渐变等。
 */
@FunctionalInterface
public interface TextRenderer {
    /**
     * 默认实现：简单白色文字
     */
    static TextRenderer defaultRenderer() {
        return (g, text, bounds, textColor) -> {
            g.setColor(new Color(255, 255, 255, 230));
            g.setFont(new Font("SansSerif", Font.BOLD, 42));
            FontMetrics fm = g.getFontMetrics();
            int x = bounds.x + (bounds.width - fm.stringWidth(text)) / 2;
            int y = bounds.y + (bounds.height - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(text, x, y);
        };
    }

    /**
     * 渲染文本到 Graphics 上下文。
     *
     * @param g         Graphics 对象
     * @param text      要渲染的文本
     * @param bounds    渲染区域
     * @param textColor 字的颜色
     */
    void render(Graphics2D g, String text, Rectangle bounds, Color textColor);
}

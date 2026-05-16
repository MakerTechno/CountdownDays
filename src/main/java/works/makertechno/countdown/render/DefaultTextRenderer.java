package works.makertechno.countdown.render;

import java.awt.*;

public class DefaultTextRenderer implements TextRenderer {
    // 阴影渲染常量
    private static final int SHADOW_OFFSET_X = 3;
    private static final int SHADOW_OFFSET_Y = 3;
    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 140);

    @Override
    public void render(Graphics2D g, String text, Rectangle bounds, Color textColor) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        Font font = new Font("SansSerif", Font.BOLD, 42);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        int x = (int) ((bounds.getWidth() - textWidth) / 2);
        int y = (int) ((bounds.getHeight() - textHeight) / 2 + fm.getAscent());

        // 绘制阴影
        g.setColor(SHADOW_COLOR);
        g.drawString(text, x + SHADOW_OFFSET_X, y + SHADOW_OFFSET_Y);

        // 绘制主文字
        g.setColor(textColor);
        g.drawString(text, x, y);

    }
}

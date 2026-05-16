package works.makertechno.countdown.render;

import works.makertechno.countdown.empower.MouseTracker;

import javax.swing.*;
import java.awt.*;

public class MouseFollowingGorse implements WindowFilter {
    @Override
    public void apply(Graphics2D g, int width, int height, JFrame target, MouseTracker tracker) {
        Point mousePos = tracker.getPosition();
        if (mousePos != null) {
            // 在鼠标位置绘制光晕
            RadialGradientPaint paint = new RadialGradientPaint(
                    mousePos.x, mousePos.y, 50,
                    new float[]{0f, 1f},
                    new Color[]{new Color(255, 255, 0, 100), new Color(255, 255, 0, 0)}
            );
            g.setPaint(paint);
            g.fillOval(mousePos.x - 50, mousePos.y - 50, 100, 100);
        }
    }

}

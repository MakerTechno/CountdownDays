package works.makertechno.countdown.util;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * 可共享访问的鼠标位置记录器
 */
public class MousePositionTracker implements MouseMotionListener {
    private volatile Point currentPosition = new Point(0, 0);

    @Override
    public void mouseMoved(MouseEvent e) {
        currentPosition = e.getPoint(); // 更新为当前鼠标位置
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        currentPosition = e.getPoint(); // 拖动时也更新
    }

    /**
     * 获取当前鼠标在窗口中的位置（返回副本，保证不可变）
     */
    public Point getCurrentPosition() {
        return new Point(currentPosition);
    }
}
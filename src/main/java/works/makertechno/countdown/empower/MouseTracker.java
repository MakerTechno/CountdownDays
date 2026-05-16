package works.makertechno.countdown.empower;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 窗口内鼠标位置追踪器
 */
public class MouseTracker {

    private final AtomicReference<Point> position = new AtomicReference<>(new Point(-1000, -1000));
    private JFrame targetWindow;
    private HWND windowHandle;
    private Timer timer;

    /**
     * 启动追踪器
     *
     * @param window 要追踪的窗口
     */
    public void start(JFrame window) {
        this.targetWindow = window;

        // 获取窗口句柄
        String title = window.getTitle();
        if (title != null && !title.isEmpty()) {
            windowHandle = User32.INSTANCE.FindWindow(null, title);
        }

        // 启动定时器
        timer = new Timer(16, e -> updatePosition());
        timer.start();
    }

    /**
     * 获取当前鼠标位置
     *
     * @return 窗口内的坐标，鼠标在窗口外时返回(-1000, -1000)
     */
    public Point getPosition() {
        return new Point(position.get());
    }

    /**
     * 停止追踪器
     */
    public void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        position.set(new Point(-1000, -1000));
    }

    private void updatePosition() {
        if (targetWindow == null || !targetWindow.isShowing()) {
            position.set(new Point(-1000, -1000));
            return;
        }

        POINT globalPoint = new POINT();
        if (!CustomUser32.INSTANCE.GetCursorPos(globalPoint)) {
            position.set(new Point(-1000, -1000));
            return;
        }

        // 检查鼠标是否在窗口内
        Rectangle windowBounds = targetWindow.getBounds();
        if (!windowBounds.contains(globalPoint.x, globalPoint.y)) {
            position.set(new Point(-1000, -1000));
            return;
        }

        // 转换为窗口客户区坐标
        if (windowHandle != null) {
            POINT clientPoint = new POINT(globalPoint.x, globalPoint.y);
            CustomUser32.INSTANCE.ScreenToClient(windowHandle, clientPoint);
            position.set(new Point(clientPoint.x, clientPoint.y));
        } else {
            // 备用方法
            Point screenPoint = new Point(globalPoint.x, globalPoint.y);
            SwingUtilities.convertPointFromScreen(screenPoint, targetWindow.getContentPane());
            position.set(screenPoint);
        }
    }

    // 自定义User32接口
    @SuppressWarnings("unused")
    private interface CustomUser32 extends User32 {
        CustomUser32 INSTANCE = Native.load("user32", CustomUser32.class);

        boolean ScreenToClient(HWND hWnd, POINT lpPoint);
    }
}
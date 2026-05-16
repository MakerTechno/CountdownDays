package works.makertechno.countdown.empower;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DesktopIntegration {

    private static final User32 USER32 = User32.INSTANCE;
    private static final Logger LOG = LoggerFactory.getLogger(DesktopIntegration.class);

    private static WinDef.HWND getRealDesktopWindow() {
        // 1. 找到 Progman
        WinDef.HWND progman = USER32.FindWindow("Progman", null);
        if (progman == null) return null;

        // 2. 发送 0x052C 消息
        USER32.SendMessageTimeout(progman, 0x052C,
                new WinDef.WPARAM(0), new WinDef.LPARAM(0),
                User32.SMTO_NORMAL, 1000, null);

        // 等待 WorkerW 生成
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignore) {
        }

        // 3. 枚举查找正确的 WorkerW
        WinDef.HWND result = new WinDef.HWND();
        result.setPointer(Pointer.NULL);

        USER32.EnumWindows((hwnd, data) -> {
            char[] className = new char[256];
            USER32.GetClassName(hwnd, className, 256);
            String name = new String(className).trim();

            if ("WorkerW".equals(name)) {
                // FindWindowEx 的参数可以是 null
                WinDef.HWND child = USER32.FindWindowEx(hwnd, null,
                        "SHELLDLL_DefView", null);
                if (child != null) {
                    result.setPointer(hwnd.getPointer());
                    return false;
                }
            }
            return true;
        }, null);

        return result.getPointer() != Pointer.NULL ? result : progman;
    }

    public static void embedToDesktop(WinDef.HWND hwnd, int x, int y, int width, int height) {
        WinDef.HWND desktop = getRealDesktopWindow();

        if (desktop == null) {
            LOG.error("No desktop window found");
            return;
        }

        // 1. 修改窗口样式为子窗口
        int style = USER32.GetWindowLong(hwnd, WinUser.GWL_STYLE);
        style = style & ~WinUser.WS_POPUP;
        style = style | WinUser.WS_CHILD;
        USER32.SetWindowLong(hwnd, WinUser.GWL_STYLE, style);

        // 2. 设置扩展样式：鼠标穿透 + 分层窗口
        int exStyle = USER32.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
        exStyle |= WinUser.WS_EX_TRANSPARENT;  // 鼠标穿透
        exStyle |= WinUser.WS_EX_LAYERED;      // 允许透明（如果窗口本身透明）
        USER32.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle);

        // 3. 设置父窗口为桌面
        USER32.SetParent(hwnd, desktop);

        // 4. 移动窗口并放到底层
        USER32.SetWindowPos(hwnd, new WinDef.HWND(Pointer.createConstant(1)), // HWND_BOTTOM
                x, y, width, height,
                WinUser.SWP_NOACTIVATE | WinUser.SWP_SHOWWINDOW);

        LOG.info("窗口已嵌入桌面底层，已启用鼠标穿透");
    }
}
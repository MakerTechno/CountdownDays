
import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinUser.*;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * 扩展的User32接口，包含JNA标准接口中没有的方法
 */
public interface User32Extended extends StdCallLibrary, User32 {
    
    User32Extended INSTANCE = Native.load("user32", User32Extended.class, 
                                         W32APIOptions.DEFAULT_OPTIONS);
    
    /**
     * 获取指定点所在的窗口句柄
     * @param point 屏幕坐标点
     * @return 窗口句柄
     */
    HWND WindowFromPoint(POINT point);
    
    /**
     * 获取指定窗口的父窗口
     * @param hWnd 窗口句柄
     * @return 父窗口句柄
     */
    HWND GetAncestor(HWND hWnd, int gaFlags);
    
    /**
     * 获取顶层父窗口的标志
     */
    int GA_ROOT = 2;
    int GA_ROOTOWNER = 3;
    
    /**
     * 获取指定点的子窗口（用于更精确的窗口检测）
     */
    HWND ChildWindowFromPoint(HWND hWndParent, POINT point);
    
    /**
     * 获取窗口的类名
     */
    int GetClassName(HWND hWnd, char[] lpClassName, int nMaxCount);
}
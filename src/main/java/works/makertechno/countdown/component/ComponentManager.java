package works.makertechno.countdown.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import works.makertechno.countdown.render.TextRenderer;
import works.makertechno.countdown.render.WindowFilter;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 组件管理器，管理所有已加载的组件
 */
public class ComponentManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(ComponentManager.class);
    
    // 当前加载的 TextRenderer 组件
    private ComponentLoader currentTextRendererLoader;
    private TextRenderer currentTextRenderer;
    
    // 当前加载的 WindowFilter 组件
    private ComponentLoader currentWindowFilterLoader;
    private WindowFilter currentWindowFilter;
    
    // 已保存的 ZIP 路径（从配置读取）
    private String textRendererZipPath;
    private String windowFilterZipPath;
    
    /**
     * 加载 TextRenderer 组件
     * @param zipPath ZIP 文件路径
     * @return 是否加载成功
     */
    public boolean loadTextRenderer(Path zipPath) {
        try {
            // 卸载旧的
            if (currentTextRendererLoader != null) {
                currentTextRendererLoader.unload();
                currentTextRendererLoader = null;
                currentTextRenderer = null;
            }
            
            ComponentLoader loader = new ComponentLoader();
            Object component = loader.loadComponent(zipPath);
            
            if (loader.getTextRenderer() != null) {
                currentTextRendererLoader = loader;
                currentTextRenderer = loader.getTextRenderer();
                textRendererZipPath = zipPath.toString();
                LOG.info("TextRenderer 组件加载成功: {}", loader.getManifest().getName());
                return true;
            } else {
                loader.unload();
                throw new Exception("组件不是 TextRenderer 类型");
            }
        } catch (Exception e) {
            LOG.error("加载 TextRenderer 失败", e);
            return false;
        }
    }
    
    /**
     * 加载 WindowFilter 组件
     * @param zipPath ZIP 文件路径
     * @return 是否加载成功
     */
    public boolean loadWindowFilter(Path zipPath) {
        try {
            // 卸载旧的
            if (currentWindowFilterLoader != null) {
                currentWindowFilterLoader.unload();
                currentWindowFilterLoader = null;
                currentWindowFilter = null;
            }
            
            ComponentLoader loader = new ComponentLoader();
            Object component = loader.loadComponent(zipPath);
            
            if (loader.getWindowFilter() != null) {
                currentWindowFilterLoader = loader;
                currentWindowFilter = loader.getWindowFilter();
                windowFilterZipPath = zipPath.toString();
                LOG.info("WindowFilter 组件加载成功: {}", loader.getManifest().getName());
                return true;
            } else {
                loader.unload();
                throw new Exception("组件不是 WindowFilter 类型");
            }
        } catch (Exception e) {
            LOG.error("加载 WindowFilter 失败", e);
            return false;
        }
    }
    
    /**
     * 卸载 TextRenderer 组件
     */
    public void unloadTextRenderer() {
        if (currentTextRendererLoader != null) {
            currentTextRendererLoader.unload();
            currentTextRendererLoader = null;
            currentTextRenderer = null;
            textRendererZipPath = null;
            LOG.info("TextRenderer 组件已卸载");
        }
    }
    
    /**
     * 卸载 WindowFilter 组件
     */
    public void unloadWindowFilter() {
        if (currentWindowFilterLoader != null) {
            currentWindowFilterLoader.unload();
            currentWindowFilterLoader = null;
            currentWindowFilter = null;
            windowFilterZipPath = null;
            LOG.info("WindowFilter 组件已卸载");
        }
    }
    
    /**
     * 获取当前 TextRenderer（如果没有自定义则返回 null）
     */
    public TextRenderer getTextRenderer() {
        return currentTextRenderer;
    }
    
    /**
     * 获取当前 WindowFilter（如果没有自定义则返回 null）
     */
    public WindowFilter getWindowFilter() {
        return currentWindowFilter;
    }
    
    /**
     * 是否正在使用自定义 TextRenderer
     */
    public boolean hasCustomTextRenderer() {
        return currentTextRenderer != null;
    }
    
    /**
     * 是否正在使用自定义 WindowFilter
     */
    public boolean hasCustomWindowFilter() {
        return currentWindowFilter != null;
    }
    
    /**
     * 获取 TextRenderer 组件信息
     */
    public String getTextRendererInfo() {
        if (currentTextRendererLoader == null || currentTextRendererLoader.getManifest() == null) {
            return "无";
        }
        return currentTextRendererLoader.getComponentInfo();
    }
    
    /**
     * 获取 WindowFilter 组件信息
     */
    public String getWindowFilterInfo() {
        if (currentWindowFilterLoader == null || currentWindowFilterLoader.getManifest() == null) {
            return "无";
        }
        return currentWindowFilterLoader.getComponentInfo();
    }
    
    /**
     * 获取 TextRenderer ZIP 路径
     */
    public String getTextRendererZipPath() {
        return textRendererZipPath;
    }
    
    /**
     * 获取 WindowFilter ZIP 路径
     */
    public String getWindowFilterZipPath() {
        return windowFilterZipPath;
    }
    
    /**
     * 设置 TextRenderer ZIP 路径（用于从配置恢复）
     */
    public void setTextRendererZipPath(String path) {
        this.textRendererZipPath = path;
    }
    
    /**
     * 设置 WindowFilter ZIP 路径（用于从配置恢复）
     */
    public void setWindowFilterZipPath(String path) {
        this.windowFilterZipPath = path;
    }
    
    /**
     * 从配置恢复已保存的组件
     */
    public void restoreFromConfig() {
        if (textRendererZipPath != null && !textRendererZipPath.isEmpty()) {
            Path path = Path.of(textRendererZipPath);
            if (path.toFile().exists()) {
                loadTextRenderer(path);
            }
        }
        if (windowFilterZipPath != null && !windowFilterZipPath.isEmpty()) {
            Path path = Path.of(windowFilterZipPath);
            if (path.toFile().exists()) {
                loadWindowFilter(path);
            }
        }
    }
}
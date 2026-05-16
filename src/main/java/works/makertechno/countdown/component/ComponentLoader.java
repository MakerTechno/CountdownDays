package works.makertechno.countdown.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import works.makertechno.countdown.render.TextRenderer;
import works.makertechno.countdown.render.WindowFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 组件加载器，负责从 ZIP 加载自定义渲染组件
 */
public class ComponentLoader {
    
    private static final Logger LOG = LoggerFactory.getLogger(ComponentLoader.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private CustomClassLoader classLoader;
    private ComponentManifest manifest;
    private Path zipPath;
    private Object loadedComponent;
    
    /**
     * 从 ZIP 文件加载组件
     * @param zipPath ZIP 文件路径
     * @return 加载的组件实例
     * @throws Exception 加载失败时抛出
     */
    public Object loadComponent(Path zipPath) throws Exception {
        // 关闭旧的 ClassLoader
        unload();
        
        this.zipPath = zipPath;
        
        // 检查文件是否存在
        if (!Files.exists(zipPath)) {
            throw new IOException("文件不存在: " + zipPath);
        }
        
        // 创建 ClassLoader
        this.classLoader = new CustomClassLoader(zipPath, Thread.currentThread().getContextClassLoader());
        
        // 读取 manifest.json
        if (!classLoader.hasManifest()) {
            throw new IOException("ZIP 中缺少 manifest.json");
        }
        
        String manifestJson = classLoader.readManifestJson();
        this.manifest = objectMapper.readValue(manifestJson, ComponentManifest.class);
        
        LOG.info("加载组件: {}, 类型: {}, 类名: {}", 
                manifest.getName(), manifest.getType(), manifest.getClassName());
        
        // 加载类并实例化
        Class<?> clazz = classLoader.loadClass(manifest.getClassName());
        loadedComponent = clazz.getDeclaredConstructor().newInstance();
        
        // 验证类型
        validateComponentType();
        
        LOG.info("组件加载成功: {}", manifest.getName());
        return loadedComponent;
    }
    
    /**
     * 验证组件类型是否正确
     */
    private void validateComponentType() throws Exception {
        if (manifest.isTextRenderer() && !(loadedComponent instanceof TextRenderer)) {
            throw new Exception("组件声明为 TEXT_RENDERER 但未实现 TextRenderer 接口");
        }
        if (manifest.isWindowFilter() && !(loadedComponent instanceof WindowFilter)) {
            throw new Exception("组件声明为 WINDOW_FILTER 但未实现 WindowFilter 接口");
        }
        if (!manifest.isTextRenderer() && !manifest.isWindowFilter()) {
            throw new Exception("组件类型无效，必须是 TEXT_RENDERER 或 WINDOW_FILTER");
        }
    }
    
    /**
     * 获取 TextRenderer 实例
     */
    public TextRenderer getTextRenderer() {
        if (manifest != null && manifest.isTextRenderer() && loadedComponent instanceof TextRenderer) {
            return (TextRenderer) loadedComponent;
        }
        return null;
    }
    
    /**
     * 获取 WindowFilter 实例
     */
    public WindowFilter getWindowFilter() {
        if (manifest != null && manifest.isWindowFilter() && loadedComponent instanceof WindowFilter) {
            return (WindowFilter) loadedComponent;
        }
        return null;
    }
    
    /**
     * 获取组件清单
     */
    public ComponentManifest getManifest() {
        return manifest;
    }
    
    /**
     * 获取 ZIP 路径
     */
    public Path getZipPath() {
        return zipPath;
    }
    
    /**
     * 卸载当前组件
     */
    public void unload() {
        if (classLoader != null) {
            classLoader.close();
            classLoader = null;
        }
        manifest = null;
        loadedComponent = null;
        zipPath = null;
        LOG.info("组件已卸载");
    }
    
    /**
     * 是否已加载组件
     */
    public boolean isLoaded() {
        return loadedComponent != null;
    }
    
    /**
     * 获取已加载组件的简单信息（用于 UI 显示）
     */
    public String getComponentInfo() {
        if (manifest == null) {
            return "未加载";
        }
        return String.format("%s v%s - %s", 
                manifest.getName(), 
                manifest.getVersion(), 
                manifest.getDescription());
    }
}
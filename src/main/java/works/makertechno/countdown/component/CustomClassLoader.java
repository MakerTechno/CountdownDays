package works.makertechno.countdown.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 自定义 ClassLoader，从 ZIP 文件中加载类
 */
public class CustomClassLoader extends URLClassLoader {
    
    private static final Logger LOG = LoggerFactory.getLogger(CustomClassLoader.class);
    
    private final Path zipPath;
    private final ZipFile zipFile;
    
    public CustomClassLoader(Path zipPath, ClassLoader parent) throws IOException {
        super(new URL[]{zipPath.toUri().toURL()}, parent);
        this.zipPath = zipPath;
        this.zipFile = new ZipFile(zipPath.toFile());
        LOG.info("创建 CustomClassLoader，加载路径: {}", zipPath);
    }
    
    /**
     * 从 ZIP 中读取类文件
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String classPath = name.replace('.', '/') + ".class";
        ZipEntry entry = zipFile.getEntry(classPath);
        
        if (entry == null) {
            throw new ClassNotFoundException("类未找到: " + name);
        }
        
        try (InputStream is = zipFile.getInputStream(entry)) {
            byte[] classBytes = readAllBytes(is);
            return defineClass(name, classBytes, 0, classBytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException("读取类文件失败: " + name, e);
        }
    }
    
    /**
     * 检查 ZIP 中是否存在 manifest.json
     */
    public boolean hasManifest() {
        return zipFile.getEntry("manifest.json") != null;
    }
    
    /**
     * 读取 manifest.json 内容
     */
    public String readManifestJson() throws IOException {
        ZipEntry entry = zipFile.getEntry("manifest.json");
        if (entry == null) {
            return null;
        }
        try (InputStream is = zipFile.getInputStream(entry)) {
            return new String(readAllBytes(is));
        }
    }
    
    /**
     * 获取 ZIP 中的资源列表
     */
    public Enumeration<? extends ZipEntry> getEntries() {
        return zipFile.entries();
    }
    
    /**
     * 关闭 ClassLoader
     */
    public void close() {
        try {
            zipFile.close();
            super.close();
        } catch (IOException e) {
            LOG.error("关闭 ClassLoader 失败", e);
        }
    }
    
    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }
}
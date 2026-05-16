package works.makertechno.countdown.component;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 组件清单，对应 manifest.json
 */
public class ComponentManifest {
    
    // 组件类型常量
    public static final String TYPE_TEXT_RENDERER = "TEXT_RENDERER";
    public static final String TYPE_WINDOW_FILTER = "WINDOW_FILTER";
    
    private String name;
    private String version;
    private String type;
    private String className;
    private String description;
    private String author;
    
    @JsonCreator
    public ComponentManifest(
            @JsonProperty("name") String name,
            @JsonProperty("version") String version,
            @JsonProperty("type") String type,
            @JsonProperty("className") String className,
            @JsonProperty("description") String description,
            @JsonProperty("author") String author) {
        this.name = name;
        this.version = version;
        this.type = type;
        this.className = className;
        this.description = description;
        this.author = author;
    }
    
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getType() { return type; }
    public String getClassName() { return className; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
    
    public boolean isTextRenderer() {
        return TYPE_TEXT_RENDERER.equals(type);
    }
    
    public boolean isWindowFilter() {
        return TYPE_WINDOW_FILTER.equals(type);
    }
    
    @Override
    public String toString() {
        return String.format("%s v%s - %s", name, version, description);
    }
}
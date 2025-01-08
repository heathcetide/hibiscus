package hibiscus.cetide.app.core.model;

// 创建一个新的类来存储参数信息
public class ParameterInfo {
    private String name;
    private String type;
    private boolean required;
    private String paramType;  // PATH, QUERY, BODY 等
    private String description;
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    
    public String getParamType() { return paramType; }
    public void setParamType(String paramType) { this.paramType = paramType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

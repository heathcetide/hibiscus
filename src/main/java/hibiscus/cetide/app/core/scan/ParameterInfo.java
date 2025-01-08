package hibiscus.cetide.app.core.scan;

public class ParameterInfo {
    private final String name;
    private final String type;
    private final boolean required;
    private final String defaultValue;
    private final String paramType;  // QUERY, PATH, HEADER, BODY
    
    public ParameterInfo(String name, String type, boolean required, 
                        String defaultValue, String paramType) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.defaultValue = defaultValue;
        this.paramType = paramType;
    }
    
    public String getName() { return name; }
    public String getType() { return type; }
    public boolean isRequired() { return required; }
    public String getDefaultValue() { return defaultValue; }
    public String getParamType() { return paramType; }
    
    @Override
    public String toString() {
        return String.format("%s %s (%s) [required=%s, default=%s]", 
                           paramType, name, type, required, defaultValue);
    }
} 
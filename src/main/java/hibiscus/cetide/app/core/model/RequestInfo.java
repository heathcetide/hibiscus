package hibiscus.cetide.app.core.model;

import java.util.List;
import java.util.Map;

public class RequestInfo {
    private String className;
    private String methodName;
    private List<String> paths;
    private Map<String, Object> parameters;
    private String methodType;

    public RequestInfo(String className, String methodName, List<String> paths, 
                      Map<String, Object> parameters, String methodType) {
        this.className = className;
        this.methodName = methodName;
        this.paths = paths;
        this.parameters = parameters;
        this.methodType = methodType;
    }

    // Getters and Setters
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public List<String> getPaths() { return paths; }
    public void setPaths(List<String> paths) { this.paths = paths; }

    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }

    public String getMethodType() { return methodType; }
    public void setMethodType(String methodType) { this.methodType = methodType; }
}


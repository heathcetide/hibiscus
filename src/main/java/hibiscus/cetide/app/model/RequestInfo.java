package hibiscus.cetide.app.model;

import java.util.List;
import java.util.Map;

public class RequestInfo {
    private String className;
    private String methodName;
    private List<String> paths;
    private Map<String, Object> parameters;
    private String params;

    public RequestInfo(String className, String methodName, List<String> paths, Map<String, Object> parameters) {
        this.className = className;
        this.methodName = methodName;
        this.paths = paths;
        this.parameters = parameters;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<String> getPaths() {
        return paths;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }
}

package hibiscus.cetide.app.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Map;

public class RequestInfo {
    @Expose
    private String className;
    @Expose
    private String methodName;
    @Expose
    private List<String> paths;
    @JsonIgnore
    private Map<String, Object> parameters;
    private String params;
    private String requestMethod;

    public RequestInfo(String className, String methodName, List<String> paths, Map<String, Object> parameters, String requestMethod) {
        this.className = className;
        this.methodName = methodName;
        this.paths = paths;
        this.parameters = parameters;
        this.requestMethod = requestMethod;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
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

    @JsonIgnore
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

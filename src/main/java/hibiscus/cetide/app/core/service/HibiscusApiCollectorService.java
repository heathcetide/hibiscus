package hibiscus.cetide.app.core.service;

import hibiscus.cetide.app.core.model.RequestInfo;
import hibiscus.cetide.app.core.scan.MappingHandler;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class HibiscusApiCollectorService {
    
    private final MappingHandler mappingHandler;
    
    public HibiscusApiCollectorService(MappingHandler mappingHandler) {
        this.mappingHandler = mappingHandler;
    }
    
    public Map<String, ApiInfo> getAllApiInfo() {
        Map<String, ApiInfo> apiInfoMap = new HashMap<>();
        List<RequestInfo> requestInfos = mappingHandler.getRequestInfos();
        
        for (RequestInfo requestInfo : requestInfos) {
            String className = requestInfo.getClassName();
            ApiInfo apiInfo = apiInfoMap.computeIfAbsent(className, k -> new ApiInfo(className));
            
            MethodInfo methodInfo = convertToMethodInfo(requestInfo);
            apiInfo.getMethods().add(methodInfo);
        }
        
        return apiInfoMap;
    }
    
    private MethodInfo convertToMethodInfo(RequestInfo requestInfo) {
        MethodInfo methodInfo = new MethodInfo();
        methodInfo.setMethodName(requestInfo.getMethodName());
        methodInfo.setRequestMethod(requestInfo.getMethodType());
        methodInfo.setPaths(requestInfo.getPaths().get(0));
        
        List<ParameterInfo> parameters = new ArrayList<>();
        if (requestInfo.getParameters() != null) {
            requestInfo.getParameters().forEach((name, value) -> {
                if (value != null) {
                    ParameterInfo parameterInfo = new ParameterInfo();
                    parameterInfo.setName(name);
                    parameterInfo.setType(value.toString());
                    parameterInfo.setRequired(true);
                    parameters.add(parameterInfo);
                }
            });
        }
        methodInfo.setParameters(parameters);
        
        return methodInfo;
    }
    
    public Map<String, List<ParameterInfo>> collectMethodParameters(Method method) {
        Map<String, List<ParameterInfo>> result = new HashMap<>();
        Parameter[] parameters = method.getParameters();
        
        for (Parameter parameter : parameters) {
            ParameterInfo paramInfo = new ParameterInfo();
            paramInfo.setName(parameter.getName());
            paramInfo.setType(parameter.getType().getSimpleName());
            
            // 处理参数注解
            RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
            PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
            RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
            
            if (requestParam != null) {
                paramInfo.setParamType("QUERY");
                paramInfo.setRequired(requestParam.required());
                paramInfo.setDescription(requestParam.defaultValue());
            } else if (pathVariable != null) {
                paramInfo.setParamType("PATH");
                paramInfo.setRequired(true);
                paramInfo.setDescription("Path parameter");
            } else if (requestBody != null) {
                paramInfo.setParamType("BODY");
                paramInfo.setRequired(requestBody.required());
                paramInfo.setDescription("Request body");
            }
            
            String paramType = paramInfo.getParamType();
            result.computeIfAbsent(paramType, k -> new ArrayList<>()).add(paramInfo);
        }
        
        return result;
    }
    
    public static class ApiInfo {
        private String className;
        private String classDescription;
        private List<MethodInfo> methods = new ArrayList<>();
        
        public ApiInfo(String className) {
            this.className = className;
            this.classDescription = className;
        }
        
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        
        public String getClassDescription() { return classDescription; }
        public void setClassDescription(String classDescription) { this.classDescription = classDescription; }
        
        public List<MethodInfo> getMethods() { return methods; }
        public void setMethods(List<MethodInfo> methods) { this.methods = methods; }
    }
    
    public static class MethodInfo {
        private String methodName;
        private String requestMethod;
        private String paths;
        private List<ParameterInfo> parameters;
        
        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
        
        public String getRequestMethod() { return requestMethod; }
        public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }
        
        public String getPaths() { return paths; }
        public void setPaths(String paths) { this.paths = paths; }
        
        public List<ParameterInfo> getParameters() { return parameters; }
        public void setParameters(List<ParameterInfo> parameters) { this.parameters = parameters; }
    }
    
    public static class ParameterInfo {
        private String name;
        private String type;
        private boolean required;
        private String paramType;
        private String description;
        
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
} 
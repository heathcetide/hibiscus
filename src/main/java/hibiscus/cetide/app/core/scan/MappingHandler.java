package hibiscus.cetide.app.core.scan;

import hibiscus.cetide.app.core.model.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@Component
public class MappingHandler {
    
    private static final List<RequestInfo> requestInfos = new ArrayList<>();
    private static final Map<String, Object> parameters = new HashMap<>();

    @Autowired
    private MethodParameterProcessor methodParameterProcessor;

    public List<RequestInfo> getRequestInfos() {
        return new ArrayList<>(requestInfos);
    }

    public void handleClass(Class<?> clazz) {
        String classMapping;
        if (clazz.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
            classMapping = requestMapping.value().length > 0 ? requestMapping.value()[0] : "";
        } else {
            classMapping = "";
        }
        
        if (clazz.isAnnotationPresent(RestController.class)) {

        }

        if (clazz.isAnnotationPresent(Controller.class)) {
        }

        if (clazz.isAnnotationPresent(Service.class)){

        }

        // 扫描方法
        Arrays.stream(clazz.getDeclaredMethods()).forEach(method -> {
            if (method.isAnnotationPresent(GetMapping.class)) {
                handleMapping(clazz, method, GetMapping.class, method.getAnnotation(GetMapping.class).value(), classMapping);
            } else if (method.isAnnotationPresent(PostMapping.class)) {
                handleMapping(clazz, method, PostMapping.class, method.getAnnotation(PostMapping.class).value(), classMapping);
            } else if (method.isAnnotationPresent(DeleteMapping.class)) {
                handleMapping(clazz, method, DeleteMapping.class, method.getAnnotation(DeleteMapping.class).value(), classMapping);
            } else if (method.isAnnotationPresent(PutMapping.class)) {
                handleMapping(clazz, method, PutMapping.class, method.getAnnotation(PutMapping.class).value(), classMapping);
            }
        });
    }

    private static void handleMapping(Class<?> aClass, Method method, Class<?> mappingType, String[] paths, String classMapping) {
        String methodType = "";
        if (mappingType == GetMapping.class) {
            methodType = "GET";
        } else if (mappingType == PostMapping.class) {
            methodType = "POST";
        } else if (mappingType == PutMapping.class) {
            methodType = "PUT";
        } else if (mappingType == DeleteMapping.class) {
            methodType = "DELETE";
        }

        for (String path : paths) {
            String fullPath = (classMapping.isEmpty() ? "" : classMapping) + path;
            RequestInfo requestInfo = new RequestInfo(
                    aClass.getName(),
                    method.getName(),
                    Arrays.asList(fullPath),
                    parameters,
                    methodType
            );
            requestInfos.add(requestInfo);

            // 处理方法参数
            Parameter[] methodParameters = method.getParameters();
            for (Parameter parameter : methodParameters) {
                String paramName = parameter.getName();
                Class<?> paramType = parameter.getType();

                // 处理 @RequestParam
                if (parameter.isAnnotationPresent(RequestParam.class)) {
                    RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                    if (!requestParam.value().isEmpty()) {
                        paramName = requestParam.value();
                    }
                    parameters.put(paramName, paramType.getSimpleName());
                    parameters.put(paramName + "_defaultValue", requestParam.defaultValue());
                    parameters.put(paramName + "_required", requestParam.required());
                }

                // 处理 @PathVariable
                if (parameter.isAnnotationPresent(PathVariable.class)) {
                    PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
                    if (!pathVariable.value().isEmpty()) {
                        paramName = pathVariable.value();
                    }
                    parameters.put(paramName, paramType.getSimpleName());
                    parameters.put(paramName + "_defaultValue", pathVariable.value());
                    parameters.put(paramName + "_required", pathVariable.required());
                }

                // 处理 @CookieValue
                if (parameter.isAnnotationPresent(CookieValue.class)) {
                    CookieValue cookieValue = parameter.getAnnotation(CookieValue.class);
                    paramName = cookieValue.value();
                    parameters.put(paramName, paramType.getSimpleName());
                    parameters.put(paramName + "_defaultValue", cookieValue.defaultValue());
                    parameters.put(paramName + "_required", cookieValue.required());
                }

                // 处理 @SessionAttribute
                if (parameter.isAnnotationPresent(SessionAttribute.class)) {
                    SessionAttribute sessionAttribute = parameter.getAnnotation(SessionAttribute.class);
                    paramName = sessionAttribute.value();
                    parameters.put(paramName, paramType.getSimpleName());
                }

                // 处理 @ModelAttribute
                if (parameter.isAnnotationPresent(ModelAttribute.class)) {
                    ModelAttribute modelAttribute = parameter.getAnnotation(ModelAttribute.class);
                    paramName = modelAttribute.value();
                    parameters.put(paramName, paramType.getSimpleName());
                    parameters.put(paramName + "_defaultValue", modelAttribute.value());
                    parameters.put(paramName + "_required", modelAttribute.binding());
                }

                // 处理 @RequestBody
                if (parameter.isAnnotationPresent(RequestBody.class)) {
                    handleRequestBody(paramType, parameters);
                }
            }
        }
    }

    private static void handleRequestBody(Object requestBody, Map<String, Object> parameters) {
        if (requestBody == null) {
            return;
        }

        Class<?> paramType = requestBody.getClass();
        Method[] methods = paramType.getMethods();
        for (Method method : methods) {
            if (method.getName().startsWith("get") && method.getParameterCount() == 0) {
                try {
                    String fieldName = method.getName().substring(3).toLowerCase();
                    Object fieldValue = method.invoke(requestBody);
                    parameters.put(fieldName, fieldValue);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void collectMethodParameters(Method method, Map<String, Object> parameters) {
        Parameter[] methodParams = method.getParameters();
        for (Parameter parameter : methodParams) {
            String paramName = parameter.getName();
            Class<?> paramType = parameter.getType();
            
            // 处理 @RequestParam
            if (parameter.isAnnotationPresent(RequestParam.class)) {
                RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                paramName = requestParam.value().isEmpty() ? paramName : requestParam.value();
                parameters.put(paramName, paramType.getSimpleName());
                parameters.put(paramName + "_defaultValue", requestParam.defaultValue());
                parameters.put(paramName + "_required", requestParam.required());
            }
            
            // 处理 @PathVariable
            if (parameter.isAnnotationPresent(PathVariable.class)) {
                PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
                paramName = pathVariable.value().isEmpty() ? paramName : pathVariable.value();
                parameters.put(paramName, paramType.getSimpleName());
                parameters.put(paramName + "_required", pathVariable.required());
            }
            
            // 处理 @ModelAttribute
            if (parameter.isAnnotationPresent(ModelAttribute.class)) {
                ModelAttribute modelAttribute = parameter.getAnnotation(ModelAttribute.class);
                paramName = modelAttribute.value();
                parameters.put(paramName, paramType.getSimpleName());
                parameters.put(paramName + "_defaultValue", modelAttribute.value());
                parameters.put(paramName + "_required", modelAttribute.binding());
            }
            
            // 处理 @RequestBody
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                handleRequestBody(paramType, parameters);
            }
        }
    }
}

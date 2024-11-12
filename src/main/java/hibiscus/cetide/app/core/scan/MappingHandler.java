package hibiscus.cetide.app.core.scan;

import hibiscus.cetide.app.basic.log.core.LogLevel;
import hibiscus.cetide.app.basic.log.core.Logger;
import hibiscus.cetide.app.common.model.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@Component
public class MappingHandler {

    @Autowired
    private Logger logger;

    public static final List<RequestInfo> requestInfos = new ArrayList<>();

    public static final Map<String, Object> parameters = new HashMap<>();

    @Autowired
    private MethodParameterProcessor methodParameterProcessor;

    public void handleClass(Class<?> clazz) {
        String classMapping;
        if (clazz.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
            classMapping = requestMapping.value().length > 0 ? requestMapping.value()[0] : "";
        } else {
            classMapping = "";
        }
        if (clazz.isAnnotationPresent(RestController.class)) {
//            System.out.println("RestController: " + clazz.getName());
            logger.log(LogLevel.INFO, "RestController: " + clazz.getName());
        }

        if (clazz.isAnnotationPresent(Controller.class)) {
//            System.out.println("Controller: " + clazz.getName());
            logger.log(LogLevel.INFO, "Controller: " + clazz.getName());
        }

        if (clazz.isAnnotationPresent(Service.class)){
//            System.out.println("Service: " + clazz.getName());
//            logger.log(LogLevel.INFO, "Service: " + clazz.getName());
        }
//        if (clazz.isAnnotationPresent(Repository.class)){
//            System.out.println("Repository: " + clazz.getName());
//        }
//        if (clazz.isAnnotationPresent(Mapper.class)){
//            System.out.println("Mapper: " + clazz.getName());
//        }
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
        String methodType = ""; // 请求方法类型 (GET, POST, 等)
        // 根据 mappingType 确定请求类型
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
                    methodType // 添加请求方式
            );
            requestInfos.add(requestInfo);
//            System.out.println(mappingType.getSimpleName() + ": " + method.getName() + " - Full Path: " + fullPath);

            // 处理方法参数
            Parameter[] methodParameters = method.getParameters();
            for (Parameter parameter : methodParameters) {
                String paramName = parameter.getName();  // 获取参数的名字
                Class<?> paramType = parameter.getType();  // 获取参数的类型

                // 处理 @RequestParam
                if (parameter.isAnnotationPresent(RequestParam.class)) {
                    RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                    if (!requestParam.value().isEmpty()) {
                        paramName = requestParam.value();
                    }
                    parameters.put(paramName, paramType.getSimpleName());
                    parameters.put(paramName + "_defaultValue", requestParam.defaultValue());
                    parameters.put(paramName + "_required", requestParam.required());
//                    System.out.println("@RequestParam detected, using param name: " + paramName);
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
//                    System.out.println("@PathVariable detected, using param name: " + paramName);
                }

                // 处理 @CookieValue
                if (parameter.isAnnotationPresent(CookieValue.class)) {
                    CookieValue cookieValue = parameter.getAnnotation(CookieValue.class);
                    paramName = cookieValue.value();
                    parameters.put(paramName, paramType.getSimpleName());
                    parameters.put(paramName + "_defaultValue", cookieValue.defaultValue());
                    parameters.put(paramName + "_required", cookieValue.required());
//                    System.out.println("@CookieValue detected, using param name: " + paramName);
                }

                // 处理 @SessionAttribute
                if (parameter.isAnnotationPresent(SessionAttribute.class)) {
                    SessionAttribute sessionAttribute = parameter.getAnnotation(SessionAttribute.class);
                    paramName = sessionAttribute.value();
                    parameters.put(paramName, paramType.getSimpleName());
//                    System.out.println("@SessionAttribute detected, using param name: " + paramName);
                }

                // 处理 @ModelAttribute
                if (parameter.isAnnotationPresent(ModelAttribute.class)) {
                    ModelAttribute modelAttribute = parameter.getAnnotation(ModelAttribute.class);
                    paramName = modelAttribute.value();
                    parameters.put(paramName, paramType.getSimpleName());
                    parameters.put(paramName + "_defaultValue", modelAttribute.value());
                    parameters.put(paramName + "_required", modelAttribute.binding());
//                    System.out.println("@ModelAttribute detected, using param name: " + paramName);
                }

                // 处理 @RequestBody
                if (parameter.isAnnotationPresent(RequestBody.class)) {
                    // 处理请求体
                    handleRequestBody(paramType, parameters);
//                    System.out.println("@RequestBody detected, class: " + paramType.getName());
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
//    private static void handleRequestBody(Object requestBody, Map<String, Object> parameters) {
//        if (requestBody == null) {
//            return;
//        }
//
//        Class<?> paramType = requestBody.getClass();
//        // 获取类中的字段
//        Field[] fields = paramType.getDeclaredFields();
//        for (Field field : fields) {
//            field.setAccessible(true); // 允许访问私有字段
//            String fieldName = field.getName();
//            try {
//                Object fieldValue = field.get(requestBody); // 获取字段值
//                parameters.put(fieldName, fieldValue);
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//    private static void handleRequestBody(Class<?> paramType, Map<String, Object> parameters) {
//        // 获取类中的字段
//        Field[] fields = paramType.getDeclaredFields();
//        for (Field field : fields) {
//            field.setAccessible(true); // 允许访问私有字段
//            String fieldName = field.getName();
//            parameters.put(fieldName, field.getType().getSimpleName());
////            System.out.println("Field: " + fieldName + " - Type: " + field.getType().getName());
//        }
//    }
}

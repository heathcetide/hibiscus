package hibiscus.cetide.app.core.scan;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Component
public class MethodParameterProcessor {

    public void processParameters(Method method, String fullPath) {
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(RequestParam.class)) {
                handleRequestParam(parameter);
            } else if (parameter.isAnnotationPresent(PathVariable.class)) {
                handlePathVariable(parameter);
            }
            // 处理其他注解
        }
    }

    private void handleRequestParam(Parameter parameter) {
        RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
        String paramName = requestParam.value();
        Class<?> paramType = parameter.getType();
        System.out.println("RequestParam: " + paramName + " - Type: " + paramType.getName());
    }

    private void handlePathVariable(Parameter parameter) {
        PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
        String paramName = pathVariable.value();
        Class<?> paramType = parameter.getType();
        System.out.println("PathVariable: " + paramName + " - Type: " + paramType.getName());
    }

    // 其他参数处理方法...
}

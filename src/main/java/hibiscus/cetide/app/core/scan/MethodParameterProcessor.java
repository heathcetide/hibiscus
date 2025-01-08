package hibiscus.cetide.app.core.scan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Parameter;

@Component
public class MethodParameterProcessor {
    private static final Logger log = LoggerFactory.getLogger(MethodParameterProcessor.class);

    public ParameterInfo processParameter(Parameter parameter) {
        log.debug("Processing parameter: {}", parameter.getName());
        
        String name = parameter.getName();
        String type = parameter.getType().getName();
        boolean required = true;
        String defaultValue = null;
        String paramType = "BODY";  // 默认作为请求体参数
        
        if (parameter.isAnnotationPresent(RequestParam.class)) {
            RequestParam annotation = parameter.getAnnotation(RequestParam.class);
            name = annotation.value().isEmpty() ? parameter.getName() : annotation.value();
            required = annotation.required();
            defaultValue = annotation.defaultValue().equals(ValueConstants.DEFAULT_NONE) ? null : annotation.defaultValue();
            paramType = "QUERY";
            log.debug("Found @RequestParam: name={}, required={}, defaultValue={}", 
                     name, required, defaultValue);
            
        } else if (parameter.isAnnotationPresent(PathVariable.class)) {
            PathVariable annotation = parameter.getAnnotation(PathVariable.class);
            name = annotation.value().isEmpty() ? parameter.getName() : annotation.value();
            required = annotation.required();
            paramType = "PATH";
            log.debug("Found @PathVariable: name={}, required={}", name, required);
            
        } else if (parameter.isAnnotationPresent(RequestHeader.class)) {
            RequestHeader annotation = parameter.getAnnotation(RequestHeader.class);
            name = annotation.value().isEmpty() ? parameter.getName() : annotation.value();
            required = annotation.required();
            defaultValue = annotation.defaultValue().equals(ValueConstants.DEFAULT_NONE) ? null : annotation.defaultValue();
            paramType = "HEADER";
            log.debug("Found @RequestHeader: name={}, required={}, defaultValue={}", 
                     name, required, defaultValue);
            
        } else if (parameter.isAnnotationPresent(RequestBody.class)) {
            RequestBody annotation = parameter.getAnnotation(RequestBody.class);
            required = annotation.required();
            paramType = "BODY";
            log.debug("Found @RequestBody: required={}", required);
        }
        
        return new ParameterInfo(name, type, required, defaultValue, paramType);
    }
}

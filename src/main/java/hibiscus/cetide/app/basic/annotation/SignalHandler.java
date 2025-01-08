package hibiscus.cetide.app.basic.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SignalHandler {
    String value(); // 要处理的信号名称
    int order() default 0; // 处理顺序
}
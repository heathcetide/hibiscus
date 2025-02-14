package hibiscus.cetide.app.basic.annotation;

import hibiscus.cetide.app.component.signal.SignalPriority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SignalEmitter {
    String value() default ""; // 信号名称
    SignalPriority priority() default SignalPriority.MEDIUM;
    boolean async() default true;
}
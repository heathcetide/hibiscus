package hibiscus.cetide.app.basic.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiTest {
    String value() default "";
    String description() default "";
    String[] params() default {};
} 
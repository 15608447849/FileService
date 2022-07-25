package server.undertow;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ServletAnnotation {
    String name();
    String path();
}

package com.wu.spring.annotation.ioc;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Cactus
 */
@Retention(RUNTIME)
@Target(TYPE)
@Component
public @interface Service {
    String value() default "";
}

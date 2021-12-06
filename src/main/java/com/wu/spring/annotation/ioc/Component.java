package com.wu.spring.annotation.ioc;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author dell
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface Component {
	String value() default "";
}

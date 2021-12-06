package com.wu.spring.annotation.mvc;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author dell
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface PathVariable {
	String value() default "";
	
	String defaultValue() default "";
}

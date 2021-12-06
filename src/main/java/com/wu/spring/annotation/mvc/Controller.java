package com.wu.spring.annotation.mvc;

import com.wu.spring.annotation.ioc.Component;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author dell
 */
@Retention(RUNTIME)
@Target(TYPE)
@Component
public @interface Controller {
	String value() default "";
}

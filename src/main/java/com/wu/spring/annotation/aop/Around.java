package com.wu.spring.annotation.aop;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Cactus
 * 环绕注解
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Around {
	String value() default "";
	int order() default -1;
}

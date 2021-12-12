package com.wu.mybatis.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author dell
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface Param {

	String value();

}

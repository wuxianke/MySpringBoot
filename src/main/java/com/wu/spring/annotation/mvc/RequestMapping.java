package com.wu.spring.annotation.mvc;

import com.wu.spring.constants.RequestMethod;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author dell
 */
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
public @interface RequestMapping {
    String value() default "";

    RequestMethod method() default RequestMethod.GET;
}
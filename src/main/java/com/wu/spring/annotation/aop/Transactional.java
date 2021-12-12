package com.wu.spring.annotation.aop;

import com.wu.spring.constants.IsolationLevelConstant;
import com.wu.spring.constants.PropagationLevelConstant;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface Transactional {
    int Isolation() default IsolationLevelConstant.TRANSACTION_REPEATABLE_READ;

    int Propagation() default PropagationLevelConstant.PROPAGATION_REQUIRED;

    Class<? extends Throwable>[] rollbackFor() default {Error.class, RuntimeException.class};


}

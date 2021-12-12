package com.wu.spring.ioc;

import com.wu.spring.annotation.ioc.Component;
import com.wu.spring.annotation.ioc.Configuration;
import com.wu.spring.annotation.ioc.Service;
import com.wu.spring.annotation.mvc.Controller;
import com.wu.spring.constants.ConfigConstant;
import com.wu.spring.utils.ClassUtil;
import com.wu.spring.utils.PropsUtil;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class ClassSetHelper {
    //定义一个Set 存放所有的加载类
    private static final Set<Class<?>> CLASS_SET;
    static{
        Properties props = PropsUtil.loadProps("application.properties");
        String basePackName = PropsUtil.getString(props, ConfigConstant.APP_BASE_PACKAGE);
        CLASS_SET = ClassUtil.getClassSet(basePackName);
    }

    /**
     * 获得所有类对象集合
     * @return 所有类对象集合
     */
    public static Set<Class<?>> getClassSet() {
        return CLASS_SET;
    }


    /**
     * 获得被Component,Service,Controller注解的类对象集合
     * @return 获得被Component,Service,Controller注解的类对象集合
     */
    public static Set<Class<?>> getBeanClassSet() {
        // HashSet避免重复。
        Set<Class<?>> beanClassSet = new HashSet<Class<?>>();
        beanClassSet.addAll(getServiceClassSet());
        beanClassSet.addAll(getControllerClassSet());
        beanClassSet.addAll(getComponentClassSet());
        return beanClassSet;
    }

    /**
     * @return 获得被Component注解的类对象集合
     */
    private static Set<Class<?>> getComponentClassSet() {
        Set<Class<?>> classSet = new HashSet<Class<?>>();
        for (Class<?> clz : CLASS_SET) {
            if (clz.isAnnotationPresent(Component.class)){
                classSet.add(clz);
            }
        }
        return classSet;
    }

    /**
     * @return 获得被Controller注解的类对象集合
     */
    public static Set<Class<?>> getControllerClassSet() {
        Set<Class<?>> classSet = new HashSet<Class<?>>();
        for (Class<?> cls : CLASS_SET) {
            if (cls.isAnnotationPresent(Controller.class)) {
                classSet.add(cls);
            }
        }
        return classSet;
    }

    /**
     * @return 获得被Service注解的类对象集合
     */
    private static Set<Class<?>> getServiceClassSet() {
        Set<Class<?>> classSet = new HashSet<Class<?>>();
        for (Class<?> clz : CLASS_SET) {
            if (clz.isAnnotationPresent(Service.class)){
                classSet.add(clz);
            }
        }
        return classSet;
    }

    public static Set<Class<?>> getClassSetByAnnotation(Class<? extends Annotation> present) {
        Set<Class<?>> classSet = new HashSet<Class<?>>();
        for (Class<?> cls : CLASS_SET) {
            if(cls.isAnnotationPresent(present)){
                classSet.add(cls);
            }
        }
        return classSet;
    }
}

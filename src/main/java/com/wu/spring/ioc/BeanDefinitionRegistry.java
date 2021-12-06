package com.wu.spring.ioc;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Cactus
 * 将bean的定义信息BeanDefinition注册到BeanDefinition容器中
 */
public class BeanDefinitionRegistry {

    private static final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    //注册BeanDefinition

    /**
     * @param beanName bean
     * @param beanDefinition  Bean 实例化信息
     */
    public static void registryBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        // TODO Auto-generated method stub
        Objects.requireNonNull(beanName, "beanName 不能为空");
        Objects.requireNonNull(beanDefinition, "beanDefinition 不能为空");
        beanDefinitionMap.put(beanName, beanDefinition);
    }

    /**
     * @param className 根据类名className获取BeanDefinition
     */
    public static BeanDefinition getBeanDefinition(String className) {
        // TODO Auto-generated method stub
        return beanDefinitionMap.get(className);
    }

    /**
     * @param className 根据类名className 判断BeanDefinition是否存在
     */
    public static boolean containsBeanDefinition(String className) {
        // TODO Auto-generated method stub
        return beanDefinitionMap.containsKey(className);
    }

    /**
     * @return BeanDefinition容器
     */
    public static Map<String, BeanDefinition> getBeanDefinitionMap() {
        return beanDefinitionMap;
    }


}

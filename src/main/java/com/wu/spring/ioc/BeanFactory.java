package com.wu.spring.ioc;

import com.wu.spring.constants.BeanScope;

/**
 * Bean 工厂接口，规定设置获取Bean的相关方法。
 * @author dell
 */
public interface BeanFactory {
    /**
     * 根据类名获得bean
     * @param beanName 根据beanName 获取bean
     * @return bean对象。
     */
    Object getBean(String beanName);

    Object getBean(String beanName, BeanScope beanScope);

    /**
     * @param cls 类对象
     * @param beanScope bean作用域
     * @return bean对象
     * 根据类对象获得bean
     */
    Object getBean(Class<?> cls, BeanScope beanScope);
    Object getBean(Class<?> cls);


    /**
     * @param beanName 通过类名匹配bean
     * @param beanObject bean对象
     * 设置bean
     */
    void setBean(String beanName, Object beanObject);
    void setBean(Class<?> cls, Object beanObject);  //通过类对象匹配。


    /**
     * @throws Exception
     * 刷新，重新注入所有的bean
     */
    void refresh() throws Exception;

    /**
     * @return Bean工厂是否为空
     */
    boolean isEmpty();

}

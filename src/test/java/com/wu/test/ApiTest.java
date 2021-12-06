package com.wu.test;

import com.wu.spring.ioc.*;
import com.wu.test.bean.UserService;
import org.junit.Test;

//public class ApiTest {
//    @Test
//    public void test_BeanFactory() {
//        // 1.初始化 BeanFactory
//        BeanFactory beanFactory = new DefaultBeanFactory();
//        // 2.注册 bean BeanDefinition
//        //UserService userService = new UserService();
//        BeanDefinition beanDefinition = new GenericBeanDefinition();
//        beanDefinition.setBeanClass(UserService.class);
//        BeanDefinitionRegistry beanDefinitionRegistry = new BeanDefinitionRegistry();
//        // 3.获取 bean
//        UserService userService = (UserService) beanFactory.getBean("userService");
//        userService.queryUserInfo();
//    }
//}

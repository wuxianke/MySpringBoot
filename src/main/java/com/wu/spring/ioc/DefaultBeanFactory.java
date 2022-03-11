package com.wu.spring.ioc;

import com.wu.spring.annotation.ioc.*;
import com.wu.spring.annotation.mvc.Controller;
import com.wu.spring.common.MyProxy;
import com.wu.spring.constants.BeanScope;
import com.wu.spring.utils.ConfigUtil;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 获取相关bean方法，以及解决循环依赖问题类。
 * @author Cactus
 */
public class DefaultBeanFactory implements BeanFactory {
    //bean工厂单例
    private static volatile DefaultBeanFactory instance = null;
    //一级缓存Bean容器，IOC容器，直接从此处获取Bean
    private static Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    //二级缓存，为了将完全地Bean和半成品的Bean分离，避免读取到不完整的Bean
    private static Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();
    //三级缓存，值为一个对象工厂，可以返回实例对象
    private static Map<String, ObjectFactory> singletonFactories = new ConcurrentHashMap<>();
    //是否在创建中
    private static Set<String> singletonsCurrennlyInCreation = new HashSet<>();

    //Bean的注册信息BeanDefinition容器
    private static Map<String, BeanDefinition> beanDefinitionMap = BeanDefinitionRegistry.getBeanDefinitionMap();

    // 静态代码块，当加载该类时，首先初始化相关的bean对象。
    static {
        //ClassSetHelper.getInheritedComponentClassSet();
        Set<Class<?>> beanClassSet = ClassSetHelper.getBeanClassSet();
        if(beanClassSet!=null && !beanClassSet.isEmpty()) {
            try {
                // 先将所有类进行BeanDefinition的注册。并未进行bean实例化。
                // 即有service, controller, component 注解的类
                for(Class<?> beanClass : beanClassSet) {
                    GenericBeanDefinition genericBeanDefinition = new GenericBeanDefinition();
                    genericBeanDefinition.setBeanClass(beanClass);
                    BeanDefinitionRegistry.registryBeanDefinition(beanClass.getName(), genericBeanDefinition);
                }
                //注册配置的bean
                getInstance().initConfigBean();
                //注册其他所有的bean
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private  DefaultBeanFactory() {
        // TODO Auto-generated constructor stub
    }

    /**
     * 获取单例Bean工厂
     * @return  Bean 工厂
     */
    public static DefaultBeanFactory getInstance() {
        if(null==instance) {
            synchronized (DefaultBeanFactory.class){
                if(null==instance) {
                    instance = new DefaultBeanFactory();
                    return instance;
                }
            }
        }
        return instance;
    }

    /**
     * 根据类的全限定名获取bean
     */
    @Override
    public Object getBean(String beanName) {
        Object bean = null;
        try {
            bean = doGetBean(beanName, BeanScope.SINGLETON);
        }catch (Exception e){
            e.printStackTrace();
        }
        return bean;
    }

    /**
     * 根据类的全限定名与作用域获取bean
     */
    @Override
    public Object getBean(String beanName, BeanScope beanScope) {
        Object bean = null;
        try {
            bean=doGetBean(beanName,beanScope);
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        return bean;
    }

    @Override
    public Object getBean(Class<?> cls) {
        Object bean = null;
        try {
            bean=doGetBean(cls.getName(),BeanScope.SINGLETON);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bean;
    }

    @Override
    public Object getBean(Class<?> cls, BeanScope beanScope) {
        Object bean = null;
        try {
            bean=doGetBean(cls.getName(),beanScope);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bean;
    }


    @Override
    public void setBean(String beanName, Object beanObject) {
        try {
            Objects.requireNonNull(beanName, "beanName 不能为空");
            singletonObjects.put(beanName, beanObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setBean(Class<?> cls, Object beanObject) {
        // 获取到类名后调用上面的方法。
        this.setBean(cls.getName(), beanObject);
    }

    @Override
    public void refresh() throws Exception {
        for(Map.Entry<String, BeanDefinition> entry: beanDefinitionMap.entrySet()) {
            getBean(entry.getKey());
        }
    }

    /**
     * beanMap是否为空
     */
    @Override
    public boolean isEmpty() {
        return singletonObjects==null || singletonObjects.isEmpty();
    }


    /**
     * @throws Exception 捕捉异常
     * 实现由Configuration注解类，并注入由Bean标注的方法对象。
     */
    private void initConfigBean() throws Exception {
        Set<Class<?>> configClassSet = ClassSetHelper.getClassSetByAnnotation(Configuration.class);
        if(configClassSet == null || configClassSet.isEmpty()) {
            return;
        }
        for(Class<?> configClass : configClassSet) {
            //注册Configuration
            GenericBeanDefinition genericBeanDefinition = new GenericBeanDefinition();
            //设置configClass类名
            genericBeanDefinition.setBeanClass(configClass);
            // 通过类名注入到BeanDefinition容器中。
            BeanDefinitionRegistry.registryBeanDefinition(configClass.getName(), genericBeanDefinition);
            // 获得Bean对象，先是将整个Config类对象放入一级缓存中
            Object configBean = getBean(configClass);
            // 获得该类对象的相关方法
            Method[] methods = configClass.getDeclaredMethods();
            for(Method method:methods) {
                // 找到由Bean属性标注的方法。
                if(method.isAnnotationPresent(Bean.class)) {
                    Class<?> returnClass = method.getReturnType();
                    Object bean = method.invoke(configBean);
                    String keyName = returnClass.getName();
                    singletonObjects.put(keyName, bean);
                    System.out.println("成功注入"+configClass.getName()+" 中的  "+returnClass.getName());
                }
            }
            singletonObjects.remove(configClass.getName());
        }
    }


    /**
     * @param beanName bean的类名
     * @param beanScope bean的作用域
     * @return bean对象
     * @throws Exception 捕捉异常
     * 通过多级缓存，解决依赖循环问题。
     */
    private Object doGetBean(String beanName, BeanScope beanScope) throws Exception{
        Objects.requireNonNull(beanName, "beanName 不能为空");
        //获取单例
        Object bean = getSingleton(beanName);
        if(bean != null) {
            return bean;
        }
        //如果未获取到bean，且bean不在创建中，则置bean的状态为在创建中
        if(!singletonsCurrennlyInCreation.contains(beanName)) {
            singletonsCurrennlyInCreation.add(beanName);
        }
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if(beanDefinition == null) {
            throw new Exception("不存在 "+beanName+" 的定义");
        }
        Class<?> beanClass = beanDefinition.getBeanClass();
        //找到实现类 是否有父类实现。
        beanClass = findImplementClass(beanClass,null);
        //判断是否需要代理，若需要则生成代理类
        if(beanDefinition.getIsProxy() && beanDefinition.getProxy()!=null) {
            MyProxy myProxy = beanDefinition.getProxy();
            bean = myProxy.getProxy(beanClass);
        }
        else {
            // 否则则通过反射创建bean对象实例。
            bean = beanClass.getDeclaredConstructor().newInstance();
        }
        //将实例化后，但未注入属性的bean，放入三级缓存中
        final Object temp = bean;
        singletonFactories.put(beanName, new ObjectFactory() {
            @Override
            public Object getObject() {
                // TODO Auto-generated method stub
                return temp;
            }
        });
        //反射调用init方法
        String initMethodName = beanDefinition.getInitMethodName();
        if(initMethodName!=null) {
            Method method = beanClass.getMethod(initMethodName, null);
            method.invoke(bean, null);
        }

        //注入bean的属性
        fieldInject(beanClass, bean, false);
        //如果三级缓存存在bean，则拿出放入二级缓存中
        if(singletonFactories.containsKey(beanName)) {
            ObjectFactory factory  = singletonFactories.get(beanName);
            earlySingletonObjects.put(beanName, factory.getObject());
            singletonFactories.remove(beanName);
        }
        //如果二级缓存存在bean，则拿出放入一级缓存中
        if(earlySingletonObjects.containsKey(beanName)) {
            bean = earlySingletonObjects.get(beanName);
            singletonObjects.put(beanName, bean);
            earlySingletonObjects.remove(beanName);
        }
        return bean;
    }

    /**
     * 从缓存中获取单例bean
     * @param beanName  bean的类名或方法名。。
     * @return 单例bean对象
     */
    private Object getSingleton(String beanName) {
        //如果一级存在bean，则直接返回
        Object bean = singletonObjects.get(beanName);
        if(bean!=null) {
            return bean;
        }
        //如果一级缓存不存在bean，且bean在创建中，
        //则从二级缓存中拿出半成品bean返回，
        //如果二级缓存也没有，则从三级缓存拿出放入二级缓存中
        if(singletonsCurrennlyInCreation.contains(beanName)) {
            bean = earlySingletonObjects.get(beanName);
            if(bean == null) {
                ObjectFactory factory = singletonFactories.get(beanName);
                if(factory != null) {
                    // 把三级缓存中的代理对象中的真实对象获取出来，放入二级缓存中
                    bean = factory.getObject();
                    earlySingletonObjects.put(beanName, bean);
                    singletonFactories.remove(beanName);
                }
            }
        }
        return bean;
    }

    /**
     * 找到实现类
     * @param interfaceClass  实现类。
     * @return 实现类
     */
    private static Class<?> findImplementClass(Class<?> interfaceClass,String name){
        Class<?> implementClass = interfaceClass;
        Set<Class<?>> classSet = new HashSet<>();
        for(Class<?> cls : ClassSetHelper.getClassSet()) {
            if(interfaceClass!=null && interfaceClass.isAssignableFrom(cls) && !interfaceClass.equals(cls)) {
                if(name!=null && !"".equals(name)) {
                    if(isClassAnnotationedName(cls, name)) {
                        return cls;
                    }
                }
                classSet.add(cls);
            }
            else if(interfaceClass==null) {
                if(name!=null && !"".equals(name)) {
                    if(isClassAnnotationedName(cls, name) || (cls.getSimpleName().equals(name) && !cls.isInterface())) {
                        return cls;
                    }
                }
            }
            if(classSet != null && !classSet.isEmpty()) {
                implementClass = classSet.iterator().next();
            }
        }
        return implementClass;
    }

    /**
     * 依赖注入
     * @param beanClass bean的类对象
     * @param bean 对应bean 实例化后的结果
     * @param isProxyed 是否需要代理
     * @throws Exception
     */
    private void fieldInject(Class<?> beanClass, Object bean, boolean isProxyed) throws Exception{
        Field[] beanFields = beanClass.getDeclaredFields();
        if (beanFields != null && beanFields.length > 0) {
            for (Field beanField : beanFields) {
                if(beanField.isAnnotationPresent(Value.class)) {
                    //注入value值
                    String key = beanField.getAnnotation(Value.class).value();
                    Class<?> type = beanField.getType();
                    if(!"".equals(key)) {
                        Object value=null;
                        if(type.equals(String.class)) {
                            value = ConfigUtil.getString(key);
                        }
                        else if (type.equals(Integer.class)) {
                            value = ConfigUtil.getInt(key);
                        }
                        else if (type.equals(Float.class)) {
                            value = ConfigUtil.getFloat(key);
                        }
                        else if (type.equals(Boolean.class)) {
                            value = ConfigUtil.getBoolean(key);
                        }
                        else if (type.equals(Long.class)) {
                            value = ConfigUtil.getLong(key);
                        }
                        else if (type.equals(Double.class)) {
                            value = ConfigUtil.getDouble(key);
                        }
                        else {
                            throw new RuntimeException("不允许的类型");
                        }
                        beanField.setAccessible(true);
                        beanField.set(bean, value);
                    }
                }
                //找Autowired/Resource注解属性
                else if (beanField.isAnnotationPresent(Autowired.class) || beanField.isAnnotationPresent(Resource.class)) {
                    Class<?> beanFieldClass = beanField.getType();
                    String qualifier = null;
                    if(beanField.isAnnotationPresent(Autowired.class)) {
                        if(beanField.isAnnotationPresent(Qualifier.class)) {
                            qualifier=beanField.getAnnotation(Qualifier.class).value();
                        }
                        //Service找实现
                        beanFieldClass = findImplementClass(beanFieldClass,qualifier);
                    }
                    else if(beanField.isAnnotationPresent(Resource.class)){
                        qualifier = beanField.getAnnotation(Resource.class).name();
                        if(qualifier==null || qualifier.equals("")) {
                            qualifier = beanFieldClass.getSimpleName();
                        }
                        Class<?> tmpClass= findImplementClass(null,qualifier);
                        if(tmpClass==null || tmpClass.isInterface()) {
                            Class<?> beanAnnotationType = beanField.getAnnotation(Resource.class).type();
                            if(beanAnnotationType!=java.lang.Object.class) {
                                beanFieldClass = findImplementClass(beanAnnotationType, null);
                            }
                            else {
                                beanFieldClass = findImplementClass(beanFieldClass, null);
                            }
                        }
                        else {
                            beanFieldClass = tmpClass;
                        }
                    }
                    //根据beanName去找实例(这时是实现类的beanName)
                    beanField.setAccessible(true);
                    try {
                        //反射注入属性实例。beanFieldClass是属性的所属类，从Bean容器中可以获得该类的bean对象
                        //第一遍缓存没有bean，再getBean。
                        beanField.set(bean,getBean(beanFieldClass.getName())
                        );
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    /**
     * @param cls 类
     * @param name  注解默认值
     * @return  是否是注解默认值
     */
    private static boolean isClassAnnotationedName(Class<?> cls,String name) {
        return (cls.isAnnotationPresent(Component.class) && cls.getAnnotation(Component.class).value().equals(name)) ||
                (cls.isAnnotationPresent(Service.class) && cls.getAnnotation(Service.class).value().equals(name)) ||
                (cls.isAnnotationPresent(Controller.class) && cls.getAnnotation(Controller.class).value().equals(name));
    }
}

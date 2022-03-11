package com.wu.spring.aop;

import com.wu.spring.annotation.aop.*;
import com.wu.spring.constants.AdviceTypeConstant;
import com.wu.spring.ioc.BeanDefinition;
import com.wu.spring.ioc.BeanDefinitionRegistry;
import com.wu.spring.ioc.ClassSetHelper;
import com.wu.spring.ioc.GenericBeanDefinition;
import org.apache.commons.collections4.map.HashedMap;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将所需要增强的方法进行注册
 */
public class AOPHelper {
    // aop助手为单例模式。
    private static volatile AOPHelper aopHelper = null;
    // 需要代理的目标类和目标方法的映射
    private static Map<Class<?>, List<Method>> classMethodMap = new ConcurrentHashMap<>();
    // 需要代理的目标方法和增强类的映射，value的map的key为通知的类型
    private static Map<Method, Map<String, List<Advice>>> methodAdvicesMap = new ConcurrentHashMap<>();
    /**
     * 初始化aop助手
     */
    static {
        try {
            AOPHelper.init();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    /**
     * 获得AOPHelper的单例
     * @return aop单例
     */
    public static AOPHelper getInstance() {
        if(aopHelper==null) {
            synchronized (AOPHelper.class) {
                if(aopHelper==null) {
                    aopHelper = new AOPHelper();
                    return aopHelper;
                }
            }
        }
        return aopHelper;
    }


    /**
     * @throws Exception
     * 初始化AOP助手
     */
    public static void init() throws Exception{
        // 找到由aspect标注的类。
        Set<Class<?>> aspectClassSet = ClassSetHelper.getClassSetByAnnotation(Aspect.class);
        for (Class<?> aspectClass : aspectClassSet) {
            Map<String, String> pointcuts = new HashedMap<>();
            // 通过反射创建aspectClass类的实例
            Object aspect = aspectClass.getDeclaredConstructor().newInstance();
            for (Method method : aspectClass.getMethods()) {
                // 对该类下的方法，如果是切点注解的，则加入到切点集合中。
                if (method.isAnnotationPresent(Pointcut.class)){
                    // 要注意这里获得方法名时会把（）去掉，因此后面进行解析时要去掉括号。
                    String pointcutName = method.getName();
                    String pointcut = method.getAnnotation(Pointcut.class).value();
                    if(pointcut!=null && !"".equals(pointcut)) {
                        pointcuts.put(pointcutName, pointcut);
                    }
                }
            }
            for (Method method : aspectClass.getMethods()) {
                injectMethodAdvices(aspect, method, pointcuts);
            }
        }
        // 由于一个方法可能存在多个增强，因此需要根据order排序
        for (Method method : methodAdvicesMap.keySet()) {
            for (String key : methodAdvicesMap.get(method).keySet()) {
                Collections.sort(methodAdvicesMap.get(method).get(key),new Comparator<Advice>(){
                    @Override
                    public int compare(Advice o1, Advice o2) {
                        return o1.getOrder() - o2.getOrder();
                    }
                });
            }
        }
        // 重新注册需要被增强的类，注入代理类，由于这些类需要代理，因此需要重新注册。
        try {
            CGLibProxy cgLibProxy = new CGLibProxy(methodAdvicesMap);
            for(Class<?> cls:classMethodMap.keySet()) {
                BeanDefinition beanDefinition=null;
                if(BeanDefinitionRegistry.containsBeanDefinition(cls.getName())) {
                    beanDefinition = BeanDefinitionRegistry.getBeanDefinition(cls.getName());
                }
                else {
                    beanDefinition = new GenericBeanDefinition();
                    beanDefinition.setBeanClass(cls);
                }
                beanDefinition.setIsProxy(true);
                beanDefinition.setProxy(cgLibProxy);
                BeanDefinitionRegistry.registryBeanDefinition(cls.getName(), beanDefinition);
                System.out.println("AOPHelper 注册 "+cls.getName());
            }
            //DefaultBeanFactory.getInstance().refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 为目标类和方法找到相应的增强列表(Advice)
     * @param aspect 在我们测试案例中 为Interceptor实例对象。
     * @param method
     * @param pointcuts
     * @throws Exception
     */
    private static void injectMethodAdvices(Object aspect,Method method,Map<String, String> pointcuts) throws Exception {
        String pointValue=null;
        String pointType = null;
        Integer order = -1;
        // 匹配该方法是对应哪中通知方法。
        if(method.isAnnotationPresent(Before.class)) {
            pointValue = (method.getAnnotation(Before.class)).value();
            order = (method.getAnnotation(Before.class)).order();
            pointType = AdviceTypeConstant.BEFORE;
        }
        else if(method.isAnnotationPresent(After.class)) {
            pointValue = (method.getAnnotation(After.class)).value();
            order = (method.getAnnotation(After.class)).order();
            pointType = AdviceTypeConstant.AFTER;
        }
        else if(method.isAnnotationPresent(Around.class)) {
            pointValue = (method.getAnnotation(Around.class)).value();
            order = (method.getAnnotation(Around.class)).order();
            pointType = AdviceTypeConstant.AROUND;
        }
        else if(method.isAnnotationPresent(AfterReturning.class)) {
            pointValue = (method.getAnnotation(AfterReturning.class)).value();
            order = (method.getAnnotation(AfterReturning.class)).order();
            pointType = AdviceTypeConstant.AFTERRETURNING;
        }
        else if(method.isAnnotationPresent(AfterThrowing.class)) {
            pointValue = (method.getAnnotation(AfterThrowing.class)).value();
            order = (method.getAnnotation(AfterThrowing.class)).order();
            pointType = AdviceTypeConstant.AFTERTHROWING;
        }
        else {
            //System.out.println(method.getName()+" 不是增强");
            // 说明该方法不是5种通知方法中的一个，可能是个切点。
            return;
        }
        // 获得确切的切点全限量名
        pointValue = parsePointValue(pointcuts, pointValue);
        Map<String, String> classAndMethod = getClassAndMethod(pointValue);
        try {
            Class<?> targetClass = Class.forName(classAndMethod.get("class"));
            //代理的目标类和目标方法的映射,如果没有映射，则建立映射。
            if (!classMethodMap.containsKey(targetClass)){
                classMethodMap.put(targetClass, new ArrayList<Method>());
            }
            for (Method targetClassMethod : targetClass.getMethods()) {
                if ("*".equals(classAndMethod.get("method")) || classAndMethod.get("method").equals(targetClassMethod.getName())){
                    // 要注意该map的value为list，说明一个class，可能映射几个方法。
                    classMethodMap.get(targetClass).add(targetClassMethod);
                    Advice advice = new Advice(aspect, method, order);
                    if (!methodAdvicesMap.containsKey(targetClassMethod)){
                        methodAdvicesMap.put(targetClassMethod, new HashedMap<String, List<Advice>>());
                    }
                    if (!methodAdvicesMap.get(targetClassMethod).containsKey(pointType)){
                        methodAdvicesMap.get(targetClassMethod).put(pointType, new ArrayList<>());
                    }
                    methodAdvicesMap.get(targetClassMethod).get(pointType).add(advice);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * 获得切点的值，可能直接在通知上定义，也可能在pointcut中定义
     * @param pointcuts
     * @param pointValue
     * @return 切点值
     * @throws Exception
     */
    private static String parsePointValue(Map<String, String> pointcuts, String pointValue) throws Exception {
        if(pointValue==null || "".equals(pointValue)) {
            return null;
        }
        String result = null;
        // 如果通知中的value值有（），说明是pointcut，需要去掉括号。然后从pointcutMap中获取。
        if(pointValue.endsWith("()")) {
            String pointcutName = pointValue.replace("()", "");
            if(!pointcuts.containsKey(pointcutName)) {
                throw new Exception(pointValue+" 未定义");
            }
            // 获取切点位置值。
            result = pointcuts.get(pointcutName);
        }else{
            result = pointValue;
        }
        return result;
    }

    /**
     * 解析切点全限量名，分解，获得代理的目标类和方法，比如com.wu.de.service.UserService为类，后面的为方法
     * @param pointValue 切点方法全限量名
     * @return 分别将类名和方法保存在Map中
     */
    private static Map<String, String> getClassAndMethod(String pointValue) {
        Map<String, String> map = new HashMap<String, String>();
        String[] split = pointValue.split("\\.");
        if (split.length == 0) {
            throw new RuntimeException("全限量名解析异常");
        }
        StringBuilder stringBuilder = new StringBuilder();
        // 分别保存class和method.
        for (int i = 0; i < split.length - 1; i++) {
            if (i == split.length - 2) {
                stringBuilder.append(split[i]);
            } else {
                stringBuilder.append(split[i]).append(".");
            }
        }
        map.put("class", stringBuilder.toString());
        map.put("method", split[split.length - 1]);
        return map;
    }

}

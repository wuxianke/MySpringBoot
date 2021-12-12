package com.wu.mybatis.core;

import com.wu.mybatis.annotation.*;
import com.wu.mybatis.constants.SqlTypeConstant;
import com.wu.mybatis.executor.ExecutorFactory;
import com.wu.spring.ioc.BeanDefinition;
import com.wu.spring.ioc.BeanDefinitionRegistry;
import com.wu.spring.ioc.ClassSetHelper;
import com.wu.spring.ioc.GenericBeanDefinition;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 扫描被@Mapper注解的类，解析方法对应的方法详情类MethodDetails
 * @author Cactus
 */
public class MapperHelper {
    private static MapperHelper mapperHelper = null;

    private static Map<Method,MethodDetails> cacheMethodDetails = new ConcurrentHashMap<>();

    private static Set<Class<?>> mapperClassSet = null;
    static {
        MapperHelper.init();
    }

    private MapperHelper(){

    }
    /**
     * 获得MapperHelper的单例
     * @return mapperHelper单例。
     */
    public static MapperHelper getInstance() {
        synchronized (MapperHelper.class) {
            if(mapperHelper==null) {
                synchronized (MapperHelper.class) {
                    mapperHelper = new MapperHelper();
                }
            }
        }
        return mapperHelper;
    }

    /**
     * 获得cacheMethodDetails
     * @return
     */
    public static Map<Method,MethodDetails> getCacheMethodDetails(){
        return cacheMethodDetails;
    }

    /**
     * 获得某个方法的详情类
     * @param method
     * @return
     */
    public static MethodDetails getMethodDetails(Method method) {
        if(cacheMethodDetails==null || cacheMethodDetails.isEmpty() || !cacheMethodDetails.containsKey(method)) {
            return null;
        }
        return cacheMethodDetails.get(method);
    }



    private static void init() {
        mapperClassSet = ClassSetHelper.getClassSetByAnnotation(Mapper.class);
        if(mapperClassSet == null || mapperClassSet.isEmpty()){
            System.out.println("Can not find Class with annotation of Mapper");
            return;
        }
        for (Class<?> aClass : mapperClassSet) {
            // 如果该类不是接口，则跳过
            if (!aClass.isInterface()){
                continue;
            }
            Method[] methods = aClass.getDeclaredMethods();
            for (Method method : methods) {
                // 对方法解析，获得该方法的详情类。
                MethodDetails methodDetails = handleParameter(method);
                // 对该方法注解上的Sql语句进行解析
                methodDetails.setSqlSource(handleAnnotation(method));
                // 将方法以及对应的方法细节存储到map中
                cacheMethodDetails.put(method, methodDetails);
            }
        }
        //将被@Mapper标注的类注册到BeanDefinition容器中，并设置代理类
        if(mapperClassSet!=null && !mapperClassSet.isEmpty()) {
            CGLibMapperProxy cgLibMapperProxy = new CGLibMapperProxy(ExecutorFactory.getExecutor());
            for(Class<?> cls:mapperClassSet) {
                BeanDefinition beanDefinition=null;
                if(BeanDefinitionRegistry.containsBeanDefinition(cls.getName())) {
                    beanDefinition = BeanDefinitionRegistry.getBeanDefinition(cls.getName());
                }
                else {
                    beanDefinition = new GenericBeanDefinition();
                    beanDefinition.setBeanClass(cls);
                }
                beanDefinition.setIsProxy(true);
                beanDefinition.setProxy(cgLibMapperProxy);
                BeanDefinitionRegistry.registryBeanDefinition(cls.getName(), beanDefinition);
                System.out.println("MapperHelper 注册 "+cls.getName());
            }
        }
    }



    private static MethodDetails handleParameter(Method method) {
        MethodDetails methodDetails = new MethodDetails();
        // 获得参数数量
        int parameterCount = method.getParameterCount();
        // 获得返回类型类，以数组形式返回
        Class<?>[] parameterTypes = method.getParameterTypes();
        List<String> parameterNames = new ArrayList<>();
        // 获得参数的数组，数组的索引为该参数的位置
        Parameter[] parameters = method.getParameters();
        // 添加参数名称，如arg0...arg1
        for (Parameter parameter : parameters) {
            parameterNames.add(parameter.getName());
        }
        //设置注解中的参数名称，即Param中的value
        for(int i = 0; i < parameterCount; i++){
            parameterNames.set(i,getParamNameFromAnnotation(method,i,parameterNames.get(i)));
        }
        //将参数类型和参数名添加到MethodDetails对象中
        methodDetails.setParameterTypes(parameterTypes);
        methodDetails.setParameterNames(parameterNames);
        Type methodReturnType = method.getGenericReturnType();
        Class<?> methodReturnClass = method.getReturnType();
        if (methodReturnType instanceof ParameterizedType){
            //如果是可参数化的类型，如List，则获取具体参数类型
            if(!List.class.equals(methodReturnClass)){
                throw new RuntimeException("now ibatis only support list");
            }
            Type type = ((ParameterizedType) methodReturnType).getActualTypeArguments()[0];
            methodDetails.setReturnType((Class<?>) type);
            methodDetails.setHasSet(true);
        }else{
            methodDetails.setReturnType(methodReturnClass);
            methodDetails.setHasSet(false);
        }
        return methodDetails;
    }

    /**
     * 获取指定method的第i个参数的Param参数命名。
     * @param method
     * @param i 位置
     * @param paramName 某个位置的占位名arg0。
     * @return 实际param value
     */
    private static String getParamNameFromAnnotation(Method method, int i, String paramName) {
        final Object[] paraAnnotations = method.getParameterAnnotations()[i];
        for (Object paraAnnotation : paraAnnotations) {
            if (paraAnnotation instanceof Param){
                paramName = ((Param) paraAnnotation).value();
            }
        }
        return paramName;
    }

    /**
     * 解析注解中的Sql语句，生成SqlSource对象
     * @param method
     * @return SqlSource
     */
    private static SqlSource handleAnnotation(Method method) {
        SqlSource sqlSource = null;
        String sql = null;
        Annotation[] annotations = method.getDeclaredAnnotations();
        for(Annotation annotation : annotations){
            if(Select.class.isInstance(annotation)){
                Select selectAnnotation = (Select)annotation;
                sql = selectAnnotation.value();
                sqlSource = new SqlSource(sql);
                sqlSource.setExecuteType(SqlTypeConstant.SELECT_TYPE);
                break;
            }else if(Update.class.isInstance(annotation)){
                Update updateAnnotation = (Update)annotation;
                sql = updateAnnotation.value();
                sqlSource = new SqlSource(sql);
                sqlSource.setExecuteType(SqlTypeConstant.UPDATE_TYPE);
                break;
            }else if(Delete.class.isInstance(annotation)){
                Delete deleteAnnotation = (Delete) annotation;
                sql = deleteAnnotation.value();
                sqlSource = new SqlSource(sql);
                sqlSource.setExecuteType(SqlTypeConstant.DELETE_TYPE);
                break;
            }else if(Insert.class.isInstance(annotation)){
                Insert insertAnnotation = (Insert) annotation;
                sql = insertAnnotation.value();
                sqlSource = new SqlSource(sql);
                sqlSource.setExecuteType(SqlTypeConstant.INSERT_TYPE);
                break;
            }
        }
        if(sqlSource == null){
            throw new RuntimeException("method annotation not null");
        }
        return sqlSource;
    }


}




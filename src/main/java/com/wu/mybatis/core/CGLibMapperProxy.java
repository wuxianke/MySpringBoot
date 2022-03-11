package com.wu.mybatis.core;

import com.wu.mybatis.annotation.Delete;
import com.wu.mybatis.annotation.Insert;
import com.wu.mybatis.annotation.Select;
import com.wu.mybatis.annotation.Update;
import com.wu.mybatis.constants.SqlTypeConstant;
import com.wu.mybatis.executor.Executor;
import com.wu.spring.common.MyProxy;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

public class CGLibMapperProxy implements MethodInterceptor, MyProxy {
    //执行器
    private Executor executor=null;

    public CGLibMapperProxy(Executor executor) {
        this.executor = executor;
    }
    /**
     * 对方法进行拦截代理
     */
    @Override
    public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        Object result = null;
        // 如果是由增删改查类标注的方法
        if(isIntercept(method)){
            // getMethodType  判断是增删改查哪一个
            Integer methodType = MapperHelper.getMethodDetails(method).getSqlSource().getExecuteType();
            if(methodType == null){
                throw new RuntimeException("method is abnormal sql method");
            }
            //如果是被@Select注解
            if(methodType == SqlTypeConstant.SELECT_TYPE){
                List<Object> list = executor.select(method,args);
                result = list;
                //根据情况返回一个对象列表或一个实例对象
                if(!MapperHelper.getMethodDetails(method).isHasSet()){
                    if(list.size() == 0){
                        result = null;
                    }else {
                        result = list.get(0);
                    }
                }
            }else{
                //如果是被@Update,@Delete,@Insert注解
                Integer count = executor.update(method,args);
                result = count;
            }
        }
        else if (Object.class.equals(method.getDeclaringClass())) {
            result = methodProxy.invokeSuper(object, args);
        }
        return result;
    }
    /**
     * 获得代理对象
     */
    @Override
    public Object getProxy(Class<?> cls) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(cls);
        enhancer.setCallback(this);
        return enhancer.create();
    }
    /**
     * 判断方法是否需要被代理
     * @param method
     * @return
     */
    private boolean isIntercept(Method method) {
        for(Annotation annotation : method.getAnnotations()) {
            if(annotation.annotationType().equals(Select.class) ||
                    annotation.annotationType().equals(Update.class) ||
                    annotation.annotationType().equals(Insert.class) ||
                    annotation.annotationType().equals(Delete.class)) {
                return true;
            }
        }
        return false;
    }

}

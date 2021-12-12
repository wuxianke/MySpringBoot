package com.wu.spring.aop;

import com.wu.spring.common.MyProxy;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class CGLibProxy implements MethodInterceptor, MyProxy {
    //目标代理方法和增强列表的映射
    Map<Method, Map<String, List<Advice>>> methodAdvicesMap = null;

    public CGLibProxy(Map<Method, Map<String, List<Advice>>> methodAdvicesMap) {
        // TODO Auto-generated constructor stub
        this.methodAdvicesMap=methodAdvicesMap;
    }

    @Override
    public Object getProxy(Class<?> cls) {
        return null;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        return null;
    }
}

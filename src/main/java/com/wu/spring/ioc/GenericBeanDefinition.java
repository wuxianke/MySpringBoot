package com.wu.spring.ioc;

import com.wu.spring.common.MyProxy;
import com.wu.spring.constants.BeanScope;

import java.util.Objects;

/**
 * @author Cactus
 */
public class GenericBeanDefinition implements BeanDefinition{

    private Class<?> beanClass;

    private BeanScope scope =BeanScope.SINGLETON;

    private String initMethodName;
    
    private boolean isProxy = false;
    
    private MyProxy myProxy = null;
    
    @Override
    public void setBeanClass(Class<?> beanClass){
        this.beanClass = beanClass;
    }

    public void setScope(BeanScope scope) {
        this.scope = scope;
    }

    public void setInitMethodName(String initMethodName) {
        this.initMethodName = initMethodName;
    }

    @Override
    public Class<?> getBeanClass() {
        return beanClass;
    }

    @Override
    public BeanScope getScope() {
        return scope;
    }

    @Override
    public boolean isSingleton() {
        return Objects.equals(scope,BeanScope.SINGLETON);
    }

    @Override
    public boolean isPrototype() {
        return Objects.equals(scope, BeanScope.PROTOTYPE);
    }

    @Override
    public String getInitMethodName() {
        return initMethodName;
    }

	@Override
	public MyProxy getProxy() {
		return myProxy;
	}

	@Override
	public void setProxy(MyProxy myProxy) {
		this.myProxy = myProxy;
	}

	@Override
	public boolean getIsProxy() {
		return isProxy;
	}

	@Override
	public void setIsProxy(boolean isProxy) {
		this.isProxy=isProxy;
	}
}

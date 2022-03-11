package com.wu.spring.aop;

import com.wu.mybatis.transaction.TransactionFactory;
import com.wu.mybatis.transaction.TransactionManager;
import com.wu.mybatis.transaction.TransactionStatus;
import com.wu.spring.annotation.aop.Transactional;
import com.wu.spring.common.MyProxy;
import com.wu.spring.constants.AdviceTypeConstant;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class CGLibProxy implements MethodInterceptor, MyProxy {
    //目标代理方法和增强列表的映射
    Map<Method, Map<String, List<Advice>>> methodAdvicesMap = null;
    // 事务管理器
    TransactionManager transactionManager = TransactionFactory.newTransaction();


    public CGLibProxy(Map<Method, Map<String, List<Advice>>> methodAdvicesMap) {
        // TODO Auto-generated constructor stub
        this.methodAdvicesMap = methodAdvicesMap;
    }


    /**
     * @param cls 需要代理的类
     * @return
     */
    @Override
    public Object getProxy(Class<?> cls) {
        Enhancer enhancer = new Enhancer();
        //设置要代理的目标类
        enhancer.setSuperclass(cls);
        //设置要代理的拦截器
        enhancer.setCallback(this);
        //生成代理类的实例
        return enhancer.create();
    }

    /**
     * 拦截目标代理方法，对目标方法进行增强
     *
     * @param object
     * @param method
     * @param arg2
     * @param methodProxy
     * @return
     * @throws Throwable
     */
    @Override
    public Object intercept(Object object, Method method, Object[] arg2, MethodProxy methodProxy) throws Throwable {
        Object result = null;
        TransactionStatus status = getTransactionStatus(object, method);
        status.isTrans = transactionManager.isTransactionPresent();
        // 判断是否增强
        Map<String, List<Advice>> advices = methodAdvicesMap.get(method);
        try {
            // 前置增强
            invokeAdvice(method, advices, AdviceTypeConstant.BEFORE);
            // 环绕增强
            System.out.println("当前事务是否存在:" + status.isTrans);
            if (isAdviceNeed(method) && advices != null && advices.containsKey(AdviceTypeConstant.AROUND)) {
                List<Advice> aroundAdvices = advices.get(AdviceTypeConstant.AROUND);
                if (aroundAdvices != null && !aroundAdvices.isEmpty()) {
                    Advice advice = aroundAdvices.get(0);
                    JoinPoint joinPoint = new JoinPoint(object, methodProxy, arg2);
                    Method adviceMethod = advice.getAdviceMethod();
                    Object aspect = advice.getAspect();
                    try {
                        // 开启事务
                        beginTransaction(status);
                        // 执行方法
                        result = adviceMethod.invoke(aspect, joinPoint);
                        // 提交事务
                        commitTransaction(status);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        // 出现异常则回滚事务
                        for (Class<? extends Throwable> aClass : status.rollbackFor) {
                            if (aClass.isAssignableFrom(e.getClass())){
                                rollbackTransaction(status);
                            }
                        }
                    } finally {
                        // 关闭事务
                        closeTransaction(status);
                    }
                }
                else {
                    try {
                        //开启事务
                        beginTransaction(status);
                        result= methodProxy.invokeSuper(object, arg2);
                        commitTransaction(status);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        //回滚事务
                        for(Class<? extends Throwable> th:status.rollbackFor) {
                            if(th.isAssignableFrom(e.getClass())) {
                                rollbackTransaction(status);
                            }
                        }
                    }
                    finally {
                        closeTransaction(status);
                    }
                }
            }
            else {
                try {
                    //开启事务
                    beginTransaction(status);
                    result= methodProxy.invokeSuper(object, arg2);
                    commitTransaction(status);
                } catch (Throwable e) {
                    e.printStackTrace();
                    //回滚事务
                    for(Class<? extends Throwable> th:status.rollbackFor) {
                        if(th.isAssignableFrom(e.getClass())) {
                            rollbackTransaction(status);
                        }
                    }
                }
                finally {
                    closeTransaction(status);
                }
            }

            // 后置增强
            invokeAdvice(method, advices, AdviceTypeConstant.AFTER);
        } catch (Exception e) {
            e.printStackTrace();
            // 如果有异常，则采用异常增强
            invokeAdvice(method,advices,AdviceTypeConstant.AFTERTHROWING);
        } finally {
            // 返回最后结果前增强
            invokeAdvice(method,advices,AdviceTypeConstant.AFTERRETURNING);
        }
        return result;
    }

    /**
     * 关闭事务
     */
    private void closeTransaction(TransactionStatus status) throws SQLException {
        if(isTransactionNeed(status)) {
            System.out.println("关闭事务，事务Id="+transactionManager.getTransactionId());
            transactionManager.closeTransaction(status);
        }
    }

    /**
     * 回滚事务
     */
    private void rollbackTransaction(TransactionStatus status) throws SQLException {
        if(isTransactionNeed(status)) {
            transactionManager.rollback();
            System.out.println("回滚事务，事务Id="+transactionManager.getTransactionId());
        }
    }

    /**
     * 提交事务
     */
    private void commitTransaction(TransactionStatus status) throws SQLException {
        if(isTransactionNeed(status)) {
            transactionManager.commit(status);
            System.out.println("提交事务，事务Id="+transactionManager.getTransactionId());
        }
    }

    /**
     * 开启事务
     */
    private void beginTransaction(TransactionStatus status) throws SQLException {
        if (isTransactionNeed(status)) {
            transactionManager.beginTransaction(status);
            System.out.println("开启事务，事务Id=" + transactionManager.getTransactionId());
        }
    }

    /**
     * 判断是否需要事务管理
     */
    private boolean isTransactionNeed(TransactionStatus status) {
        return status.isNeed;
    }

    private TransactionStatus getTransactionStatus(Object object, Method method) {
        TransactionStatus status = new TransactionStatus();
        status.isNeed = false;
        if(transactionManager!=null && (object.getClass().isAnnotationPresent(Transactional.class) || method.isAnnotationPresent(Transactional.class))) {
            status.isNeed = true;
            if (object.getClass().isAnnotationPresent(Transactional.class)){
                Transactional transactional = object.getClass().getAnnotation(Transactional.class);
                status.isolationLevel = transactional.Isolation();
                status.propagationLevel = transactional.Propagation();
                status.rollbackFor = transactional.rollbackFor();
            }
            if(method.isAnnotationPresent(Transactional.class)){
                Transactional transactional = method.getAnnotation(Transactional.class);
                status.isolationLevel = transactional.Isolation();
                status.propagationLevel = transactional.Propagation();
                status.rollbackFor = transactional.rollbackFor();
            }
        }
        return status;
    }


    /**
     * 判断是否增强，也就是看增强方法Map中是否有该方法。
     *
     * @param method
     * @return
     */
    private boolean isAdviceNeed(Method method) {
        return methodAdvicesMap.containsKey(method);
    }


    //执行通知增强
    //根据增强类型，从
    private void invokeAdvice(Method method, Map<String, List<Advice>> advices, String type) throws Throwable {
        if (isAdviceNeed(method) && advices != null && advices.containsKey(type)) {
            List<Advice> adviceList = advices.get(type);
            if (adviceList != null && !adviceList.isEmpty()) {
                for (Advice advice : adviceList) {
                    Method adviceMethod = advice.getAdviceMethod();
                    // aspect 之前在存储时，就已经通过反射创建了实例。
                    Object aspect = advice.getAspect();
                    adviceMethod.invoke(aspect);
                }
            }
        }

    }

}

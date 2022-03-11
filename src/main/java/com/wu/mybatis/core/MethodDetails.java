package com.wu.mybatis.core;

import java.util.List;

public class MethodDetails {
    /**
     * 方法返回值类型
     */
    private Class<?> returnType;
    /**
     * 是否返回集合： 当返回类型会可参数化类型时
     */
    private boolean hasSet;

    /**
     * 参数类型
     */
    private Class<?>[] parameterTypes;
    /**
     * 参数名称，通过param中的value获得
     */
    private List<String> parameterNames;

    /**
     * // 解析得到Sql包装对象
     */
    private SqlSource sqlSource;


    public Class<?> getReturnType() {
        return returnType;
    }

    public void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }

    public boolean isHasSet() {
        return hasSet;
    }

    public void setHasSet(boolean hasSet) {
        this.hasSet = hasSet;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public List<String> getParameterNames() {
        return parameterNames;
    }

    public void setParameterNames(List<String> parameterNames) {
        this.parameterNames = parameterNames;
    }

    public SqlSource getSqlSource() {
        return sqlSource;
    }

    public void setSqlSource(SqlSource sqlSource) {
        this.sqlSource = sqlSource;
    }

}

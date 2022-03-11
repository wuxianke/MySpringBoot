package com.wu.mybatis.core;

import java.util.ArrayList;
import java.util.List;

/**
 * @example select * from users where id = ${id} and name = #{name}
 * -> sql = select * from users where id = ? and name = ?
 * -> params = {id,name}
 * -> paramInjectTypes = {0,1}
 **/
public class SqlSource {
    //sql语句，待输入字段替换成?
    private String sql;
    //待输入字段
    private List<String> params = new ArrayList<>();
    //注入的类型,0表示拼接，1表示动态注入, 一条语句可能存在多个类型
    private List<Integer> paramInjectTypes = new ArrayList<>();
    //Sql语句的类型，select update insert delete等
    private Integer executeType;
    
    public SqlSource(String sql){
        this.sql = sqlInject(sql);
    }
    /**
     * 解析Sql语句，将#{ }及${ }参数替换为?，并获得参数名与注入方式列表
     * @param sql
     * @return
     */
    private String sqlInject(String sql){
    	
        String labelPrefix1 = "${";
        String labelPrefix2 = "#{";
        String labelSuffix = "}";
        // 判断是否存在占位符
        while ((sql.indexOf(labelPrefix1) > 0 || sql.indexOf(labelPrefix2) > 0) && sql.indexOf(labelSuffix) > 0){
        	// 找到占位符所在索引
            Integer labelPrefix1Index = sql.indexOf(labelPrefix1);
        	Integer labelPrefix2Index = sql.indexOf(labelPrefix2);
        	String sqlParamName;
        	Integer injectType;
        	// 如果是${}占位符
        	if(labelPrefix1Index>0 && labelPrefix2Index<=0) {
        		sqlParamName = sql.substring(labelPrefix1Index,sql.indexOf(labelSuffix)+1);
        		injectType = 0;
        	} // 如果是#{}占位符
        	else if (labelPrefix2Index>0 && labelPrefix1Index<=0) {
        		sqlParamName = sql.substring(labelPrefix2Index,sql.indexOf(labelSuffix)+1);
        		injectType = 1;
			} // 如果两个都存在，则判断哪个在前
        	else if(labelPrefix1Index>0 && labelPrefix2Index>0 && labelPrefix1Index<labelPrefix2Index){
        		sqlParamName = sql.substring(labelPrefix1Index,sql.indexOf(labelSuffix)+1);
        		injectType = 0;
			}
        	else if(labelPrefix1Index>0 && labelPrefix2Index>0 && labelPrefix1Index>labelPrefix2Index){
        		sqlParamName = sql.substring(labelPrefix2Index,sql.indexOf(labelSuffix)+1);
        		injectType = 1;
			}
        	else {
				continue;
			}
        	// 将占位符变为？
            sql = sql.replace(sqlParamName,"?");
        	// 将前缀后缀去掉。
            if(injectType==0) {
            	params.add(sqlParamName.replace("${","").replace("}",""));
            	paramInjectTypes.add(0);
            }
            else {
            	params.add(sqlParamName.replace("#{","").replace("}",""));
            	paramInjectTypes.add(1);
			}
            
        }
        return sql;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public List<String> getParam() {
        return params;
    }

    public void setParam(List<String> params) {
        this.params = params;
    }
    
    public List<Integer> getParamInjectType() {
        return paramInjectTypes;
    }

    public void setParamInjectType(List<Integer> paramInjectTypes) {
        this.paramInjectTypes = paramInjectTypes;
    }
    

    public Integer getExecuteType() {
        return executeType;
    }

    public void setExecuteType(Integer executeType) {
        this.executeType = executeType;
    }
}

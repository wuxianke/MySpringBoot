package com.wu.mybatis.handler;

import com.wu.mybatis.core.MapperHelper;
import com.wu.mybatis.core.MethodDetails;
import com.wu.mybatis.core.SqlSource;
import com.wu.mybatis.transaction.TransactionManager;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;


/**
 * 处理sql语句和传进来的参数，并对不同的${} #{}占位符进行不同的处理。
 */
public class PreparedStatementHandler {

    private Method method;

    private TransactionManager transactionManager;

    private Connection connection;

    private Object[] args; // 客户端发送的参数值，也就是实际接收到的参数值。

    public PreparedStatementHandler(TransactionManager transactionManager,Method method, Object[] args) throws SQLException {
        this.method = method;
        this.transactionManager = transactionManager;
        this.connection = transactionManager.getConnection();
        this.args = args;
    }

    public PreparedStatement generateStatement() throws SQLException {
        // 获得方法的详情类
        MethodDetails methodDetails = MapperHelper.getMethodDetails(method);
        SqlSource sqlSource = methodDetails.getSqlSource();
        Class<?>[] parameterTypes = methodDetails.getParameterTypes();
        List<String> parameterNames = methodDetails.getParameterNames();
        List<String> params = sqlSource.getParam();
        List<Integer> paramInjectTypes = sqlSource.getParamInjectType();
        String sql = sqlSource.getSql();
        //先对${}参数进行字符串替换,也就是先生成String的sql语句。
        String parsedSql = parseSql(sql, parameterTypes, parameterNames, params, paramInjectTypes, args);
        PreparedStatement preparedStatement = connection.prepareStatement(parsedSql);
        // 再对#{}动态注入参数，可以直接对语句进行注入，而不用先生成String语句。
        preparedStatement = typeInject(preparedStatement, parameterTypes, parameterNames, params, paramInjectTypes, args);
        return preparedStatement;
    }

    /**
     * preparedStatement构建，注入#{ }参数
     *
     * @param preparedStatement
     * @param parameterTypes
     * @param parameterNames
     * @param params
     * @param paramInjectTypes
     * @param args
     * @return PreparedStatement
     */
    private PreparedStatement typeInject(PreparedStatement preparedStatement, Class<?>[] parameterTypes, List<String> parameterNames, List<String> params, List<Integer> paramInjectTypes, Object[] args) throws SQLException {
        for (int i = 0; i < parameterNames.size(); i++) {
            String parameterName = parameterNames.get(i);
            Class<?> parameterType = parameterTypes[i];
            int injectIndex = params.indexOf(parameterName);
            if (paramInjectTypes.get(injectIndex) == 0) {
                continue;
            }
            if (String.class.equals(parameterType)) {
                //此处是判断sql中是否有待注入的名称({name})和方法内输入对象名(name)相同，若相同，则直接注入
                if (injectIndex >= 0) {
                    preparedStatement.setString(injectIndex + 1, (String) args[i]);
                }
            } else if (Integer.class.equals(parameterType) || int.class.equals(parameterType)) {
                if (injectIndex >= 0) {
                    preparedStatement.setInt(injectIndex + 1, (Integer) args[i]);
                }
            } else if (Float.class.equals(parameterType) || float.class.equals(parameterType)) {
                if (injectIndex >= 0) {
                    preparedStatement.setFloat(injectIndex + 1, (Float) args[i]);
                }
            } else if (Double.class.equals(parameterType) || double.class.equals(parameterType)) {
                if (injectIndex >= 0) {
                    preparedStatement.setDouble(injectIndex + 1, (Double) args[i]);
                }
            } else if (Long.class.equals(parameterType) || long.class.equals(parameterType)) {
                if (injectIndex >= 0) {
                    preparedStatement.setLong(injectIndex + 1, (Long) args[i]);
                }
            }
        }
        return preparedStatement;
    }


    /**
     * 对${ }参数进行字符串替换
     *
     * @param sql
     * @param parameterTypes
     * @param parameterNames
     * @param params
     * @param paramInjectTypes
     * @param args
     * @return 处理好的sql语句
     */
    private String parseSql(String sql, Class<?>[] parameterTypes, List<String> parameterNames, List<String> params, List<Integer> paramInjectTypes, Object[] args) {
        StringBuilder sqlBuilder = new StringBuilder(sql);
        Integer index = sqlBuilder.indexOf("?");
        Integer i = 0; // 记录第几个参数需要拼接。
        while (index > 0 && i < paramInjectTypes.size()) {
            // 如果注入类型是1，则表示动态注入，跳过。
            if (paramInjectTypes.get(i) == 1) {
                i++;
                continue;
            }
            String param = params.get(i);
            int paramIndex = parameterNames.indexOf(param);
            // 取出对应的参数值
            Object arg = args[paramIndex];
            Class<?> type = parameterTypes[paramIndex];
            String injectValue = "";
            if (String.class.equals(type)) {
                injectValue = "'" + (String) arg + "'";
            } else if (Integer.class.equals(type)) {
                injectValue = Integer.toString((Integer) arg);
            } else if (Float.class.equals(type)) {
                injectValue = Float.toString((Float) arg);
            } else if (Double.class.equals(type)) {
                injectValue = Double.toString((Double) arg);
            } else if (Long.class.equals(type)) {
                injectValue = Long.toString((Long) arg);
            } else if (Short.class.equals(type)) {
                injectValue = Short.toString((Short) arg);
            }
            sqlBuilder.replace(index, index + 1, injectValue);
            index = sqlBuilder.indexOf("?");
            i++;
        }
        return sqlBuilder.toString();
    }

    /**
     * 关闭连接
     * @throws SQLException
     */
    public void closeConnection() throws SQLException{
        transactionManager.close();
    }
}

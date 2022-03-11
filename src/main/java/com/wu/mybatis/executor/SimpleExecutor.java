package com.wu.mybatis.executor;

import com.wu.mybatis.core.MapperHelper;
import com.wu.mybatis.core.SqlResultCache;
import com.wu.mybatis.handler.PreparedStatementHandler;
import com.wu.mybatis.handler.ResultSetHandler;
import com.wu.mybatis.transaction.TransactionFactory;
import com.wu.mybatis.transaction.TransactionManager;
import com.wu.mybatis.transaction.TransactionStatus;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SimpleExecutor implements Executor {
    public TransactionManager transactionManager;

    public SqlResultCache sqlResultCache;


    /**
     * 通过构造方法，设置是否开启事务和缓存。
     *
     * @param openTransaction 是否开启事务
     * @param openCache       是否开启sql缓存
     */
    public SimpleExecutor(boolean openTransaction, boolean openCache) {
        if (openCache) {
            this.sqlResultCache = new SqlResultCache();
        }
        if (openTransaction) {
            this.transactionManager = TransactionFactory.newTransaction(Connection.TRANSACTION_REPEATABLE_READ, false);
        } else {
            this.transactionManager = TransactionFactory.newTransaction();
        }
    }

    /**
     * 执行select语句
     *
     * @param method 方法
     * @param args   参数值
     * @param <E>    返回类型
     * @return
     * @throws Exception
     */
    @Override
    public <E> List<E> select(Method method, Object[] args) throws Exception {
        // 首先得到缓存key.
        String cacheKey = generateCacheKey(method, args);
        if (sqlResultCache != null && sqlResultCache.getCache(cacheKey) != null) {
            System.out.println("this is cache");
            // 由于一个key可能查出多个结果，因此返回list类型。
            return (List<E>) sqlResultCache.getCache(cacheKey);
        }
        //生成PreparedStatement对象，注入参数，并执行查询
        PreparedStatementHandler preparedStatementHandler = new PreparedStatementHandler(transactionManager, method, args);
        PreparedStatement preparedStatement = (PreparedStatement) preparedStatementHandler.generateStatement();
        ResultSet resultSet = null;
        // 执行查询
        preparedStatement.executeQuery();
        resultSet = preparedStatement.getResultSet();
        //对结果集resultSet进行解析，映射为实例对象
        Class<?> returnType = MapperHelper.getMethodDetails(method).getReturnType();
        if (returnType == null || void.class.equals(returnType)) {
            preparedStatement.close();
            preparedStatementHandler.closeConnection();
            return null;
        } else {
            ResultSetHandler resultSetHandler = new ResultSetHandler(returnType, resultSet);
            List<E> res = resultSetHandler.handle();
            if (sqlResultCache != null) {
                // 将查询关键词和查询结果放入缓存中。
                sqlResultCache.putCache(cacheKey, res);
            }
            preparedStatement.close();
            resultSet.close();
            preparedStatementHandler.closeConnection();
            return res;
        }
    }

    /**
     * 生成缓存sql执行结果的key
     *
     * @param method
     * @param args
     * @return 缓存sql的key。
     */
    private String generateCacheKey(Method method, Object args[]) {
        StringBuilder cacheKey = new StringBuilder(method.getDeclaringClass().getName() + method.getName());
        for (Object o : args) {
            cacheKey.append(o.toString());
        }
        return cacheKey.toString();
    }

    /**
     * 执行update,insert,delete等sql语句
     *
     * @param method 调用的sql方法
     * @param args   从客户端获得的参数值
     * @return
     * @throws SQLException
     */
    @Override
    public int update(Method method, Object[] args) throws SQLException {
        PreparedStatementHandler preparedStatementHandler = null;
        PreparedStatement preparedStatement = null;
        Integer count = 0;
        if (sqlResultCache != null) {
            sqlResultCache.cleanCache();
        }
        try {
            preparedStatementHandler = new PreparedStatementHandler(transactionManager, method, args);
            preparedStatement = preparedStatementHandler.generateStatement();
            count = preparedStatement.executeUpdate();
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            preparedStatementHandler.closeConnection();
        }
        return count;
    }

    @Override
    public void commit(TransactionStatus status) throws SQLException {
        transactionManager.commit(status);
    }

    @Override
    public void rollback() throws SQLException {
        transactionManager.rollback();
    }

    @Override
    public void close() throws SQLException {
        transactionManager.close();
    }
}

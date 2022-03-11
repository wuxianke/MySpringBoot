package com.wu.mybatis.transaction;

import com.wu.mybatis.datasource.DataSource;
import com.wu.mybatis.datasource.NormalDataSource;
import com.wu.mybatis.datasource.PoolDataSource;
import com.wu.spring.utils.ConfigUtil;

/**
 * TransactionManager工厂类
 *
 * @author Cactus
 */
public class TransactionFactory {
    private static volatile TransactionManager transaction = null;

    /**
     * 生成一个TransactionManager实例，并且是单例的
     *
     * @param level  事务的隔离级别
     * @param autoCommmit 是否自动提交事务
     * @return
     */
    public static TransactionManager newTransaction(Integer level, Boolean autoCommmit) {
        if (transaction == null) {
            synchronized (TransactionManager.class) {
                if (transaction == null) {
                    DataSource dataSource = null;
                    //根据配置决定是否使用数据库连接池
                    if (ConfigUtil.isDataSourcePool()) {
                        dataSource = new PoolDataSource(ConfigUtil.getJdbcDriver(), ConfigUtil.getJdbcUrl(), ConfigUtil.getJdbcUsername(), ConfigUtil.getJdbcPassword(),
                                ConfigUtil.getDataSourcePoolMaxSize(), ConfigUtil.getDataSourcePoolWaitTimeMill());
                    } else {
                        dataSource = new NormalDataSource(ConfigUtil.getJdbcDriver(), ConfigUtil.getJdbcUrl(), ConfigUtil.getJdbcUsername(), ConfigUtil.getJdbcPassword());
                    }

                    transaction = new SimpleTransactionManager(dataSource, level, autoCommmit);
                    return transaction;
                }
            }
        }
        return transaction;
    }

    public static TransactionManager newTransaction() {
        if (transaction == null) {
            synchronized (TransactionManager.class) {
                if (transaction == null) {
                    DataSource dataSource = null;
                    if (ConfigUtil.isDataSourcePool()) {
                        dataSource = new PoolDataSource(ConfigUtil.getJdbcDriver(), ConfigUtil.getJdbcUrl(), ConfigUtil.getJdbcUsername(), ConfigUtil.getJdbcPassword(),
                                ConfigUtil.getDataSourcePoolMaxSize(), ConfigUtil.getDataSourcePoolWaitTimeMill());
                    } else {
                        dataSource = new NormalDataSource(ConfigUtil.getJdbcDriver(), ConfigUtil.getJdbcUrl(), ConfigUtil.getJdbcUsername(), ConfigUtil.getJdbcPassword());
                    }
                    transaction = new SimpleTransactionManager(dataSource);
                    return transaction;
                }
            }
        }
        return transaction;
    }
}

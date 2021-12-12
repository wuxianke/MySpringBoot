package com.wu.mybatis.executor;

/**
 * 执行器工厂
 * @author Cactus
 *
 */
public class ExecutorFactory {
	public static Executor getExecutor(){
		Executor executor = new SimpleExecutor(false, false);
		return executor;
	}
}

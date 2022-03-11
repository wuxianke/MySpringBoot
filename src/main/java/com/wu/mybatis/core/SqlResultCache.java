package com.wu.mybatis.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Cactus
 */
public class SqlResultCache {
    /**
     * Sql语句的执行缓存， 用ConcurrentHashMap做缓存，key为方法类加方法名，val为查询值(可能为列表形式)
     */
    private static Map<String,Object> map = new ConcurrentHashMap<>();

    public void putCache(String key, Object val) {
        map.put(key,val);
    }
    
    public Object getCache(String key) {
        return map.get(key);
    }

    public void cleanCache() {
        map.clear();
    }

    public int getSize() {
        return map.size();
    }

    public void removeCache(String key) {
        map.remove(key);
    }
}
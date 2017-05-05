package com.s3u4.jdbc.pool;

import java.util.HashMap;
import java.util.Map;

/**
 * 连接池管理器
 * Created by Captain on 5/5/17.
 */
public final class S3U4PoolManager {

    private S3U4PoolManager(){
    }

    /**
     * 存储多配置连接池
     */
    private static final Map<String,S3U4Pool> pools = new HashMap<>();

    /**
     * 获取指定连接池,单例获取,防止多线程数据错乱
     * @param key   s3u4-pool.peoperties 配置文件的code
     * @return
     */
    public static S3U4Pool getPool(String key) {
        if ( !pools.containsKey(key) ){
            synchronized (S3U4PoolManager.class) {
                if ( !pools.containsKey(key) ){
                    pools.put(key,new S3U4PoolImpl(key));
                }
            }
        }
        return pools.get(key);
    }

}

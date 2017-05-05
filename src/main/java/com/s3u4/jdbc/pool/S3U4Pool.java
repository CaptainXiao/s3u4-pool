package com.s3u4.jdbc.pool;

import java.sql.Connection;

/**
 * 连接池接口定义
 * Created by Captain on 5/5/17.
 */
public interface S3U4Pool {

    /**
     * 获取连接池的一个连接
     * @return
     */
    Connection getConnection();

    /**
     * 释放连接
     * @param conn
     */
    void close(Connection conn);

}

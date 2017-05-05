package com.s3u4.jdbc.pool;

import java.sql.Connection;

/**
 * 自定义连接
 * Created by Captain on 5/5/17.
 */
public final class S3U4Connection {

    /** 原始的数据库连接 **/
    private Connection connection;
    /** 连接是否正在使用中 **/
    private boolean isBusy;
    /** 最近一次活跃活跃时间 **/
    private long lastActiveTime;

    public S3U4Connection(Connection connection){
        this.connection = connection;
        this.isBusy = false;
        lastActiveTime = System.currentTimeMillis();
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public boolean isBusy() {
        return isBusy;
    }

    public void setBusy(boolean busy) {
        isBusy = busy;
    }

    public long getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }
}

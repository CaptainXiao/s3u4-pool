package com.s3u4.jdbc.pool;

import com.s3u4.util.PropertiesUtil;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * s3u4 连接池具体实现
 * Created by Captain on 5/5/17.
 */
public class S3U4PoolImpl implements S3U4Pool {

    private List<S3U4Connection> connections;
    private String jdbcDriver;
    private String jdbcUrl;
    private String jdbcUsername;
    private String jdbcPassword;
    private int stepSize;
    private int maxPoolSize;
    private int minPoolSize;
    private int initPoolSize;

    /** 默认扩容步长 **/
    private static final String default_step_size = "10";
    /** 初始胡连接池连接数 **/
    private static final String default_pool_init_size = "10";
    /** 默认连接池最大连接数 **/
    private static final String default_pool_max_size = "200";
    /** 默认连接池最小连接数 **/
    private static final String default_pool_min_size = "10";

    /**
     * 连接池构造器
     * @param key
     */
    protected S3U4PoolImpl(String key){
        if ( key == null || key.trim().length() < 1 ){
            throw new RuntimeException("Error Param");
        }
        try {
            connections = new ArrayList<>();
            init(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 判断是否有空闲的数据库连接
     * @return
     */
    private synchronized boolean hasFreeConnection(){
        for (S3U4Connection conn : connections){
            if ( !conn.isBusy() ){
                return true;
            }
        }
        return false;
    }

    /**
     * 释放连接
     */
    public synchronized void close(Connection conn){
        for ( S3U4Connection s3u4Conn : connections ){
            if ( s3u4Conn != null && s3u4Conn.getConnection() == conn ){
                s3u4Conn.setBusy(false);
                return;
            }
        }
    }

    /**
     * 获取一个连接
     * @return
     */
    @Override
    public synchronized Connection getConnection() {
        // 获取一个空闲的数据库连接
        for ( S3U4Connection conn : connections ){
            if ( !conn.isBusy() ){
                synchronized (this){
                    if ( !conn.isBusy() ){
                        conn.setBusy(true);
                        conn.setLastActiveTime(System.currentTimeMillis());
                        return conn.getConnection();
                    }
                }
            }
        }
        // 如果没有空闲的数据库连接,则创建新连接
        synchronized (this){
            if ( !hasFreeConnection() ){
                if ( connections.size() < maxPoolSize ){
                    try {
                        createRealConnection(connections.size() + stepSize > maxPoolSize ? maxPoolSize - connections.size() : stepSize);
                    } catch (SQLException e) {
                        throw new RuntimeException("Create Connection Error",e);
                    }
                } else {
                    throw new RuntimeException("Max pool Size : " + maxPoolSize);
                }
            }
        }
        return getConnection();
    }

    /**
     * 创建指定数量的数据库连接
     * @param count
     * @throws SQLException
     */
    private synchronized void createRealConnection(int count) throws SQLException {
        for ( int i = 0 ; i < count ; i++ ){
            Connection conn = DriverManager.getConnection(jdbcUrl,jdbcUsername,jdbcPassword);
            S3U4Connection pkgConn = new S3U4Connection(conn);
            connections.add(pkgConn);
        }
    }

    /**
     * 初始化连接
     * @param key
     */
    private void init(String key) throws Exception {
        InputStream is = S3U4PoolImpl.class.getClassLoader().getResourceAsStream("s3u4-pool.properties");
        Properties prop = new Properties();
        prop.load(is);

        jdbcDriver = PropertiesUtil.getProperty(prop,"s3u4."+key+".jdbcDriver");
        jdbcUrl = PropertiesUtil.getProperty(prop,"s3u4."+key+".jdbcUrl");
        jdbcUsername = PropertiesUtil.getProperty(prop,"s3u4."+key+".jdbcUsername");
        jdbcPassword = PropertiesUtil.getProperty(prop,"s3u4."+key+".jdbcPassword");

        stepSize = Integer.parseInt(PropertiesUtil.getProperty(prop,"s3u4.stepSize",default_step_size));
        maxPoolSize = Integer.parseInt(PropertiesUtil.getProperty(prop,"s3u4.maxPoolSize",default_pool_max_size));
        minPoolSize = Integer.parseInt(PropertiesUtil.getProperty(prop,"s3u4.minPoolSize",default_pool_min_size));
        initPoolSize = Integer.parseInt(PropertiesUtil.getProperty(prop,"s3u4.initialPoolSize",default_pool_init_size));

        if ( initPoolSize < 0 || initPoolSize < minPoolSize || initPoolSize > maxPoolSize ){
            throw new RuntimeException("Invalid pool size , please check them");
        }

        try {
            Driver driver = (Driver) Class.forName(jdbcDriver).newInstance();
            DriverManager.registerDriver(driver);
        } catch (Exception e){
            throw new RuntimeException("init pool error",e);
        }

        // 创建连接
        createRealConnection(initPoolSize);
    }

}

package com.s3u4.jdbc.pool;

import com.s3u4.util.PropertiesUtil;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    /** 默认两分钟未使用的连接直接释放 **/
    private static final long default_release_time = 2 * 60 * 60 * 1000;

    /**
     * 连接池构造器
     * @param key
     */
    protected S3U4PoolImpl(String key){
        if ( key == null || key.trim().length() < 1 ){
            throw new RuntimeException("Error Param");
        }
        try {
            // 多线程环境下遍历 list 是线程安全的
            connections = new CopyOnWriteArrayList<>();
            init(key);

            // 开启释放长时间未使用的连接定时调度任务
            releaseSchedule();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 释放连接线程
     */
    private class ReleaseConnection implements Runnable {
        public void run(){
            // 默认如果
            if ( connections.size() > initPoolSize ){
                for ( S3U4Connection conn : connections ){
                    if ( !conn.isBusy() && (System.currentTimeMillis() - conn.getLastActiveTime() > default_release_time) && connections.size() > initPoolSize ){
                        synchronized (conn){
                            if ( !conn.isBusy() ){
                                conn.setBusy(true);
                                try{
                                    conn.getConnection().close();
                                    connections.remove(conn);
                                } catch (Exception e){
                                    throw new RuntimeException("Close Connection Error",e);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 定时任务,释放长时间未使用的数据库连接
     * 每隔一段时间就触发释放操作
     */
    private void releaseSchedule(){
        ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
        exec.scheduleAtFixedRate(new ReleaseConnection(), 1000, default_release_time, TimeUnit.MILLISECONDS);
    }

    /**
     * 判断是否有空闲的数据库连接
     * @return
     */
    private boolean hasFreeConnection(){
        for (S3U4Connection conn : connections){
            if ( conn != null && !conn.isBusy() ){
                return true;
            }
        }
        return false;
    }

    /**
     * 释放连接
     */
    public void close(Connection conn){
        for ( S3U4Connection s3u4Conn : connections ){
            if ( s3u4Conn != null && s3u4Conn.getConnection() == conn ){
                s3u4Conn.setBusy(false);
                return;
            }
        }
    }

    /**
     * 获取当前活跃的线程数量
     * @return
     */
    @Override
    public int getActiveCount() {
        return connections.size();
    }

    /**
     * 获取一个连接
     * @return
     */
    @Override
    public Connection getConnection() {
        // 获取一个空闲的数据库连接
        for ( S3U4Connection conn : connections ){
            if ( conn != null && !conn.isBusy() ){
                synchronized (conn){
                    if ( !conn.isBusy() ){
                        conn.setLastActiveTime(System.currentTimeMillis());
                        conn.setBusy(true);
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
                    // 当前可用线程已经达到最大
                    try {
                        this.wait(100); // 等待 100ms
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
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
    private void createRealConnection(int count) throws SQLException {
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

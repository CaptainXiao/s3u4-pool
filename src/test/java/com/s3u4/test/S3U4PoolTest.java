package com.s3u4.test;

import com.s3u4.jdbc.pool.v1.S3U4Pool;
import com.s3u4.jdbc.pool.S3U4PoolManager;

import java.sql.Connection;

/**
 * Created by Captain on 5/5/17.
 */
public class S3U4PoolTest {

    public static void main(String[] args) {
        S3U4Pool pool = S3U4PoolManager.getPool("myKey");

        // 1万个线程并发请求,查看活跃的线程数量
        for (int i = 0; i <= 10000; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Connection conn = pool.getConnection();
                    System.out.println("当前活跃的线程数量:" + pool.getActiveCount());
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    pool.close(conn);
                }
            }).start();
        }

        try {
            Thread.sleep(5000);
        } catch (Exception e) {
            // ignore
        }

        System.out.println("当前活跃的线程数量:" + pool.getActiveCount());

    }

}

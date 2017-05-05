package com.s3u4.test;

import com.s3u4.jdbc.pool.S3U4Pool;
import com.s3u4.jdbc.pool.S3U4PoolManager;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Captain on 5/5/17.
 */
public class S3U4PoolTest {

    public static void main(String[] args) {
        S3U4Pool pool = S3U4PoolManager.getPool("code");
        Set<String> set = new HashSet<>();
        for (int i = 0; i <= 100; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Connection conn = pool.getConnection();
                    System.out.println(conn);
                    set.add(String.valueOf(conn));
                    pool.close(conn);
                }
            }).start();
        }

        try {
            Thread.sleep(5000);
        } catch (Exception e) {
            // ignore
        }

        set.stream().forEach(key -> {
            System.out.println(key);
        });

        System.out.println(set.size());

    }

}

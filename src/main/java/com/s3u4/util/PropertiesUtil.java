package com.s3u4.util;

import java.util.Properties;

/**
 * Created by Captain on 5/5/17.
 */
public class PropertiesUtil {

    /**
     * 获取属性值,如不存在,则返回默认值
     * @param key   参数key
     * @param defaultValue  如果参数不存在,则返回默认参数
     * @return
     */
    public static String getProperty(Properties prop, String key, String defaultValue){
        try {
            String value = prop.getProperty(key);
            if( value == null || value.trim().length() < 1 ){
                return defaultValue;
            }
            return value;
        } catch (Exception e){
            return defaultValue;
        }
    }

    /**
     * 获取参数
     * @param prop
     * @param key   参数key
     * @return
     */
    public static String getProperty(Properties prop,String key){
        try {
            String value = prop.getProperty(key);
            if( value == null || value.trim().length() < 1 ){
                throw new RuntimeException("Param ["+key+"] doesn't exist!");
            }
            return value;
        } catch (Exception e){
            throw new RuntimeException("Param ["+key+"] doesn't exist!",e);
        }
    }

}

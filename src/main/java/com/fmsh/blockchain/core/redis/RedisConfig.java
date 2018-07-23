package com.fmsh.blockchain.core.redis;

import java.util.ResourceBundle;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/7/23 15:02
 * @Description:
 */
public class RedisConfig {

    private RedisConfig() {}

    private static final ResourceBundle bundle = ResourceBundle.getBundle("redis");

    private static String nodes;

    private static Integer maxTotal;

    private static Integer minIdle;

    private static Integer maxIdle;

    private static Integer maxWaitMillis;

    private static Boolean testWhileIdle;

    private static Integer timeout;

    private static Integer maxRedirects;

    static {
        nodes = bundle.getString("core.client.redis.nodes");
        maxTotal = Integer.valueOf(bundle.getString("core.client.redis.maxTotal"));
        minIdle = Integer.valueOf(bundle.getString("core.client.redis.minIdle"));
        maxIdle = Integer.valueOf(bundle.getString("core.client.redis.maxIdle"));
        maxWaitMillis = Integer.valueOf(bundle.getString("core.client.redis.maxWaitMillis"));
        testWhileIdle = Boolean.valueOf(bundle.getString("core.client.redis.testWhileIdle"));
        timeout = Integer.valueOf(bundle.getString("core.client.redis.timeout"));
        maxRedirects = Integer.valueOf(bundle.getString("core.client.redis.maxRedirects"));
    }

    public static String getNodes() {
        return nodes;
    }

    public static Integer getMaxTotal() {
        return maxTotal;
    }

    public static Integer getMinIdle() {
        return minIdle;
    }

    public static Integer getMaxIdle() {
        return maxIdle;
    }

    public static Integer getMaxWaitMillis() {
        return maxWaitMillis;
    }

    public static Boolean getTestWhileIdle() {
        return testWhileIdle;
    }

    public static Integer getTimeout() {
        return timeout;
    }

    public static Integer getMaxRedirects() {
        return maxRedirects;
    }
}

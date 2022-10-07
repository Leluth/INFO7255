package com.info7255.medicalplan.dao;

import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisPooled;

import java.util.Map;
import java.util.Set;

/**
 * @author Shaoshuai Xu
 * @version 1.0
 * @description: MedicalPlanDAO
 * @date 2022/10/6 21:44
 */
@Repository
public class MedicalPlanDAO {
    private final static String REDIS_URL = "localhost";
    private final static int PORT = 6379;
    private final JedisPooled jedis = new JedisPooled(REDIS_URL, PORT);

    public void sadd(String key, String value) {
        jedis.sadd(key, value);
    }

    public Set<String> sMembers(String key) {
        return jedis.smembers(key);
    }

    public void hSet(String key, String field, String value) {
        jedis.hset(key, field, value);
    }

    public String hGet(String key, String field) {
        return jedis.hget(key, field);
    }

    public Map<String, String> hGetAll(String key) {
        return jedis.hgetAll(key);
    }

    public boolean existsKey(String key) {
        return jedis.exists(key);
    }

    public Set<String> getKeysByPattern(String pattern) {
        return jedis.keys(pattern);
    }

    public long deleteKeys(String[] keys) {
        return jedis.del(keys);
    }
}

package com.example.aichat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 缓存服务 - 封装Redis操作，支持优雅降级
 * 当Redis不可用时，所有操作静默失败返回null/false，不影响业务流程
 */
@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 写入缓存（带TTL）
     */
    public void set(String key, Object value, Duration ttl) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.debug("Redis写入失败(降级忽略), key={}, err={}", key, e.getMessage());
        }
    }

    /**
     * 写入缓存（带TTL秒数）
     */
    public void set(String key, Object value, long ttlSeconds) {
        set(key, value, Duration.ofSeconds(ttlSeconds));
    }

    /**
     * 读取缓存
     */
    public Object get(String key) {
        if (redisTemplate == null) return null;
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logger.debug("Redis读取失败(降级忽略), key={}, err={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 读取缓存并转为指定类型
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (value == null) return null;
        try {
            return (T) value;
        } catch (ClassCastException e) {
            logger.warn("Redis缓存类型转换失败, key={}, expected={}", key, type.getSimpleName());
            return null;
        }
    }

    /**
     * 删除缓存
     */
    public boolean delete(String key) {
        if (redisTemplate == null) return false;
        try {
            Boolean result = redisTemplate.delete(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            logger.debug("Redis删除失败(降级忽略), key={}, err={}", key, e.getMessage());
            return false;
        }
    }

    /**
     * 判断key是否存在
     */
    public boolean exists(String key) {
        if (redisTemplate == null) return false;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            logger.debug("Redis存在性检查失败(降级忽略), key={}", key);
            return false;
        }
    }

    /**
     * 递增（用于计数器）
     */
    public long increment(String key, long delta) {
        if (redisTemplate == null) return -1;
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            logger.debug("Redis递增失败(降级忽略), key={}", key);
            return -1;
        }
    }

    /**
     * 为已存在的 key 设置过期时间（用于计数器首次创建后补 TTL）。
     */
    public boolean expire(String key, Duration ttl) {
        if (redisTemplate == null) return false;
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            logger.debug("Redis设置过期失败(降级忽略), key={}", key);
            return false;
        }
    }
}

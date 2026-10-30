package com.example.aichat.service;

import com.example.aichat.entity.DataPoolConfig;
import com.example.aichat.mapper.DataPoolConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据池配置服务。
 *
 * <p>负责 autoSaveEnabled / retentionDays / maxRecordsPerUser 等配置的持久化读写，
 * 带进程内缓存：读多写少，写操作后立即失效对应缓存项，保证管理端改配置后实时生效。</p>
 *
 * <p>默认值：autoSaveEnabled=true（数据池需要数据，默认开启自动采集）、
 * retentionDays=180、maxRecordsPerUser=10000。</p>
 */
@Service
public class DataPoolConfigService {

    private static final Logger logger = LoggerFactory.getLogger(DataPoolConfigService.class);

    public static final String KEY_AUTO_SAVE_ENABLED = "autoSaveEnabled";
    public static final String KEY_RETENTION_DAYS = "retentionDays";
    public static final String KEY_MAX_RECORDS_PER_USER = "maxRecordsPerUser";

    public static final boolean DEFAULT_AUTO_SAVE_ENABLED = true;
    public static final int DEFAULT_RETENTION_DAYS = 180;
    public static final int DEFAULT_MAX_RECORDS_PER_USER = 10000;

    private final DataPoolConfigMapper dataPoolConfigMapper;

    /** 配置缓存，key -> value；写操作后失效。 */
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public DataPoolConfigService(DataPoolConfigMapper dataPoolConfigMapper) {
        this.dataPoolConfigMapper = dataPoolConfigMapper;
    }

    /** 读取字符串配置，缺失时返回默认值。 */
    public String getString(String key, String defaultValue) {
        String cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            DataPoolConfig config = dataPoolConfigMapper.selectByKey(key);
            if (config != null && config.getConfigValue() != null) {
                cache.put(key, config.getConfigValue());
                return config.getConfigValue();
            }
        } catch (Exception e) {
            logger.warn("读取数据池配置失败, key={}, 使用默认值", key, e);
        }
        return defaultValue;
    }

    /** 读取布尔配置。 */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    /** 读取整型配置。 */
    public int getInt(String key, int defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("配置值非法, key={}, value={}, 使用默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    /** 写入配置（upsert），并失效缓存。 */
    public void setValue(String key, String value) {
        try {
            DataPoolConfig existing = dataPoolConfigMapper.selectByKey(key);
            if (existing == null) {
                DataPoolConfig config = new DataPoolConfig();
                config.setConfigKey(key);
                config.setConfigValue(value);
                config.setUpdatedAt(LocalDateTime.now());
                dataPoolConfigMapper.insert(config);
            } else {
                existing.setConfigValue(value);
                existing.setUpdatedAt(LocalDateTime.now());
                dataPoolConfigMapper.updateById(existing);
            }
            cache.put(key, value);
            logger.info("数据池配置已更新, key={}, value={}", key, value);
        } catch (Exception e) {
            logger.error("写入数据池配置失败, key={}", key, e);
            throw e;
        }
    }

    /** 失效指定 key 缓存。 */
    public void evict(String key) {
        cache.remove(key);
    }

    /** 失效全部缓存。 */
    public void evictAll() {
        cache.clear();
    }

    /** 自动采集开关。 */
    public boolean isAutoSaveEnabled() {
        return getBoolean(KEY_AUTO_SAVE_ENABLED, DEFAULT_AUTO_SAVE_ENABLED);
    }

    public void setAutoSaveEnabled(boolean enabled) {
        setValue(KEY_AUTO_SAVE_ENABLED, String.valueOf(enabled));
    }

    /** 数据保留天数。 */
    public int getRetentionDays() {
        return getInt(KEY_RETENTION_DAYS, DEFAULT_RETENTION_DAYS);
    }

    public void setRetentionDays(int days) {
        setValue(KEY_RETENTION_DAYS, String.valueOf(days));
    }

    /** 单用户最大记录数。 */
    public int getMaxRecordsPerUser() {
        return getInt(KEY_MAX_RECORDS_PER_USER, DEFAULT_MAX_RECORDS_PER_USER);
    }

    public void setMaxRecordsPerUser(int max) {
        setValue(KEY_MAX_RECORDS_PER_USER, String.valueOf(max));
    }
}

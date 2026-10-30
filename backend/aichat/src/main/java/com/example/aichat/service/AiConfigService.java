package com.example.aichat.service;

import com.example.aichat.dto.ai.AiConfigDTO;
import com.example.aichat.entity.AiConfig;
import com.example.aichat.mapper.AiConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 全局配置服务。
 *
 * <p>负责 defaultModel / temperature / maxTokens / requestTimeoutSec /
 * aiChatEnabled / maintenanceNotice 的持久化读写，带进程内缓存：读多写少，
 * 写操作后立即失效对应缓存项，保证管理端改配置后实时生效。模式参考 DataPoolConfigService。</p>
 *
 * <p>默认值：defaultModel=llama3.2:3b、temperature=0.7、maxTokens=2048、
 * requestTimeoutSec=60、aiChatEnabled=true、maintenanceNotice=""。</p>
 */
@Service
public class AiConfigService {

    private static final Logger logger = LoggerFactory.getLogger(AiConfigService.class);

    public static final String KEY_DEFAULT_MODEL = "defaultModel";
    public static final String KEY_TEMPERATURE = "temperature";
    public static final String KEY_MAX_TOKENS = "maxTokens";
    public static final String KEY_REQUEST_TIMEOUT_SEC = "requestTimeoutSec";
    public static final String KEY_AI_CHAT_ENABLED = "aiChatEnabled";
    public static final String KEY_MAINTENANCE_NOTICE = "maintenanceNotice";

    public static final String DEFAULT_DEFAULT_MODEL = "llama3.2:3b";
    public static final double DEFAULT_TEMPERATURE = 0.7d;
    public static final int DEFAULT_MAX_TOKENS = 2048;
    public static final int DEFAULT_REQUEST_TIMEOUT_SEC = 60;
    public static final boolean DEFAULT_AI_CHAT_ENABLED = true;
    public static final String DEFAULT_MAINTENANCE_NOTICE = "";

    private final AiConfigMapper aiConfigMapper;

    /** 配置缓存，key -> value；写操作后失效。 */
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public AiConfigService(AiConfigMapper aiConfigMapper) {
        this.aiConfigMapper = aiConfigMapper;
    }

    /** 读取字符串配置，缺失时返回默认值。 */
    public String getString(String key, String defaultValue) {
        String cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            AiConfig config = aiConfigMapper.selectByKey(key);
            if (config != null && config.getConfigValue() != null) {
                cache.put(key, config.getConfigValue());
                return config.getConfigValue();
            }
        } catch (Exception e) {
            logger.warn("读取AI配置失败, key={}, 使用默认值", key, e);
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

    /** 读取浮点配置。 */
    public double getDouble(String key, double defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("配置值非法, key={}, value={}, 使用默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    /** 写入配置（upsert），并刷新缓存。 */
    public void setValue(String key, String value) {
        AiConfig existing = aiConfigMapper.selectByKey(key);
        if (existing == null) {
            AiConfig config = new AiConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setUpdatedAt(LocalDateTime.now());
            aiConfigMapper.insert(config);
        } else {
            existing.setConfigValue(value);
            existing.setUpdatedAt(LocalDateTime.now());
            aiConfigMapper.updateById(existing);
        }
        cache.put(key, value);
        logger.info("AI配置已更新, key={}, value={}", key, value);
    }

    /** 失效指定 key 缓存。 */
    public void evict(String key) {
        cache.remove(key);
    }

    /** 失效全部缓存。 */
    public void evictAll() {
        cache.clear();
    }

    /** 读取全部配置为 DTO。 */
    public AiConfigDTO getConfig() {
        AiConfigDTO dto = new AiConfigDTO();
        dto.setDefaultModel(getString(KEY_DEFAULT_MODEL, DEFAULT_DEFAULT_MODEL));
        dto.setTemperature(getDouble(KEY_TEMPERATURE, DEFAULT_TEMPERATURE));
        dto.setMaxTokens(getInt(KEY_MAX_TOKENS, DEFAULT_MAX_TOKENS));
        dto.setRequestTimeoutSec(getInt(KEY_REQUEST_TIMEOUT_SEC, DEFAULT_REQUEST_TIMEOUT_SEC));
        dto.setAiChatEnabled(getBoolean(KEY_AI_CHAT_ENABLED, DEFAULT_AI_CHAT_ENABLED));
        dto.setMaintenanceNotice(getString(KEY_MAINTENANCE_NOTICE, DEFAULT_MAINTENANCE_NOTICE));
        return dto;
    }

    /** 更新全部配置项，并刷新缓存。 */
    public void updateConfig(AiConfigDTO dto) {
        if (dto.getDefaultModel() != null) {
            setValue(KEY_DEFAULT_MODEL, dto.getDefaultModel());
        }
        if (dto.getTemperature() != null) {
            setValue(KEY_TEMPERATURE, String.valueOf(dto.getTemperature()));
        }
        if (dto.getMaxTokens() != null) {
            setValue(KEY_MAX_TOKENS, String.valueOf(dto.getMaxTokens()));
        }
        if (dto.getRequestTimeoutSec() != null) {
            setValue(KEY_REQUEST_TIMEOUT_SEC, String.valueOf(dto.getRequestTimeoutSec()));
        }
        if (dto.getAiChatEnabled() != null) {
            setValue(KEY_AI_CHAT_ENABLED, String.valueOf(dto.getAiChatEnabled()));
        }
        if (dto.getMaintenanceNotice() != null) {
            setValue(KEY_MAINTENANCE_NOTICE, dto.getMaintenanceNotice());
        }
    }
}

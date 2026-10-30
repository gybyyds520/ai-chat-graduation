package com.example.aichat.service;

import com.example.aichat.dto.ai.AiConfigDTO;
import com.example.aichat.entity.AiConfig;
import com.example.aichat.mapper.AiConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AiConfigService 单元测试。
 * 覆盖：默认值读取、更新后回读、缓存失效。
 */
@ExtendWith(MockitoExtension.class)
class AiConfigServiceTest {

    @Mock
    private AiConfigMapper mapper;

    private AiConfigService service;

    @BeforeEach
    void setUp() {
        service = new AiConfigService(mapper);
    }

    private AiConfig cfg(String key, String value) {
        AiConfig c = new AiConfig();
        c.setConfigKey(key);
        c.setConfigValue(value);
        return c;
    }

    @Test
    void 配置缺失应返回默认值() {
        when(mapper.selectByKey(anyString())).thenReturn(null);

        AiConfigDTO dto = service.getConfig();

        assertEquals("llama3.2:3b", dto.getDefaultModel());
        assertEquals(0.7d, dto.getTemperature(), 1e-9);
        assertEquals(2048, dto.getMaxTokens());
        assertEquals(60, dto.getRequestTimeoutSec());
        assertTrue(dto.getAiChatEnabled());
        assertEquals("", dto.getMaintenanceNotice());
    }

    @Test
    void 更新配置后应回读新值() {
        // 第一次读默认值（DB 无记录），更新时 insert，再次读取命中缓存
        when(mapper.selectByKey(anyString())).thenReturn(null);

        AiConfigDTO update = new AiConfigDTO();
        update.setDefaultModel("qwen2.5:7b");
        update.setTemperature(0.2);
        update.setMaxTokens(4096);
        update.setRequestTimeoutSec(120);
        update.setAiChatEnabled(false);
        update.setMaintenanceNotice("维护中");
        service.updateConfig(update);

        AiConfigDTO dto = service.getConfig();
        assertEquals("qwen2.5:7b", dto.getDefaultModel());
        assertEquals(0.2d, dto.getTemperature(), 1e-9);
        assertEquals(4096, dto.getMaxTokens());
        assertEquals(120, dto.getRequestTimeoutSec());
        assertFalse(dto.getAiChatEnabled());
        assertEquals("维护中", dto.getMaintenanceNotice());
    }

    @Test
    void 更新后缓存应失效并刷新为新值() {
        // 先预置旧值并缓存，更新时 upsert，再读应得到新值且不再命中旧缓存
        when(mapper.selectByKey(eq("defaultModel")))
                .thenReturn(cfg("defaultModel", "llama3.2:3b"));

        assertEquals("llama3.2:3b", service.getConfig().getDefaultModel());
        verify(mapper, atLeastOnce()).selectByKey("defaultModel");

        when(mapper.selectByKey(eq("defaultModel")))
                .thenReturn(cfg("defaultModel", "mistral:7b"));

        AiConfigDTO update = new AiConfigDTO();
        update.setDefaultModel("mistral:7b");
        service.updateConfig(update);

        // 更新后 getConfig 应拿到新值（写入时已把新值放入缓存）
        assertEquals("mistral:7b", service.getConfig().getDefaultModel());
        verify(mapper, atLeastOnce()).updateById(any(AiConfig.class));
    }
}

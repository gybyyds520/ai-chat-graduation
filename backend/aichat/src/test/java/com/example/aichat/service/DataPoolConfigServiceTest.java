package com.example.aichat.service;

import com.example.aichat.entity.DataPoolConfig;
import com.example.aichat.mapper.DataPoolConfigMapper;
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
 * DataPoolConfigService 单元测试。
 * 覆盖：配置读取缓存、写入 upsert 与缓存失效、默认值。
 */
@ExtendWith(MockitoExtension.class)
class DataPoolConfigServiceTest {

    @Mock
    private DataPoolConfigMapper mapper;

    private DataPoolConfigService service;

    @BeforeEach
    void setUp() {
        service = new DataPoolConfigService(mapper);
    }

    private DataPoolConfig cfg(String key, String value) {
        DataPoolConfig c = new DataPoolConfig();
        c.setConfigKey(key);
        c.setConfigValue(value);
        return c;
    }

    @Test
    void 配置存在应从DB读取并缓存() {
        when(mapper.selectByKey("autoSaveEnabled")).thenReturn(cfg("autoSaveEnabled", "false"));

        boolean first = service.isAutoSaveEnabled();
        boolean second = service.isAutoSaveEnabled();

        assertFalse(first);
        assertFalse(second);
        // 第二次命中缓存，不再查 DB
        verify(mapper, times(1)).selectByKey("autoSaveEnabled");
    }

    @Test
    void 配置不存在应返回默认值() {
        when(mapper.selectByKey(anyString())).thenReturn(null);

        assertTrue(service.isAutoSaveEnabled(), "默认应开启");
        assertEquals(180, service.getRetentionDays(), "默认保留180天");
        assertEquals(10000, service.getMaxRecordsPerUser(), "默认单用户1万条");
    }

    @Test
    void 更新配置应upsert并失效缓存() {
        // 第一次写入时查无 -> insert；第二次写入前查到已有 -> updateById
        when(mapper.selectByKey(eq("retentionDays")))
                .thenReturn(null)
                .thenReturn(cfg("retentionDays", "90"));

        service.setRetentionDays(90);
        service.setRetentionDays(30);

        verify(mapper, times(1)).insert(any(DataPoolConfig.class));
        verify(mapper, times(1)).updateById(any(DataPoolConfig.class));
        // 写入后缓存为 30，再次读取命中缓存
        assertEquals(30, service.getRetentionDays());
        // 缓存命中，不再查 DB
        verify(mapper, times(2)).selectByKey("retentionDays");
    }
}

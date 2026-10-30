package com.example.aichat.service.impl;

import com.example.aichat.entity.PrivacySetting;
import com.example.aichat.mapper.PrivacySettingMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * PrivacyServiceImpl 单元测试。
 *
 * 覆盖点：
 *   1. 查询单条设置透传 mapper；
 *   2. 全量查询：JSON 值反序列化、非法 JSON 回退原始字符串；
 *   3. 更新：已存在则更新、不存在则插入；
 *   4. 删除：存在则删、不存在静默忽略。
 *
 * ObjectMapper 使用真实实例（Spy 仅为占位，实际走真实 JSON 解析）。
 */
@ExtendWith(MockitoExtension.class)
class PrivacyServiceImplTest {

    @Mock private PrivacySettingMapper privacySettingMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PrivacyServiceImpl privacyService;

    @Test
    void getSetting_透传mapper结果() {
        PrivacySetting s = new PrivacySetting();
        s.setKey("profileVisible");
        s.setValue("true");
        when(privacySettingMapper.findByUserAndKey(1L, "profileVisible")).thenReturn(s);

        assertSame(s, privacyService.getSetting(1L, "profileVisible"));
    }

    @Test
    void getAllSettings_JSON值应反序列化() {
        PrivacySetting json = new PrivacySetting();
        json.setKey("visibleFields");
        json.setValue("[\"nickname\",\"avatar\"]");
        when(privacySettingMapper.getSettingsByUserId(1L)).thenReturn(List.of(json));

        Map<String, Object> result = privacyService.getAllSettings(1L);

        assertTrue(result.get("visibleFields") instanceof List);
    }

    @Test
    void getAllSettings_非法JSON回退原始字符串() {
        PrivacySetting raw = new PrivacySetting();
        raw.setKey("note");
        raw.setValue("not-a-json-value");
        when(privacySettingMapper.getSettingsByUserId(1L)).thenReturn(List.of(raw));

        Map<String, Object> result = privacyService.getAllSettings(1L);

        assertEquals("not-a-json-value", result.get("note"));
    }

    @Test
    void updateSetting_已存在应更新() {
        PrivacySetting existing = new PrivacySetting();
        existing.setId(3L);
        existing.setKey("k");
        existing.setValue("old");
        when(privacySettingMapper.findByUserAndKey(1L, "k")).thenReturn(existing);

        PrivacySetting result = privacyService.updateSetting(1L, "k", "new");

        assertEquals("new", result.getValue());
        verify(privacySettingMapper).updateById(existing);
        verify(privacySettingMapper, never()).insert(any(PrivacySetting.class));
    }

    @Test
    void updateSetting_不存在应插入() {
        when(privacySettingMapper.findByUserAndKey(1L, "k")).thenReturn(null);

        PrivacySetting result = privacyService.updateSetting(1L, "k", "v");

        assertEquals(1L, result.getUserId());
        assertEquals("k", result.getKey());
        verify(privacySettingMapper).insert(any(PrivacySetting.class));    }

    @Test
    void deleteSetting_存在应删除() {
        PrivacySetting existing = new PrivacySetting();
        existing.setId(9L);
        when(privacySettingMapper.findByUserAndKey(1L, "k")).thenReturn(existing);

        privacyService.deleteSetting(1L, "k");

        verify(privacySettingMapper).deleteById(9L);
    }

    @Test
    void deleteSetting_不存在静默忽略() {
        when(privacySettingMapper.findByUserAndKey(1L, "k")).thenReturn(null);

        privacyService.deleteSetting(1L, "k");

        verify(privacySettingMapper, never()).deleteById(anyLong());
    }
}

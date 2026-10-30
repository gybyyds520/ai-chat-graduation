package com.example.aichat.event;

import com.example.aichat.entity.UserDataRecord;
import com.example.aichat.mapper.UserDataRecordMapper;
import com.example.aichat.service.DataPoolConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UserDataCaptureEventListener 单元测试（Mockito 隔离）。
 *
 * 覆盖：正常采集落库、autoSave 关闭跳过、去重跳过、空内容拒绝、隐私脱敏、完整性评分。
 */
@ExtendWith(MockitoExtension.class)
class UserDataCaptureEventListenerTest {

    @Mock
    private DataPoolConfigService configService;

    @Mock
    private UserDataRecordMapper userDataRecordMapper;

    private UserDataCaptureEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new UserDataCaptureEventListener(configService, userDataRecordMapper, new ObjectMapper());
    }

    private UserDataCaptureEvent event(String userId, String dataType, String content,
                                       Map<String, Object> metadata, String sourceId) {
        return new UserDataCaptureEvent(userId == null ? null : Long.valueOf(userId),
                dataType, content, metadata, sourceId, LocalDateTime.now());
    }

    @Test
    void 正常采集应落库并打满分() {
        when(configService.isAutoSaveEnabled()).thenReturn(true);
        when(userDataRecordMapper.selectByUniqueKey(any(), anyString(), anyString())).thenReturn(null);

        Map<String, Object> meta = new HashMap<>();
        meta.put("groupId", 100L);
        listener.onCapture(event("42", "group_chat", "这是一条长度足够的群聊消息内容", meta, "gmsg:1"));

        ArgumentCaptor<UserDataRecord> captor = ArgumentCaptor.forClass(UserDataRecord.class);
        verify(userDataRecordMapper).insert(captor.capture());
        UserDataRecord saved = captor.getValue();
        assertEquals(42L, saved.getUserId());
        assertEquals("group_chat", saved.getDataType());
        assertEquals("VALIDATED", saved.getStatus());
        assertEquals("PENDING_BACKUP", saved.getBackupStatus());
        assertEquals(100.0, saved.getIntegrityScore(), 0.01, "全维度满分应为100");
        assertEquals("100", saved.getGroupId(), "groupId 应从 metadata 提取");
    }

    @Test
    void autoSave关闭应跳过采集() {
        when(configService.isAutoSaveEnabled()).thenReturn(false);

        listener.onCapture(event("42", "group_chat", "任意内容", null, "gmsg:1"));

        verify(userDataRecordMapper, never()).insert(any(UserDataRecord.class));
        verify(userDataRecordMapper, never()).selectByUniqueKey(any(), anyString(), anyString());
    }

    @Test
    void 重复事件同主键不应重复插入() {
        when(configService.isAutoSaveEnabled()).thenReturn(true);
        when(userDataRecordMapper.selectByUniqueKey(eq(42L), eq("social_post"), anyString()))
                .thenReturn(new UserDataRecord());

        listener.onCapture(event("42", "social_post", "完全相同的帖子内容", null, "post:9"));

        verify(userDataRecordMapper, never()).insert(any(UserDataRecord.class));
    }

    @Test
    void 空内容事件应被拒绝不落库() {
        when(configService.isAutoSaveEnabled()).thenReturn(true);

        listener.onCapture(event("42", "social_post", "   ", null, "post:1"));

        verify(userDataRecordMapper, never()).insert(any(UserDataRecord.class));
    }

    @Test
    void 非法dataType应被拒绝() {
        when(configService.isAutoSaveEnabled()).thenReturn(true);

        listener.onCapture(event("42", "not_allowed_type", "任意内容", null, "x:1"));

        verify(userDataRecordMapper, never()).insert(any(UserDataRecord.class));
    }

    @Test
    void 隐私脱敏手机号邮箱应被替换() {
        when(configService.isAutoSaveEnabled()).thenReturn(true);
        when(userDataRecordMapper.selectByUniqueKey(any(), anyString(), anyString())).thenReturn(null);

        String raw = "我的手机是13812345678，邮箱是test@example.com，身份证110101199001011234";
        listener.onCapture(event("42", "private_chat", raw, null, "pmsg:1"));

        ArgumentCaptor<UserDataRecord> captor = ArgumentCaptor.forClass(UserDataRecord.class);
        verify(userDataRecordMapper).insert(captor.capture());
        String savedContent = captor.getValue().getContent();
        assertFalse(savedContent.contains("13812345678"), "手机号应被脱敏");
        assertTrue(savedContent.contains("***"), "手机号应替换为***");
        assertFalse(savedContent.contains("test@example.com"), "邮箱应被脱敏");
        assertTrue(savedContent.contains("***@***"), "邮箱应替换为***@***");
        assertFalse(savedContent.contains("110101199001011234"), "身份证应被脱敏");
    }

    @Test
    void 完整性评分短内容应扣分() {
        when(configService.isAutoSaveEnabled()).thenReturn(true);
        when(userDataRecordMapper.selectByUniqueKey(any(), anyString(), anyString())).thenReturn(null);

        // metadata 非空：内容非空40+metadata20+tags10+长度>=10得0+dataType10+captureTime10 = 90
        Map<String, Object> meta = new HashMap<>();
        meta.put("k", "v");
        listener.onCapture(event("42", "ai_chat", "你好", meta, "aic:1"));

        ArgumentCaptor<UserDataRecord> captor = ArgumentCaptor.forClass(UserDataRecord.class);
        verify(userDataRecordMapper).insert(captor.capture());
        assertEquals(90.0, captor.getValue().getIntegrityScore(), 0.01);
    }
}

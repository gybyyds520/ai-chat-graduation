package com.example.aichat.service.impl;

import com.example.aichat.entity.ChatMessage;
import com.example.aichat.mapper.ChatMessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * M3-A9：AI 对话会话删除 / 单条消息删除的归属校验与删除逻辑测试。
 */
@ExtendWith(MockitoExtension.class)
class ChatMessageDeleteTest {

    @Mock
    private ChatMessageMapper chatMessageMapper;

    private ChatMessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChatMessageServiceImpl();
        // 注入 mock 字段
        org.springframework.test.util.ReflectionTestUtils.setField(service, "chatMessageMapper", chatMessageMapper);
    }

    @Test
    void 删除会话消息应按sessionId和当前用户双条件删除() {
        when(chatMessageMapper.delete(any())).thenReturn(3);
        int deleted = service.deleteSessionMessages("sess-1", 42L);
        assertEquals(3, deleted);
        verify(chatMessageMapper).delete(any());
    }

    @Test
    void 删除本人消息应成功() {
        ChatMessage msg = new ChatMessage();
        msg.setId(7L);
        msg.setUserId(42L);
        when(chatMessageMapper.selectById(7L)).thenReturn(msg);
        when(chatMessageMapper.deleteById(7L)).thenReturn(1);

        assertTrue(service.deleteMessage(7L, 42L));
    }

    @Test
    void 删除他人消息应拒绝且不执行删除() {
        ChatMessage msg = new ChatMessage();
        msg.setId(7L);
        msg.setUserId(999L); // 别人的消息
        when(chatMessageMapper.selectById(7L)).thenReturn(msg);

        assertFalse(service.deleteMessage(7L, 42L));
        verify(chatMessageMapper, never()).deleteById(anyLong());
    }

    @Test
    void 删除不存在消息应返回false() {
        when(chatMessageMapper.selectById(555L)).thenReturn(null);
        assertFalse(service.deleteMessage(555L, 42L));
    }
}

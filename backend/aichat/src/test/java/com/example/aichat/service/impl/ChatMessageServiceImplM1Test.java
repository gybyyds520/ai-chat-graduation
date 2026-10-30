package com.example.aichat.service.impl;

import com.example.aichat.entity.ChatMessage;
import com.example.aichat.mapper.ChatMessageMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * M1 ①④：ChatMessageServiceImpl 新增方法（归属校验、内容编辑、下游删除、撤回）单元测试。
 */
@ExtendWith(MockitoExtension.class)
class ChatMessageServiceImplM1Test {

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @InjectMocks
    private ChatMessageServiceImpl service;

    private ChatMessage msg(Long id, Long userId, String senderType, LocalDateTime time) {
        ChatMessage m = new ChatMessage();
        m.setId(id);
        m.setUserId(userId);
        m.setSenderType(senderType);
        m.setContent("hello");
        m.setCreateTime(time);
        m.setSessionId("s1");
        return m;
    }

    @Test
    void findOwnedMessage_属本人_返回消息() {
        when(chatMessageMapper.selectById(1L)).thenReturn(msg(1L, 100L, "user", LocalDateTime.now()));
        assertNotNull(service.findOwnedMessage(1L, 100L));
    }

    @Test
    void findOwnedMessage_属他人_返回null() {
        when(chatMessageMapper.selectById(1L)).thenReturn(msg(1L, 999L, "user", LocalDateTime.now()));
        assertNull(service.findOwnedMessage(1L, 100L));
    }

    @Test
    void findOwnedMessage_不存在_返回null() {
        when(chatMessageMapper.selectById(any())).thenReturn(null);
        assertNull(service.findOwnedMessage(404L, 100L));
    }

    @Test
    void updateContent_存在_更新成功() {
        ChatMessage m = msg(1L, 100L, "user", LocalDateTime.now());
        when(chatMessageMapper.selectById(1L)).thenReturn(m);
        when(chatMessageMapper.updateById(any(ChatMessage.class))).thenReturn(1);
        assertTrue(service.updateContent(1L, "new content"));
        assertEquals("new content", m.getContent());
    }

    @Test
    void updateContent_不存在_返回false() {
        when(chatMessageMapper.selectById(any())).thenReturn(null);
        assertFalse(service.updateContent(404L, "x"));
    }

    @Test
    void recallMessage_改写为占位文案() {
        ChatMessage m = msg(1L, 100L, "user", LocalDateTime.now());
        when(chatMessageMapper.selectById(1L)).thenReturn(m);
        when(chatMessageMapper.updateById(any(ChatMessage.class))).thenReturn(1);
        assertTrue(service.recallMessage(1L));
        assertEquals(ChatMessageServiceImpl.RECALLED_PLACEHOLDER, m.getContent());
    }

    @Test
    void deleteAfterMessage_锚点不存在_返回0() {
        when(chatMessageMapper.selectById(any())).thenReturn(null);
        assertEquals(0, service.deleteAfterMessage("s1", 1L));
    }
}

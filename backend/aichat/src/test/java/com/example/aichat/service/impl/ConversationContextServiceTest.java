package com.example.aichat.service.impl;

import com.example.aichat.entity.ConversationContext;
import com.example.aichat.mapper.ConversationContextMapper;
import com.example.aichat.service.ConversationContextService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AI 模块 - 对话上下文服务单元测试（Mock Mapper，不连数据库）。
 *
 * 覆盖点：
 *   1. 空历史/空上下文 -> buildContextPrompt 返回空串；
 *   2. setContext 首次写入 insert、再次写入 update；
 *   3. 已有上下文 -> buildContextPrompt 拼接键值；
 *   4. getContext 无记录返回 null。
 */
@ExtendWith(MockitoExtension.class)
class ConversationContextServiceTest {

    @Mock
    private ConversationContextMapper mapper;

    @InjectMocks
    private ConversationContextServiceImpl service;

    private ConversationContext ctx(String key, String value) {
        ConversationContext c = new ConversationContext();
        c.setContextKey(key);
        c.setContextValue(value);
        return c;
    }

    @Test
    void 空上下文_buildContextPrompt返回空串() {
        when(mapper.getContextBySessionId(anyString())).thenReturn(new ArrayList<>());
        assertEquals("", service.buildContextPrompt("s1"));
    }

    @Test
    void getContext_无记录返回null() {
        when(mapper.getContextByKey(anyString(), anyString())).thenReturn(null);
        assertNull(service.getContext("s1", "missing"));
    }

    @Test
    void setContext_首次_应insert() {
        when(mapper.getContextByKey(eq("s1"), eq("k1"))).thenReturn(null);
        service.setContext("s1", 1L, 100L, "k1", "v1");
        verify(mapper).insert(any(ConversationContext.class));
        verify(mapper, never()).updateById(any(ConversationContext.class));
    }

    @Test
    void setContext_已存在_应update且不insert() {
        ConversationContext existing = ctx("k1", "old");
        when(mapper.getContextByKey(eq("s1"), eq("k1"))).thenReturn(existing);
        service.setContext("s1", 1L, 100L, "k1", "new");
        verify(mapper).updateById(existing);
        verify(mapper, never()).insert(any(ConversationContext.class));
        assertEquals("new", existing.getContextValue());
    }

    @Test
    void 已有上下文_buildContextPrompt应拼接键值() {
        List<ConversationContext> list = new ArrayList<>();
        list.add(ctx("user_emotion", "happy"));
        list.add(ctx("emotion_intensity", "0.8"));
        when(mapper.getContextBySessionId("s1")).thenReturn(list);
        String prompt = service.buildContextPrompt("s1");
        assertTrue(prompt.contains("user_emotion: happy"));
        assertTrue(prompt.contains("emotion_intensity: 0.8"));
    }
}

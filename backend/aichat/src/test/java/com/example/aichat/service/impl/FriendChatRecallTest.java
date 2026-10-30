package com.example.aichat.service.impl;

import com.example.aichat.entity.FriendChatMessage;
import com.example.aichat.mapper.FriendChatMessageMapper;
import com.example.aichat.mapper.FriendshipMapper;
import com.example.aichat.service.ContentAuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * M1 ④：好友聊天撤回（2 分钟窗口、仅本人）单元测试。
 */
@ExtendWith(MockitoExtension.class)
class FriendChatRecallTest {

    @Mock
    private FriendChatMessageMapper chatMessageMapper;
    @Mock
    private FriendshipMapper friendshipMapper;
    @Mock
    private ContentAuditService contentAuditService;

    private FriendChatServiceImpl service() {
        return new FriendChatServiceImpl(chatMessageMapper, friendshipMapper, contentAuditService);
    }

    private FriendChatMessage msg(Long id, Long senderId, LocalDateTime time) {
        FriendChatMessage m = new FriendChatMessage();
        m.setId(id);
        m.setSenderId(senderId);
        m.setReceiverId(200L);
        m.setContent("hi");
        m.setCreateTime(time);
        return m;
    }

    @Test
    void recall_本人且2分钟内_成功改写() {
        FriendChatMessage m = msg(1L, 100L, LocalDateTime.now().minusSeconds(30));
        when(chatMessageMapper.selectById(1L)).thenReturn(m);
        when(chatMessageMapper.updateById(any(FriendChatMessage.class))).thenReturn(1);

        assertTrue(service().recallMessage(1L, 100L, 2));
        assertEquals("消息已撤回", m.getContent());
        assertEquals("recall", m.getMessageType());
    }

    @Test
    void recall_非本人_拒绝() {
        FriendChatMessage m = msg(1L, 999L, LocalDateTime.now());
        when(chatMessageMapper.selectById(1L)).thenReturn(m);
        assertFalse(service().recallMessage(1L, 100L, 2));
    }

    @Test
    void recall_超过2分钟_拒绝() {
        FriendChatMessage m = msg(1L, 100L, LocalDateTime.now().minusMinutes(3));
        when(chatMessageMapper.selectById(1L)).thenReturn(m);
        assertFalse(service().recallMessage(1L, 100L, 2));
    }

    @Test
    void recall_消息不存在_拒绝() {
        when(chatMessageMapper.selectById(any())).thenReturn(null);
        assertFalse(service().recallMessage(404L, 100L, 2));
    }
}

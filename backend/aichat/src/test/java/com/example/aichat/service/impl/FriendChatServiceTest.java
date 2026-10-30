package com.example.aichat.service.impl;

import com.example.aichat.entity.FriendChatMessage;
import com.example.aichat.entity.Friendship;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.dto.audit.AuditResult;
import com.example.aichat.mapper.FriendChatMessageMapper;
import com.example.aichat.mapper.FriendshipMapper;
import com.example.aichat.service.ContentAuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * FriendChatService 单元测试（Mockito 隔离）。
 *
 * 覆盖点：
 *   1. sendMessage：非好友返回 null；
 *   2. sendMessage：空内容返回 null（不抛 NPE）；
 *   3. sendMessage：null 内容返回 null（原实现 content.length() NPE）；
 *   4. sendMessage：超长内容（>8000）返回 null；
 *   5. sendMessage：正常流程插入并返回消息；
 *   6. getChatHistory：调用 mapper、反转排序并自动标记已读。
 */
@ExtendWith(MockitoExtension.class)
class FriendChatServiceTest {

    @Mock
    private FriendChatMessageMapper chatMessageMapper;
    @Mock
    private FriendshipMapper friendshipMapper;
    @Mock
    private ContentAuditService contentAuditService;

    @InjectMocks
    private FriendChatServiceImpl friendChatService;

    private Friendship acceptedFriendship() {
        Friendship f = new Friendship();
        f.setUserId(1L);
        f.setFriendId(2L);
        f.setStatus("accepted");
        return f;
    }

    @Test
    void sendMessage_非好友应返回null() {
        when(friendshipMapper.findByUserAndFriend(1L, 2L)).thenReturn(null);

        FriendChatMessage result = friendChatService.sendMessage(1L, 2L, "你好");

        assertNull(result);
        verify(chatMessageMapper, never()).insert(any(FriendChatMessage.class));
    }

    @Test
    void sendMessage_空内容应返回null() {
        FriendChatMessage result = friendChatService.sendMessage(1L, 2L, "   ");

        assertNull(result);
        verifyNoInteractions(friendshipMapper);
        verify(chatMessageMapper, never()).insert(any(FriendChatMessage.class));
    }

    @Test
    void sendMessage_null内容应返回null不抛NPE() {
        // P0 回归：原实现 content.length() 对 null 抛 NPE
        FriendChatMessage result = friendChatService.sendMessage(1L, 2L, null);

        assertNull(result);
        verifyNoInteractions(friendshipMapper);
    }

    @Test
    void sendMessage_超长内容应返回null() {
        String tooLong = "x".repeat(8001);

        FriendChatMessage result = friendChatService.sendMessage(1L, 2L, tooLong);

        assertNull(result);
        verifyNoInteractions(friendshipMapper);
    }

    @Test
    void sendMessage_正常应插入并返回消息() {
        when(friendshipMapper.findByUserAndFriend(1L, 2L)).thenReturn(acceptedFriendship());
        when(contentAuditService.audit(anyString(), anyString(), anyLong())).thenReturn(AuditResult.pass());
        when(chatMessageMapper.insert(any(FriendChatMessage.class))).thenReturn(1);

        FriendChatMessage result = friendChatService.sendMessage(1L, 2L, "你好");

        assertNotNull(result);
        assertEquals(1L, result.getSenderId());
        assertEquals(2L, result.getReceiverId());
        assertEquals("你好", result.getContent());
        assertFalse(result.getIsRead());
        verify(chatMessageMapper).insert(any(FriendChatMessage.class));
    }

    @Test
    void sendMessage_含敏感词应抛CONTENT_VIOLATION且不落库() {
        when(friendshipMapper.findByUserAndFriend(1L, 2L)).thenReturn(acceptedFriendship());
        AuditResult rejected = AuditResult.reject(List.of("加微信"), "MEDIUM", "命中敏感词");
        when(contentAuditService.audit(anyString(), anyString(), anyLong())).thenReturn(rejected);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> friendChatService.sendMessage(1L, 2L, "快加微信详聊"));

        assertEquals(com.example.aichat.common.exception.SocialErrorCode.CONTENT_VIOLATION, ex.getErrorCode());
        verify(chatMessageMapper, never()).insert(any(FriendChatMessage.class));
        verify(contentAuditService).recordAuditLog(eq(1L), eq("private_chat"), eq(null), eq("快加微信详聊"), eq(rejected));
    }

    @Test
    void getChatHistory_应反转排序并标记已读() {
        List<FriendChatMessage> desc = new ArrayList<>();
        FriendChatMessage m1 = new FriendChatMessage();
        m1.setCreateTime(LocalDateTime.now().minusMinutes(10));
        FriendChatMessage m2 = new FriendChatMessage();
        m2.setCreateTime(LocalDateTime.now());
        desc.add(m2); // mapper 按 DESC 返回：最新在前
        desc.add(m1);
        when(chatMessageMapper.getChatHistory(1L, 2L, 20)).thenReturn(desc);

        List<FriendChatMessage> result = friendChatService.getChatHistory(1L, 2L, 20);

        assertEquals(2, result.size());
        // reverse 后最早的在前
        assertEquals(m1.getCreateTime(), result.get(0).getCreateTime());
        verify(chatMessageMapper).markMessagesAsRead(1L, 2L);
    }
}

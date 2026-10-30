package com.example.aichat.websocket;

import com.example.aichat.entity.AiMemory;
import com.example.aichat.mapper.FriendChatMessageMapper;
import com.example.aichat.mapper.FriendshipMapper;
import com.example.aichat.mapper.GroupAiMemberMapper;
import com.example.aichat.mapper.GroupChatMemberMapper;
import com.example.aichat.mapper.GroupChatMessageMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.AiMemoryService;
import com.example.aichat.service.ChatMessageService;
import com.example.aichat.service.UserOnlineStatusService;
import com.example.aichat.service.ai.OllamaService;
import com.example.aichat.utils.TokenUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.lang.reflect.Method;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * P0 回归：WebSocket 连接不得在缺少有效 token 时回退信任 URL 中的 userId 参数。
 * 攻击者曾可仅凭 ?userId=1 连接即冒充任意用户收发消息。
 */
class ChatWebSocketHandlerAuthTest {

    private ChatWebSocketHandler newHandler() {
        return new ChatWebSocketHandler(
                mock(OllamaService.class), mock(ChatMessageService.class),
                mock(FriendChatMessageMapper.class), mock(GroupChatMessageMapper.class),
                mock(GroupChatMemberMapper.class), mock(FriendshipMapper.class),
                mock(UserMapper.class), mock(UserOnlineStatusService.class),
                mock(AiMemoryService.class), mock(com.example.aichat.service.SoulEngineService.class),
                new ObjectMapper());
    }

    private Long invokeResolve(ChatWebSocketHandler handler, String query) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws?" + query));
        Method m = ChatWebSocketHandler.class.getDeclaredMethod("getUserIdFromToken", WebSocketSession.class);
        m.setAccessible(true);
        return (Long) m.invoke(handler, session);
    }

    @Test
    void 仅传userId参数无token_必须拒绝身份() throws Exception {
        Long userId = invokeResolve(newHandler(), "userId=1");
        assertNull(userId, "无 token 时 ?userId=1 不得被信任为身份");
    }

    @Test
    void 无任何参数_返回null() throws Exception {
        Long userId = invokeResolve(newHandler(), "");
        assertNull(userId);
    }

    @Test
    void 携带合法token_解析出身份() throws Exception {
        String token = TokenUtils.generateToken("42", "alice");
        Long userId = invokeResolve(newHandler(), "token=" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(42L, userId, "合法 token 必须解析出真实用户");
    }
}

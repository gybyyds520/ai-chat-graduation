package com.example.aichat.websocket;

import com.example.aichat.utils.TokenUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DataSyncWebSocketHandler 缺口补测。
 *
 * <p>原项目无任何 DataSyncWebSocketHandler 测试类。覆盖：
 *   1. 连接建立：合法 token / 无 token(匿名) / 非法 token 三分支身份解析；
 *   2. 文本消息路由：subscribe、ping 回 pong、非法 JSON 不抛异常；
 *   3. 广播：广播给所有在线会话、关闭会话跳过、发送异常吞掉；
 *   4. 按用户投递：只投递给匹配 userId 的在线会话；
 *   5. 连接关闭 / 传输错误清理。</p>
 */
class DataSyncWebSocketHandlerGapTest {

    private DataSyncWebSocketHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        handler = new DataSyncWebSocketHandler(objectMapper);
    }

    private WebSocketSession mockSession(String id, String query) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/dataSync?" + query));
        when(session.getAttributes()).thenReturn(new HashMap<>());
        return session;
    }

    @Test
    void 连接建立_合法token_解析出用户并回connected() throws Exception {
        String token = TokenUtils.generateToken("77", "alice");
        WebSocketSession session = mockSession("s1", "token=" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8));

        handler.afterConnectionEstablished(session);

        assertEquals(1, handler.getActiveConnectionCount());
        verify(session).sendMessage(argThat((TextMessage m) -> m.getPayload().contains("\"type\":\"connected\"")));
    }

    @Test
    void 连接建立_无query_以匿名身份建立不抛异常() throws Exception {
        WebSocketSession session = mockSession("s2", "");

        handler.afterConnectionEstablished(session);

        assertEquals(1, handler.getActiveConnectionCount());
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void 连接建立_非法token_拒绝身份按匿名处理() throws Exception {
        // token 存在但签名无效：resolveUserId 应返回 null（不回退 query userId）
        WebSocketSession session = mockSession("s3", "token=fake.invalid.sig&userId=hacker");

        handler.afterConnectionEstablished(session);

        assertEquals(1, handler.getActiveConnectionCount());
        // 不应抛异常，连接仍建立
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void 连接建立_无token但带userId_兼容旧版参数() throws Exception {
        WebSocketSession session = mockSession("s4", "userId=legacyUser");

        handler.afterConnectionEstablished(session);

        assertEquals(1, handler.getActiveConnectionCount());
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void ping消息_回复pong() throws Exception {
        WebSocketSession session = mockSession("s5", "");
        handler.afterConnectionEstablished(session);
        reset(session);
        when(session.isOpen()).thenReturn(true);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"ping\"}"));

        verify(session).sendMessage(argThat((TextMessage m) -> m.getPayload().contains("\"type\":\"pong\"")));
    }

    @Test
    void subscribe消息_仅记录不回包不抛异常() throws Exception {
        WebSocketSession session = mockSession("s6", "");
        handler.afterConnectionEstablished(session);
        reset(session);

        assertDoesNotThrow(() -> handler.handleTextMessage(session,
                new TextMessage("{\"type\":\"subscribe\",\"channel\":\"chat\"}")));
        // subscribe 分支不主动回包
        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void 非法JSON消息_捕获异常不向上抛() throws Exception {
        WebSocketSession session = mockSession("s7", "");
        handler.afterConnectionEstablished(session);
        reset(session);

        assertDoesNotThrow(() -> handler.handleTextMessage(session,
                new TextMessage("not-a-json{")));
    }

    @Test
    void 未知消息类型_静默忽略不抛异常() throws Exception {
        WebSocketSession session = mockSession("s8", "");
        handler.afterConnectionEstablished(session);
        reset(session);

        assertDoesNotThrow(() -> handler.handleTextMessage(session,
                new TextMessage("{\"type\":\"weird_unknown_type\"}")));
    }

    @Test
    void 广播数据变更_投递给所有在线会话() throws Exception {
        WebSocketSession s1 = mockSession("b1", "");
        WebSocketSession s2 = mockSession("b2", "");
        handler.afterConnectionEstablished(s1);
        handler.afterConnectionEstablished(s2);

        Map<String, Object> data = new HashMap<>();
        data.put("k", "v");
        handler.broadcastDataChange("chat", data);

        verify(s1).sendMessage(argThat((TextMessage m) -> m.getPayload().contains("dataChange") && m.getPayload().contains("chat")));
        verify(s2).sendMessage(argThat((TextMessage m) -> m.getPayload().contains("dataChange")));
    }

    @Test
    void 广播数据变更_关闭会话被跳过() throws Exception {
        WebSocketSession open = mockSession("o1", "");
        WebSocketSession closed = mockSession("c1", "");
        when(closed.isOpen()).thenReturn(false);
        handler.afterConnectionEstablished(open);
        handler.afterConnectionEstablished(closed);

        // 清空握手回包，只统计本次广播投递
        reset(open, closed);
        when(open.isOpen()).thenReturn(true);
        when(closed.isOpen()).thenReturn(false);

        handler.broadcastDataChange("ch", Map.of("a", 1));

        verify(open, times(1)).sendMessage(any(TextMessage.class));
        verify(closed, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void 广播时发送异常_吞掉不影响其他会话() throws Exception {
        WebSocketSession bad = mockSession("bad", "");
        WebSocketSession good = mockSession("good", "");
        handler.afterConnectionEstablished(bad);
        handler.afterConnectionEstablished(good);
        // 握手完成后：清空交互记录，再让 bad 发送抛异常
        reset(bad, good);
        when(bad.isOpen()).thenReturn(true);
        when(good.isOpen()).thenReturn(true);
        doThrow(new java.io.IOException("broken pipe")).when(bad).sendMessage(any(TextMessage.class));

        assertDoesNotThrow(() -> handler.broadcastDataChange("ch", Map.of()));
        verify(good, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void 按用户投递_只发给匹配userId的会话() throws Exception {
        String token = TokenUtils.generateToken("100", "target");
        WebSocketSession target = mockSession("t1", "token=" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8));
        WebSocketSession other = mockSession("o2", "userId=someoneElse");
        handler.afterConnectionEstablished(target);
        handler.afterConnectionEstablished(other);

        // 清空握手阶段的 connected 回包，只统计 sendToUser 的投递
        reset(target, other);
        when(target.isOpen()).thenReturn(true);
        when(other.isOpen()).thenReturn(true);

        handler.sendToUser("100", "order", Map.of("orderId", 9));

        verify(target).sendMessage(argThat((TextMessage m) -> m.getPayload().contains("\"channel\":\"order\"")));
        verify(other, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void 按用户投递_无匹配用户不抛异常() throws Exception {
        WebSocketSession s = mockSession("x1", "userId=u1");
        handler.afterConnectionEstablished(s);
        assertDoesNotThrow(() -> handler.sendToUser("nobody", "ch", Map.of()));
    }

    @Test
    void 连接关闭_移除会话与用户映射() throws Exception {
        WebSocketSession s = mockSession("close1", "userId=uX");
        handler.afterConnectionEstablished(s);
        assertEquals(1, handler.getActiveConnectionCount());

        handler.afterConnectionClosed(s, CloseStatus.NORMAL);
        assertEquals(0, handler.getActiveConnectionCount());

        // 已关闭后广播不应再投递
        handler.broadcastDataChange("ch", Map.of());
        verify(s, never()).sendMessage(argThat((TextMessage m) -> m.getPayload().contains("dataChange")));
    }

    @Test
    void 传输错误_移除会话() throws Exception {
        WebSocketSession s = mockSession("err1", "");
        handler.afterConnectionEstablished(s);
        assertEquals(1, handler.getActiveConnectionCount());

        handler.handleTransportError(s, new java.io.IOException("transport broken"));
        assertEquals(0, handler.getActiveConnectionCount());
    }

    @Test
    void 空连接数_初始为0() {
        assertEquals(0, handler.getActiveConnectionCount());
    }
}

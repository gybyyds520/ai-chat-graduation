package com.example.aichat.websocket;

import com.example.aichat.entity.FriendChatMessage;
import com.example.aichat.entity.Friendship;
import com.example.aichat.entity.GroupChatMember;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.FriendChatMessageMapper;
import com.example.aichat.mapper.FriendshipMapper;
import com.example.aichat.mapper.GroupChatMemberMapper;
import com.example.aichat.mapper.GroupChatMessageMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.AiMemoryService;
import com.example.aichat.service.ChatMessageService;
import com.example.aichat.service.SoulEngineService;
import com.example.aichat.service.UserOnlineStatusService;
import com.example.aichat.service.ai.OllamaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ChatWebSocketHandler 消息路由 / 广播 / 断线清理 缺口补测。
 *
 * <p>原仅存在 ChatWebSocketHandlerAuthTest（token 解析）。本类补齐 handleTextMessage 的
 * 各消息类型分支、好友/群消息安全校验、广播与按用户投递、连接关闭清理。</p>
 */
class ChatWebSocketHandlerGapTest {

    private OllamaService ollamaService;
    private ChatMessageService chatMessageService;
    private FriendChatMessageMapper friendChatMessageMapper;
    private GroupChatMessageMapper groupChatMessageMapper;
    private GroupChatMemberMapper groupChatMemberMapper;
    private FriendshipMapper friendshipMapper;
    private UserMapper userMapper;
    private UserOnlineStatusService userOnlineStatusService;
    private AiMemoryService aiMemoryService;
    private SoulEngineService soulEngineService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ChatWebSocketHandler handler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ollamaService = mock(OllamaService.class);
        chatMessageService = mock(ChatMessageService.class);
        friendChatMessageMapper = mock(FriendChatMessageMapper.class);
        groupChatMessageMapper = mock(GroupChatMessageMapper.class);
        groupChatMemberMapper = mock(GroupChatMemberMapper.class);
        friendshipMapper = mock(FriendshipMapper.class);
        userMapper = mock(UserMapper.class);
        userOnlineStatusService = mock(UserOnlineStatusService.class);
        aiMemoryService = mock(AiMemoryService.class);
        soulEngineService = mock(SoulEngineService.class);

        handler = new ChatWebSocketHandler(ollamaService, chatMessageService,
                friendChatMessageMapper, groupChatMessageMapper, groupChatMemberMapper,
                friendshipMapper, userMapper, userOnlineStatusService, aiMemoryService,
                soulEngineService, objectMapper);
    }

    /** 构造一个已认证（attributes 带 userId）的 mock 会话。 */
    private WebSocketSession authSession(String id, Long userId) throws Exception {
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getId()).thenReturn(id);
        when(s.isOpen()).thenReturn(true);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", userId);
        when(s.getAttributes()).thenReturn(attrs);
        return s;
    }

    private void send(WebSocketSession s, String json) throws Exception {
        handler.handleTextMessage(s, new TextMessage(json));
    }

    private boolean replyContains(WebSocketSession s, String fragment) throws Exception {
        var captor = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(s, atLeastOnce()).sendMessage(captor.capture());
        return captor.getAllValues().stream().anyMatch(tm -> tm.getPayload().contains(fragment));
    }

    // ===== 未登录 / 路由 =====

    @Test
    void 未登录发消息_返回未登录错误() throws Exception {
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getId()).thenReturn("anon");
        when(s.isOpen()).thenReturn(true);
        when(s.getAttributes()).thenReturn(new HashMap<>()); // 无 userId

        send(s, "{\"type\":\"ping\"}");

        assertTrue(replyContains(s, "未登录"));
    }

    @Test
    void ping_认证用户_回复pong() throws Exception {
        WebSocketSession s = authSession("p1", 1L);
        send(s, "{\"type\":\"ping\"}");
        assertTrue(replyContains(s, "\"type\":\"pong\""));
    }

    @Test
    void 未知消息类型_返回错误() throws Exception {
        WebSocketSession s = authSession("u1", 1L);
        send(s, "{\"type\":\"space_warp\"}");
        assertTrue(replyContains(s, "未知消息类型"));
    }

    @Test
    void 非法JSON_返回消息处理失败而非崩溃() throws Exception {
        WebSocketSession s = authSession("j1", 1L);
        send(s, "{bad json");
        assertTrue(replyContains(s, "消息处理失败"));
    }

    // ===== typing =====

    @Test
    void typing_目标在线_转发typing状态() throws Exception {
        WebSocketSession me = authSession("me", 1L);
        WebSocketSession target = authSession("target", 2L);
        // 注册 target 在线：手动触发 afterConnectionEstablished 较麻烦，这里用 sendMessageToUser 间接
        // 通过把 target 放入 userIdToSession 需经 afterConnectionEstablished；改用反射注入更直接：
        registerSession(2L, target);

        send(me, "{\"type\":\"typing\",\"targetId\":2,\"isTyping\":true}");

        assertTrue(replyContains(target, "\"type\":\"typing\""));
    }

    @Test
    void typing_目标离线_静默不抛异常() throws Exception {
        WebSocketSession me = authSession("me2", 1L);
        assertDoesNotThrow(() -> send(me, "{\"type\":\"typing\",\"targetId\":999,\"isTyping\":false}"));
    }

    // ===== read =====

    @Test
    void read_按messageId_接收人匹配_标记已读() throws Exception {
        WebSocketSession me = authSession("r1", 10L);
        FriendChatMessage msg = new FriendChatMessage();
        msg.setId(55L);
        msg.setReceiverId(10L); // 当前用户是接收人
        when(friendChatMessageMapper.selectById(55L)).thenReturn(msg);

        send(me, "{\"type\":\"read\",\"messageId\":55}");

        verify(friendChatMessageMapper).updateById(argThat((FriendChatMessage m) -> Boolean.TRUE.equals(m.getIsRead())));
    }

    @Test
    void read_按messageId_接收人不匹配_不更新() throws Exception {
        WebSocketSession me = authSession("r2", 10L);
        FriendChatMessage msg = new FriendChatMessage();
        msg.setId(56L);
        msg.setReceiverId(999L); // 属于别人
        when(friendChatMessageMapper.selectById(56L)).thenReturn(msg);

        send(me, "{\"type\":\"read\",\"messageId\":56}");

        verify(friendChatMessageMapper, never()).updateById(any(FriendChatMessage.class));
    }

    @Test
    void read_按senderId_批量标记已读() throws Exception {
        WebSocketSession me = authSession("r3", 10L);
        send(me, "{\"type\":\"read\",\"senderId\":7}");
        verify(friendChatMessageMapper).update(any(FriendChatMessage.class), any());
    }

    @Test
    void read_两个id都为空_仅回包不抛异常() throws Exception {
        WebSocketSession me = authSession("r4", 10L);
        assertDoesNotThrow(() -> send(me, "{\"type\":\"read\"}"));
    }

    // ===== notification_read =====

    @Test
    void notificationRead_回包成功() throws Exception {
        WebSocketSession s = authSession("n1", 1L);
        send(s, "{\"type\":\"notification_read\",\"notificationId\":33}");
        assertTrue(replyContains(s, "\"type\":\"notification_read\""));
        assertTrue(replyContains(s, "\"status\":\"success\""));
    }

    // ===== friend_message 安全校验 =====

    @Test
    void friendMessage_空内容_返回错误() throws Exception {
        WebSocketSession s = authSession("f1", 1L);
        send(s, "{\"type\":\"friend_message\",\"receiverId\":2,\"content\":\"  \"}");
        assertTrue(replyContains(s, "消息内容不能为空"));
    }

    @Test
    void friendMessage_超长内容_返回错误() throws Exception {
        WebSocketSession s = authSession("f2", 1L);
        String longContent = "话".repeat(8001);
        send(s, "{\"type\":\"friend_message\",\"receiverId\":2,\"content\":\"" + longContent + "\"}");
        assertTrue(replyContains(s, "不能超过8000"));
    }

    @Test
    void friendMessage_非好友_拒绝发送() throws Exception {
        WebSocketSession s = authSession("f3", 1L);
        when(friendshipMapper.findByUserAndFriend(1L, 2L)).thenReturn(null);
        when(friendshipMapper.findByUserAndFriend(2L, 1L)).thenReturn(null);

        send(s, "{\"type\":\"friend_message\",\"receiverId\":2,\"content\":\"hi\"}");

        assertTrue(replyContains(s, "还不是好友"));
        verify(friendChatMessageMapper, never()).insert(any(FriendChatMessage.class));
    }

    @Test
    void friendMessage_已是好友_落库并确认() throws Exception {
        WebSocketSession s = authSession("f4", 1L);
        Friendship f = new Friendship();
        f.setStatus("accepted");
        when(friendshipMapper.findByUserAndFriend(1L, 2L)).thenReturn(f);
        when(friendChatMessageMapper.insert(any(FriendChatMessage.class))).thenAnswer(inv -> {
            FriendChatMessage m = inv.getArgument(0);
            m.setId(900L);
            return 1;
        });
        User sender = new User();
        sender.setNickname("Alice");
        when(userMapper.selectById(1L)).thenReturn(sender);

        send(s, "{\"type\":\"friend_message\",\"receiverId\":2,\"content\":\"hello\"}");

        verify(friendChatMessageMapper).insert(any(FriendChatMessage.class));
        assertTrue(replyContains(s, "\"type\":\"message_sent\""));
    }

    // ===== group_message 安全校验 =====

    @Test
    void groupMessage_空内容_返回错误() throws Exception {
        WebSocketSession s = authSession("g1", 1L);
        send(s, "{\"type\":\"group_message\",\"groupId\":5,\"content\":\"\"}");
        assertTrue(replyContains(s, "消息内容不能为空"));
    }

    @Test
    void groupMessage_超长内容_返回错误() throws Exception {
        WebSocketSession s = authSession("g2", 1L);
        String longContent = "群".repeat(8001);
        send(s, "{\"type\":\"group_message\",\"groupId\":5,\"content\":\"" + longContent + "\"}");
        assertTrue(replyContains(s, "不能超过8000"));
    }

    @Test
    void groupMessage_非群成员_拒绝发送() throws Exception {
        WebSocketSession s = authSession("g3", 1L);
        when(groupChatMemberMapper.countMemberByGroupAndUser(5L, 1L)).thenReturn(0);

        send(s, "{\"type\":\"group_message\",\"groupId\":5,\"content\":\"hi\"}");

        assertTrue(replyContains(s, "不是该群成员"));
        verify(groupChatMessageMapper, never()).insert(any(com.example.aichat.entity.GroupChatMessage.class));
    }

    @Test
    void groupMessage_是群成员_落库并广播给其他成员() throws Exception {
        WebSocketSession s = authSession("g4", 1L);
        when(groupChatMemberMapper.countMemberByGroupAndUser(5L, 1L)).thenReturn(1);
        when(groupChatMessageMapper.insert(any(com.example.aichat.entity.GroupChatMessage.class))).thenAnswer(inv -> {
            ((com.example.aichat.entity.GroupChatMessage) inv.getArgument(0)).setId(700L);
            return 1;
        });
        GroupChatMember m2 = new GroupChatMember();
        m2.setUserId(2L);
        GroupChatMember m3 = new GroupChatMember();
        m3.setUserId(3L);
        List<GroupChatMember> members = new ArrayList<>();
        members.add(m2);
        members.add(m3);
        when(groupChatMemberMapper.getMembersByGroupId(5L)).thenReturn(members);

        WebSocketSession member2 = authSession("gm2", 2L);
        registerSession(2L, member2);

        send(s, "{\"type\":\"group_message\",\"groupId\":5,\"content\":\"大家好\"}");

        verify(groupChatMessageMapper).insert(any(com.example.aichat.entity.GroupChatMessage.class));
        verify(groupChatMemberMapper).getMembersByGroupId(5L);
        assertTrue(replyContains(member2, "\"type\":\"group_message\""));
    }

    // ===== 生命周期 / 广播 =====

    @Test
    void 连接关闭_已认证用户标记离线() throws Exception {
        WebSocketSession s = authSession("c1", 55L);
        // 通过 afterConnectionEstablished 注册需要 token；这里直接用反射注册 userIdToSession
        registerSession(55L, s);

        handler.afterConnectionClosed(s, CloseStatus.NORMAL);

        verify(userOnlineStatusService).updateStatus(55L, "offline");
        assertNull(handler.getSessionByUserId(55L));
    }

    @Test
    void 传输错误_清理会话映射() throws Exception {
        WebSocketSession s = authSession("te1", 77L);
        registerSession(77L, s);

        handler.handleTransportError(s, new java.io.IOException("boom"));

        assertNull(handler.getSessionByUserId(77L));
    }

    @Test
    void broadcast_发给所有开放会话并吞掉异常() throws Exception {
        WebSocketSession ok = authSession("bc1", 1L);
        WebSocketSession bad = authSession("bc2", 2L);
        doThrow(new java.io.IOException("x")).when(bad).sendMessage(any(TextMessage.class));
        registerSession(1L, ok);
        registerSession(2L, bad);

        assertDoesNotThrow(() -> handler.broadcast("hello-all"));
        verify(ok).sendMessage(argThat((TextMessage m) -> m.getPayload().equals("hello-all")));
    }

    @Test
    void sendMessageToUser_在线用户收到消息() throws Exception {
        WebSocketSession target = authSession("su1", 88L);
        registerSession(88L, target);

        handler.sendMessageToUser(88L, "direct-msg");

        verify(target).sendMessage(argThat((TextMessage m) -> m.getPayload().equals("direct-msg")));
    }

    @Test
    void sendMessageToUser_离线用户_静默不抛异常() throws Exception {
        assertDoesNotThrow(() -> handler.sendMessageToUser(4242L, "nobody-home"));
    }

    @Test
    void getOnlineUserIds_返回已认证用户集合() throws Exception {
        WebSocketSession a = authSession("o1", 301L);
        WebSocketSession b = authSession("o2", 302L);
        registerSession(301L, a);
        registerSession(302L, b);

        Set<Long> online = handler.getOnlineUserIds();
        assertTrue(online.contains(301L));
        assertTrue(online.contains(302L));
    }

    @Test
    void getSessionByUserId_未在线返回null() {
        assertNull(handler.getSessionByUserId(9999L));
    }

    // ===== 工具：把会话登记进在线映射（绕过握手 token 校验）=====
    @SuppressWarnings("unchecked")
    private void registerSession(Long userId, WebSocketSession session) throws Exception {
        java.lang.reflect.Field f = ChatWebSocketHandler.class.getDeclaredField("userIdToSession");
        f.setAccessible(true);
        ((java.util.Map<Long, WebSocketSession>) f.get(handler)).put(userId, session);
        java.lang.reflect.Field sf = ChatWebSocketHandler.class.getDeclaredField("sessionIdToUserId");
        sf.setAccessible(true);
        ((java.util.Map<String, Long>) sf.get(handler)).put(session.getId(), userId);
        // broadcast() 遍历 activeSessions，需一并登记
        java.lang.reflect.Field af = ChatWebSocketHandler.class.getDeclaredField("activeSessions");
        af.setAccessible(true);
        ((java.util.Map<String, WebSocketSession>) af.get(handler)).put(session.getId(), session);
    }
}

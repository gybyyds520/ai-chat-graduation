package com.example.aichat.service.impl;

import com.example.aichat.entity.UserOnlineStatus;
import com.example.aichat.mapper.UserOnlineStatusMapper;
import com.example.aichat.websocket.ChatWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserOnlineStatusServiceImpl 缺口补测。
 *
 * <p>原项目无该实现类的测试。覆盖：
 * updateStatus / updateSessionId 的"已存在更新 vs 新建插入"两分支、
 * recordActivity、好友状态批量查询与默认 offline 填充、isOnline、清理、广播异常吞掉。</p>
 */
class UserOnlineStatusGapTest {

    private UserOnlineStatusServiceImpl service;
    private UserOnlineStatusMapper mapper;
    private ApplicationContext applicationContext;
    private ChatWebSocketHandler wsHandler;

    @BeforeEach
    void setUp() throws Exception {
        service = new UserOnlineStatusServiceImpl();
        mapper = mock(UserOnlineStatusMapper.class);
        applicationContext = mock(ApplicationContext.class);
        wsHandler = mock(ChatWebSocketHandler.class);

        inject("userOnlineStatusMapper", mapper);
        inject("applicationContext", applicationContext);
        when(applicationContext.getBean(ChatWebSocketHandler.class)).thenReturn(wsHandler);
    }

    private void inject(String field, Object value) throws Exception {
        java.lang.reflect.Field f = UserOnlineStatusServiceImpl.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(service, value);
    }

    // ===== getStatus / isOnline =====

    @Test
    void getStatus_透传mapper结果() {
        UserOnlineStatus s = new UserOnlineStatus();
        s.setStatus("online");
        when(mapper.findByUserId(1L)).thenReturn(s);
        assertSame(s, service.getStatus(1L));
    }

    @Test
    void isOnline_在线返回true() {
        UserOnlineStatus s = new UserOnlineStatus();
        s.setStatus("online");
        when(mapper.findByUserId(1L)).thenReturn(s);
        assertTrue(service.isOnline(1L));
    }

    @Test
    void isOnline_离线返回false() {
        UserOnlineStatus s = new UserOnlineStatus();
        s.setStatus("offline");
        when(mapper.findByUserId(1L)).thenReturn(s);
        assertFalse(service.isOnline(1L));
    }

    @Test
    void isOnline_无记录返回false() {
        when(mapper.findByUserId(anyLong())).thenReturn(null);
        assertFalse(service.isOnline(1L));
    }

    // ===== updateStatus 两分支 =====

    @Test
    void updateStatus_已存在_走更新并广播() {
        UserOnlineStatus existing = new UserOnlineStatus();
        existing.setUserId(1L);
        existing.setStatus("offline");
        when(mapper.findByUserId(1L)).thenReturn(existing);

        UserOnlineStatus result = service.updateStatus(1L, "offline");

        verify(mapper).updateById(existing);
        verify(mapper, never()).insert(any(UserOnlineStatus.class));
        assertEquals("offline", result.getStatus());
    }

    @Test
    void updateStatus_不存在_走插入() {
        when(mapper.findByUserId(1L)).thenReturn(null);

        service.updateStatus(1L, "online");

        verify(mapper).insert(argThat((UserOnlineStatus s) -> s.getUserId().equals(1L) && "online".equals(s.getStatus())));
    }

    // ===== updateSessionId 两分支 =====

    @Test
    void updateSessionId_已存在_更新会话并置在线() {
        UserOnlineStatus existing = new UserOnlineStatus();
        existing.setUserId(1L);
        existing.setStatus("offline");
        when(mapper.findByUserId(1L)).thenReturn(existing);

        service.updateSessionId(1L, "sess-9");

        assertEquals("online", existing.getStatus());
        assertEquals("sess-9", existing.getSessionId());
        verify(mapper).updateById(existing);
    }

    @Test
    void updateSessionId_不存在_新建在线记录() {
        when(mapper.findByUserId(1L)).thenReturn(null);
        service.updateSessionId(1L, "sess-new");
        verify(mapper).insert(argThat((UserOnlineStatus s) ->
                "online".equals(s.getStatus()) && "sess-new".equals(s.getSessionId())));
    }

    // ===== recordActivity =====

    @Test
    void recordActivity_已存在_刷新活跃时间且不插入() {
        UserOnlineStatus existing = new UserOnlineStatus();
        existing.setUserId(1L);
        existing.setStatus("offline");
        when(mapper.findByUserId(1L)).thenReturn(existing);

        service.recordActivity(1L);

        // 非 online 状态应被置为 online
        assertEquals("online", existing.getStatus());
        assertNotNull(existing.getLastActiveTime());
        verify(mapper).updateById(existing);
        verify(mapper, never()).insert(any(UserOnlineStatus.class));
    }

    @Test
    void recordActivity_无记录_不抛异常也不插入() {
        when(mapper.findByUserId(anyLong())).thenReturn(null);
        assertDoesNotThrow(() -> service.recordActivity(1L));
        verify(mapper, never()).insert(any(UserOnlineStatus.class));
    }

    // ===== 好友状态批量 =====

    @Test
    void getFriendStatuses_null列表_返回空() {
        assertTrue(service.getFriendStatuses(null).isEmpty());
    }

    @Test
    void getFriendStatuses_空列表_返回空() {
        assertTrue(service.getFriendStatuses(List.of()).isEmpty());
    }

    @Test
    void getFriendStatusMap_null列表_返回空Map() {
        assertTrue(service.getFriendStatusMap(null).isEmpty());
    }

    @Test
    void getFriendStatusMap_含缺失好友_补默认offline() {
        UserOnlineStatus online = new UserOnlineStatus();
        online.setUserId(10L);
        online.setStatus("online");
        when(mapper.findByUserIds(List.of(10L, 20L))).thenReturn(List.of(online));

        Map<Long, String> map = service.getFriendStatusMap(List.of(10L, 20L));

        assertEquals("online", map.get(10L));
        assertEquals("offline", map.get(20L), "无记录的好友应默认 offline");
    }

    // ===== cleanup =====

    @Test
    void cleanupOfflineUsers_透传mapper影响行数() {
        when(mapper.updateOfflineByInactivity(any(LocalDateTime.class))).thenReturn(3);
        assertEquals(3, service.cleanupOfflineUsers());
    }

    // ===== broadcastStatus =====

    @Test
    void broadcastStatus_无记录_不广播() {
        when(mapper.findByUserId(anyLong())).thenReturn(null);
        service.broadcastStatus(1L);
        verify(wsHandler, never()).broadcastToFriends(anyLong(), anyString());
    }

    @Test
    void broadcastStatus_正常_广播给好友() {
        UserOnlineStatus s = new UserOnlineStatus();
        s.setUserId(1L);
        s.setStatus("online");
        when(mapper.findByUserId(1L)).thenReturn(s);

        service.broadcastStatus(1L);

        verify(wsHandler).broadcastToFriends(eq(1L), contains("user_status"));
    }

    @Test
    void broadcastStatus_广播异常_被吞掉不抛出() {
        UserOnlineStatus s = new UserOnlineStatus();
        s.setUserId(1L);
        s.setStatus("online");
        when(mapper.findByUserId(1L)).thenReturn(s);
        doThrow(new RuntimeException("ws down")).when(wsHandler).broadcastToFriends(anyLong(), anyString());

        assertDoesNotThrow(() -> service.broadcastStatus(1L));
    }
}

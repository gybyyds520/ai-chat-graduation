package com.example.aichat.service.tool.impl;

import com.example.aichat.entity.Friendship;
import com.example.aichat.service.FriendshipService;
import com.example.aichat.service.tool.ToolResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * QueryUserFriendsTool 单元测试（Mockito 隔离 FriendshipService，完全离线）。
 */
class QueryUserFriendsToolTest {

    @Test
    void 应返回已接受好友列表() {
        FriendshipService svc = Mockito.mock(FriendshipService.class);
        Friendship f1 = new Friendship();
        f1.setFriendId(2L);
        f1.setUsername("alice");
        f1.setNickname("爱丽丝");
        f1.setStatus("accepted");
        Friendship pending = new Friendship();
        pending.setFriendId(3L);
        pending.setStatus("pending");
        when(svc.getFriendList(100L)).thenReturn(List.of(f1, pending));

        QueryUserFriendsTool tool = new QueryUserFriendsTool(svc);
        assertEquals("query_user_friends", tool.getName());

        ToolResult r = tool.execute(Map.of("userId", 100L));
        assertTrue(r.isSuccess(), r.getError());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) r.getData();
        assertEquals(1, data.get("total")); // 仅 accepted
    }

    @Test
    void 缺少userId应失败() {
        FriendshipService svc = Mockito.mock(FriendshipService.class);
        QueryUserFriendsTool tool = new QueryUserFriendsTool(svc);
        ToolResult r = tool.execute(Map.of());
        assertFalse(r.isSuccess());
        assertNotNull(r.getError());
    }

    @Test
    void 服务异常应友好失败不泄露堆栈() {
        FriendshipService svc = Mockito.mock(FriendshipService.class);
        when(svc.getFriendList(anyLong())).thenThrow(new RuntimeException("db connection pool exhausted"));
        QueryUserFriendsTool tool = new QueryUserFriendsTool(svc);

        ToolResult r = tool.execute(Map.of("userId", 1L));
        assertFalse(r.isSuccess());
        assertFalse(r.getError().contains("pool exhausted"));
    }
}

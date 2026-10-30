package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.UserErrorCode;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.AiMemoryMapper;
import com.example.aichat.mapper.ChatMessageMapper;
import com.example.aichat.mapper.FriendChatMessageMapper;
import com.example.aichat.mapper.FriendshipMapper;
import com.example.aichat.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * M3 ⑨ 账号自助注销服务层测试。
 *
 * 覆盖点：
 *   1. 正常删除：confirmText="DELETE" 且账号正常 → 软删除标记 deleted，清理对话/好友/记忆；
 *   2. 未确认拒绝：confirmText 不正确 → 抛 ACCOUNT_DELETE_CONFIRM_REQUIRED，不触碰任何数据；
 *   3. 已删除拒绝：账号已是 deleted → 抛 ACCOUNT_ALREADY_DELETED；
 *   4. 用户不存在 → USER_NOT_FOUND；
 *   5. 用户为 null → TOKEN_INVALID。
 */
class UserAccountDeletionTest {

    private UserServiceImpl userService;
    private UserMapper userMapper;
    private ChatMessageMapper chatMessageMapper;
    private FriendChatMessageMapper friendChatMessageMapper;
    private FriendshipMapper friendshipMapper;
    private AiMemoryMapper aiMemoryMapper;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl();
        userMapper = mock(UserMapper.class);
        chatMessageMapper = mock(ChatMessageMapper.class);
        friendChatMessageMapper = mock(FriendChatMessageMapper.class);
        friendshipMapper = mock(FriendshipMapper.class);
        aiMemoryMapper = mock(AiMemoryMapper.class);

        ReflectionTestUtils.setField(userService, "userMapper", userMapper);
        ReflectionTestUtils.setField(userService, "chatMessageMapper", chatMessageMapper);
        ReflectionTestUtils.setField(userService, "friendChatMessageMapper", friendChatMessageMapper);
        ReflectionTestUtils.setField(userService, "friendshipMapper", friendshipMapper);
        ReflectionTestUtils.setField(userService, "aiMemoryMapper", aiMemoryMapper);
    }

    private User activeUser() {
        User u = new User();
        u.setId(42L);
        u.setUsername("alice");
        u.setNickname("Alice");
        u.setAccountStatus("active");
        u.setAvatar("/uploads/user/avatar_alice.png");
        return u;
    }

    @Test
    void deleteAccount_正常删除应软删除并清理数据() {
        User u = activeUser();
        when(userMapper.selectById(42L)).thenReturn(u);

        userService.deleteAccount(42L, "DELETE");

        // 各类关联数据均被清理
        verify(chatMessageMapper).delete(any());
        verify(friendChatMessageMapper).delete(any());
        verify(friendshipMapper).delete(any());
        verify(aiMemoryMapper).delete(any());
        // 用户被软删除
        assertEquals("deleted", u.getAccountStatus());
        assertNull(u.getAvatar());
        assertNull(u.getEmail());
        assertNotNull(u.getUsername());
        assertTrue(u.getUsername().startsWith("deleted_42_"));
        verify(userMapper).updateById(u);
    }

    @Test
    void deleteAccount_未确认应拒绝且不触碰数据() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.deleteAccount(42L, "delete"));
        assertEquals(UserErrorCode.ACCOUNT_DELETE_CONFIRM_REQUIRED, ex.getErrorCode());

        verify(userMapper, never()).selectById(anyLong());
        verify(userMapper, never()).updateById(any(User.class));
        verify(chatMessageMapper, never()).delete(any());
    }

    @Test
    void deleteAccount_确认文本为null应拒绝() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.deleteAccount(42L, null));
        assertEquals(UserErrorCode.ACCOUNT_DELETE_CONFIRM_REQUIRED, ex.getErrorCode());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void deleteAccount_已删除应拒绝重复操作() {
        User u = activeUser();
        u.setAccountStatus("deleted");
        when(userMapper.selectById(42L)).thenReturn(u);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.deleteAccount(42L, "DELETE"));
        assertEquals(UserErrorCode.ACCOUNT_ALREADY_DELETED, ex.getErrorCode());
        verify(userMapper, never()).updateById(any(User.class));
        verify(chatMessageMapper, never()).delete(any());
    }

    @Test
    void deleteAccount_用户不存在应抛USER_NOT_FOUND() {
        when(userMapper.selectById(999L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.deleteAccount(999L, "DELETE"));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void deleteAccount_userId为null应抛TOKEN_INVALID() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.deleteAccount(null, "DELETE"));
        assertEquals(UserErrorCode.TOKEN_INVALID, ex.getErrorCode());
    }
}

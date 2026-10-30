package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.FriendChatMessage;
import com.example.aichat.entity.Friendship;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.ChatMessageMapper;
import com.example.aichat.mapper.FriendChatMessageMapper;
import com.example.aichat.mapper.FriendshipMapper;
import com.example.aichat.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * P1-13 回归：管理员删除用户不得物理删行导致外键悬挂，
 * 必须在同一事务内清理关联数据后软删除（accountStatus=deleted）。
 */
class AdminDeleteUserSoftTest {

    @Test
    void 删除用户_软删除并清理关联_不物理删行() {
        UserMapper userMapper = mock(UserMapper.class);
        ChatMessageMapper chatMessageMapper = mock(ChatMessageMapper.class);
        FriendChatMessageMapper friendChatMessageMapper = mock(FriendChatMessageMapper.class);
        FriendshipMapper friendshipMapper = mock(FriendshipMapper.class);

        User user = new User();
        user.setId(5L);
        user.setPhone("13900000000");
        user.setEmail("a@b.com");
        when(userMapper.selectById(5L)).thenReturn(user);

        AdminController controller = new AdminController();
        ReflectionTestUtils.setField(controller, "userMapper", userMapper);
        ReflectionTestUtils.setField(controller, "chatMessageMapper", chatMessageMapper);
        ReflectionTestUtils.setField(controller, "friendChatMessageMapper", friendChatMessageMapper);
        ReflectionTestUtils.setField(controller, "friendshipMapper", friendshipMapper);

        R resp = controller.deleteUser(5L);
        assertEquals(200, resp.getCode());

        // 物理删行不得再发生
        verify(userMapper, never()).deleteById(any(java.io.Serializable.class));
        // 关联数据被清理
        verify(chatMessageMapper).delete(any());
        verify(friendChatMessageMapper).delete(any());
        verify(friendshipMapper).delete(any());
        // 用户行被软删除
        verify(userMapper).updateById(argThat((User u) ->
                "deleted".equals(u.getAccountStatus())
                        && u.getPhone() == null
                        && u.getEmail() == null));
    }
}

package com.example.aichat.service.impl;

import com.example.aichat.entity.Friendship;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.FriendshipMapper;
import com.example.aichat.mapper.UserBlockMapper;
import com.example.aichat.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * FriendshipService 单元测试（Mockito 隔离）。
 *
 * 覆盖点：
 *   1. sendFriendRequest：自己加自己应拒绝；
 *   2. sendFriendRequest：重复 pending 请求应拒绝；
 *   3. sendFriendRequest：正常发送应落库；
 *   4. sendFriendRequest：对方已接受（反向）应拒绝；
 *   5. acceptFriendRequest：正常接受建立双向关系；
 *   6. acceptFriendRequest：请求不存在应返回 false；
 *   7. rejectFriendRequest：正常拒绝；
 *   8. removeFriend：正常删除双向关系；
 *   9. getFriendList：用户不存在返回空列表；
 *  10. checkFriendStatus：自己 -> self；好友 -> friend。
 */
@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    private FriendshipMapper friendshipMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserBlockMapper userBlockMapper;

    @InjectMocks
    private FriendshipServiceImpl friendshipService;

    private User user(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("user" + id);
        u.setNickname("User" + id);
        return u;
    }

    private Friendship friendship(Long userId, Long friendId, String status) {
        Friendship f = new Friendship();
        f.setId((userId * 100 + friendId));
        f.setUserId(userId);
        f.setFriendId(friendId);
        f.setStatus(status);
        return f;
    }

    // ============ sendFriendRequest ============

    @Test
    void sendFriendRequest_自己加自己应拒绝() {
        when(userMapper.selectById(1L)).thenReturn(user(1L));

        boolean result = friendshipService.sendFriendRequest(1L, 1L);

        assertFalse(result);
        verify(friendshipMapper, never()).insert(any(Friendship.class));
    }

    @Test
    void sendFriendRequest_用户不存在应返回false() {
        when(userMapper.selectById(99L)).thenReturn(null);

        boolean result = friendshipService.sendFriendRequest(99L, 2L);

        assertFalse(result);
        verifyNoInteractions(friendshipMapper);
    }

    @Test
    void sendFriendRequest_重复pending请求应拒绝() {
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        when(userMapper.selectById(2L)).thenReturn(user(2L));
        when(friendshipMapper.findByUserAndFriend(1L, 2L))
                .thenReturn(friendship(1L, 2L, "pending"));

        boolean result = friendshipService.sendFriendRequest(1L, 2L);

        assertFalse(result);
        verify(friendshipMapper, never()).insert(any(Friendship.class));
    }

    @Test
    void sendFriendRequest_已是好友应拒绝() {
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        when(userMapper.selectById(2L)).thenReturn(user(2L));
        when(friendshipMapper.findByUserAndFriend(1L, 2L))
                .thenReturn(friendship(1L, 2L, "accepted"));

        boolean result = friendshipService.sendFriendRequest(1L, 2L);

        assertFalse(result);
        verify(friendshipMapper, never()).insert(any(Friendship.class));
    }

    @Test
    void sendFriendRequest_正常发送应落库() {
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        when(userMapper.selectById(2L)).thenReturn(user(2L));
        when(friendshipMapper.findByUserAndFriend(1L, 2L)).thenReturn(null);
        when(friendshipMapper.findByUserAndFriend(2L, 1L)).thenReturn(null);

        boolean result = friendshipService.sendFriendRequest(1L, 2L);

        assertTrue(result);
        verify(friendshipMapper).insert(any(Friendship.class));
    }

    @Test
    void sendFriendRequest_我拉黑对方应拒绝() {
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        when(userMapper.selectById(2L)).thenReturn(user(2L));
        when(userBlockMapper.isBlocked(1L, 2L)).thenReturn(1);

        boolean result = friendshipService.sendFriendRequest(1L, 2L);

        assertFalse(result);
        verify(friendshipMapper, never()).insert(any(Friendship.class));
    }

    @Test
    void sendFriendRequest_对方拉黑我应拒绝() {
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        when(userMapper.selectById(2L)).thenReturn(user(2L));
        when(userBlockMapper.isBlocked(1L, 2L)).thenReturn(0);
        when(userBlockMapper.isBlocked(2L, 1L)).thenReturn(1);

        boolean result = friendshipService.sendFriendRequest(1L, 2L);

        assertFalse(result);
        verify(friendshipMapper, never()).insert(any(Friendship.class));
    }

    @Test
    void sendFriendRequest_对方已发pending应自动建立双向关系() {
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        when(userMapper.selectById(2L)).thenReturn(user(2L));
        when(friendshipMapper.findByUserAndFriend(1L, 2L)).thenReturn(null);
        when(friendshipMapper.findByUserAndFriend(2L, 1L))
                .thenReturn(friendship(2L, 1L, "pending"));

        boolean result = friendshipService.sendFriendRequest(1L, 2L);

        assertTrue(result);
        verify(friendshipMapper).updateById(any(Friendship.class));
        verify(friendshipMapper).insert(any(Friendship.class));
    }

    // ============ acceptFriendRequest ============

    @Test
    void acceptFriendRequest_正常接受应建立双向关系() {
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        when(userMapper.selectById(2L)).thenReturn(user(2L));
        when(friendshipMapper.findByUserAndFriend(2L, 1L))
                .thenReturn(friendship(2L, 1L, "pending"));
        when(friendshipMapper.findByUserAndFriend(1L, 2L)).thenReturn(null);

        boolean result = friendshipService.acceptFriendRequest(1L, 2L);

        assertTrue(result);
        verify(friendshipMapper).updateById(any(Friendship.class));
        verify(friendshipMapper).insert(any(Friendship.class));
    }

    @Test
    void acceptFriendRequest_请求不存在应返回false() {
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        when(userMapper.selectById(2L)).thenReturn(user(2L));
        when(friendshipMapper.findByUserAndFriend(2L, 1L)).thenReturn(null);

        boolean result = friendshipService.acceptFriendRequest(1L, 2L);

        assertFalse(result);
        verify(friendshipMapper, never()).insert(any(Friendship.class));
    }

    // ============ rejectFriendRequest ============

    @Test
    void rejectFriendRequest_正常拒绝() {
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        when(userMapper.selectById(2L)).thenReturn(user(2L));
        when(friendshipMapper.findByUserAndFriend(2L, 1L))
                .thenReturn(friendship(2L, 1L, "pending"));

        boolean result = friendshipService.rejectFriendRequest(1L, 2L);

        assertTrue(result);
        verify(friendshipMapper).updateById(any(Friendship.class));
    }

    // ============ removeFriend ============

    @Test
    void removeFriend_正常删除双向关系() {
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        when(userMapper.selectById(2L)).thenReturn(user(2L));
        when(friendshipMapper.findByUserAndFriend(1L, 2L))
                .thenReturn(friendship(1L, 2L, "accepted"));
        when(friendshipMapper.findByUserAndFriend(2L, 1L))
                .thenReturn(friendship(2L, 1L, "accepted"));

        boolean result = friendshipService.removeFriend(1L, 2L);

        assertTrue(result);
        verify(friendshipMapper, atLeastOnce()).deleteById(anyLong());
    }

    // ============ getFriendList / checkFriendStatus ============

    @Test
    void getFriendList_用户不存在应返回空列表() {
        when(userMapper.selectById(99L)).thenReturn(null);

        List<Friendship> list = friendshipService.getFriendList(99L);

        assertTrue(list.isEmpty());
    }

    @Test
    void checkFriendStatus_自己应返回self() {
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        assertEquals("self", friendshipService.checkFriendStatus(1L, 1L));
    }

    @Test
    void checkFriendStatus_已是好友应返回friend() {
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        when(userMapper.selectById(2L)).thenReturn(user(2L));
        when(friendshipMapper.findByUserAndFriend(1L, 2L))
                .thenReturn(friendship(1L, 2L, "accepted"));

        assertEquals("friend", friendshipService.checkFriendStatus(1L, 2L));
    }
}

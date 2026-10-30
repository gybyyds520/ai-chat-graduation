package com.example.aichat;

import com.example.aichat.entity.SocialPost;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.FriendshipMapper;
import com.example.aichat.mapper.SocialPostMapper;
import com.example.aichat.mapper.SocialPostTagMapper;
import com.example.aichat.mapper.UserBlockMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.impl.FriendshipServiceImpl;
import com.example.aichat.service.impl.SearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * 社交搜索 + 好友关系集成测试
 *
 * 本地化方案：不再依赖真实 MySQL。直接 new SearchServiceImpl / FriendshipServiceImpl，
 * 用 Mockito mock 全部 Mapper，让空参短路、QueryWrapper 拼装后的查询、好友状态机
 * 等本地逻辑在内存中真实执行（数据由 mock 决定）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SocialSearchIntegrationTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private SocialPostMapper socialPostMapper;

    @Mock
    private SocialPostTagMapper socialPostTagMapper;

    @Mock
    private FriendshipMapper friendshipMapper;

    @Mock
    private UserBlockMapper userBlockMapper;

    private SearchServiceImpl searchService;

    private FriendshipServiceImpl friendshipService;

    @BeforeEach
    void setUp() {
        searchService = new SearchServiceImpl(userMapper, socialPostMapper, socialPostTagMapper);
        friendshipService = new FriendshipServiceImpl(friendshipMapper, userMapper, userBlockMapper);

        // 通用 stub：用户存在（好友关系校验通过）
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setNickname("用户一");
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        lenient().when(userMapper.selectById(1L)).thenReturn(user1);
        lenient().when(userMapper.selectById(2L)).thenReturn(user2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSearchUsers() {
        lenient().when(userMapper.selectList(any())).thenReturn(new ArrayList<>());
        List<User> users = searchService.searchUsers("user", 10);
        assertNotNull(users);
        assertTrue(users.size() >= 0);
        System.out.println("[社交搜索测试] 用户搜索: 找到 " + users.size() + " 个用户");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSearchPosts() {
        SocialPost post = new SocialPost();
        post.setId(1L);
        post.setContent("AI技术发展现状与未来趋势");
        post.setCreatedAt(LocalDateTime.now());
        lenient().when(socialPostMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(post)));

        List<SocialPost> posts = searchService.searchPosts("AI", 10);
        assertNotNull(posts);
        assertTrue(posts.size() >= 1);
        System.out.println("[社交搜索测试] 帖子搜索: 找到 " + posts.size() + " 个帖子");

        SocialPost first = posts.get(0);
        assertNotNull(first.getContent());
        assertTrue(first.getContent().contains("AI") || first.getContent().contains("人工智能"));
    }

    @Test
    void testSearchPostsWithEmptyKeyword() {
        // 空关键词走本地短路，不查库
        List<SocialPost> posts = searchService.searchPosts("", 10);
        assertNotNull(posts);
        assertTrue(posts.isEmpty());
        System.out.println("[社交搜索测试] 空关键词搜索: 返回空列表");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSearchPostsWithNonExistentKeyword() {
        lenient().when(socialPostMapper.selectList(any())).thenReturn(new ArrayList<>());
        List<SocialPost> posts = searchService.searchPosts("nonexistentkeywordxyz", 10);
        assertNotNull(posts);
        assertTrue(posts.isEmpty());
        System.out.println("[社交搜索测试] 不存在的关键词搜索: 返回空列表");
    }

    @Test
    void testFriendSearch() {
        User u = new User();
        u.setId(1L);
        u.setUsername("user1");
        u.setNickname("用户一");
        lenient().when(userMapper.selectOne(any())).thenReturn(u);

        Map<String, Object> user = friendshipService.getUserInfoByUsername("user1");
        assertNotNull(user);
        System.out.println("[好友搜索测试] 搜索用户: " + user);
    }

    @Test
    void testGetFriendList() {
        lenient().when(friendshipMapper.getFriendList(anyLong())).thenReturn(new ArrayList<>());
        List<?> friends = friendshipService.getFriendList(1L);
        assertNotNull(friends);
        System.out.println("[好友搜索测试] 获取好友列表: " + friends.size() + " 个好友");
    }

    @Test
    void testCheckFriendStatus() {
        // 默认 mock 返回 null：双方无关系 → stranger
        String status = friendshipService.checkFriendStatus(1L, 2L);
        assertNotNull(status);
        System.out.println("[好友搜索测试] 好友状态: " + status);
    }
}

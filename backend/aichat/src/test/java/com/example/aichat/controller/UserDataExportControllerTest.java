package com.example.aichat.controller;

import com.example.aichat.common.security.UserPrincipal;
import com.example.aichat.dto.R;
import com.example.aichat.entity.ChatMessage;
import com.example.aichat.entity.SocialPost;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.ChatMessageMapper;
import com.example.aichat.mapper.CommunityPostMapper;
import com.example.aichat.mapper.SocialPostMapper;
import com.example.aichat.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * M3-A9：个人数据导出接口测试（包含资料 + 对话 + 帖子；仅本人可导出）。
 */
@ExtendWith(MockitoExtension.class)
class UserDataExportControllerTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private SocialPostMapper socialPostMapper;
    @Mock
    private CommunityPostMapper communityPostMapper;

    private UserDataExportController controller;

    @BeforeEach
    void login() {
        controller = new UserDataExportController(userMapper, chatMessageMapper,
                socialPostMapper, communityPostMapper);
        UserPrincipal principal = new UserPrincipal(42L, "alice");
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 导出应包含资料对话帖子三部分() {
        User u = new User();
        u.setId(42L);
        u.setUsername("alice");
        u.setNickname("Alice");
        when(userMapper.selectById(42L)).thenReturn(u);
        when(chatMessageMapper.selectList(any())).thenReturn(List.of(new ChatMessage()));
        when(socialPostMapper.selectList(any())).thenReturn(List.of(new SocialPost()));
        when(communityPostMapper.selectList(any())).thenReturn(List.of());

        R<Map<String, Object>> resp = controller.exportMyData();
        assertEquals(200, resp.getCode());
        Map<String, Object> data = resp.getData();
        assertEquals(42L, data.get("userId"));
        assertNotNull(data.get("profile"));
        assertNotNull(data.get("aiConversations"));
        assertNotNull(data.get("socialPosts"));
        assertNotNull(data.get("communityPosts"));
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) data.get("profile");
        assertEquals("alice", profile.get("username"));
    }
}

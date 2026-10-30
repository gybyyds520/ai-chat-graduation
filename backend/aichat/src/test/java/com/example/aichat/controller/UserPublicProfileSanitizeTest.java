package com.example.aichat.controller;

import com.example.aichat.common.security.UserPrincipal;
import com.example.aichat.entity.User;
import com.example.aichat.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * P1-8 回归：任意登录用户通过 /api/user/{id}、/api/user/profile 遍历他人时，
 * 不得读到 phone/email/lastLoginIp 等 PII；本人查看自己仍可见。
 */
@ExtendWith(MockitoExtension.class)
class UserPublicProfileSanitizeTest {

    @Mock private UserService userService;
    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        UserPrincipal principal = new UserPrincipal(1L, "me", List.of("ROLE_USER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private User victim() {
        User u = new User();
        u.setId(2L);
        u.setNickname("victim");
        u.setPhone("13900000000");
        u.setEmail("victim@example.com");
        u.setLastLoginIp("10.0.0.5");
        return u;
    }

    @Test
    void 查询他人_手机号邮箱登录IP被剥离() {
        when(userService.getUserById(2L)).thenReturn(victim());

        com.example.aichat.dto.R<User> resp = controller.getUserById(2L);
        User data = resp.getData();
        assertEquals("victim", data.getNickname());
        assertNull(data.getPhone(), "他人不可见手机号");
        assertNull(data.getEmail(), "他人不可见邮箱");
        assertNull(data.getLastLoginIp(), "他人不可见最近登录IP");
    }

    @Test
    void 查询本人_PII保留() {
        User me = victim();
        me.setId(1L);
        when(userService.getUserById(1L)).thenReturn(me);

        com.example.aichat.dto.R<User> resp = controller.getUserById(1L);
        User data = resp.getData();
        assertEquals("13900000000", data.getPhone(), "本人应可见自己的手机号");
        assertEquals("victim@example.com", data.getEmail());
    }
}

package com.example.aichat.service.impl;

import com.example.aichat.entity.User;
import com.example.aichat.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdminServiceImpl 单元测试。
 *
 * 覆盖点：
 *   1. adminLogin：空用户名/空密码/非管理员/库中无 admin/密码为空 均返回 null；
 *   2. adminLogin：`{bcrypt}` 前缀哈希正确匹配、无前缀 BCrypt 哈希正确匹配、密码错误；
 *   3. isAdmin：null、普通用户、管理员三种情况。
 *
 * 注：明文口令兼容分支已移除（安全修复，杜绝明文口令可登录管理员），
 * 存储口令一律按 DelegatingPasswordEncoder 校验（`{bcrypt}` 前缀或裸 $2a$ 哈希）。
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User adminWithPassword(String stored) {
        User u = new User();
        u.setId(1L);
        u.setUsername("admin");
        u.setPassword(stored);
        return u;
    }

    @Test
    void adminLogin_用户名为空返回null() {
        assertNull(adminService.adminLogin(null, "pw"));
        assertNull(adminService.adminLogin("  ", "pw"));
    }

    @Test
    void adminLogin_密码为空返回null() {
        assertNull(adminService.adminLogin("admin", null));
        assertNull(adminService.adminLogin("admin", ""));
    }

    @Test
    void adminLogin_非管理员用户名返回null() {
        assertNull(adminService.adminLogin("bob", "secret"));
        verifyNoInteractions(userMapper);
    }

    @Test
    void adminLogin_库中无admin记录返回null() {
        when(userMapper.selectOne(any())).thenReturn(null);
        assertNull(adminService.adminLogin("admin", "secret"));
    }

    @Test
    void adminLogin_密码字段为空返回null() {
        when(userMapper.selectOne(any())).thenReturn(adminWithPassword(""));
        assertNull(adminService.adminLogin("admin", "secret"));
    }

    @Test
    void adminLogin_BCrypt密码正确应成功() {
        when(userMapper.selectOne(any())).thenReturn(adminWithPassword("$2a$10$hashed"));
        when(passwordEncoder.matches("secret", "$2a$10$hashed")).thenReturn(true);

        User result = adminService.adminLogin("admin", "secret");

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
    }

    @Test
    void adminLogin_BCrypt密码错误返回null() {
        when(userMapper.selectOne(any())).thenReturn(adminWithPassword("$2a$10$hashed"));
        when(passwordEncoder.matches("wrong", "$2a$10$hashed")).thenReturn(false);
        assertNull(adminService.adminLogin("admin", "wrong"));
    }

    @Test
    void adminLogin_前缀哈希密码正确应成功() {
        when(userMapper.selectOne(any())).thenReturn(adminWithPassword("{bcrypt}$2a$10$hashed"));
        when(passwordEncoder.matches("secret", "{bcrypt}$2a$10$hashed")).thenReturn(true);

        User result = adminService.adminLogin("admin", "secret");

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
    }

    @Test
    void adminLogin_明文口令不兼容应返回null() {
        // 安全契约：明文口令不得作为管理员口令被接受（必须走 PasswordEncoder 校验）
        when(userMapper.selectOne(any())).thenReturn(adminWithPassword("plainsecret"));
        when(passwordEncoder.matches("plainsecret", "plainsecret")).thenReturn(false);
        assertNull(adminService.adminLogin("admin", "plainsecret"));
    }

    @Test
    void isAdmin_null返回false() {
        assertFalse(adminService.isAdmin(null));
    }

    @Test
    void isAdmin_普通用户返回false() {
        when(userMapper.selectOne(any())).thenReturn(null);
        assertFalse(adminService.isAdmin(5L));
    }

    @Test
    void isAdmin_admin用户返回true() {
        when(userMapper.selectOne(any())).thenReturn(adminWithPassword("x"));
        assertTrue(adminService.isAdmin(1L));
    }
}

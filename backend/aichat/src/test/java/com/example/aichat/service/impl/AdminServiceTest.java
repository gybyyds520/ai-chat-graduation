package com.example.aichat.service.impl;

import com.example.aichat.entity.User;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AdminService 单元测试（Mockito 隔离测试）。
 *
 * 覆盖点：
 *   1. adminLogin - 正确密码登录成功
 *   2. adminLogin - 错误密码返回 null
 *   3. adminLogin - 不存在的用户返回 null
 *   4. adminLogin - 用户名为空返回 null
 *   5. adminLogin - 密码为空返回 null
 *   6. adminLogin - 非 admin 用户返回 null
 *   7. isAdmin - 管理员返回 true
 *   8. isAdmin - 普通用户返回 false
 *   9. isAdmin - null userId 返回 false
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setPassword("$2a$10$hashedPassword");
    }

    @Test
    void adminLogin_正确密码应返回admin用户() {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        when(userMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class))).thenReturn(adminUser);
        when(passwordEncoder.matches("correctPassword", "$2a$10$hashedPassword")).thenReturn(true);

        User result = adminService.adminLogin("admin", "correctPassword");

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
    }

    @Test
    void adminLogin_错误密码应返回null() {
        when(userMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class))).thenReturn(adminUser);
        when(passwordEncoder.matches("wrongPassword", "$2a$10$hashedPassword")).thenReturn(false);

        User result = adminService.adminLogin("admin", "wrongPassword");

        assertNull(result);
    }

    @Test
    void adminLogin_用户不存在应返回null() {
        when(userMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class))).thenReturn(null);

        User result = adminService.adminLogin("admin", "anyPassword");

        assertNull(result);
    }

    @Test
    void adminLogin_用户名为空应返回null() {
        User result = adminService.adminLogin(null, "password");
        assertNull(result);

        result = adminService.adminLogin("", "password");
        assertNull(result);
    }

    @Test
    void adminLogin_密码为空应返回null() {
        User result = adminService.adminLogin("admin", null);
        assertNull(result);

        result = adminService.adminLogin("admin", "");
        assertNull(result);
    }

    @Test
    void adminLogin_非admin用户应返回null() {
        User result = adminService.adminLogin("regularuser", "password");
        assertNull(result);
        verify(userMapper, never()).selectOne(any());
    }

    @Test
    void isAdmin_管理员应返回true() {
        when(userMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class))).thenReturn(adminUser);

        assertTrue(adminService.isAdmin(1L));
    }

    @Test
    void isAdmin_普通用户应返回false() {
        when(userMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class))).thenReturn(null);

        assertFalse(adminService.isAdmin(42L));
    }

    @Test
    void isAdmin_nullUserId应返回false() {
        assertFalse(adminService.isAdmin(null));
        verifyNoInteractions(userMapper);
    }
}

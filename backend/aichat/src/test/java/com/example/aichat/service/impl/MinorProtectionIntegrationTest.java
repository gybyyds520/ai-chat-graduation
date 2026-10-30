package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.UserErrorCode;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.MinorProtectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 未成年保护集成测试 — 验证"注册未成年 → 受保护行为"完整链路。
 *
 * 覆盖：
 * 1. 注册 age=12 → isMinor=true，minorModeActive=true（自动生效）；
 * 2. 注册 age=17 → 未成年；age=18 → 成年；age=null → 成年；
 * 3. 未成年用户尝试 AI 聊天 → MINOR_AI_CHAT_RESTRICTED；
 * 4. 未成年用户夜间（23:00）访问 → MINOR_NIGHT_RESTRICTED；
 * 5. 未成年用户白天访问 → 放行；
 * 6. 未成年用户尝试手动关闭保护 → MINOR_MODE_TOGGLE_DENIED；
 * 7. 成年用户手动开启保护 → 生效；
 * 8. 成年用户手动关闭保护 → 不生效。
 */
@ExtendWith(MockitoExtension.class)
class MinorProtectionIntegrationTest {

    @Mock
    private UserMapper userMapper;

    private MinorProtectionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MinorProtectionServiceImpl();
        // 注入 mock UserMapper（updateMinorMode 需要）
        org.springframework.test.util.ReflectionTestUtils.setField(service, "userMapper", userMapper);
    }

    // === 1. 注册年龄判定 ===

    @Test
    void 注册年龄12_应判定未成年且保护自动生效() {
        User u = new User();
        u.setId(10L);
        u.setAge(12);
        u.setMinorModeEnabled(null); // 未手动设置 → 按年龄自动

        assertTrue(service.isMinor(u), "age=12 应为未成年");
        assertTrue(service.isMinorModeActive(u), "未成年用户保护模式应自动生效");
        assertTrue(service.shouldFilterSensitiveContent(u), "未成年应加强敏感内容过滤");
    }

    @Test
    void 注册年龄17_应判定未成年() {
        User u = new User();
        u.setAge(17);
        u.setMinorModeEnabled(null);
        assertTrue(service.isMinor(u));
        assertTrue(service.isMinorModeActive(u));
    }

    @Test
    void 注册年龄18_应判定成年保护不自动生效() {
        User u = new User();
        u.setAge(18);
        u.setMinorModeEnabled(null);
        assertFalse(service.isMinor(u), "age=18 应为成年");
        assertFalse(service.isMinorModeActive(u), "成年用户保护模式不应自动生效");
    }

    @Test
    void 注册未填年龄_应按成年处理() {
        User u = new User();
        u.setAge(null);
        u.setMinorModeEnabled(null);
        assertFalse(service.isMinor(u));
        assertFalse(service.isMinorModeActive(u));
    }

    @Test
    void 注册年龄0_应按成年处理() {
        User u = new User();
        u.setAge(0);
        u.setMinorModeEnabled(null);
        assertFalse(service.isMinor(u));
    }

    // === 2. 未成年用户 AI 聊天限制 ===

    @Test
    void 未成年用户尝试AI聊天_应被拒绝() {
        User minor = new User();
        minor.setId(10L);
        minor.setAge(12);
        minor.setMinorModeEnabled(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.checkAiChatAccess(minor));
        assertEquals(UserErrorCode.MINOR_AI_CHAT_RESTRICTED, ex.getErrorCode());
    }

    @Test
    void 成年用户AI聊天_不被拒绝() {
        User adult = new User();
        adult.setAge(25);
        adult.setMinorModeEnabled(null);
        assertDoesNotThrow(() -> service.checkAiChatAccess(adult));
    }

    // === 3. 未成年用户夜间限制 ===

    @Test
    void 未成年用户夜间访问_应被拒绝() {
        User minor = new User();
        minor.setId(10L);
        minor.setAge(12);
        minor.setMinorModeEnabled(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.checkNightAccess(minor, LocalTime.of(23, 0)));
        assertEquals(UserErrorCode.MINOR_NIGHT_RESTRICTED, ex.getErrorCode());
    }

    @Test
    void 未成年用户白天访问_应放行() {
        User minor = new User();
        minor.setId(10L);
        minor.setAge(12);
        minor.setMinorModeEnabled(null);
        assertDoesNotThrow(() -> service.checkNightAccess(minor, LocalTime.of(14, 0)));
    }

    @Test
    void 成年用户夜间访问_应放行() {
        User adult = new User();
        adult.setAge(25);
        adult.setMinorModeEnabled(null);
        assertDoesNotThrow(() -> service.checkNightAccess(adult, LocalTime.of(23, 0)));
    }

    // === 4. 未成年用户手动关闭保护被拒 ===

    @Test
    void 未成年用户尝试关闭保护_应被拒绝() {
        User minor = new User();
        minor.setId(10L);
        minor.setAge(12);
        minor.setMinorModeEnabled(null);

        when(userMapper.selectById(10L)).thenReturn(minor);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateMinorMode(10L, false));
        assertEquals(UserErrorCode.MINOR_MODE_TOGGLE_DENIED, ex.getErrorCode());
        // 不应调用 updateById（保护状态不变）
        verify(userMapper, never()).updateById(any(User.class));
    }

    // === 5. 成年用户手动开关保护 ===

    @Test
    void 成年用户手动开启保护_应生效() {
        User adult = new User();
        adult.setId(20L);
        adult.setAge(25);
        adult.setMinorModeEnabled(null);

        when(userMapper.selectById(20L)).thenReturn(adult);

        User result = service.updateMinorMode(20L, true);
        assertEquals(true, result.getMinorModeEnabled());
        verify(userMapper).updateById(adult);
    }

    @Test
    void 成年用户手动关闭保护_不生效() {
        User adult = new User();
        adult.setId(20L);
        adult.setAge(25);
        adult.setMinorModeEnabled(true);

        when(userMapper.selectById(20L)).thenReturn(adult);

        User result = service.updateMinorMode(20L, false);
        assertEquals(false, result.getMinorModeEnabled());
        // 关闭后 isMinorModeActive 应为 false
        assertFalse(service.isMinorModeActive(result));
    }

    // === 6. 状态快照完整性 ===

    @Test
    void 未成年用户状态快照_应包含完整字段() {
        User minor = new User();
        minor.setId(10L);
        minor.setAge(12);
        minor.setMinorModeEnabled(null);

        var status = service.getMinorModeStatus(minor);
        assertEquals(true, status.get("isMinor"));
        assertEquals(true, status.get("minorModeActive"));
        assertNotNull(status.get("nightRestricted"));
        assertEquals("22:00", status.get("nightStartTime"));
        assertEquals("06:00", status.get("nightEndTime"));
    }
}

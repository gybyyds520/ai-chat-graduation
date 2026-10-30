package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.UserErrorCode;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MinorProtectionService 单元测试（纯 Mockito，不依赖 Spring / H2 / 系统时钟）。
 *
 * 覆盖：
 *   - isMinor：年龄边界 17/18，null/0/正数；
 *   - isMinorModeActive：手动开关 true/false/null + 年龄自动判断 + 成年人手动开启；
 *   - isNightTime：22:00-06:00 夜间窗口边界；
 *   - checkNightAccess：未成年模式+夜间抛异常，未成年模式+白天不抛，非未成年夜间不抛；
 *   - checkAiChatAccess：未成年模式生效抛异常，不生效不抛；
 *   - shouldFilterSensitiveContent；
 *   - getMinorModeStatus：返回结构正确；
 *   - updateMinorMode：成年人开启/关闭成功，未成年人关闭被拒绝。
 */
@ExtendWith(MockitoExtension.class)
class MinorProtectionServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private MinorProtectionServiceImpl service;

    private User minorUser;   // 未成年人
    private User adultUser;    // 成年人
    private User unknownAge;   // 未声明年龄

    @BeforeEach
    void setUp() {
        minorUser = new User();
        minorUser.setId(1L);
        minorUser.setAge(15);
        minorUser.setMinorModeEnabled(null); // 默认按年龄自动

        adultUser = new User();
        adultUser.setId(2L);
        adultUser.setAge(30);
        adultUser.setMinorModeEnabled(null);

        unknownAge = new User();
        unknownAge.setId(3L);
        unknownAge.setAge(null);
        unknownAge.setMinorModeEnabled(null);
    }

    // ============ isMinor ============

    @Test
    void isMinor_年龄17应判定未成年() {
        User u = new User();
        u.setAge(17);
        assertTrue(service.isMinor(u));
    }

    @Test
    void isMinor_年龄18应判定成年() {
        User u = new User();
        u.setAge(18);
        assertFalse(service.isMinor(u));
    }

    @Test
    void isMinor_年龄为null应判定成年() {
        assertFalse(service.isMinor(unknownAge));
    }

    @Test
    void isMinor_年龄为0应判定成年() {
        User u = new User();
        u.setAge(0);
        assertFalse(service.isMinor(u));
    }

    @Test
    void isMinor_年龄25应判定成年() {
        assertFalse(service.isMinor(adultUser));
    }

    // ============ isMinorModeActive ============

    @Test
    void minorModeActive_手动开启应生效() {
        adultUser.setMinorModeEnabled(true);
        assertTrue(service.isMinorModeActive(adultUser));
    }

    @Test
    void minorModeActive_手动关闭不应生效() {
        minorUser.setMinorModeEnabled(false);
        assertFalse(service.isMinorModeActive(minorUser));
    }

    @Test
    void minorModeActive_未设置且未成年应自动生效() {
        assertTrue(service.isMinorModeActive(minorUser));
    }

    @Test
    void minorModeActive_未设置且成年不应生效() {
        assertFalse(service.isMinorModeActive(adultUser));
    }

    @Test
    void minorModeActive_未设置且未声明年龄不应生效() {
        assertFalse(service.isMinorModeActive(unknownAge));
    }

    // ============ isNightTime ============

    @Test
    void isNightTime_23点应判定夜间() {
        assertTrue(service.isNightTime(LocalTime.of(23, 0)));
    }

    @Test
    void isNightTime_凌晨3点应判定夜间() {
        assertTrue(service.isNightTime(LocalTime.of(3, 0)));
    }

    @Test
    void isNightTime_下午14点不应判定夜间() {
        assertFalse(service.isNightTime(LocalTime.of(14, 0)));
    }

    @Test
    void isNightTime_22点整应判定夜间() {
        assertTrue(service.isNightTime(LocalTime.of(22, 0)));
    }

    @Test
    void isNightTime_6点整不应判定夜间() {
        assertFalse(service.isNightTime(LocalTime.of(6, 0)));
    }

    @Test
    void isNightTime_21点59不应判定夜间() {
        assertFalse(service.isNightTime(LocalTime.of(21, 59)));
    }

    // ============ checkNightAccess ============

    @Test
    void checkNightAccess_未成年模式加夜间应抛MINOR_NIGHT_RESTRICTED() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.checkNightAccess(minorUser, LocalTime.of(23, 0)));
        assertEquals(UserErrorCode.MINOR_NIGHT_RESTRICTED, ex.getErrorCode());
    }

    @Test
    void checkNightAccess_未成年模式加白天不应抛异常() {
        assertDoesNotThrow(() -> service.checkNightAccess(minorUser, LocalTime.of(14, 0)));
    }

    @Test
    void checkNightAccess_非未成年加夜间不应抛异常() {
        assertDoesNotThrow(() -> service.checkNightAccess(adultUser, LocalTime.of(23, 0)));
    }

    @Test
    void checkNightAccess_成年人手动开启加夜间应抛异常() {
        adultUser.setMinorModeEnabled(true);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.checkNightAccess(adultUser, LocalTime.of(23, 0)));
        assertEquals(UserErrorCode.MINOR_NIGHT_RESTRICTED, ex.getErrorCode());
    }

    // ============ checkAiChatAccess ============

    @Test
    void checkAiChatAccess_未成年模式生效应抛MINOR_AI_CHAT_RESTRICTED() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.checkAiChatAccess(minorUser));
        assertEquals(UserErrorCode.MINOR_AI_CHAT_RESTRICTED, ex.getErrorCode());
    }

    @Test
    void checkAiChatAccess_未成年模式不生效不应抛异常() {
        assertDoesNotThrow(() -> service.checkAiChatAccess(adultUser));
    }

    // ============ shouldFilterSensitiveContent ============

    @Test
    void shouldFilterSensitiveContent_未成年模式生效应返回true() {
        assertTrue(service.shouldFilterSensitiveContent(minorUser));
        adultUser.setMinorModeEnabled(true);
        assertTrue(service.shouldFilterSensitiveContent(adultUser));
    }

    @Test
    void shouldFilterSensitiveContent_未成年模式不生效应返回false() {
        assertFalse(service.shouldFilterSensitiveContent(adultUser));
        assertFalse(service.shouldFilterSensitiveContent(unknownAge));
    }

    // ============ getMinorModeStatus ============

    @Test
    void getMinorModeStatus_应返回完整结构() {
        Map<String, Object> status = service.getMinorModeStatus(minorUser);
        assertEquals(true, status.get("isMinor"));
        assertEquals(true, status.get("minorModeActive"));
        assertNotNull(status.get("nightRestricted"));
        assertNotNull(status.get("currentTime"));
        assertEquals("22:00", status.get("nightStartTime"));
        assertEquals("06:00", status.get("nightEndTime"));
        assertNull(status.get("manualEnabled"));
    }

    @Test
    void getMinorModeStatus_成年未开启应不生效() {
        Map<String, Object> status = service.getMinorModeStatus(adultUser);
        assertEquals(false, status.get("isMinor"));
        assertEquals(false, status.get("minorModeActive"));
    }

    // ============ updateMinorMode ============

    @Test
    void updateMinorMode_成年人开启应成功() {
        when(userMapper.selectById(2L)).thenReturn(adultUser);
        User result = service.updateMinorMode(2L, true);
        assertEquals(true, result.getMinorModeEnabled());
        assertNotNull(result.getMinorModeUpdatedAt());
        verify(userMapper).updateById(adultUser);
    }

    @Test
    void updateMinorMode_成年人关闭应成功() {
        adultUser.setMinorModeEnabled(true);
        when(userMapper.selectById(2L)).thenReturn(adultUser);
        User result = service.updateMinorMode(2L, false);
        assertEquals(false, result.getMinorModeEnabled());
        verify(userMapper).updateById(adultUser);
    }

    @Test
    void updateMinorMode_未成年人关闭应被拒绝保持开启() {
        when(userMapper.selectById(1L)).thenReturn(minorUser);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateMinorMode(1L, false));
        assertEquals(UserErrorCode.MINOR_MODE_TOGGLE_DENIED, ex.getErrorCode());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void updateMinorMode_用户不存在应抛USER_NOT_FOUND() {
        when(userMapper.selectById(404L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateMinorMode(404L, true));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    // ============ 注册年龄透传集成 ============

    @Test
    void isMinor_边界年龄18与17() {
        User u17 = new User();
        u17.setAge(17);
        User u18 = new User();
        u18.setAge(18);
        assertTrue(service.isMinor(u17));
        assertFalse(service.isMinor(u18));
    }
}

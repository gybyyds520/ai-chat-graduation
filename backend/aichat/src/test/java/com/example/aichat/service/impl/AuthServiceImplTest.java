package com.example.aichat.service.impl;

import com.example.aichat.entity.LoginDevice;
import com.example.aichat.entity.SmsCode;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.LoginAlertMapper;
import com.example.aichat.mapper.LoginDeviceMapper;
import com.example.aichat.mapper.SmsCodeMapper;
import com.example.aichat.mapper.UserBadgeMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.mapper.UserTagMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AuthServiceImpl 单元测试（Mockito 隔离）。
 *
 * 覆盖点：
 *   1. 短信验证码：发送（6 位、入库）、校验（正确/错误/无有效码）；
 *   2. 手机号登录：验证码错误失败、短手机号兜底失败（防 substring 越界）、
 *      已存在用户登录、新用户自动注册；
 *   3. MFA：用户不存在/未启用/空码均返回 false；
 *   4. 设备信任/登出：仅设备属主方可操作（越权静默忽略）；
 *   5. 徽章/标签：重复颁发不重复插入。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private SmsCodeMapper smsCodeMapper;
    @Mock private LoginDeviceMapper loginDeviceMapper;
    @Mock private LoginAlertMapper loginAlertMapper;
    @Mock private UserMapper userMapper;
    @Mock private UserBadgeMapper userBadgeMapper;
    @Mock private UserTagMapper userTagMapper;
    @Mock private com.example.aichat.service.MinorProtectionService minorProtectionService;

    @InjectMocks
    private AuthServiceImpl authService;

    private SmsCode validSmsCode;

    @BeforeEach
    void setUp() {
        validSmsCode = new SmsCode();
        validSmsCode.setId(10L);
        validSmsCode.setPhone("13800000000");
        validSmsCode.setCode("123456");
        validSmsCode.setExpireTime(LocalDateTime.now().plusMinutes(5));
        validSmsCode.setUsed(false);
    }

    // ============ 短信验证码 ============

    @Test
    void sendSmsCode_生成6位验证码并入库() {
        String code = authService.sendSmsCode("13800000000");
        assertNotNull(code);
        assertTrue(code.matches("\\d{6}"), "验证码必须是 6 位数字");
        verify(smsCodeMapper).insert(any(SmsCode.class));
    }

    @Test
    void verifySmsCode_验证码正确应标记已用() {
        when(smsCodeMapper.getValidCode("13800000000")).thenReturn(validSmsCode);

        boolean ok = authService.verifySmsCode("13800000000", "123456");

        assertTrue(ok);
        verify(smsCodeMapper).markAsUsed(10L);
    }

    @Test
    void verifySmsCode_验证码错误应返回false且不标记() {
        when(smsCodeMapper.getValidCode("13800000000")).thenReturn(validSmsCode);

        boolean ok = authService.verifySmsCode("13800000000", "000000");

        assertFalse(ok);
        verify(smsCodeMapper, never()).markAsUsed(any());
    }

    @Test
    void verifySmsCode_无有效验证码应返回false() {
        when(smsCodeMapper.getValidCode("13800000000")).thenReturn(null);
        assertFalse(authService.verifySmsCode("13800000000", "123456"));
    }

    // ============ 手机号登录 ============

    @Test
    void loginByPhone_验证码错误返回失败() {
        when(smsCodeMapper.getValidCode("13800000000")).thenReturn(null);

        Map<String, Object> result = authService.loginByPhone("13800000000", "999999", "dev", "10.0.0.1");

        assertEquals(false, result.get("success"));
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void loginByPhone_手机号过短应兜底失败不查询用户() {
        Map<String, Object> result = authService.loginByPhone("12", "123456", "dev", "10.0.0.1");

        assertEquals(false, result.get("success"));
        // 修复前这里会因 substring(length-4) 抛 IndexOutOfBounds
        verifyNoInteractions(userMapper);
    }

    @Test
    void loginByPhone_手机号为空应兜底失败() {
        Map<String, Object> result = authService.loginByPhone(null, "123456", "dev", "10.0.0.1");
        assertEquals(false, result.get("success"));
        verifyNoInteractions(userMapper);
    }

    @Test
    void loginByPhone_已存在用户应登录成功并签发token() {
        when(smsCodeMapper.getValidCode("13800000000")).thenReturn(validSmsCode);
        User existing = new User();
        existing.setId(5L);
        existing.setUsername("alice");
        when(userMapper.selectOne(any())).thenReturn(existing);
        when(loginDeviceMapper.findByDeviceId(5L, "dev")).thenReturn(null);
        when(userMapper.selectById(5L)).thenReturn(existing);

        Map<String, Object> result = authService.loginByPhone("13800000000", "123456", "dev", "10.0.0.1");

        assertEquals(true, result.get("success"));
        assertNotNull(result.get("token"));
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void loginByPhone_账号被禁用即使验证码正确也拒绝() {
        when(smsCodeMapper.getValidCode("13800000000")).thenReturn(validSmsCode);
        User existing = new User();
        existing.setId(5L);
        existing.setUsername("alice");
        existing.setAccountStatus("disabled");
        when(userMapper.selectOne(any())).thenReturn(existing);

        Map<String, Object> result = authService.loginByPhone("13800000000", "123456", "dev", "10.0.0.1");

        assertEquals(false, result.get("success"));
        assertNull(result.get("token"));
        verify(loginDeviceMapper, never()).insert(any(LoginDevice.class));
    }

    @Test
    void loginByPhone_新用户应自动注册() {
        when(smsCodeMapper.getValidCode("13800000000")).thenReturn(validSmsCode);
        // 首次按手机号查无用户；模拟 MyBatis-Plus insert 回填自增主键
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            ((User) inv.getArgument(0)).setId(99L);
            return 1;
        });
        when(loginDeviceMapper.findByDeviceId(eq(99L), anyString())).thenReturn(null);
        when(userMapper.selectById(99L)).thenAnswer(inv -> {
            User u = new User();
            u.setId(99L);
            return u;
        });

        Map<String, Object> result = authService.loginByPhone("13800000000", "123456", "dev", "10.0.0.1");

        assertEquals(true, result.get("success"));
        verify(userMapper).insert(any(User.class));
    }

    // ============ MFA ============

    @Test
    void enableMfa_用户不存在返回false() {
        when(userMapper.selectById(404L)).thenReturn(null);
        assertFalse(authService.enableMfa(404L, "secret"));
    }

    @Test
    void enableMfa_用户存在返回true() {
        User u = new User();
        u.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(u);
        assertTrue(authService.enableMfa(1L, "BASE32SECRET"));
        assertTrue(u.getMfaEnabled());
    }

    @Test
    void verifyMfa_未启用MFA返回false() {
        User u = new User();
        u.setId(1L);
        u.setMfaEnabled(false);
        when(userMapper.selectById(1L)).thenReturn(u);
        assertFalse(authService.verifyMfa(1L, "123456"));
    }

    @Test
    void verifyMfa_空验证码返回false() {
        User u = new User();
        u.setId(1L);
        u.setMfaEnabled(true);
        u.setMfaSecret("BASE32SECRET");
        when(userMapper.selectById(1L)).thenReturn(u);
        assertFalse(authService.verifyMfa(1L, "  "));
    }

    @Test
    void verifyMfa_用户不存在返回false() {
        when(userMapper.selectById(404L)).thenReturn(null);
        assertFalse(authService.verifyMfa(404L, "123456"));
    }

    // ============ 设备属主校验 ============

    @Test
    void trustDevice_非属主不应更新() {
        LoginDevice device = new LoginDevice();
        device.setId(7L);
        device.setUserId(2L); // 属于用户 2
        when(loginDeviceMapper.selectById(7L)).thenReturn(device);

        authService.trustDevice(999L, 7L, true); // 用户 999 操作

        verify(loginDeviceMapper, never()).updateById(any(LoginDevice.class));
    }

    @Test
    void trustDevice_属主应更新() {
        LoginDevice device = new LoginDevice();
        device.setId(7L);
        device.setUserId(1L);
        when(loginDeviceMapper.selectById(7L)).thenReturn(device);

        authService.trustDevice(1L, 7L, true);

        assertTrue(device.getIsTrusted());
        verify(loginDeviceMapper).updateById(device);
    }

    @Test
    void logoutDevice_非属主不应删除() {
        LoginDevice device = new LoginDevice();
        device.setId(7L);
        device.setUserId(2L);
        when(loginDeviceMapper.selectById(7L)).thenReturn(device);

        authService.logoutDevice(999L, 7L);

        verify(loginDeviceMapper, never()).deleteById(anyLong());
    }

    // ============ 徽章幂等 ============

    @Test
    void awardBadge_已存在徽章不应重复插入() {
        com.example.aichat.entity.UserBadge existing = new com.example.aichat.entity.UserBadge();
        when(userBadgeMapper.selectOne(any())).thenReturn(existing);

        authService.awardBadge(1L, "VIP", "VIP徽章", "desc");

        verify(userBadgeMapper, never()).insert(any(com.example.aichat.entity.UserBadge.class));
    }
}

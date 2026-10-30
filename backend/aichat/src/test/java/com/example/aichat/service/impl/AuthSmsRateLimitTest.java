package com.example.aichat.service.impl;

import com.example.aichat.entity.SmsCode;
import com.example.aichat.mapper.LoginAlertMapper;
import com.example.aichat.mapper.LoginDeviceMapper;
import com.example.aichat.mapper.SmsCodeMapper;
import com.example.aichat.mapper.UserBadgeMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.mapper.UserTagMapper;
import com.example.aichat.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * P1-1 回归：短信验证码登录必须按手机号限流，防止 6 位验证码被穷举。
 */
@ExtendWith(MockitoExtension.class)
class AuthSmsRateLimitTest {

    @Mock private SmsCodeMapper smsCodeMapper;
    @Mock private LoginDeviceMapper loginDeviceMapper;
    @Mock private LoginAlertMapper loginAlertMapper;
    @Mock private UserMapper userMapper;
    @Mock private UserBadgeMapper userBadgeMapper;
    @Mock private UserTagMapper userTagMapper;
    @Mock private com.example.aichat.service.MinorProtectionService minorProtectionService;
    @Mock private CacheService cacheService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(smsCodeMapper, loginDeviceMapper, loginAlertMapper,
                userMapper, userBadgeMapper, userTagMapper, minorProtectionService);
        ReflectionTestUtils.setField(authService, "cacheService", cacheService);
    }

    private SmsCode code(String c) {
        SmsCode s = new SmsCode();
        s.setId(10L);
        s.setPhone("13800000000");
        s.setCode(c);
        s.setExpireTime(LocalDateTime.now().plusMinutes(5));
        s.setUsed(false);
        return s;
    }

    @Test
    void 已锁定时直接拒绝且不查库() {
        when(cacheService.exists("sms:login:lock:13800000000")).thenReturn(true);

        boolean ok = authService.verifySmsCode("13800000000", "123456");

        assertFalse(ok);
        verify(smsCodeMapper, never()).getValidCode(anyString());
    }

    @Test
    void 验证码错误_累计失败次数() {
        when(cacheService.exists(anyString())).thenReturn(false);
        when(smsCodeMapper.getValidCode("13800000000")).thenReturn(code("123456"));
        when(cacheService.increment(anyString(), eq(1L))).thenReturn(1L);

        boolean ok = authService.verifySmsCode("13800000000", "000000");

        assertFalse(ok);
        verify(cacheService).increment(eq("sms:login:fail:13800000000"), eq(1L));
        verify(smsCodeMapper, never()).markAsUsed(anyLong());
    }

    @Test
    void 验证码正确_清空失败计数() {
        when(cacheService.exists(anyString())).thenReturn(false);
        when(smsCodeMapper.getValidCode("13800000000")).thenReturn(code("123456"));

        boolean ok = authService.verifySmsCode("13800000000", "123456");

        assertTrue(ok);
        verify(smsCodeMapper).markAsUsed(10L);
        verify(cacheService).delete("sms:login:fail:13800000000");
    }
}

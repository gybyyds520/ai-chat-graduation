package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.PaymentErrorCode;
import com.example.aichat.config.QuotaProperties;
import com.example.aichat.entity.User;
import com.example.aichat.entity.UserQuota;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.mapper.UserQuotaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDate;

/**
 * M3-A5：配额开关（enabled=false 跳过检查）与超大额度（oversizedLimit）行为测试。
 */
@ExtendWith(MockitoExtension.class)
class QuotaServiceSwitchTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserQuotaMapper userQuotaMapper;

    private QuotaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new QuotaServiceImpl(userMapper, userQuotaMapper);
    }

    private User user(String level) {
        User u = new User();
        u.setId(1L);
        u.setMembershipLevel(level);
        return u;
    }

    @Test
    void 开关关闭时checkAndConsume应直接放行不查库() {
        QuotaProperties props = new QuotaProperties();
        props.setEnabled(false);
        service.setQuotaProperties(props);

        service.checkAndConsume(1L, "ai_chat");

        // 既不查用户也不写配额表
        verify(userMapper, never()).selectById(any());
        verify(userQuotaMapper, never()).insert(any(UserQuota.class));
        verify(userQuotaMapper, never()).updateById(any(UserQuota.class));
    }

    @Test
    void 超大额度应放大FREE用户上限不抛超额() {
        QuotaProperties props = new QuotaProperties();
        props.setEnabled(true);
        props.setOversizedLimit(10_000L);
        service.setQuotaProperties(props);

        // FREE 原生 ai_chat 上限 20，已用 20 本应超额；放大到 10000 后应放行并自增
        UserQuota existing = new UserQuota();
        existing.setUserId(1L);
        existing.setFeatureType("ai_chat");
        existing.setUsedCount(20);
        existing.setLimitCount(20);
        when(userMapper.selectById(1L)).thenReturn(user("FREE"));
        when(userQuotaMapper.selectOne(any())).thenReturn(existing);
        // P1-3：超大额度 ceiling=10000，原子扣减成功（受影响行数=1）
        when(userQuotaMapper.tryConsumeQuota(eq(1L), eq("ai_chat"), any(LocalDate.class), eq(10000)))
                .thenReturn(1);

        assertDoesNotThrow(() -> service.checkAndConsume(1L, "ai_chat"));
        verify(userQuotaMapper).tryConsumeQuota(eq(1L), eq("ai_chat"), any(LocalDate.class), eq(10000));
    }

    @Test
    void 开关开启且未配置超大额度时沿用等级原生上限() {
        QuotaProperties props = new QuotaProperties();
        props.setEnabled(true);
        service.setQuotaProperties(props);

        // FREE ai_chat 上限 20，已用 20 → 超额
        UserQuota existing = new UserQuota();
        existing.setUsedCount(20);
        existing.setLimitCount(20);
        when(userMapper.selectById(1L)).thenReturn(user("FREE"));
        when(userQuotaMapper.selectOne(any())).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.checkAndConsume(1L, "ai_chat"));
        assertEquals(PaymentErrorCode.QUOTA_EXCEEDED, ex.getErrorCode());
    }
}

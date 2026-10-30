package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.PaymentErrorCode;
import com.example.aichat.entity.User;
import com.example.aichat.entity.UserQuota;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.mapper.UserQuotaMapper;
import com.example.aichat.service.quota.FeatureType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * QuotaService 测试：默认配额、消耗成功/超额/无权限、跨日重置、等级差异、重置。
 */
@ExtendWith(MockitoExtension.class)
class QuotaServiceImplTest {

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
    void getCurrentQuota_FREE应返回默认配额() {
        when(userMapper.selectById(1L)).thenReturn(user("FREE"));
        when(userQuotaMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> q = service.getCurrentQuota(1L);
        assertEquals("FREE", q.get("membershipLevel"));
        @SuppressWarnings("unchecked")
        Map<String, Object> features = (Map<String, Object>) q.get("features");
        Map<String, Object> chat = (Map<String, Object>) features.get("ai_chat");
        assertEquals(20, chat.get("limit"));
        assertEquals(0, chat.get("used"));
        Map<String, Object> img = (Map<String, Object>) features.get("image_gen");
        assertEquals(0, img.get("limit"));
    }

    @Test
    void checkAndConsume_当日首次应新建记录并计1次() {
        when(userMapper.selectById(1L)).thenReturn(user("FREE"));
        when(userQuotaMapper.selectOne(any())).thenReturn(null);

        service.checkAndConsume(1L, "ai_chat");

        ArgumentCaptor<UserQuota> captor = ArgumentCaptor.forClass(UserQuota.class);
        verify(userQuotaMapper).insert(captor.capture());
        UserQuota saved = captor.getValue();
        assertEquals("ai_chat", saved.getFeatureType());
        assertEquals(1, saved.getUsedCount());
        assertEquals(20, saved.getLimitCount());
        assertEquals(LocalDate.now(), saved.getResetDate());
    }

    @Test
    void checkAndConsume_已有记录应原子自增() {
        UserQuota existing = new UserQuota();
        existing.setUserId(1L);
        existing.setFeatureType("ai_chat");
        existing.setUsedCount(5);
        existing.setLimitCount(20);
        existing.setResetDate(LocalDate.now());
        when(userMapper.selectById(1L)).thenReturn(user("FREE"));
        when(userQuotaMapper.selectOne(any())).thenReturn(existing);
        // P1-3：扣减改为数据库原子条件 UPDATE，由 mapper 返回受影响行数
        when(userQuotaMapper.tryConsumeQuota(eq(1L), eq("ai_chat"), any(LocalDate.class), eq(20)))
                .thenReturn(1);

        service.checkAndConsume(1L, "ai_chat");

        // 成功路径必须走原子扣减，而不是先读后写
        verify(userQuotaMapper).tryConsumeQuota(eq(1L), eq("ai_chat"), any(LocalDate.class), eq(20));
        verify(userQuotaMapper, never()).updateById(any(UserQuota.class));
    }

    @Test
    void checkAndConsume_超额应抛QUOTA_EXCEEDED() {
        UserQuota existing = new UserQuota();
        existing.setUsedCount(20);
        existing.setLimitCount(20);
        when(userMapper.selectById(1L)).thenReturn(user("FREE"));
        when(userQuotaMapper.selectOne(any())).thenReturn(existing);
        // 数据库判断 used_count < ceiling 不成立，受影响行数=0
        when(userQuotaMapper.tryConsumeQuota(eq(1L), eq("ai_chat"), any(LocalDate.class), eq(20)))
                .thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.checkAndConsume(1L, "ai_chat"));
        assertEquals(PaymentErrorCode.QUOTA_EXCEEDED, ex.getErrorCode());
        verify(userQuotaMapper, never()).updateById(any(UserQuota.class));
    }

    @Test
    void checkAndConsume_FREE无图片权限应抛MEMBERSHIP_REQUIRED() {
        when(userMapper.selectById(1L)).thenReturn(user("FREE"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.checkAndConsume(1L, "image_gen"));
        assertEquals(PaymentErrorCode.MEMBERSHIP_REQUIRED, ex.getErrorCode());
    }

    @Test
    void 跨日记录不命中当日查询即视为重置() {
        // 当日查询返回 null（昨日旧记录因 reset_date 不同不命中）→ 应新建，usedCount 从 1 开始
        when(userMapper.selectById(1L)).thenReturn(user("BASIC"));
        when(userQuotaMapper.selectOne(any())).thenReturn(null);

        service.checkAndConsume(1L, "ai_chat");

        ArgumentCaptor<UserQuota> captor = ArgumentCaptor.forClass(UserQuota.class);
        verify(userQuotaMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getUsedCount());
        assertEquals(100, captor.getValue().getLimitCount());
    }

    @Test
    void 不同等级配额应不同() {
        assertEquals(100, com.example.aichat.service.quota.MembershipLevel.BASIC.limitFor(FeatureType.AI_CHAT));
        assertEquals(500, com.example.aichat.service.quota.MembershipLevel.PREMIUM.limitFor(FeatureType.AI_CHAT));
        assertEquals(100, com.example.aichat.service.quota.MembershipLevel.PREMIUM.limitFor(FeatureType.VOICE_CALL));
    }

    @Test
    void resetDailyQuotas_应清空配额() {
        service.resetDailyQuotas();
        verify(userQuotaMapper).delete(any());

        service.resetDailyQuotas(1L);
        verify(userQuotaMapper, times(2)).delete(any());
    }
}

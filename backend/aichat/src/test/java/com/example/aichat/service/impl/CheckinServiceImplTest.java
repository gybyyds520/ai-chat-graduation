package com.example.aichat.service.impl;

import com.example.aichat.entity.User;
import com.example.aichat.entity.UserCheckin;
import com.example.aichat.entity.UserQuota;
import com.example.aichat.mapper.UserCheckinMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.mapper.UserQuotaMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 每日签到服务测试：连续天数计算、免费用户奖励、7/30 天里程碑、当日重复签到幂等、奖励写入配额。
 */
@ExtendWith(MockitoExtension.class)
class CheckinServiceImplTest {

    @Mock
    private UserCheckinMapper userCheckinMapper;
    @Mock
    private UserQuotaMapper userQuotaMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private CheckinServiceImpl service;

    private User freeUser() {
        User u = new User();
        u.setId(1L);
        u.setMembershipLevel("FREE");
        return u;
    }

    private UserCheckin checkinRecord(LocalDate date, int consecutive) {
        UserCheckin r = new UserCheckin();
        r.setUserId(1L);
        r.setCheckinDate(date);
        r.setConsecutiveDays(consecutive);
        r.setRewardGranted(5);
        return r;
    }

    @Test
    void checkin_首日签到_连续1天奖励5() {
        when(userCheckinMapper.findByUserAndDate(eq(1L), any())).thenReturn(null);
        when(userCheckinMapper.findLatestByUser(1L)).thenReturn(null);
        when(userMapper.selectById(1L)).thenReturn(freeUser());
        when(userQuotaMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> result = service.checkin(1L);

        assertEquals(1, result.get("consecutiveDays"));
        assertEquals(5, result.get("dailyReward"));
        assertEquals(0, result.get("milestoneReward"));
        assertEquals(5, result.get("totalReward"));
        assertEquals(true, result.get("newlyChecked"));
        verify(userCheckinMapper).insert(any(UserCheckin.class));
        verify(userQuotaMapper).insert(any(UserQuota.class));
    }

    @Test
    void checkin_连续昨日_连续天数加1() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        when(userCheckinMapper.findByUserAndDate(eq(1L), any())).thenReturn(null);
        when(userCheckinMapper.findLatestByUser(1L)).thenReturn(checkinRecord(yesterday, 3));
        when(userMapper.selectById(1L)).thenReturn(freeUser());
        when(userQuotaMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> result = service.checkin(1L);

        assertEquals(4, result.get("consecutiveDays"));
        assertEquals(5, result.get("totalReward"));
    }

    @Test
    void checkin_断签后_连续天数重置为1() {
        LocalDate threeDaysAgo = LocalDate.now().minusDays(3);
        when(userCheckinMapper.findByUserAndDate(eq(1L), any())).thenReturn(null);
        when(userCheckinMapper.findLatestByUser(1L)).thenReturn(checkinRecord(threeDaysAgo, 12));
        when(userMapper.selectById(1L)).thenReturn(freeUser());
        when(userQuotaMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> result = service.checkin(1L);

        assertEquals(1, result.get("consecutiveDays"));
        assertEquals(5, result.get("totalReward"));
    }

    @Test
    void checkin_连续第7天_额外奖励20() {
        // 昨天连续 6 天 → 今天第 7 天
        LocalDate yesterday = LocalDate.now().minusDays(1);
        UserCheckin prev = checkinRecord(yesterday, 6);
        when(userCheckinMapper.findByUserAndDate(eq(1L), any())).thenReturn(null);
        when(userCheckinMapper.findLatestByUser(1L)).thenReturn(prev);
        when(userMapper.selectById(1L)).thenReturn(freeUser());
        when(userQuotaMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> result = service.checkin(1L);

        assertEquals(7, result.get("consecutiveDays"));
        assertEquals(5, result.get("dailyReward"));
        assertEquals(20, result.get("milestoneReward"));
        assertEquals(25, result.get("totalReward"));
    }

    @Test
    void checkin_连续第30天_额外奖励50() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        when(userCheckinMapper.findByUserAndDate(eq(1L), any())).thenReturn(null);
        when(userCheckinMapper.findLatestByUser(1L)).thenReturn(checkinRecord(yesterday, 29));
        when(userMapper.selectById(1L)).thenReturn(freeUser());
        when(userQuotaMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> result = service.checkin(1L);

        assertEquals(30, result.get("consecutiveDays"));
        assertEquals(50, result.get("milestoneReward"));
        assertEquals(55, result.get("totalReward"));
    }

    @Test
    void checkin_当日已签到_幂等不重复发奖() {
        when(userCheckinMapper.findByUserAndDate(eq(1L), any()))
                .thenReturn(checkinRecord(LocalDate.now(), 5));

        Map<String, Object> result = service.checkin(1L);

        assertEquals(5, result.get("consecutiveDays"));
        assertEquals(0, result.get("totalReward"));
        assertEquals(true, result.get("alreadyChecked"));
        verify(userCheckinMapper, never()).insert(any(UserCheckin.class));
        verify(userQuotaMapper, never()).insert(any(UserQuota.class));
    }

    @Test
    void checkin_已有当日配额行_奖励累加到bonusLimit() {
        when(userCheckinMapper.findByUserAndDate(eq(1L), any())).thenReturn(null);
        when(userCheckinMapper.findLatestByUser(1L)).thenReturn(null);
        when(userMapper.selectById(1L)).thenReturn(freeUser());
        UserQuota existing = new UserQuota();
        existing.setUserId(1L);
        existing.setFeatureType("ai_chat");
        existing.setUsedCount(2);
        existing.setBonusLimit(3);
        when(userQuotaMapper.selectOne(any())).thenReturn(existing);

        service.checkin(1L);

        assertEquals(8, existing.getBonusLimit()); // 3 + 5
        verify(userQuotaMapper).updateById(existing);
        verify(userQuotaMapper, never()).insert(any(UserQuota.class));
    }

    @Test
    void status_今日未签到_返回预览奖励与预估连续天数() {
        when(userCheckinMapper.findByUserAndDate(eq(1L), any())).thenReturn(null);
        when(userCheckinMapper.findLatestByUser(1L)).thenReturn(checkinRecord(LocalDate.now().minusDays(1), 4));

        Map<String, Object> s = service.status(1L);

        assertEquals(false, s.get("checkedToday"));
        assertEquals(5, s.get("consecutiveDays")); // 昨天4天，今天签的话第5天
    }

    @Test
    void status_今日已签到_返回已签到与实际天数() {
        when(userCheckinMapper.findByUserAndDate(eq(1L), any()))
                .thenReturn(checkinRecord(LocalDate.now(), 9));

        Map<String, Object> s = service.status(1L);

        assertEquals(true, s.get("checkedToday"));
        assertEquals(9, s.get("consecutiveDays"));
    }
}

package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aichat.entity.User;
import com.example.aichat.entity.UserCheckin;
import com.example.aichat.entity.UserQuota;
import com.example.aichat.mapper.UserCheckinMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.mapper.UserQuotaMapper;
import com.example.aichat.service.CheckinService;
import com.example.aichat.service.quota.MembershipLevel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 每日签到打卡实现。
 *
 * 连续天数规则：今天签 → 昨天签过则连续天数+1，否则重置为 1。
 * 奖励：免费用户每日 +5；连续满 7 天额外 +20；满 30 天额外 +50（二者可叠加在同一天结算）。
 * 奖励写入当日 ai_chat 配额行的 bonus_limit，由 QuotaServiceImpl 叠加到上限。
 */
@Service
public class CheckinServiceImpl implements CheckinService {

    /** 每日基础奖励（仅免费用户）。 */
    static final int DAILY_REWARD_FREE = 5;
    /** 连续 7 天里程碑奖励。 */
    static final int MILESTONE_7D = 20;
    /** 连续 30 天里程碑奖励。 */
    static final int MILESTONE_30D = 50;

    private final UserCheckinMapper userCheckinMapper;
    private final UserQuotaMapper userQuotaMapper;
    private final UserMapper userMapper;

    public CheckinServiceImpl(UserCheckinMapper userCheckinMapper,
                              UserQuotaMapper userQuotaMapper,
                              UserMapper userMapper) {
        this.userCheckinMapper = userCheckinMapper;
        this.userQuotaMapper = userQuotaMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> checkin(Long userId) {
        LocalDate today = LocalDate.now();
        UserCheckin existed = userCheckinMapper.findByUserAndDate(userId, today);
        if (existed != null) {
            // 幂等：当日重复签到不重复发奖
            return buildResult(existed.getConsecutiveDays(), 0, 0, false, true);
        }

        int consecutive = computeConsecutive(userId, today);

        MembershipLevel level = MembershipLevel.fromString(readMembershipLevel(userId));
        int dailyReward = (level == MembershipLevel.FREE) ? DAILY_REWARD_FREE : 0;
        int milestone = 0;
        if (consecutive % 30 == 0) {
            milestone += MILESTONE_30D;
        } else if (consecutive % 7 == 0) {
            milestone += MILESTONE_7D;
        }
        int totalReward = dailyReward + milestone;

        UserCheckin record = new UserCheckin();
        record.setUserId(userId);
        record.setCheckinDate(today);
        record.setConsecutiveDays(consecutive);
        record.setRewardGranted(totalReward);
        record.setCreatedAt(LocalDateTime.now());
        userCheckinMapper.insert(record);

        if (totalReward > 0) {
            grantBonusToTodayQuota(userId, totalReward);
        }

        return buildResult(consecutive, dailyReward, milestone, true, false);
    }

    @Override
    public Map<String, Object> status(Long userId) {
        LocalDate today = LocalDate.now();
        UserCheckin todayRecord = userCheckinMapper.findByUserAndDate(userId, today);
        boolean checkedToday = todayRecord != null;
        int currentStreak = checkedToday
                ? todayRecord.getConsecutiveDays()
                : computeConsecutive(userId, today);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkedToday", checkedToday);
        result.put("consecutiveDays", currentStreak);
        result.put("todayReward", checkedToday ? todayRecord.getRewardGranted() : previewReward(currentStreak + 1));
        result.put("resetDate", today.toString());
        return result;
    }

    /** 计算今天若签到后的连续天数（昨天签过则 +1，否则 1）。 */
    private int computeConsecutive(Long userId, LocalDate today) {
        UserCheckin latest = userCheckinMapper.findLatestByUser(userId);
        if (latest == null || latest.getCheckinDate() == null) {
            return 1;
        }
        // 最近一次是昨天 → 连续 +1；今天已单独判断过不会走到这里；更早则断签重置为 1
        return latest.getCheckinDate().equals(today.minusDays(1)) ? latest.getConsecutiveDays() + 1 : 1;
    }

    /** 预览：连续到 nextStreak 天时可获得的奖励总额。 */
    private int previewReward(int nextStreak) {
        int milestone = 0;
        if (nextStreak % 30 == 0) milestone += MILESTONE_30D;
        else if (nextStreak % 7 == 0) milestone += MILESTONE_7D;
        return DAILY_REWARD_FREE + milestone;
    }

    private String readMembershipLevel(Long userId) {
        User u = userMapper.selectById(userId);
        return u == null ? null : u.getMembershipLevel();
    }

    /** 把奖励额度叠加到当日 ai_chat 配额行的 bonus_limit。 */
    private void grantBonusToTodayQuota(Long userId, int reward) {
        LocalDate today = LocalDate.now();
        UserQuota row = userQuotaMapper.selectOne(
                new QueryWrapper<UserQuota>()
                        .eq("user_id", userId)
                        .eq("feature_type", "ai_chat")
                        .eq("reset_date", today)
                        .last("LIMIT 1"));
        if (row == null) {
            UserQuota bonusRow = new UserQuota();
            bonusRow.setUserId(userId);
            bonusRow.setFeatureType("ai_chat");
            bonusRow.setUsedCount(0);
            bonusRow.setLimitCount(0);
            bonusRow.setBonusLimit(reward);
            bonusRow.setResetDate(today);
            bonusRow.setCreatedAt(LocalDateTime.now());
            bonusRow.setUpdatedAt(LocalDateTime.now());
            userQuotaMapper.insert(bonusRow);
        } else {
            int existing = row.getBonusLimit() == null ? 0 : row.getBonusLimit();
            row.setBonusLimit(existing + reward);
            row.setUpdatedAt(LocalDateTime.now());
            userQuotaMapper.updateById(row);
        }
    }

    private Map<String, Object> buildResult(int consecutive, int dailyReward, int milestone,
                                             boolean newlyChecked, boolean alreadyChecked) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("consecutiveDays", consecutive);
        result.put("dailyReward", dailyReward);
        result.put("milestoneReward", milestone);
        result.put("totalReward", dailyReward + milestone);
        result.put("newlyChecked", newlyChecked);
        result.put("alreadyChecked", alreadyChecked);
        return result;
    }
}

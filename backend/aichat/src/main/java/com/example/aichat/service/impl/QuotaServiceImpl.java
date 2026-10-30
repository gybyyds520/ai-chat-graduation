package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.PaymentErrorCode;
import com.example.aichat.config.QuotaProperties;
import com.example.aichat.entity.User;
import com.example.aichat.entity.UserQuota;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.mapper.UserQuotaMapper;
import com.example.aichat.service.QuotaService;
import com.example.aichat.service.quota.FeatureType;
import com.example.aichat.service.quota.MembershipLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuotaServiceImpl implements QuotaService {

    private final UserMapper userMapper;
    private final UserQuotaMapper userQuotaMapper;

    /** 配额开关 / 超大额度配置。可空：单元测试直接 new 本类时不注入，默认开启。 */
    @Autowired(required = false)
    private QuotaProperties quotaProperties;

    public QuotaServiceImpl(UserMapper userMapper, UserQuotaMapper userQuotaMapper) {
        this.userMapper = userMapper;
        this.userQuotaMapper = userQuotaMapper;
    }

    /** 测试 / 自助注入配额配置。 */
    public void setQuotaProperties(QuotaProperties quotaProperties) {
        this.quotaProperties = quotaProperties;
    }

    private boolean quotaEnabled() {
        return quotaProperties == null || quotaProperties.isEnabled();
    }

    /** 实际生效的每日上限：配置了超大额度则放大，否则取等级内置上限。 */
    private int effectiveLimit(MembershipLevel level, FeatureType feature) {
        int base = level.limitFor(feature);
        if (quotaProperties != null && quotaProperties.getOversizedLimit() > 0) {
            return (int) Math.min(Integer.MAX_VALUE, quotaProperties.getOversizedLimit());
        }
        return base;
    }

    @Override
    public Map<String, Object> getCurrentQuota(Long userId) {
        User user = userMapper.selectById(userId);
        MembershipLevel level = MembershipLevel.fromString(user == null ? null : user.getMembershipLevel());

        LocalDate today = LocalDate.now();
        List<UserQuota> rows = userQuotaMapper.selectList(
                new QueryWrapper<UserQuota>().eq("user_id", userId).eq("reset_date", today));
        Map<String, Integer> usedMap = new LinkedHashMap<>();
        int todayBonus = 0;
        for (UserQuota row : rows) {
            usedMap.put(row.getFeatureType(), row.getUsedCount() == null ? 0 : row.getUsedCount());
            if ("ai_chat".equals(row.getFeatureType()) && row.getBonusLimit() != null) {
                todayBonus = row.getBonusLimit();
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("membershipLevel", level.name());
        result.put("membershipLabel", level.getLabel());
        result.put("advanced", level.isAdvanced());
        result.put("membershipExpireAt", user == null ? null : user.getMembershipExpireAt());
        result.put("resetDate", today.toString());
        result.put("checkinBonus", todayBonus);

        Map<String, Object> features = new LinkedHashMap<>();
        for (FeatureType f : FeatureType.values()) {
            int limit = effectiveLimit(level, f);
            // 签到奖励仅叠加在 ai_chat 功能上
            int bonus = f == FeatureType.AI_CHAT ? todayBonus : 0;
            int used = usedMap.getOrDefault(f.getCode(), 0);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("used", used);
            item.put("limit", limit + bonus);
            item.put("available", limit > 0);
            // 今日剩余可用次数（前端直接展示"今日剩余 X 次"）
            item.put("remaining", Math.max(0, limit + bonus - used));
            features.put(f.getCode(), item);
        }
        result.put("features", features);
        return result;
    }

    @Override
    @Transactional
    public void checkAndConsume(Long userId, String featureTypeCode) {
        // 配置开关关闭时（测试环境）直接放行，不查库不扣减
        if (!quotaEnabled()) {
            return;
        }
        FeatureType feature = FeatureType.fromCode(featureTypeCode);
        User user = userMapper.selectById(userId);
        MembershipLevel level = MembershipLevel.fromString(user == null ? null : user.getMembershipLevel());

        int limit = effectiveLimit(level, feature);
        if (limit <= 0) {
            // 该等级不提供此功能
            throw new BusinessException(PaymentErrorCode.MEMBERSHIP_REQUIRED);
        }

        LocalDate today = LocalDate.now();
        UserQuota existing = userQuotaMapper.selectOne(
                new QueryWrapper<UserQuota>()
                        .eq("user_id", userId)
                        .eq("feature_type", feature.getCode())
                        .eq("reset_date", today)
                        .last("LIMIT 1"));

        // 签到等活动奖励叠加在会员等级上限之上（仅 ai_chat 记录携带 bonus_limit）
        int bonus = (existing != null && existing.getBonusLimit() != null) ? existing.getBonusLimit() : 0;
        int ceiling = limit + bonus;

        if (existing == null) {
            // 当日首次使用：新建记录并消耗一次（跨日自动重置——昨日记录因 resetDate 不同不会命中）
            UserQuota quota = new UserQuota();
            quota.setUserId(userId);
            quota.setFeatureType(feature.getCode());
            quota.setUsedCount(1);
            quota.setLimitCount(limit);
            quota.setBonusLimit(0);
            quota.setResetDate(today);
            quota.setCreatedAt(LocalDateTime.now());
            quota.setUpdatedAt(LocalDateTime.now());
            userQuotaMapper.insert(quota);
            return;
        }

        // P1-3 修复：原子条件自增。由数据库在一行内完成"判断 used<ceiling 且 used+1"，
        // 并发请求只会在 used 到达 ceiling 后被 WHERE 条件拒绝，消除原读改写竞态导致的超额。
        int affected = userQuotaMapper.tryConsumeQuota(userId, feature.getCode(), today, ceiling);
        if (affected == 0) {
            throw new BusinessException(PaymentErrorCode.QUOTA_EXCEEDED);
        }
    }

    @Override
    public void resetDailyQuotas() {
        userQuotaMapper.delete(new QueryWrapper<>());
    }

    @Override
    public void resetDailyQuotas(Long userId) {
        userQuotaMapper.delete(new QueryWrapper<UserQuota>().eq("user_id", userId));
    }
}

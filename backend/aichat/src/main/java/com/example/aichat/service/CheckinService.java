package com.example.aichat.service;

import java.util.Map;

/**
 * 每日签到打卡服务。
 *
 * 奖励规则：
 *  - 每日签到：免费用户 AI 聊天额度 +5
 *  - 连续签到 7 天：额外 +20
 *  - 连续签到 30 天：额外 +50
 * 额度奖励叠加在当日 ai_chat 配额上限之上（见 QuotaServiceImpl.bonus_limit）。
 */
public interface CheckinService {

    /**
     * 执行今日签到。当日重复签到抛 BusinessException。
     *
     * @return 签到结果：consecutiveDays / dailyReward / milestoneReward / totalReward / checkedToday
     */
    Map<String, Object> checkin(Long userId);

    /**
     * 查询签到状态：今日是否已签到、当前连续天数、今日奖励预览。
     */
    Map<String, Object> status(Long userId);
}

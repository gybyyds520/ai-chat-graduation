package com.example.aichat.service;

import java.util.Map;

/**
 * 配额管理服务：查询当前配额、检查并消耗配额、重置每日配额。
 */
public interface QuotaService {

    /**
     * 获取用户当前配额使用情况（会员等级、各功能已用/上限、重置时间）。
     */
    Map<String, Object> getCurrentQuota(Long userId);

    /**
     * 检查并消耗一次配额。超额抛 QUOTA_EXCEEDED，该等级无此功能抛 MEMBERSHIP_REQUIRED。
     */
    void checkAndConsume(Long userId, String featureType);

    /**
     * 重置全部用户当日配额（定时任务 / 维护用）。
     */
    void resetDailyQuotas();

    /**
     * 重置指定用户当日配额（测试用 / 自助）。
     */
    void resetDailyQuotas(Long userId);
}

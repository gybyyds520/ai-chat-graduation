package com.example.aichat.service;

import com.example.aichat.entity.UserProfile;

import java.util.List;
import java.util.Map;

/**
 * 用户画像服务（简单版）。
 *
 * 能力：
 *   - 查询当前用户画像（不存在则返回默认空画像，不抛错）；
 *   - 手动更新风格偏好 / 话题 / 活跃时段；
 *   - 对话后自动更新：抽取话题词、累计活跃时段（供 ChatController 调用）。
 *
 * 设计约束：完全离线、纯本地算法，不依赖 LLM / 向量库；
 *           所有读写均按 user_id 隔离。
 */
public interface UserProfileService {

    /** 一天 24 小时划入 4 个时段桶（0-5 凌晨 / 6-11 上午 / 12-17 下午 / 18-23 晚间）。 */
    String SLOT_MORNING = "morning";
    String SLOT_AFTERNOON = "afternoon";
    String SLOT_EVENING = "evening";
    String SLOT_NIGHT = "night";

    /**
     * 查询当前用户画像；不存在时返回一条内存中的默认画像（不落库）。
     */
    UserProfile getOrCreateProfile(Long userId);

    /**
     * 手动更新画像字段。传 null 的字段保持不变；空串同样视为不更新。
     *
     * @param userId         用户 ID
     * @param preferredStyle 对话风格偏好（可空）
     * @param commonTopics   常见话题列表（可空）
     * @param activeTimeSlots 活跃时段覆盖映射（可空）
     * @return 更新后的画像
     */
    UserProfile updateProfile(Long userId, String preferredStyle,
                              List<String> commonTopics,
                              Map<String, Integer> activeTimeSlots);

    /**
     * 对话后自动更新画像：
     *   - 从 message 中提取候选话题词并并入 commonTopics（去重、上限 10 个）；
     *   - 按当前小时累加 activeTimeSlots 对应时段计数。
     * 该方法应在 ChatController 中以 try/catch 包裹调用，失败不影响主对话。
     *
     * @param userId  用户 ID
     * @param message 用户本轮发言（可空/空串时仅更新活跃时段）
     */
    void recordConversation(Long userId, String message);
}

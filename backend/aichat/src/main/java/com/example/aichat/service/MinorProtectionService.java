package com.example.aichat.service;

import com.example.aichat.entity.User;

import java.time.LocalTime;
import java.util.Map;

/**
 * 未成年保护模式（Minor Protection）服务。
 *
 * 设计目标：
 *   - 以"自声明年龄"作为简化的年龄门槛（age &lt; 18 视为未成年人）；
 *   - 未成年人默认开启未成年保护模式，成年人可手动开启加强保护；
 *   - 未成年模式生效时提供：夜间时段禁用、AI 聊天受限、敏感内容加强过滤三类限制。
 *
 * 夜间时段：22:00:00 - 次日 06:00:00（含端点）。
 */
public interface MinorProtectionService {

    /** 夜间保护开始时间 */
    LocalTime NIGHT_START = LocalTime.of(22, 0);
    /** 夜间保护结束时间 */
    LocalTime NIGHT_END = LocalTime.of(6, 0);
    /** 成年年龄门槛 */
    int ADULT_AGE = 18;

    /**
     * 判断用户是否为未成年人（自声明年龄 0&lt;age&lt;18）。
     * age 为 null 或 0（未设置）时按成年人处理，返回 false。
     */
    boolean isMinor(User user);

    /**
     * 未成年保护模式是否对该用户生效。
     * 生效条件：
     *   - minorModeEnabled == Boolean.TRUE（手动开启）；或
     *   - minorModeEnabled == null（未手动设置）且 isMinor(user) 为 true（按年龄自动生效）。
     */
    boolean isMinorModeActive(User user);

    /**
     * 夜间访问检查。未成年模式生效且当前处于夜间时段时，
     * 抛出 BusinessException(MINOR_NIGHT_RESTRICTED)。
     */
    void checkNightAccess(User user);

    /**
     * AI 聊天访问检查。未成年模式生效时抛出 BusinessException(MINOR_AI_CHAT_RESTRICTED)。
     */
    void checkAiChatAccess(User user);

    /**
     * 是否需要对内容做加强过滤。未成年模式生效时返回 true，
     * 供内容审核模块（ContentAuditService）采用更严格的过滤策略。
     */
    boolean shouldFilterSensitiveContent(User user);

    /**
     * 获取当前用户未成年模式状态快照，供前端设置页展示。
     * 返回字段：isMinor、minorModeActive、nightRestricted、currentTime、
     *          nightStartTime、nightEndTime、manualEnabled。
     */
    Map<String, Object> getMinorModeStatus(User user);

    /**
     * 用户手动开启/关闭未成年保护模式。
     * 规则：
     *   - 成年人（非未成年人）可自由开启/关闭；
     *   - 未成年人不可手动关闭（传 false 时忽略，保持开启）；
     *   - 未成年人开启为冗余操作（本就默认开启），仍记录 updatedAt。
     *
     * @param userId 目标用户 ID
     * @param enabled true 开启，false 关闭
     * @return 更新后的用户
     */
    User updateMinorMode(Long userId, Boolean enabled);

    /**
     * 抽取的时间判断方法，便于单元测试传入固定时间，避免依赖系统时钟。
     * 夜间窗口：[22:00, 24:00) ∪ [00:00, 06:00)。
     */
    boolean isNightTime(LocalTime now);
}

package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.UserErrorCode;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.MinorProtectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 未成年保护模式服务实现。
 *
 * 纯业务规则可独立单测：isMinor / isMinorModeActive / isNightTime /
 * shouldFilterSensitiveContent / getMinorModeStatus 均不依赖系统时间以外的外部资源；
 * updateMinorMode 通过 UserMapper 持久化，checkNightAccess 内部调用 isNightTime(LocalTime.now())。
 */
@Service
public class MinorProtectionServiceImpl implements MinorProtectionService {

    private static final Logger logger = LoggerFactory.getLogger(MinorProtectionServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean isMinor(User user) {
        if (user == null) {
            return false;
        }
        Integer age = user.getAge();
        return age != null && age > 0 && age < ADULT_AGE;
    }

    @Override
    public boolean isMinorModeActive(User user) {
        if (user == null) {
            return false;
        }
        Boolean manual = user.getMinorModeEnabled();
        if (Boolean.TRUE.equals(manual)) {
            return true;
        }
        if (Boolean.FALSE.equals(manual)) {
            // 用户显式关闭，即使是未成年人也以用户显式设置为准？
            // 安全约束：未成年人不可手动关闭，因此 DB 中不会出现"未成年人 + minorModeEnabled=false"的合法状态。
            // 防御性处理：若显式 false，则不生效。
            return false;
        }
        // 未手动设置：按年龄自动判断
        return isMinor(user);
    }

    @Override
    public boolean isNightTime(LocalTime now) {
        if (now == null) {
            return false;
        }
        // [22:00, 24:00) 或 [00:00, 06:00)
        return !now.isBefore(NIGHT_START) || now.isBefore(NIGHT_END);
    }

    @Override
    public void checkNightAccess(User user) {
        checkNightAccess(user, LocalTime.now());
    }

    /**
     * 包级重载：允许测试传入固定时间，避免依赖系统时钟。
     */
    void checkNightAccess(User user, LocalTime now) {
        if (!isMinorModeActive(user)) {
            return;
        }
        if (isNightTime(now)) {
            logger.warn("[MINOR_NIGHT_RESTRICTED] userId={} 夜间 22:00-06:00 访问被拒绝",
                    user == null ? null : user.getId());
            throw new BusinessException(UserErrorCode.MINOR_NIGHT_RESTRICTED);
        }
    }

    @Override
    public void checkAiChatAccess(User user) {
        if (isMinorModeActive(user)) {
            logger.warn("[MINOR_AI_CHAT_RESTRICTED] userId={} 未成年模式下 AI 聊天被拒绝",
                    user == null ? null : user.getId());
            throw new BusinessException(UserErrorCode.MINOR_AI_CHAT_RESTRICTED);
        }
    }

    @Override
    public boolean shouldFilterSensitiveContent(User user) {
        return isMinorModeActive(user);
    }

    @Override
    public Map<String, Object> getMinorModeStatus(User user) {
        Map<String, Object> status = new LinkedHashMap<>();
        boolean minor = isMinor(user);
        boolean active = isMinorModeActive(user);
        LocalTime now = LocalTime.now();

        status.put("isMinor", minor);
        status.put("minorModeActive", active);
        status.put("nightRestricted", active && isNightTime(now));
        status.put("currentTime", now.toString());
        status.put("nightStartTime", NIGHT_START.toString());
        status.put("nightEndTime", NIGHT_END.toString());
        status.put("manualEnabled", user == null ? null : user.getMinorModeEnabled());
        return status;
    }

    @Override
    @Transactional
    public User updateMinorMode(Long userId, Boolean enabled) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        boolean target = Boolean.TRUE.equals(enabled);
        boolean minor = isMinor(user);

        if (minor && !target) {
            // 未成年人不可手动关闭
            logger.warn("[MINOR_MODE_TOGGLE_DENIED] userId={} 为未成年人，尝试关闭未成年保护被拒绝", userId);
            throw new BusinessException(UserErrorCode.MINOR_MODE_TOGGLE_DENIED);
        }

        user.setMinorModeEnabled(target);
        user.setMinorModeUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        logger.info("[MINOR_MODE_UPDATED] userId={} enabled={}", userId, target);
        return user;
    }
}

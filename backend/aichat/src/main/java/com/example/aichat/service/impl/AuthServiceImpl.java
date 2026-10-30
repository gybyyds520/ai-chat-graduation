package com.example.aichat.service.impl;

import com.example.aichat.entity.LoginAlert;
import com.example.aichat.entity.LoginDevice;
import com.example.aichat.entity.SmsCode;
import com.example.aichat.entity.User;
import com.example.aichat.entity.UserBadge;
import com.example.aichat.entity.UserTag;
import com.example.aichat.mapper.LoginAlertMapper;
import com.example.aichat.mapper.LoginDeviceMapper;
import com.example.aichat.mapper.SmsCodeMapper;
import com.example.aichat.mapper.UserBadgeMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.mapper.UserTagMapper;
import com.example.aichat.service.AuthService;
import com.example.aichat.util.TOTPUtil;
import com.example.aichat.utils.TokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();
    
    private final SmsCodeMapper smsCodeMapper;
    private final LoginDeviceMapper loginDeviceMapper;
    private final LoginAlertMapper loginAlertMapper;
    private final UserMapper userMapper;
    private final UserBadgeMapper userBadgeMapper;
    private final UserTagMapper userTagMapper;
    private final com.example.aichat.service.MinorProtectionService minorProtectionService;

    /** P1-1：短信登录失败计数/限流存储。Redis 不可用时为 null，降级为不限流（不阻断登录）。 */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.example.aichat.service.CacheService cacheService;

    /** 同一手机号短信登录失败次数上限，达到后锁定。 */
    private static final int MAX_SMS_FAIL = 5;
    /** 失败计数 / 锁定窗口（分钟）。 */
    private static final long SMS_FAIL_WINDOW_MINUTES = 10;
    private static final String SMS_FAIL_KEY = "sms:login:fail:";
    private static final String SMS_LOCK_KEY = "sms:login:lock:";

    public AuthServiceImpl(SmsCodeMapper smsCodeMapper, LoginDeviceMapper loginDeviceMapper,
                          LoginAlertMapper loginAlertMapper, UserMapper userMapper,
                          UserBadgeMapper userBadgeMapper, UserTagMapper userTagMapper,
                          com.example.aichat.service.MinorProtectionService minorProtectionService) {
        this.smsCodeMapper = smsCodeMapper;
        this.loginDeviceMapper = loginDeviceMapper;
        this.loginAlertMapper = loginAlertMapper;
        this.userMapper = userMapper;
        this.userBadgeMapper = userBadgeMapper;
        this.userTagMapper = userTagMapper;
        this.minorProtectionService = minorProtectionService;
    }

    @Override
    public String sendSmsCode(String phone) {
        // 安全加固（P1）：使用 SecureRandom 生成验证码，避免可预测随机数
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1000000));

        // P1-1：新码下发前使该手机号此前所有未使用的旧码失效，缩小可被穷举的有效码数量
        smsCodeMapper.invalidateUnusedCodes(phone);

        SmsCode smsCode = new SmsCode();
        smsCode.setPhone(phone);
        smsCode.setCode(code);
        smsCode.setExpireTime(LocalDateTime.now().plusMinutes(5));
        smsCode.setUsed(false);
        smsCode.setCreatedAt(LocalDateTime.now());

        smsCodeMapper.insert(smsCode);

        // 安全加固（P1）：不再将验证码明文写入日志（原实现会记录验证码，
        // 日志泄露即等于短信验证码泄露）
        logger.info("[短信验证] 已向手机号 {} 发送验证码（内容不记录日志）", phone);
        return code;
    }

    @Override
    public boolean verifySmsCode(String phone, String code) {
        // P1-1：按手机号做失败次数限流。10 分钟内连续失败达上限则临时锁定，
        // 防止对 6 位验证码穷举。Redis 不可用（cacheService==null）时降级为不限流。
        if (cacheService != null && cacheService.exists(SMS_LOCK_KEY + phone)) {
            logger.warn("[短信验证] 手机号失败次数过多已临时锁定: {}", phone);
            return false;
        }

        SmsCode smsCode = smsCodeMapper.getValidCode(phone);
        if (smsCode != null && smsCode.getCode().equals(code)) {
            smsCodeMapper.markAsUsed(smsCode.getId());
            if (cacheService != null) {
                // 验证成功，清空失败计数
                cacheService.delete(SMS_FAIL_KEY + phone);
            }
            return true;
        }

        // 验证失败：累计失败次数；达到上限则加锁
        if (cacheService != null) {
            String failKey = SMS_FAIL_KEY + phone;
            long fails = cacheService.increment(failKey, 1);
            if (fails == 1) {
                cacheService.expire(failKey, java.time.Duration.ofMinutes(SMS_FAIL_WINDOW_MINUTES));
            }
            if (fails >= MAX_SMS_FAIL) {
                cacheService.set(SMS_LOCK_KEY + phone, "1",
                        java.time.Duration.ofMinutes(SMS_FAIL_WINDOW_MINUTES));
                logger.warn("[短信验证] 手机号验证码连续失败 {} 次，锁定 {} 分钟: {}",
                        fails, SMS_FAIL_WINDOW_MINUTES, phone);
            }
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> loginByPhone(String phone, String code, String deviceId, String ipAddress) {
        // 修复：手机号为空/过短时，后续 substring(length-4) 会抛 StringIndexOutOfBoundsException。
        // 服务层做兜底校验，直接返回失败，避免 500。
        if (phone == null || phone.trim().length() < 4) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "手机号格式错误");
            return result;
        }
        if (!verifySmsCode(phone, code)) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "验证码错误");
            return result;
        }
        
        User user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("phone", phone));
        
        if (user == null) {
            user = new User();
            user.setUsername("user_" + phone.substring(phone.length() - 4));
            user.setPhone(phone);
            user.setPhoneVerified(true);
            user.setCreatedAt(LocalDateTime.now());
            userMapper.insert(user);
        }
        
        String token = TokenUtils.generateToken(user.getId().toString(), user.getUsername());

        // 安全加固：禁用/锁定账号即使短信验证码正确也拒绝登录。
        String accountStatus = user.getAccountStatus();
        if ("disabled".equals(accountStatus)) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "账户已被禁用，请联系管理员");
            return result;
        }
        if ("locked".equals(accountStatus)) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "账户已锁定，请联系管理员");
            return result;
        }

        handleLogin(user.getId(), deviceId, "移动端", "mobile", "iOS", "Safari", ipAddress);

        // 未成年保护：手机号登录成功后检查夜间访问限制（未成年模式 + 夜间时段 → 拒绝登录）
        minorProtectionService.checkNightAccess(user);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("token", token);
        result.put("user", user);
        return result;
    }

    @Override
    public boolean enableMfa(Long userId, String secret) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setMfaEnabled(true);
            user.setMfaSecret(secret);
            userMapper.updateById(user);
            return true;
        }
        return false;
    }

    @Override
    public boolean verifyMfa(Long userId, String code) {
        User user = userMapper.selectById(userId);
        if (user == null || !user.getMfaEnabled() || code == null || code.isBlank()) {
            return false;
        }
        // 安全修复（P0）：原实现只要 MFA 已启用就返回 true，验证码形同虚设。
        // 改为按 RFC 6238 TOTP（SHA1/30s/6位）真实校验，支持前后 ±1 时间窗。
        try {
            return TOTPUtil.verify(user.getMfaSecret(), code, 1);
        } catch (Exception e) {
            logger.warn("MFA校验异常: userId={}", userId, e);
            return false;
        }
    }

    @Override
    public List<LoginDevice> getLoginDevices(Long userId) {
        return loginDeviceMapper.getDevicesByUserId(userId);
    }

    @Override
    public void trustDevice(Long userId, Long deviceId, boolean trusted) {
        LoginDevice device = loginDeviceMapper.selectById(deviceId);
        if (device != null && device.getUserId().equals(userId)) {
            device.setIsTrusted(trusted);
            loginDeviceMapper.updateById(device);
        }
    }

    @Override
    public void logoutDevice(Long userId, Long deviceId) {
        LoginDevice device = loginDeviceMapper.selectById(deviceId);
        if (device != null && device.getUserId().equals(userId)) {
            loginDeviceMapper.deleteById(deviceId);
        }
    }

    @Override
    public List<LoginAlert> getLoginAlerts(Long userId) {
        return loginAlertMapper.getAlertsByUserId(userId);
    }

    @Override
    public void markAlertsAsRead(Long userId) {
        loginAlertMapper.markAllAsRead(userId);
    }

    @Override
    public void handleLogin(Long userId, String deviceId, String deviceName, String deviceType,
                           String os, String browser, String ipAddress) {
        LoginDevice existingDevice = loginDeviceMapper.findByDeviceId(userId, deviceId);
        
        if (existingDevice != null) {
            existingDevice.setLastLoginTime(LocalDateTime.now());
            existingDevice.setIpAddress(ipAddress);
            loginDeviceMapper.updateById(existingDevice);
        } else {
            LoginDevice newDevice = new LoginDevice();
            newDevice.setUserId(userId);
            newDevice.setDeviceId(deviceId);
            newDevice.setDeviceName(deviceName);
            newDevice.setDeviceType(deviceType);
            newDevice.setOs(os);
            newDevice.setBrowser(browser);
            newDevice.setIpAddress(ipAddress);
            newDevice.setLastLoginTime(LocalDateTime.now());
            newDevice.setIsTrusted(false);
            newDevice.setCreatedAt(LocalDateTime.now());
            loginDeviceMapper.insert(newDevice);
            
            LoginAlert alert = new LoginAlert();
            alert.setUserId(userId);
            alert.setAlertType("设备变更");
            alert.setIpAddress(ipAddress);
            alert.setDeviceName(deviceName);
            alert.setLoginTime(LocalDateTime.now());
            alert.setStatus("unread");
            alert.setCreatedAt(LocalDateTime.now());
            loginAlertMapper.insert(alert);
        }
        
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setLastLoginIp(ipAddress);
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(user);
        }
    }

    @Override
    public List<UserBadge> getUserBadges(Long userId) {
        return userBadgeMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserBadge>()
                .eq("user_id", userId)
        );
    }

    @Override
    public void awardBadge(Long userId, String badgeId, String badgeName, String description) {
        UserBadge existing = userBadgeMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserBadge>()
                .eq("user_id", userId)
                .eq("badge_id", badgeId)
        );
        
        if (existing == null) {
            UserBadge badge = new UserBadge();
            badge.setUserId(userId);
            badge.setBadgeId(badgeId);
            badge.setBadgeName(badgeName);
            badge.setDescription(description);
            badge.setEarnedAt(LocalDateTime.now());
            userBadgeMapper.insert(badge);
            logger.info("[成就徽章] 用户 {} 获得徽章: {}", userId, badgeName);
        }
    }

    @Override
    public List<UserTag> getUserTags(Long userId) {
        return userTagMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserTag>()
                .eq("user_id", userId)
        );
    }

    @Override
    public void addUserTag(Long userId, String tagName, String category) {
        UserTag existing = userTagMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserTag>()
                .eq("user_id", userId)
                .eq("tag_name", tagName)
        );
        
        if (existing == null) {
            UserTag tag = new UserTag();
            tag.setUserId(userId);
            tag.setTagName(tagName);
            tag.setTagCategory(category);
            tag.setCreatedAt(LocalDateTime.now());
            userTagMapper.insert(tag);
        }
    }

    @Override
    public void removeUserTag(Long userId, String tagName) {
        userTagMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserTag>()
                .eq("user_id", userId)
                .eq("tag_name", tagName)
        );
    }

    @Override
    public String exportUserData(Long userId, String exportType) {
        return "data_export_" + userId + "_" + exportType + "_" + System.currentTimeMillis() + ".json";
    }
}
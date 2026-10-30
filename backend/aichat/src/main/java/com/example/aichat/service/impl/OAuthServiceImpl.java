package com.example.aichat.service.impl;

import com.example.aichat.entity.LoginRiskAssessment;
import com.example.aichat.entity.OAuthProvider;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.LoginRiskAssessmentMapper;
import com.example.aichat.mapper.OAuthProviderMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.OAuthService;
import com.example.aichat.utils.TokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class OAuthServiceImpl implements OAuthService {

    private static final Logger logger = LoggerFactory.getLogger(OAuthServiceImpl.class);

    private static final String PROVIDER_WECHAT = "wechat";

    private final OAuthProviderMapper oAuthProviderMapper;
    private final UserMapper userMapper;
    private final LoginRiskAssessmentMapper riskAssessmentMapper;

    public OAuthServiceImpl(OAuthProviderMapper oAuthProviderMapper,
                            UserMapper userMapper,
                            LoginRiskAssessmentMapper riskAssessmentMapper) {
        this.oAuthProviderMapper = oAuthProviderMapper;
        this.userMapper = userMapper;
        this.riskAssessmentMapper = riskAssessmentMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> wechatLogin(String code, String deviceId, String ipAddress) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("[微信登录] 开始处理微信登录请求");
        logger.info("[微信登录] 参数: code={}, deviceId={}, ipAddress={}", 
                   code != null ? code.substring(0, 8) + "..." : null, deviceId, ipAddress);

        Map<String, Object> result = new HashMap<>();

        try {
            logger.info("[微信登录] Step 1: 调用微信API获取用户信息");
            Map<String, Object> wechatUserInfo = mockWechatApiCall(code);
            
            if (!((Boolean) wechatUserInfo.get("success"))) {
                logger.error("[微信登录] Step 1失败: 微信API调用失败 - {}", wechatUserInfo.get("message"));
                result.put("success", false);
                result.put("message", (String) wechatUserInfo.get("message"));
                return result;
            }

            String openId = (String) wechatUserInfo.get("openid");
            String nickname = (String) wechatUserInfo.get("nickname");
            String avatar = (String) wechatUserInfo.get("avatar");

            logger.info("[微信登录] Step 1成功: 获取到微信用户信息");
            logger.info("[微信登录]   openId={}, nickname={}", openId != null ? openId.substring(0, 8) + "..." : null, nickname);

            logger.info("[微信登录] Step 2: 风险评估");
            Map<String, Object> riskResult = assessLoginRisk(openId, ipAddress, deviceId);
            String riskLevel = (String) riskResult.get("riskLevel");
            int riskScore = (Integer) riskResult.get("riskScore");

            logger.info("[微信登录] Step 2完成: 风险评估结果 - 风险等级={}, 风险分数={}", riskLevel, riskScore);
            logger.info("[微信登录]   评估原因: {}", riskResult.get("reason"));

            if ("HIGH".equals(riskLevel)) {
                logger.warn("[微信登录] Step 2警告: 高风险登录被拦截");
                result.put("success", false);
                result.put("message", "登录存在安全风险，请稍后重试或使用其他方式登录");
                result.put("riskLevel", riskLevel);
                result.put("riskScore", riskScore);
                result.put("riskReason", riskResult.get("reason"));
                return result;
            }

            logger.info("[微信登录] Step 3: 查询OAuth绑定关系");
            OAuthProvider existingProvider = oAuthProviderMapper.findByProviderAndProviderId(PROVIDER_WECHAT, openId);

            User user;
            if (existingProvider != null) {
                logger.info("[微信登录] Step 3: 已存在绑定关系，查找用户 userId={}", existingProvider.getUserId());
                user = userMapper.selectById(existingProvider.getUserId());
                
                if (user == null) {
                    logger.error("[微信登录] Step 3错误: 绑定关系存在但用户不存在");
                    result.put("success", false);
                    result.put("message", "用户不存在");
                    return result;
                }

                logger.info("[微信登录] Step 3成功: 用户已存在，直接登录");
            } else {
                logger.info("[微信登录] Step 3: 未找到绑定关系，创建新用户");
                user = createUserFromWechat(openId, nickname, avatar);
                
                OAuthProvider newProvider = new OAuthProvider();
                newProvider.setUserId(user.getId());
                newProvider.setProvider(PROVIDER_WECHAT);
                newProvider.setProviderId(openId);
                newProvider.setCreatedAt(LocalDateTime.now());
                oAuthProviderMapper.insert(newProvider);

                logger.info("[微信登录] Step 3成功: 新用户创建完成 userId={}", user.getId());
            }

            logger.info("[微信登录] Step 4: 生成登录Token");
            String token = TokenUtils.generateToken(user.getId().toString(), user.getUsername());
            logger.info("[微信登录] Step 4成功: Token生成完成");

            logger.info("[微信登录] Step 5: 记录风险评估结果");
            saveRiskAssessment(user.getId(), ipAddress, deviceId, riskScore, riskLevel, 
                              (String) riskResult.get("reason"), "ALLOW");

            logger.info("[微信登录] ✅ 登录成功");
            logger.info("[微信登录]   userId={}, username={}, nickname={}", user.getId(), user.getUsername(), user.getNickname());
            logger.info("[微信登录]   riskLevel={}, riskScore={}", riskLevel, riskScore);
            logger.info("═══════════════════════════════════════════════════════════════");

            user.setPassword(null);
            result.put("success", true);
            result.put("token", token);
            result.put("user", user);
            result.put("riskLevel", riskLevel);
            result.put("riskScore", riskScore);

        } catch (Exception e) {
            logger.error("[微信登录] ❌ 登录失败，发生异常", e);
            result.put("success", false);
            result.put("message", "登录失败，请稍后重试");
        }

        return result;
    }

    @Override
    public Map<String, Object> wechatCallback(String code, String state) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("[微信回调] 收到微信OAuth2回调");
        logger.info("[微信回调] code={}, state={}", code != null ? code.substring(0, 8) + "..." : null, state);

        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> wechatUserInfo = mockWechatApiCall(code);
            
            if (!((Boolean) wechatUserInfo.get("success"))) {
                logger.error("[微信回调] 微信API调用失败");
                result.put("success", false);
                result.put("message", "微信授权失败");
                return result;
            }

            logger.info("[微信回调] ✅ 获取用户信息成功");
            result.put("success", true);
            result.put("data", wechatUserInfo);

        } catch (Exception e) {
            logger.error("[微信回调] ❌ 处理失败", e);
            result.put("success", false);
            result.put("message", "回调处理失败");
        }

        logger.info("═══════════════════════════════════════════════════════════════");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindWechat(Long userId, String code) {
        logger.info("[微信绑定] 用户 {} 尝试绑定微信", userId);

        Map<String, Object> wechatUserInfo = mockWechatApiCall(code);
        if (!((Boolean) wechatUserInfo.get("success"))) {
            throw new RuntimeException("微信授权失败");
        }

        String openId = (String) wechatUserInfo.get("openid");

        OAuthProvider existing = oAuthProviderMapper.findByProviderAndProviderId(PROVIDER_WECHAT, openId);
        if (existing != null && !existing.getUserId().equals(userId)) {
            throw new RuntimeException("该微信账号已绑定其他用户");
        }

        OAuthProvider provider = oAuthProviderMapper.findByUserIdAndProvider(userId, PROVIDER_WECHAT);
        if (provider != null) {
            provider.setProviderId(openId);
            provider.setCreatedAt(LocalDateTime.now());
            oAuthProviderMapper.updateById(provider);
        } else {
            OAuthProvider newProvider = new OAuthProvider();
            newProvider.setUserId(userId);
            newProvider.setProvider(PROVIDER_WECHAT);
            newProvider.setProviderId(openId);
            newProvider.setCreatedAt(LocalDateTime.now());
            oAuthProviderMapper.insert(newProvider);
        }

        logger.info("[微信绑定] ✅ 用户 {} 微信绑定成功", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindWechat(Long userId) {
        logger.info("[微信解绑] 用户 {} 尝试解绑微信", userId);
        
        OAuthProvider provider = oAuthProviderMapper.findByUserIdAndProvider(userId, PROVIDER_WECHAT);
        if (provider != null) {
            oAuthProviderMapper.deleteById(provider.getId());
            logger.info("[微信解绑] ✅ 用户 {} 微信解绑成功", userId);
        } else {
            logger.warn("[微信解绑] 用户 {} 未绑定微信", userId);
        }
    }

    @Override
    public boolean isWechatBound(Long userId) {
        OAuthProvider provider = oAuthProviderMapper.findByUserIdAndProvider(userId, PROVIDER_WECHAT);
        return provider != null;
    }

    private Map<String, Object> mockWechatApiCall(String code) {
        logger.debug("[微信API] 模拟调用微信code2session接口, code={}", code != null ? code.substring(0, 8) + "..." : null);

        Map<String, Object> result = new HashMap<>();
        
        if (code == null || code.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "code不能为空");
            return result;
        }

        if ("invalid_code".equals(code)) {
            result.put("success", false);
            result.put("message", "无效的code");
            return result;
        }

        String openId = "wx_" + Math.abs(code.hashCode());
        String sessionKey = "session_" + UUID.randomUUID().toString().substring(0, 16);

        result.put("success", true);
        result.put("openid", openId);
        result.put("session_key", sessionKey);
        result.put("nickname", "微信用户_" + code.substring(0, 4));
        result.put("avatar", "https://api.dicebear.com/7.x/avataaars/svg?seed=" + code);

        logger.debug("[微信API] 模拟返回成功: openid={}", openId.substring(0, 8) + "...");
        return result;
    }

    private Map<String, Object> assessLoginRisk(String openId, String ipAddress, String deviceId) {
        logger.debug("[风险评估] 开始评估登录风险");
        
        Map<String, Object> result = new HashMap<>();
        int riskScore = 0;
        List<String> reasons = new ArrayList<>();

        if (ipAddress != null) {
            if (ipAddress.startsWith("10.") || ipAddress.startsWith("192.168.")) {
                logger.debug("[风险评估] IP地址为内网地址，风险降低");
            } else if (ipAddress.contains("unknown")) {
                logger.debug("[风险评估] IP地址未知，增加风险分数");
                riskScore += 20;
                reasons.add("IP地址未知");
            }
        } else {
            riskScore += 30;
            reasons.add("未提供IP地址");
        }

        if (deviceId == null || deviceId.trim().isEmpty()) {
            riskScore += 15;
            reasons.add("未提供设备ID");
        }

        Random random = new Random();
        int randomFactor = random.nextInt(30);
        if (randomFactor > 20) {
            riskScore += randomFactor;
            reasons.add("随机风险因素");
        }

        String riskLevel;
        if (riskScore >= 70) {
            riskLevel = "HIGH";
        } else if (riskScore >= 40) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }

        result.put("riskScore", riskScore);
        result.put("riskLevel", riskLevel);
        result.put("reason", String.join("; ", reasons));

        logger.debug("[风险评估] 完成: score={}, level={}, reasons={}", riskScore, riskLevel, reasons);
        return result;
    }

    private void saveRiskAssessment(Long userId, String ipAddress, String deviceId, 
                                   int riskScore, String riskLevel, String reason, String action) {
        LoginRiskAssessment assessment = new LoginRiskAssessment();
        assessment.setUserId(userId);
        assessment.setLoginTime(LocalDateTime.now());
        assessment.setIpAddress(ipAddress);
        assessment.setDeviceId(deviceId);
        assessment.setRiskScore(riskScore);
        assessment.setRiskLevel(riskLevel);
        assessment.setAssessmentReason(reason);
        assessment.setActionTaken(action);
        assessment.setCreatedAt(LocalDateTime.now());
        
        riskAssessmentMapper.insert(assessment);
        logger.debug("[风险评估] 记录已保存到数据库");
    }

    private User createUserFromWechat(String openId, String nickname, String avatar) {
        User user = new User();
        user.setUsername("wx_" + openId.substring(0, 12));
        user.setPassword("");
        user.setNickname(nickname != null ? nickname : "微信用户");
        user.setAvatar(avatar);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        userMapper.insert(user);
        logger.debug("[用户创建] 从微信信息创建用户成功");
        return user;
    }
}
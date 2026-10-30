package com.example.aichat.service.impl;

import com.example.aichat.entity.LoginRiskAssessment;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.LoginRiskAssessmentMapper;
import com.example.aichat.mapper.LoginDeviceMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.RiskAssessmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RiskAssessmentServiceImpl implements RiskAssessmentService {

    private static final Logger logger = LoggerFactory.getLogger(RiskAssessmentServiceImpl.class);

    private static final int HIGH_RISK_THRESHOLD = 70;
    private static final int MEDIUM_RISK_THRESHOLD = 40;

    private final LoginRiskAssessmentMapper riskAssessmentMapper;
    private final LoginDeviceMapper loginDeviceMapper;
    private final UserMapper userMapper;

    public RiskAssessmentServiceImpl(LoginRiskAssessmentMapper riskAssessmentMapper,
                                     LoginDeviceMapper loginDeviceMapper,
                                     UserMapper userMapper) {
        this.riskAssessmentMapper = riskAssessmentMapper;
        this.loginDeviceMapper = loginDeviceMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Map<String, Object> assessLoginRisk(Long userId, String ipAddress, String deviceId, String loginType) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("[风险评估] 开始登录风险评估");
        logger.info("[风险评估] 参数: userId={}, ipAddress={}, deviceId={}, loginType={}", 
                   userId, ipAddress, deviceId, loginType);

        Map<String, Object> result = new HashMap<>();
        int totalRiskScore = 0;
        List<String> assessmentReasons = new ArrayList<>();

        logger.info("[风险评估] Step 1: 用户状态检查");
        int userStatusScore = checkUserStatus(userId);
        if (userStatusScore > 0) {
            totalRiskScore += userStatusScore;
            assessmentReasons.add("用户账号状态异常");
        }

        logger.info("[风险评估] Step 2: IP地址风险评估");
        int ipScore = assessIpRisk(ipAddress);
        totalRiskScore += ipScore;
        if (ipScore > 0) {
            assessmentReasons.add("IP地址风险: " + ipScore + "分");
        }

        logger.info("[风险评估] Step 3: 设备风险评估");
        int deviceScore = assessDeviceRisk(userId, deviceId);
        totalRiskScore += deviceScore;
        if (deviceScore > 0) {
            assessmentReasons.add("设备风险: " + deviceScore + "分");
        }

        logger.info("[风险评估] Step 4: 登录频率检查");
        int frequencyScore = checkLoginFrequency(userId);
        totalRiskScore += frequencyScore;
        if (frequencyScore > 0) {
            assessmentReasons.add("登录频率异常: " + frequencyScore + "分");
        }

        logger.info("[风险评估] Step 5: 登录类型评估");
        int typeScore = assessLoginType(loginType);
        totalRiskScore += typeScore;
        if (typeScore > 0) {
            assessmentReasons.add("登录方式风险: " + typeScore + "分");
        }

        String riskLevel = getRiskLevel(totalRiskScore);
        
        logger.info("[风险评估] ✅ 评估完成");
        logger.info("[风险评估]   总分: {}", totalRiskScore);
        logger.info("[风险评估]   等级: {}", riskLevel);
        logger.info("[风险评估]   原因: {}", assessmentReasons);
        logger.info("═══════════════════════════════════════════════════════════════");

        result.put("riskScore", totalRiskScore);
        result.put("riskLevel", riskLevel);
        result.put("reason", assessmentReasons.isEmpty() ? "正常登录" : String.join("; ", assessmentReasons));
        result.put("pass", !"HIGH".equals(riskLevel));

        return result;
    }

    @Override
    public void saveRiskAssessment(Long userId, String ipAddress, String deviceId, 
                                  int riskScore, String riskLevel, String reason, String action) {
        logger.debug("[风险评估] 保存评估记录: userId={}, score={}, level={}, action={}", 
                     userId, riskScore, riskLevel, action);

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
        logger.debug("[风险评估] 评估记录保存成功, id={}", assessment.getId());
    }

    @Override
    public List<LoginRiskAssessment> getRiskHistory(Long userId, int limit) {
        logger.debug("[风险评估] 查询用户风险历史: userId={}, limit={}", userId, limit);
        return riskAssessmentMapper.getAssessmentsByUserId(userId, limit);
    }

    @Override
    public boolean isHighRiskLogin(Long userId, String ipAddress) {
        int highRiskCount = riskAssessmentMapper.countHighRiskLogins24h(userId);
        logger.debug("[风险评估] 24小时内高风险登录次数: {}", highRiskCount);
        
        if (highRiskCount >= 3) {
            logger.warn("[风险评估] 用户 {} 24小时内高风险登录超过3次", userId);
            return true;
        }

        int ipRisk = assessIpRisk(ipAddress);
        return ipRisk >= 50;
    }

    @Override
    public String getRiskLevel(int score) {
        if (score >= HIGH_RISK_THRESHOLD) {
            return "HIGH";
        } else if (score >= MEDIUM_RISK_THRESHOLD) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private int checkUserStatus(Long userId) {
        if (userId == null) {
            logger.debug("[风险评估] 用户ID为空，返回基础风险分");
            return 10;
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            logger.debug("[风险评估] 用户不存在");
            return 20;
        }

        String accountStatus = user.getAccountStatus();
        if ("locked".equals(accountStatus)) {
            logger.warn("[风险评估] 用户账号已锁定");
            return 60;
        }
        if ("disabled".equals(accountStatus)) {
            logger.warn("[风险评估] 用户账号已禁用");
            return 50;
        }

        return 0;
    }

    private int assessIpRisk(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            logger.debug("[风险评估] IP地址为空");
            return 30;
        }

        if (ipAddress.startsWith("127.") || 
            ipAddress.startsWith("10.") || 
            ipAddress.startsWith("192.168.") ||
            ipAddress.startsWith("172.16.")) {
            logger.debug("[风险评估] 内网IP地址，风险较低");
            return 0;
        }

        if (ipAddress.contains("unknown") || 
            ipAddress.contains("localhost") ||
            ipAddress.equals("0:0:0:0:0:0:0:1")) {
            logger.debug("[风险评估] 未知或本地IP");
            return 15;
        }

        List<String> riskIps = Arrays.asList(
            "203.0.113.", "198.51.100.", "169.254."
        );
        for (String riskIp : riskIps) {
            if (ipAddress.startsWith(riskIp)) {
                logger.warn("[风险评估] 检测到风险IP段: {}", ipAddress);
                return 40;
            }
        }

        return 5;
    }

    private int assessDeviceRisk(Long userId, String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            logger.debug("[风险评估] 设备ID为空");
            return 20;
        }

        if (userId == null) {
            return 10;
        }

        try {
            var existingDevice = loginDeviceMapper.findByDeviceId(userId, deviceId);
            if (existingDevice != null) {
                logger.debug("[风险评估] 设备已存在，信任设备");
                if (Boolean.TRUE.equals(existingDevice.getIsTrusted())) {
                    return 0;
                }
                return 5;
            }
        } catch (Exception e) {
            logger.debug("[风险评估] 查询设备信息异常: {}", e.getMessage());
        }

        logger.debug("[风险评估] 新设备登录");
        return 15;
    }

    private int checkLoginFrequency(Long userId) {
        if (userId == null) {
            return 0;
        }

        int highRiskCount = riskAssessmentMapper.countHighRiskLogins24h(userId);
        
        if (highRiskCount >= 5) {
            logger.warn("[风险评估] 用户 {} 24小时内高风险登录超过5次", userId);
            return 30;
        }
        if (highRiskCount >= 3) {
            logger.warn("[风险评估] 用户 {} 24小时内高风险登录超过3次", userId);
            return 20;
        }

        return 0;
    }

    private int assessLoginType(String loginType) {
        if (loginType == null || loginType.trim().isEmpty()) {
            return 10;
        }

        switch (loginType.toLowerCase()) {
            case "password":
                return 5;
            case "sms":
                return 0;
            case "wechat":
            case "oauth":
                return 0;
            case "mfa":
                return -5;
            default:
                return 15;
        }
    }
}
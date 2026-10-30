package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.LoginRiskAssessment;
import com.example.aichat.service.RiskAssessmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/risk")
public class RiskAssessmentController {

    private static final Logger logger = LoggerFactory.getLogger(RiskAssessmentController.class);

    private final RiskAssessmentService riskAssessmentService;

    public RiskAssessmentController(RiskAssessmentService riskAssessmentService) {
        this.riskAssessmentService = riskAssessmentService;
    }

    @PostMapping("/assess")
    public R assessRisk(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        String ipAddress = (String) request.get("ipAddress");
        String deviceId = (String) request.get("deviceId");
        String loginType = (String) request.get("loginType");

        logger.info("[RiskController] 收到风险评估请求: userId={}, ipAddress={}, deviceId={}, loginType={}", 
                   userIdObj, ipAddress, deviceId, loginType);

        // 安全加固：若调用方已登录，则以登录用户为准（登录流程内可匿名调用）
        Long userId = null;
        if (com.example.aichat.common.security.AuthContext.isAuthenticated()) {
            userId = com.example.aichat.common.security.AuthContext.requireSelf(
                    userIdObj == null ? null : ((Number) userIdObj).longValue());
        } else if (userIdObj != null) {
            userId = ((Number) userIdObj).longValue();
        }

        Map<String, Object> result = riskAssessmentService.assessLoginRisk(userId, ipAddress, deviceId, loginType);
        return R.success(result);
    }

    @GetMapping("/history")
    public R getRiskHistory(@RequestParam Long userId, @RequestParam(defaultValue = "10") int limit) {
        // 安全修复（P0）：风险历史仅本人可查
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        limit = Math.min(Math.max(limit, 1), 100);
        logger.info("[RiskController] 查询风险评估历史: userId={}, limit={}", current, limit);
        
        List<LoginRiskAssessment> history = riskAssessmentService.getRiskHistory(current, limit);
        return R.success(history);
    }

    @GetMapping("/check")
    public R checkHighRisk(@RequestParam Long userId, @RequestParam String ipAddress) {
        // 安全修复（P0）：仅本人可检查
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        logger.info("[RiskController] 检查高风险登录: userId={}, ipAddress={}", current, ipAddress);
        
        boolean isHighRisk = riskAssessmentService.isHighRiskLogin(current, ipAddress);
        return R.success(Map.of("isHighRisk", isHighRisk));
    }
}
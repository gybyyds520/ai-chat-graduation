package com.example.aichat.service;

import com.example.aichat.entity.LoginRiskAssessment;

import java.util.List;
import java.util.Map;

public interface RiskAssessmentService {

    Map<String, Object> assessLoginRisk(Long userId, String ipAddress, String deviceId, String loginType);

    void saveRiskAssessment(Long userId, String ipAddress, String deviceId, int riskScore, 
                           String riskLevel, String reason, String action);

    List<LoginRiskAssessment> getRiskHistory(Long userId, int limit);

    boolean isHighRiskLogin(Long userId, String ipAddress);

    String getRiskLevel(int score);
}
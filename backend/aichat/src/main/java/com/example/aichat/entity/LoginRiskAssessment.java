package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("login_risk_assessment")
public class LoginRiskAssessment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDateTime loginTime;

    private String ipAddress;

    private String deviceId;

    private String location;

    private Integer riskScore;

    private String riskLevel;

    private String assessmentReason;

    private String actionTaken;

    private LocalDateTime createdAt;
}
package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String username;
    // 安全修复：使用@JsonIgnore确保密码哈希永远不会被序列化到JSON响应中，防止全端点密码泄露
    @JsonIgnore
    private String password;
    private String nickname;
    private String avatar;
    private String avatarUrl;
    private String email;
    private String bio;
    private String interests;
    private String location;
    private String website;
    private String gender;
    private String birthday;
    private String versionType;
    private LocalDateTime versionExpireTime;
    private String accountStatus;

    // ===== 付费分级会员（Membership Tier） =====
    // 会员等级：FREE / BASIC / PREMIUM，默认 FREE
    private String membershipLevel;
    // 会员到期时间（可空，未付费或永久会员为 null）
    private LocalDateTime membershipExpireAt;

    private String phone;
    private Boolean phoneVerified;
    private Boolean emailVerified;
    private Boolean mfaEnabled;
    // 安全修复：MFA密钥与安全问题答案属于敏感凭证，序列化到接口响应会造成凭证泄露
    @JsonIgnore
    private String mfaSecret;
    private String lastLoginIp;
    private LocalDateTime lastLoginTime;
    private Integer failedLoginCount;
    private LocalDateTime lockTime;
    private String securityQuestion;
    // 安全修复：安全问题答案用于身份验证，不应通过接口外泄
    @JsonIgnore
    private String securityAnswer;
    
    @TableField(exist = false)
    private LocalDateTime createTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ===== 未成年保护模式（Minor Protection） =====
    // 自声明年龄：0 或 null 表示未设置（按成年处理，不触发未成年限制）
    private Integer age;
    // 未成年风险标记：由自声明年龄(age<18)或人工审核维护，供审核/风控快速判定，默认 false
    private Boolean isMinor;
    // 是否手动开启未成年保护模式：true=强制开启，false=强制关闭，null=按年龄自动判断
    private Boolean minorModeEnabled;
    // 未成年模式最后一次被用户手动调整的时间
    private LocalDateTime minorModeUpdatedAt;

    // ===== 语音音色偏好（多音色 TTS） =====
    // 取值见 VoiceProfile 枚举（default/warm/cold/young/old/female/male），默认 default
    private String voicePreference;
}
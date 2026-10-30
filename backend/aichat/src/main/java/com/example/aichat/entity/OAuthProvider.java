package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("oauth_provider")
public class OAuthProvider {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String provider;
    
    private String providerId;
    
    private String accessToken;
    
    private String refreshToken;
    
    private LocalDateTime expireTime;
    
    private LocalDateTime createdAt;
}
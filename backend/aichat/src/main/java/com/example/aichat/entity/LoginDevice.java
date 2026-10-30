package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("login_device")
public class LoginDevice {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String deviceId;
    
    private String deviceName;
    
    private String deviceType;
    
    private String os;
    
    private String browser;
    
    private String ipAddress;
    
    private String location;
    
    private LocalDateTime lastLoginTime;
    
    private Boolean isTrusted;
    
    private LocalDateTime createdAt;
}
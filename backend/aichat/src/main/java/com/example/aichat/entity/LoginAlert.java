package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("login_alert")
public class LoginAlert {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String alertType;
    
    private String ipAddress;
    
    private String location;
    
    private String deviceName;
    
    private LocalDateTime loginTime;
    
    private String status;
    
    private LocalDateTime createdAt;
}
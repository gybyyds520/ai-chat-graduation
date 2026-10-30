package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_online_status")
public class UserOnlineStatus {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    private String status;
    
    @TableField("last_active_time")
    private LocalDateTime lastActiveTime;
    
    @TableField("session_id")
    private String sessionId;
    
    @TableField("ip_address")
    private String ipAddress;
    
    @TableField("device_info")
    private String deviceInfo;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
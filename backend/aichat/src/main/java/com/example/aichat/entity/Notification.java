package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    private String type;
    
    @TableField("target_type")
    private String targetType;
    
    @TableField("target_id")
    private Long targetId;
    
    @TableField("actor_id")
    private Long actorId;
    
    @TableField("actor_name")
    private String actorName;
    
    private String message;
    
    @TableField("is_read")
    private Boolean isRead;
    
    @TableField("is_deleted")
    private Boolean isDeleted;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
}
package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("social_like")
public class SocialLike {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("target_type")
    private String targetType;
    
    @TableField("target_id")
    private Long targetId;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
}
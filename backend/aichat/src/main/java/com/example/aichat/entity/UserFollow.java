package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_follow")
public class UserFollow {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("follower_id")
    private Long followerId;
    
    @TableField("following_id")
    private Long followingId;
    
    @TableField("is_mutual")
    private Boolean isMutual;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
}
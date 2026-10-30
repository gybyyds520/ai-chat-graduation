package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("social_share")
public class SocialShare {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("post_id")
    private Long postId;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("user_name")
    private String userName;
    
    @TableField("share_text")
    private String shareText;
    
    @TableField("share_type")
    private String shareType;
    
    @TableField("target_id")
    private Long targetId;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
}
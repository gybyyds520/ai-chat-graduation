package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("social_comment")
public class SocialComment {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("post_id")
    private Long postId;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("user_name")
    private String userName;
    
    private String content;
    
    @TableField("parent_id")
    private Long parentId;
    
    @TableField("like_count")
    private Integer likeCount;
    
    @TableField("is_deleted")
    private Boolean isDeleted;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
}
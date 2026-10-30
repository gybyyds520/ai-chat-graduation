package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("social_post")
public class SocialPost {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("user_name")
    private String userName;
    
    private String content;
    
    @TableField("media_type")
    private String mediaType;
    
    @TableField("media_urls")
    private String mediaUrls;
    
    @TableField("video_url")
    private String videoUrl;
    
    @TableField("video_cover")
    private String videoCover;
    
    private String location;
    
    private String visibility;
    
    @TableField("like_count")
    private Integer likeCount;
    
    @TableField("comment_count")
    private Integer commentCount;
    
    @TableField("share_count")
    private Integer shareCount;
    
    @TableField("is_deleted")
    private Boolean isDeleted;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
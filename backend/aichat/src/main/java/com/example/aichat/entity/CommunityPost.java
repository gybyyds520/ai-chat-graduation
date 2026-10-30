package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("community_post")
public class CommunityPost {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long communityId;
    
    private Long userId;
    
    private String userName;
    
    private String content;
    
    private String mediaType;
    
    private String mediaUrls;
    
    private Integer likeCount;
    
    private Integer commentCount;
    
    private Boolean isDeleted;

    /** 是否置顶（社群管理员可置顶）。 */
    private Boolean isPinned;
    
    private LocalDateTime createdAt;
}
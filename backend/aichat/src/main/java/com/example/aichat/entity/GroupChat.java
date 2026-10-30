package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("group_chat")
public class GroupChat {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String description;
    
    private String avatar;
    
    private Long ownerId;
    
    private Integer memberCount;
    
    private Boolean isPublic;
    
    private String announcement;
    
    private String settings;
    
    private Integer activityScore;
    
    private LocalDateTime lastActivityTime;
    
    private LocalDateTime createdAt;
}
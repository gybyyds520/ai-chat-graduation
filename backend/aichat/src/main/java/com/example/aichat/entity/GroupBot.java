package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("group_bot")
public class GroupBot {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long groupId;
    
    private String botName;
    
    private String botDescription;
    
    private Boolean enabled;
    
    private String triggerKeywords;
    
    private String autoReplyRules;
    
    private LocalDateTime createdAt;
}

package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("conversation_context")
public class ConversationContext {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String sessionId;
    
    private Long userId;
    
    private Long characterId;
    
    private String contextKey;
    
    private String contextValue;
    
    private LocalDateTime expireTime;
    
    private LocalDateTime createdAt;
}
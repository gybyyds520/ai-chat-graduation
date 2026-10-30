package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("session_id")
    private String sessionId;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("character_id")
    private Long characterId;
    
    @TableField("sender_type")
    private String senderType;
    
    private String content;
    
    @TableField("create_time")
    private LocalDateTime createTime;
}
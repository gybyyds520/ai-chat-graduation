package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("character_interaction")
public class CharacterInteraction {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("character_id")
    private Long characterId;
    
    @TableField("interaction_count")
    private Integer interactionCount;
    
    @TableField("total_messages")
    private Integer totalMessages;
    
    @TableField("first_interaction_at")
    private LocalDateTime firstInteractionAt;
    
    @TableField("last_interaction_at")
    private LocalDateTime lastInteractionAt;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
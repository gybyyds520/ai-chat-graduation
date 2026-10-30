package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("character_share")
public class CharacterShare {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("character_id")
    private Long characterId;
    
    @TableField("shared_by_user_id")
    private Long sharedByUserId;
    
    @TableField("shared_to_user_id")
    private Long sharedToUserId;
    
    private String permissions;
    
    private String status;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    
    @TableField(exist = false)
    private String characterName;
    
    @TableField(exist = false)
    private String characterAvatar;
    
    @TableField(exist = false)
    private String sharedByNickname;
    
    @TableField(exist = false)
    private String sharedToNickname;
}
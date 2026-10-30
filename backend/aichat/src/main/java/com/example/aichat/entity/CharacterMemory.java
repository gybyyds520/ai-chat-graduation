package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("character_memory")
public class CharacterMemory {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long characterId;
    
    private Long userId;
    
    private String memoryKey;
    
    private String memoryValue;
    
    private String memoryType;
    
    private Double importance;
    
    private LocalDateTime lastAccessed;
    
    private LocalDateTime createdAt;
}
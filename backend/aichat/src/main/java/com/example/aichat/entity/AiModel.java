package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_model")
public class AiModel {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String modelName;
    
    private String modelKey;
    
    private String provider;
    
    private String description;
    
    private Double costPerToken;
    
    private Integer maxTokens;
    
    private Boolean available;
    
    private String category;
    
    private Integer priority;
    
    private LocalDateTime createdAt;
}
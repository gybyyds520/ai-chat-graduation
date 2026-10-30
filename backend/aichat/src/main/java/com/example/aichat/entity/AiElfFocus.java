package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_elf_focus")
public class AiElfFocus {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long elfId;
    
    private Long userId;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private Integer durationMinutes;
    
    private String goal;
    
    private Boolean completed;
    
    private LocalDateTime createdAt;
}
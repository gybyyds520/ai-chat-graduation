package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_elf_schedule")
public class AiElfSchedule {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long elfId;
    
    private Long userId;
    
    private String title;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private String location;
    
    private String description;
    
    private Integer reminderMinutes;
    
    private Boolean completed;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
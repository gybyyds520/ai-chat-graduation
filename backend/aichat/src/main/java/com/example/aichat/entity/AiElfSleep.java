package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ai_elf_sleep")
public class AiElfSleep {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long elfId;
    
    private Long userId;
    
    private String sleepTime;
    
    private String wakeTime;
    
    private Integer quality;
    
    private Double durationHours;
    
    private String notes;
    
    private LocalDate recordDate;
    
    private LocalDateTime createdAt;
}
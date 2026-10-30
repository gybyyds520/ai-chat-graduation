package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_elf_health_reminder")
public class AiElfHealthReminder {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long elfId;
    
    private Long userId;
    
    private String reminderType;
    
    private LocalDateTime lastReminded;
    
    private Boolean enabled;
    
    private Integer intervalMinutes;
    
    private LocalDateTime createdAt;
}
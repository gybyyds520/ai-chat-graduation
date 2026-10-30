package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_elf_alarm")
public class AiElfAlarm {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long elfId;
    
    private Long userId;
    
    private String title;
    
    private String time;
    
    private String repeatDays;
    
    private Boolean enabled;
    
    private String ringtone;
    
    private String message;
    
    private Boolean smartEnabled;
    
    private Integer adjustedMinutes;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
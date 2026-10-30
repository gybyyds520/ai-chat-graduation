package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sleep_analysis_report")
public class SleepAnalysisReport {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long elfId;
    
    private Long userId;
    
    private LocalDate reportDate;
    
    private Double avgDuration;
    
    private Integer avgQuality;
    
    private Integer sleepScore;
    
    private String suggestions;
    
    private Double deepSleepRatio;
    
    private LocalDateTime createdAt;
}
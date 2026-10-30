package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_analysis_record")
public class HealthAnalysisRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long elfId;
    
    private Long userId;
    
    private LocalDate analysisDate;
    
    private Integer focusDuration;
    
    private Integer sleepQuality;
    
    private Integer stepCount;
    
    private Integer moodScore;
    
    private Integer healthScore;
    
    private String suggestions;
    
    private LocalDateTime createdAt;
}

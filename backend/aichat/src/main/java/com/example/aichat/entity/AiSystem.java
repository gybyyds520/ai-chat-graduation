package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI系统注册实体
 */
@Data
@TableName("ai_system")
public class AiSystem {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("system_name")
    private String systemName;
    
    @TableField("system_type")
    private String systemType;
    
    @TableField("api_key")
    private String apiKey;
    
    @TableField("secret_key")
    private String secretKey;
    
    private String description;
    
    private Integer status;
    
    @TableField("rate_limit")
    private Integer rateLimit;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    
    @TableField("last_access_at")
    private LocalDateTime lastAccessAt;

    /** 累计调用次数（认证成功后自增） */
    @TableField("call_count")
    private Integer callCount;
}

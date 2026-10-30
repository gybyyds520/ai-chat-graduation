package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访问日志实体
 */
@Data
@TableName("access_log")
public class AccessLog {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("ai_system_id")
    private Long aiSystemId;
    
    @TableField("data_id")
    private Long dataId;
    
    @TableField("access_type")
    private String accessType;
    
    @TableField("request_method")
    private String requestMethod;
    
    @TableField("request_path")
    private String requestPath;
    
    @TableField("request_params")
    private String requestParams;
    
    @TableField("response_status")
    private Integer responseStatus;
    
    @TableField("response_time")
    private Integer responseTime;
    
    @TableField("ip_address")
    private String ipAddress;
    
    @TableField("user_agent")
    private String userAgent;
    
    @TableField("error_message")
    private String errorMessage;
    
    @TableField("accessed_at")
    private LocalDateTime accessedAt;
}

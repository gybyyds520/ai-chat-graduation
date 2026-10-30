package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 数据池实体
 */
@Data
@TableName(value = "data_pool", autoResultMap = true)
public class DataPool {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private String title;
    
    private String content;
    
    @TableField("data_type")
    private String dataType;
    
    private String format;
    
    @TableField("file_path")
    private String filePath;
    
    @TableField("file_size")
    private Long fileSize;
    
    private String checksum;
    
    private String category;
    
    private String description;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;
    
    private Integer version;
    
    @TableField("is_public")
    private Integer isPublic;
    
    private Integer status;
    
    @TableField("created_by")
    private String createdBy;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    
    @TableField("expires_at")
    private LocalDateTime expiresAt;
}

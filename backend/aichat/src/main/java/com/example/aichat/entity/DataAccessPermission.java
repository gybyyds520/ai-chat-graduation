package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据访问权限实体
 */
@Data
@TableName("data_access_permission")
public class DataAccessPermission {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("data_id")
    private Long dataId;
    
    @TableField("ai_system_id")
    private Long aiSystemId;
    
    @TableField("permission_type")
    private String permissionType;
    
    @TableField("data_category")
    private String dataCategory;
    
    @TableField("expires_at")
    private LocalDateTime expiresAt;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("created_by")
    private String createdBy;
}

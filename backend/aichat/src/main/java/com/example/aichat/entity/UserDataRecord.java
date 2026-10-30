package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_data_record")
public class UserDataRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("data_type")
    private String dataType;

    @TableField("content")
    private String content;

    @TableField("metadata")
    private String metadata;

    @TableField("tags")
    private String tags;

    @TableField("integrity_score")
    private Double integrityScore;

    @TableField("status")
    private String status;

    @TableField("capture_time")
    private LocalDateTime captureTime;

    @TableField("group_id")
    private String groupId;

    @TableField("backup_status")
    private String backupStatus;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
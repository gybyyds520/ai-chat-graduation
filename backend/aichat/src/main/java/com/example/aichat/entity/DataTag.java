package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据标签实体
 */
@Data
@TableName("data_tag")
public class DataTag {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("tag_name")
    private String tagName;
    
    @TableField("tag_type")
    private String tagType;
    
    private String description;
    
    private String color;
    
    @TableField("parent_id")
    private Long parentId;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
}

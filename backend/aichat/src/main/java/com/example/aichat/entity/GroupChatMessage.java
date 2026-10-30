package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("group_chat_message")
public class GroupChatMessage {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("group_id")
    private Long groupId;
    
    @TableField("sender_id")
    private Long senderId;
    
    @TableField("sender_name")
    private String senderName;
    
    private String content;
    
    @TableField("message_type")
    private String messageType;
    
    private String fileName;
    
    private String filePath;
    
    private Long fileSize;
    
    @TableField("extra_data")
    private String extraData;
    
    @TableField("is_read")
    private Boolean isRead;
    
    @TableField("create_time")
    private LocalDateTime createTime;
    
    private Integer status;
}
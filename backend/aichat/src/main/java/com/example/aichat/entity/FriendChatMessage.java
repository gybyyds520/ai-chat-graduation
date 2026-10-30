package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friend_chat_message")
public class FriendChatMessage {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long senderId;
    
    @TableField("sender_name")
    private String senderName;
    
    private Long receiverId;
    
    @TableField("receiver_name")
    private String receiverName;
    
    private String content;
    
    @TableField("message_type")
    private String messageType;
    
    private String type;
    
    @TableField("is_read")
    private Boolean isRead;
    
    @TableField("read_at")
    private LocalDateTime readAt;
    
    private String fileName;
    
    private String filePath;
    
    private Long fileSize;
    
    @TableField("extra_data")
    private String extraData;
    
    @TableField("create_time")
    private LocalDateTime createTime;
    
    private Integer status;
}
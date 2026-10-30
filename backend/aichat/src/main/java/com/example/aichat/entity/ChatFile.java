package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_file")
public class ChatFile {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String sessionId;
    
    private Long userId;
    
    private Long characterId;
    
    private String fileName;
    
    private String fileUrl;
    
    private String fileType;
    
    private Long fileSize;
    
    private String extractedContent;
    
    private LocalDateTime createdAt;
}

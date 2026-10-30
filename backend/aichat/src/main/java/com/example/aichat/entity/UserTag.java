package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_tag")
public class UserTag {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String tagName;
    
    private String tagCategory;
    
    private LocalDateTime createdAt;
}
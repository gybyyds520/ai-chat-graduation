package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("interest_community")
public class InterestCommunity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String description;
    
    private String avatar;
    
    private String category;
    
    private Long ownerId;
    
    private Integer memberCount;
    
    private Integer postCount;
    
    private String visibility;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
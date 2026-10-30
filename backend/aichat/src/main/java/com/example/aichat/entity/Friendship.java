package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friendship")
public class Friendship {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private Long friendId;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime acceptTime;
    
    private String username;
    private String nickname;
    private String avatar;
}
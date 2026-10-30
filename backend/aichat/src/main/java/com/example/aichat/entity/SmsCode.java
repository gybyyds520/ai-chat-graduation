package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sms_code")
public class SmsCode {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String phone;
    
    private String code;
    
    private LocalDateTime expireTime;
    
    private Boolean used;
    
    private LocalDateTime createdAt;
}
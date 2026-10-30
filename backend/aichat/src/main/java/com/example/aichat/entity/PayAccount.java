package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pay_account")
public class PayAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String accountType;

    private String accountNumber;

    private String accountName;

    private String status;

    private String verificationCode;

    private LocalDateTime verifiedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
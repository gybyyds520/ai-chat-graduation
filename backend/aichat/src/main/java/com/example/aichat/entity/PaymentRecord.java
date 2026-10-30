package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_record")
public class PaymentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String paymentType;

    private String versionType;

    private BigDecimal amount;

    private Integer durationDays;

    private String paymentMethod;

    private String transactionId;

    private String paymentStatus;

    private String reviewStatus;

    private String reviewRemark;

    private LocalDateTime reviewTime;

    private Long reviewedBy;

    private LocalDateTime paymentTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
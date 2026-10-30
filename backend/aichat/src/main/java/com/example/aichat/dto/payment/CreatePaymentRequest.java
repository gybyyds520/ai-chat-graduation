package com.example.aichat.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建支付订单请求 DTO。
 *
 * Phase 3 (P1-2) 引入：替代 PaymentController 接收 Map + 手写校验，
 * 配合 PaymentErrorCode.ORDER_AMOUNT_INVALID / ORDER_STATUS_INVALID 标准化错误响应。
 */
@Data
public class CreatePaymentRequest {

    @NotNull(message = "支付金额不能为空")
    @Positive(message = "支付金额必须大于 0")
    private BigDecimal amount;

    @NotBlank(message = "支付方式不能为空")
    @Pattern(regexp = "^(ALIPAY|WECHAT|BANK_CARD|BALANCE)$",
             message = "支付方式必须为 ALIPAY/WECHAT/BANK_CARD/BALANCE 之一")
    private String paymentMethod;

    @NotBlank(message = "订单标题不能为空")
    @Size(max = 200, message = "订单标题长度不能超过200个字符")
    private String subject;

    /** 关联业务订单 ID（如订阅订单 ID） */
    @Positive
    private Long orderId;

    /** 客户端回调 URL（支付完成后跳转） */
    @Size(max = 500, message = "回调 URL 长度超限")
    private String returnUrl;
}

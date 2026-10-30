package com.example.aichat.common.exception;

/**
 * 支付域错误码（前缀 4000x）。
 *
 * Phase 3 (P1-1) 引入：覆盖支付订单/审核/退款/账户等场景，
 * 替代 PaymentController 里散落的 R.error("订单不存在") 等手写判断。
 */
public enum PaymentErrorCode implements ErrorCode {

    // 订单相关（40001-40099）
    ORDER_NOT_FOUND(40001, "订单不存在", 404),
    ORDER_ALREADY_PAID(40002, "订单已支付，请勿重复支付"),
    ORDER_EXPIRED(40003, "订单已过期，请重新发起"),
    ORDER_AMOUNT_INVALID(40004, "订单金额不合法"),
    ORDER_STATUS_INVALID(40005, "订单状态不允许此操作"),

    // 审核相关（40100-40199）
    PAYMENT_ALREADY_REVIEWED(40100, "支付已审核，请勿重复操作"),
    PAYMENT_NOT_FOUND(40101, "支付记录不存在", 404),
    PAYMENT_APPROVE_FORBIDDEN(40102, "无权审核该支付", 403),

    // 账户相关（40200-40299）
    PAY_ACCOUNT_NOT_FOUND(40200, "支付账户不存在", 404),
    PAY_ACCOUNT_DUPLICATE(40201, "支付账户已存在"),

    // 退款相关（40300-40399）
    REFUND_AMOUNT_EXCEED(40300, "退款金额超过原支付金额"),
    REFUND_NOT_SUPPORTED(40301, "该支付方式不支持退款", 422),

    // 配额与会员分级（40400-40499）
    QUOTA_EXCEEDED(40400, "今日用量已达上限，请升级会员或明日再试", 403),
    MEMBERSHIP_REQUIRED(40401, "该功能需要会员权限", 403);

    private final int code;
    private final String message;
    private final int httpStatus;

    PaymentErrorCode(int code, String message) {
        this(code, message, 400);
    }

    PaymentErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }
}

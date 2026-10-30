package com.example.aichat.common.exception;

/**
 * 业务异常基类。
 *
 * 重构目的（Phase 1 缺陷修复）：
 *   原实现通过 Service 返回 null / Controller 手写 if/else 判断业务错误，
 *   导致错误码不统一、Controller 层充斥校验代码、GlobalExceptionHandler 无法区分业务错误与系统异常。
 *
 * 改造方案：
 *   1. Service 在遇到业务规则失败时直接抛出 BusinessException(具体错误码)；
 *   2. GlobalExceptionHandler 统一捕获并转换为标准 R 响应；
 *   3. Controller 不再手写校验与错误判断，仅负责调用 Service 与包装返回。
 *
 * 兼容性：保留以 String message 为参数的构造器，让历史代码逐步过渡。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

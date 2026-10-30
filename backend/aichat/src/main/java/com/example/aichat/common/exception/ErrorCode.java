package com.example.aichat.common.exception;

/**
 * 统一错误码接口。
 * 各业务模块（user/social/ai/payment/admin）通过实现此接口定义专属错误码枚举，
 * 实现"模块前缀 + 错误号"的标准化错误码体系，便于前端识别与国际化。
 *
 * 例：UserErrorCode.USER_NOT_FOUND = 10001
 *     SocialErrorCode.POST_NOT_FOUND = 20001
 */
public interface ErrorCode {
    /** 错误码数值 */
    int getCode();

    /** 错误信息（默认中文，可扩展为国际化 key） */
    String getMessage();

    /** 对应 HTTP 状态码（默认 400，鉴权类重写为 401/403） */
    default int getHttpStatus() {
        return 400;
    }
}

package com.example.aichat.common.exception;

/**
 * 用户域错误码（前缀 1000x）。
 *
 * 重构目的：将原 UserController 里散落的字符串错误信息（"用户名不能为空"、"用户名已存在"等）
 * 统一为错误码枚举，便于前端按 code 分支处理、便于日志统计与告警。
 */
public enum UserErrorCode implements ErrorCode {

    // 参数校验类（10001-10099）
    USERNAME_BLANK(10001, "用户名不能为空"),
    USERNAME_LENGTH_INVALID(10002, "用户名长度必须在3-20个字符之间"),
    USERNAME_FORMAT_INVALID(10003, "用户名只能包含字母、数字和下划线"),
    PASSWORD_BLANK(10004, "密码不能为空"),
    PASSWORD_LENGTH_INVALID(10005, "密码长度必须在6-20个字符之间"),
    EMAIL_FORMAT_INVALID(10006, "邮箱格式不正确"),

    // 业务规则类（10100-10199）
    USERNAME_EXISTS(10100, "用户名已存在"),
    LOGIN_FAILED(10101, "用户名或密码错误", 401),
    USER_NOT_FOUND(10102, "用户不存在", 404),
    ACCOUNT_LOCKED(10103, "账户已锁定，请联系管理员", 403),
    OLD_PASSWORD_INCORRECT(10104, "原密码不正确"),
    TOKEN_INVALID(10105, "令牌无效或已过期", 401),
    VERIFICATION_CODE_INVALID(10106, "验证码无效或已过期"),
    ACCOUNT_DISABLED(10107, "账户已被禁用，请联系管理员", 403),
    NICKNAME_TOO_LONG(10108, "昵称长度不能超过50个字符"),
    BIO_TOO_LONG(10109, "简介长度不能超过500个字符"),

    // 未成年保护模式（10200-10299）
    MINOR_AGE_INVALID(10200, "年龄输入无效"),
    MINOR_NIGHT_RESTRICTED(10201, "未成年保护模式：夜间 22:00-06:00 暂停服务，请合理安排作息", 403),
    MINOR_AI_CHAT_RESTRICTED(10202, "未成年保护模式下 AI 聊天功能受限", 403),
    MINOR_MODE_TOGGLE_DENIED(10203, "未成年用户不可手动关闭未成年保护模式", 403),

    // 角色越权（10300-10399）
    CHARACTER_NOT_OWNER(10300, "无权操作该角色，仅创建者可分享到广场", 403),
    CHARACTER_NOT_FOUND(10301, "角色不存在", 404),

    // 账号自助注销（10400-10499）
    ACCOUNT_DELETE_CONFIRM_REQUIRED(10400, "二次确认文本不正确，请输入 DELETE 以确认注销", 400),
    ACCOUNT_ALREADY_DELETED(10401, "该账号已注销，无法重复操作", 400);

    private final int code;
    private final String message;
    private final int httpStatus;

    UserErrorCode(int code, String message) {
        this(code, message, 400);
    }

    UserErrorCode(int code, String message, int httpStatus) {
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

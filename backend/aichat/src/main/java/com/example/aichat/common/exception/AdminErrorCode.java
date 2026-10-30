package com.example.aichat.common.exception;

/**
 * 管理域错误码（前缀 5000x）。
 *
 * Phase 3 (P1-1) 引入：覆盖管理员登录/用户管理/公告/广告/系统配置等场景，
 * 替代 AdminController 里散落的 R.error("管理员密码错误") 等手写判断。
 */
public enum AdminErrorCode implements ErrorCode {

    // 管理员认证相关（50001-50099）
    ADMIN_LOGIN_FAILED(50001, "管理员账号或密码错误", 401),
    ADMIN_TOKEN_INVALID(50002, "管理员令牌无效或已过期", 401),
    ADMIN_PERMISSION_DENIED(50003, "需要管理员权限", 403),

    // 用户管理相关（50100-50199）
    USER_ALREADY_EXISTS(50100, "用户已存在"),
    USER_OPERATION_FORBIDDEN(50101, "不能操作管理员账户", 403),
    USER_STATUS_INVALID(50102, "用户状态不合法"),

    // 公告/广告相关（50200-50299）
    ANNOUNCEMENT_NOT_FOUND(50200, "公告不存在", 404),
    ADVERTISEMENT_NOT_FOUND(50201, "广告不存在", 404),

    // 系统配置相关（50300-50399）
    CONFIG_KEY_EXISTS(50300, "配置项已存在"),
    CONFIG_NOT_FOUND(50301, "配置项不存在", 404),

    // 数据备份/恢复相关（50400-50499）
    BACKUP_FAILED(50400, "数据备份失败", 500),
    RESTORE_FAILED(50401, "数据恢复失败", 500),
    BACKUP_FILE_NOT_FOUND(50402, "备份文件不存在", 404);

    private final int code;
    private final String message;
    private final int httpStatus;

    AdminErrorCode(int code, String message) {
        this(code, message, 400);
    }

    AdminErrorCode(int code, String message, int httpStatus) {
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

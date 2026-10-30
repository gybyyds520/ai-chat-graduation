package com.example.aichat.common;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.ErrorCode;
import com.example.aichat.common.exception.UserErrorCode;
import com.example.aichat.dto.R;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 缺陷修复验证测试。
 *
 * 覆盖点：
 * 1. UserErrorCode 枚举的 code/message/httpStatus 取值正确；
 * 2. BusinessException 携带 ErrorCode 被正确读取；
 * 3. R.fail(ErrorCode) 工厂方法生成标准化失败响应（保留原 R.success/error 兼容性）。
 */
class ExceptionAndRTest {

    @Test
    void userErrorCode_valuesAreCorrect() {
        assertEquals(10001, UserErrorCode.USERNAME_BLANK.getCode());
        assertEquals("用户名不能为空", UserErrorCode.USERNAME_BLANK.getMessage());
        assertEquals(400, UserErrorCode.USERNAME_BLANK.getHttpStatus());

        // 鉴权类错误码应返回 401
        assertEquals(401, UserErrorCode.LOGIN_FAILED.getHttpStatus());
        assertEquals(401, UserErrorCode.TOKEN_INVALID.getHttpStatus());

        // 未找到资源应返回 404
        assertEquals(404, UserErrorCode.USER_NOT_FOUND.getHttpStatus());

        // 账户锁定应返回 403
        assertEquals(403, UserErrorCode.ACCOUNT_LOCKED.getHttpStatus());
    }

    @Test
    void businessException_carriesErrorCode() {
        BusinessException ex = new BusinessException(UserErrorCode.USERNAME_EXISTS);
        // 验证异常携带的 ErrorCode 可被读取（GlobalExceptionHandler 依赖此 getter）
        assertSame(UserErrorCode.USERNAME_EXISTS, ex.getErrorCode());
        assertEquals(10100, ex.getErrorCode().getCode());
        assertEquals("用户名已存在", ex.getErrorCode().getMessage());
        // 异常 message 应与 ErrorCode 一致，便于日志输出
        assertEquals("用户名已存在", ex.getMessage());
    }

    @Test
    void businessException_supportsCustomDetail() {
        BusinessException ex = new BusinessException(UserErrorCode.LOGIN_FAILED, "用户ID=42 IP=1.2.3.4");
        assertSame(UserErrorCode.LOGIN_FAILED, ex.getErrorCode());
        // 当传入自定义 detail 时，异常 message 用 detail（用于日志脱敏信息）
        assertEquals("用户ID=42 IP=1.2.3.4", ex.getMessage());
        // 但 ErrorCode 自身的 message 仍保留，前端展示时取 errorCode.getMessage()
        assertEquals("用户名或密码错误", ex.getErrorCode().getMessage());
    }

    @Test
    void r_failGeneratesStandardErrorResponse() {
        R<Void> r = R.fail(UserErrorCode.USERNAME_EXISTS);
        assertEquals(10100, r.getCode());
        assertEquals("用户名已存在", r.getMsg());
        assertNull(r.getData());
    }

    @Test
    void r_failWithCustomDataPreservesErrorCode() {
        var extra = java.util.Map.of("field", "username", "suggestion", "请改用其他用户名");
        R<java.util.Map<String, String>> r = R.fail(UserErrorCode.USERNAME_EXISTS, extra);
        assertEquals(10100, r.getCode());
        assertEquals("用户名已存在", r.getMsg());
        assertNotNull(r.getData());
        assertEquals("username", r.getData().get("field"));
    }

    @Test
    void r_successAndErrorRemainCompatible() {
        // 兼容性测试：原有工厂方法不应被新方法破坏
        R<String> success = R.success("ok");
        assertEquals(200, success.getCode());
        assertEquals("success", success.getMsg());
        assertEquals("ok", success.getData());

        R<String> error = R.error("系统异常");
        assertEquals(500, error.getCode());

        R<String> badRequest = R.badRequest("参数错误");
        assertEquals(400, badRequest.getCode());

        R<String> unauthorized = R.unauthorized("未登录");
        assertEquals(401, unauthorized.getCode());
    }

    @Test
    void errorCodeInterface_isImplementedByUserErrorCode() {
        // 多态验证：UserErrorCode 可作为 ErrorCode 接口引用传递
        ErrorCode code = UserErrorCode.PASSWORD_LENGTH_INVALID;
        assertEquals(10005, code.getCode());
        assertEquals("密码长度必须在6-20个字符之间", code.getMessage());
        assertEquals(400, code.getHttpStatus());
    }
}

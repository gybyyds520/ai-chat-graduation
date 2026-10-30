package com.example.aichat.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BusinessException 业务异常缺口补测。
 *
 * <p>验证两个构造器：以错误码构造（message 取错误码默认文案）、
 * 以错误码+detail 构造（message 覆盖为 detail），以及 getErrorCode 透传。</p>
 */
class BusinessExceptionGapTest {

    @Test
    void 仅错误码构造_message取错误码默认文案() {
        BusinessException ex = new BusinessException(UserErrorCode.USER_NOT_FOUND);
        assertSame(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        assertEquals(UserErrorCode.USER_NOT_FOUND.getMessage(), ex.getMessage());
    }

    @Test
    void 错误码加detail_message覆盖为detail() {
        BusinessException ex = new BusinessException(UserErrorCode.LOGIN_FAILED, "账号 admin 密码错误");
        assertSame(UserErrorCode.LOGIN_FAILED, ex.getErrorCode());
        assertEquals("账号 admin 密码错误", ex.getMessage());
    }

    @Test
    void 是RuntimeException子类可被抛出捕获() {
        BusinessException ex = assertThrows(BusinessException.class, () -> {
            throw new BusinessException(UserErrorCode.ACCOUNT_LOCKED);
        });
        assertEquals(UserErrorCode.ACCOUNT_LOCKED.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void 错误码HTTP状态_鉴权类重写为401() {
        assertEquals(401, UserErrorCode.LOGIN_FAILED.getHttpStatus());
        assertEquals(401, UserErrorCode.TOKEN_INVALID.getHttpStatus());
    }

    @Test
    void 错误码HTTP状态_默认400() {
        assertEquals(400, UserErrorCode.USERNAME_BLANK.getHttpStatus());
        assertEquals(400, UserErrorCode.USERNAME_EXISTS.getHttpStatus());
    }

    @Test
    void 错误码HTTP状态_403与404() {
        assertEquals(404, UserErrorCode.USER_NOT_FOUND.getHttpStatus());
        assertEquals(403, UserErrorCode.ACCOUNT_LOCKED.getHttpStatus());
        assertEquals(403, UserErrorCode.MINOR_NIGHT_RESTRICTED.getHttpStatus());
    }

    @Test
    void 错误码枚举_互不重复的业务码段() {
        // 抽查关键码值
        assertEquals(10001, UserErrorCode.USERNAME_BLANK.getCode());
        assertEquals(10100, UserErrorCode.USERNAME_EXISTS.getCode());
        assertEquals(10105, UserErrorCode.TOKEN_INVALID.getCode());
    }
}

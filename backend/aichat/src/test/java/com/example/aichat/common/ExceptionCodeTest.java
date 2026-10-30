package com.example.aichat.common;

import com.example.aichat.common.exception.AdminErrorCode;
import com.example.aichat.common.exception.AiErrorCode;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.ErrorCode;
import com.example.aichat.common.exception.PaymentErrorCode;
import com.example.aichat.common.exception.SocialErrorCode;
import com.example.aichat.common.exception.UserErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全栈审查测试 - 覆盖错误码体系与 BusinessException 行为。
 *
 * 覆盖范围：
 *   1. 5 个 ErrorCode 枚举（User/Social/Ai/Payment/Admin）的 getCode/getMessage/getHttpStatus
 *   2. BusinessException 构造与 getErrorCode
 *   3. 错误码命名空间无冲突（前缀按 10xxx/20xxx/30xxx/40xxx/50xxx 分组）
 */
class ExceptionCodeTest {

    @Test
    void userErrorCode_命名空间前缀应为1000x() {
        assertTrue(UserErrorCode.USERNAME_EXISTS.getCode() / 1000 == 10);
        assertEquals(401, UserErrorCode.LOGIN_FAILED.getHttpStatus());
        assertEquals(404, UserErrorCode.USER_NOT_FOUND.getHttpStatus());
        assertEquals("账户已锁定，请联系管理员", UserErrorCode.ACCOUNT_LOCKED.getMessage());
    }

    @Test
    void socialErrorCode_命名空间前缀应为2000x() {
        assertTrue(SocialErrorCode.POST_NOT_FOUND.getCode() / 1000 == 20);
        assertEquals(404, SocialErrorCode.POST_NOT_FOUND.getHttpStatus());
        assertEquals(403, SocialErrorCode.POST_NO_PERMISSION.getHttpStatus());
        assertEquals("评论内容不能为空", SocialErrorCode.COMMENT_CONTENT_BLANK.getMessage());
    }

    @Test
    void aiErrorCode_命名空间前缀应为3000x() {
        assertTrue(AiErrorCode.MODEL_UNAVAILABLE.getCode() / 1000 == 30);
        assertEquals(503, AiErrorCode.MODEL_UNAVAILABLE.getHttpStatus());
        assertEquals(429, AiErrorCode.MODEL_RATE_LIMITED.getHttpStatus());
        assertEquals("图像生成功能未启用", AiErrorCode.IMAGE_GENERATION_DISABLED.getMessage());
    }

    @Test
    void paymentErrorCode_命名空间前缀应为4000x() {
        assertTrue(PaymentErrorCode.ORDER_NOT_FOUND.getCode() / 1000 == 40);
        assertEquals(404, PaymentErrorCode.ORDER_NOT_FOUND.getHttpStatus());
        assertEquals("订单金额不合法", PaymentErrorCode.ORDER_AMOUNT_INVALID.getMessage());
    }

    @Test
    void adminErrorCode_命名空间前缀应为5000x() {
        assertTrue(AdminErrorCode.ADMIN_LOGIN_FAILED.getCode() / 1000 == 50);
        assertEquals(401, AdminErrorCode.ADMIN_LOGIN_FAILED.getHttpStatus());
        assertEquals(403, AdminErrorCode.ADMIN_PERMISSION_DENIED.getHttpStatus());
    }

    @Test
    void businessException_携带错误码透传() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> { throw new BusinessException(UserErrorCode.USERNAME_EXISTS); });
        assertEquals(UserErrorCode.USERNAME_EXISTS, ex.getErrorCode());
        assertEquals(UserErrorCode.USERNAME_EXISTS.getMessage(), ex.getMessage());
    }

    @Test
    void businessException_自定义详情保留错误码() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> { throw new BusinessException(SocialErrorCode.POST_CONTENT_TOO_LONG, "实际长度 3000"); });
        assertEquals(SocialErrorCode.POST_CONTENT_TOO_LONG, ex.getErrorCode());
        assertEquals("实际长度 3000", ex.getMessage());
    }

    @Test
    void errorCode_所有枚举httpStatus在合法范围() {
        // 验证所有错误码 httpStatus 都是合法 HTTP 状态码（400-504）
        checkHttpStatus(UserErrorCode.values());
        checkHttpStatus(SocialErrorCode.values());
        checkHttpStatus(AiErrorCode.values());
        checkHttpStatus(PaymentErrorCode.values());
        checkHttpStatus(AdminErrorCode.values());
    }

    private void checkHttpStatus(ErrorCode[] codes) {
        for (ErrorCode code : codes) {
            int status = code.getHttpStatus();
            assertTrue(status >= 400 && status <= 504,
                    "错误码 " + code + " 的 httpStatus " + status + " 不在合法范围 400-504");
        }
    }
}

package com.example.aichat.config;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.PaymentErrorCode;
import com.example.aichat.common.exception.UserErrorCode;
import com.example.aichat.dto.R;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler 单元测试。
 *
 * 覆盖点：
 *   1. BusinessException - 业务异常返回正确错误码
 *   2. AccessDeniedException - 权限不足返回403
 *   3. AuthenticationException - 认证失败返回401
 *   4. NullPointerException - 空指针返回500且不泄露内部信息
 *   5. RuntimeException - 运行时异常返回500且不泄露内部信息
 *   6. Exception - 未知异常返回500且不泄露内部信息
 *   7. IllegalArgumentException - 参数异常返回400
 *   8. MethodArgumentNotValidException - 参数校验异常返回400
 *   9. DataAccessException - 数据访问异常返回500
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusinessException_应返回对应错误码() {
        BusinessException ex = new BusinessException(PaymentErrorCode.ORDER_NOT_FOUND, "订单不存在");

        ResponseEntity<R<Void>> response = handler.handleBusinessException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(PaymentErrorCode.ORDER_NOT_FOUND.getCode(), response.getBody().getCode());
        assertEquals(PaymentErrorCode.ORDER_NOT_FOUND.getMessage(), response.getBody().getMsg());
    }

    @Test
    void handleAccessDenied_应返回403且不泄露详情() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ResponseEntity<R<Void>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getCode());
        // 不应回显 e.getMessage()
        assertFalse(response.getBody().getMsg().contains("Access denied"));
    }

    @Test
    void handleAuthentication_应返回401且使用统一错误码() {
        AuthenticationException ex = new AuthenticationException("Bad credentials") {};

        ResponseEntity<R<Void>> response = handler.handleAuthentication(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(UserErrorCode.TOKEN_INVALID.getCode(), response.getBody().getCode());
    }

    @Test
    void handleNullPointerException_应返回500且不泄露内部信息() {
        NullPointerException ex = new NullPointerException("Cannot invoke method on null object at line 42");

        ResponseEntity<R<Map<String, Object>>> response = handler.handleNullPointerException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        // 不应回显 e.getMessage() 中的技术细节
        String msg = response.getBody().getMsg();
        assertFalse(msg.contains("Cannot invoke"), "不应回显NPE详情: " + msg);
        assertTrue(msg.contains("系统内部错误") || msg.contains("请稍后重试"));
    }

    @Test
    void handleRuntimeException_应返回500且不泄露内部信息() {
        RuntimeException ex = new RuntimeException("SQL error: Table 'db.users' doesn't exist");

        ResponseEntity<R<Map<String, Object>>> response = handler.handleRuntimeException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        String msg = response.getBody().getMsg();
        assertFalse(msg.contains("SQL"), "不应泄露SQL信息: " + msg);
        assertFalse(msg.contains("doesn't exist"), "不应泄露内部表名: " + msg);
    }

    @Test
    void handleException_应返回500且不泄露内部信息() {
        Exception ex = new Exception("Root cause: connection refused to 10.0.0.1:3306");

        ResponseEntity<R<Map<String, Object>>> response = handler.handleException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        String msg = response.getBody().getMsg();
        assertFalse(msg.contains("10.0.0.1"), "不应泄露内部IP: " + msg);
        assertFalse(msg.contains("3306"), "不应泄露端口: " + msg);
    }

    @Test
    void handleIllegalArgumentException_应返回400() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

        ResponseEntity<R<Map<String, Object>>> response = handler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCode());
    }

    @Test
    void handleValidationException_应返回400并包含字段错误() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "amount", "金额必须大于0"));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<R<Map<String, Object>>> response = handler.handleValidationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMsg().contains("amount"));
    }

    @Test
    void handleDataAccessException_应返回500() {
        org.springframework.dao.DataAccessException ex =
                new org.springframework.dao.QueryTimeoutException("Query timed out");

        ResponseEntity<R<Map<String, Object>>> response = handler.handleDataAccessException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        String msg = response.getBody().getMsg();
        assertFalse(msg.contains("Query timed out"), "不应回显内部异常详情");
        assertTrue(msg.contains("数据访问异常") || msg.contains("联系管理员"));
    }
}

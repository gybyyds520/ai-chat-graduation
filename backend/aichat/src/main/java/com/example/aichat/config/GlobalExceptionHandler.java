package com.example.aichat.config;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.UserErrorCode;
import com.example.aichat.dto.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理。
 *
 * 安全修复（P1）：所有响应一律不回显内部异常消息（e.getMessage()、
 * SQL 片段、堆栈位置等），仅返回固定的用户可读文案；细节只写服务端日志。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：Service 主动抛出的业务规则失败（如"用户名已存在"）。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleBusinessException(BusinessException e) {
        var errorCode = e.getErrorCode();
        log.warn("[BIZ_ERROR] code={}, message={}", errorCode.getCode(), errorCode.getMessage());
        HttpStatus status = HttpStatus.resolve(errorCode.getHttpStatus());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(R.fail(errorCode));
    }

    /**
     * 权限不足异常：@PreAuthorize 拒绝时抛出，统一转 403。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("[ACCESS_DENIED] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(R.error(403, "权限不足，需要管理员权限"));
    }

    /**
     * 认证异常：JwtAuthenticationFilter 解析失败或 Spring Security 鉴权失败时抛出。
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<R<Void>> handleAuthentication(AuthenticationException e) {
        log.warn("[AUTH_FAILED] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(R.fail(UserErrorCode.TOKEN_INVALID));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<R<Map<String, Object>>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("数据完整性异常", e);

        String message = e.getMessage();
        String userMessage = "数据验证失败，请检查输入内容";

        if (message != null) {
            if (message.contains("Data too long")) {
                userMessage = "数据过长，请检查输入内容";
            } else if (message.contains("Duplicate entry")) {
                userMessage = "数据重复，请检查输入内容";
            } else if (message.contains("foreign key constraint")) {
                userMessage = "关联数据不存在，请检查输入";
            } else if (message.contains("cannot be null")) {
                userMessage = "必填字段不能为空";
            }
        }

        return buildErrorResponse(HttpStatus.BAD_REQUEST, userMessage);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<R<Map<String, Object>>> handleDataAccessException(DataAccessException e) {
        log.error("数据访问异常", e);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "数据访问异常，请联系管理员");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Map<String, Object>>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("参数验证异常", e);

        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<Map<String, Object>>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型异常: {}", e.getMessage());

        String message = String.format("参数 '%s' 的类型不正确，期望类型: %s",
                e.getName(),
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown");

        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<R<Map<String, Object>>> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "缺少必填参数: " + e.getParameterName());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Map<String, Object>>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {} {}", e.getMethod(), e.getMessage());
        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, "请求方法不支持");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Map<String, Object>>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败", e);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "请求体格式错误");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<R<Map<String, Object>>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数异常", e);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "请求参数错误");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<R<Map<String, Object>>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("请求路径不存在: {} {}", e.getHttpMethod(), e.getRequestURL());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "请求路径不存在");
    }

    /**
     * Spring Boot 3.2+：访问不存在的静态资源/未映射路径时抛 NoResourceFoundException
     * （此时 NoHandlerFoundException 默认不会触发），统一转 404 而非 500。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<Map<String, Object>>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("请求资源不存在: {}", e.getResourcePath());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "请求路径不存在");
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<R<Map<String, Object>>> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常", e);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误，请稍后重试");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<R<Map<String, Object>>> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常", e);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误，请稍后重试");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Map<String, Object>>> handleException(Exception e) {
        log.error("全局异常处理", e);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误，请稍后重试");
    }

    private ResponseEntity<R<Map<String, Object>>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("message", message);
        errorData.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(status)
                .body(R.error(status.value(), message, errorData));
    }
}

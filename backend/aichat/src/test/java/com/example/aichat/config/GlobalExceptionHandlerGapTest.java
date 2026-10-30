package com.example.aichat.config;

import com.example.aichat.dto.R;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GlobalExceptionHandler 缺口补测。
 *
 * <p>原有 GlobalExceptionHandlerTest 已覆盖 Business/AccessDenied/Authentication/NPE/
 * Runtime/Exception/IllegalArgument/MethodArgumentNotValid/DataAccess。
 * 本类补齐未覆盖的异常类型与分支：
 * DataIntegrityViolation 的 5 种 SQL 文案分支、类型不匹配、缺参、方法不支持、
 * 请求体不可读、404（NoHandler / NoResource）。</p>
 */
class GlobalExceptionHandlerGapTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ===== DataIntegrityViolationException 文案分支 =====

    @Test
    void 数据完整性_DataTooLong_返回数据过长提示() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("### Error updating. Cause: com.mysql.Data too long for column");
        ResponseEntity<R<Map<String, Object>>> r = handler.handleDataIntegrityViolationException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertTrue(r.getBody().getMsg().contains("数据过长"));
    }

    @Test
    void 数据完整性_DuplicateEntry_返回数据重复提示() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("Duplicate entry 'alice' for key 'uk_username'");
        ResponseEntity<R<Map<String, Object>>> r = handler.handleDataIntegrityViolationException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertTrue(r.getBody().getMsg().contains("数据重复"));
    }

    @Test
    void 数据完整性_ForeignKey_返回关联数据不存在提示() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("Cannot delete or update a parent row: a foreign key constraint fails");
        ResponseEntity<R<Map<String, Object>>> r = handler.handleDataIntegrityViolationException(ex);
        assertTrue(r.getBody().getMsg().contains("关联数据不存在"));
    }

    @Test
    void 数据完整性_CannotBeNull_返回必填字段提示() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("Column 'nickname' cannot be null");
        ResponseEntity<R<Map<String, Object>>> r = handler.handleDataIntegrityViolationException(ex);
        assertTrue(r.getBody().getMsg().contains("必填字段不能为空"));
    }

    @Test
    void 数据完整性_未知原因_返回通用校验失败提示() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("some obscure db error");
        ResponseEntity<R<Map<String, Object>>> r = handler.handleDataIntegrityViolationException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertTrue(r.getBody().getMsg().contains("数据验证失败"));
    }

    @Test
    void 数据完整性_message为null_不抛NPE() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException((String) null);
        assertDoesNotThrow(() -> handler.handleDataIntegrityViolationException(ex));
    }

    // ===== 其他未覆盖异常类型 =====

    @Test
    void 参数类型不匹配_返回400并带参数名() {
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("abc", Long.class, "userId", null,
                        new NumberFormatException("for input string: \"abc\""));
        ResponseEntity<R<Map<String, Object>>> r = handler.handleTypeMismatchException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertTrue(r.getBody().getMsg().contains("userId"));
        assertTrue(r.getBody().getMsg().contains("Long"));
    }

    @Test
    void 缺少必填请求参数_返回400并带参数名() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("keyword", "String");
        ResponseEntity<R<Map<String, Object>>> r = handler.handleMissingParameter(ex);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertTrue(r.getBody().getMsg().contains("keyword"));
    }

    @Test
    void 请求方法不支持_返回405() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");
        ResponseEntity<R<Map<String, Object>>> r = handler.handleMethodNotSupported(ex);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, r.getStatusCode());
    }

    @Test
    void 请求体不可读_返回400提示格式错误() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Malformed JSON request");
        ResponseEntity<R<Map<String, Object>>> r = handler.handleMessageNotReadable(ex);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertTrue(r.getBody().getMsg().contains("请求体格式错误"));
    }

    @Test
    void NoHandlerFound_返回404() {
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/no-such-path", null);
        ResponseEntity<R<Map<String, Object>>> r = handler.handleNoHandlerFoundException(ex);
        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
        assertTrue(r.getBody().getMsg().contains("请求路径不存在"));
    }

    @Test
    void NoResourceFound_返回404而非500() {
        NoResourceFoundException ex = new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "/static/missing.js");
        ResponseEntity<R<Map<String, Object>>> r = handler.handleNoResourceFoundException(ex);
        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
        assertTrue(r.getBody().getMsg().contains("请求路径不存在"));
    }
}

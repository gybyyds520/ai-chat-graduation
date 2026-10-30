package com.example.aichat.dto;

import com.example.aichat.common.exception.UserErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R 统一响应包装类缺口补测。
 *
 * <p>覆盖所有静态工厂方法 success/error/unauthorized/badRequest/fail 的 code/msg/data 字段。</p>
 */
class RGapTest {

    @Test
    void success_带数据_字段正确() {
        R<Map<String, Object>> r = R.success(Map.of("a", 1));
        assertEquals(200, r.getCode());
        assertEquals("success", r.getMsg());
        assertEquals(1, r.getData().get("a"));
    }

    @Test
    void success_无数据_data为null() {
        R<Object> r = R.success();
        assertEquals(200, r.getCode());
        assertEquals("success", r.getMsg());
        assertNull(r.getData());
    }

    @Test
    void error_仅msg_默认500() {
        R<Object> r = R.error("boom");
        assertEquals(500, r.getCode());
        assertEquals("boom", r.getMsg());
    }

    @Test
    void error_指定code与msg() {
        R<Object> r = R.error(418, "I'm a teapot");
        assertEquals(418, r.getCode());
        assertEquals("I'm a teapot", r.getMsg());
    }

    @Test
    void error_msg加数据_code500() {
        R<Map<String, Object>> r = R.error("err", Map.of("x", 2));
        assertEquals(500, r.getCode());
        assertEquals(2, r.getData().get("x"));
    }

    @Test
    void error_code加msg加数据() {
        R<Map<String, Object>> r = R.error(422, "无法处理", Map.of("field", "email"));
        assertEquals(422, r.getCode());
        assertEquals("无法处理", r.getMsg());
        assertEquals("email", r.getData().get("field"));
    }

    @Test
    void unauthorized_code401() {
        R<Object> r = R.unauthorized("未登录");
        assertEquals(401, r.getCode());
        assertEquals("未登录", r.getMsg());
    }

    @Test
    void badRequest_code400() {
        R<Object> r = R.badRequest("参数错误");
        assertEquals(400, r.getCode());
        assertEquals("参数错误", r.getMsg());
    }

    @Test
    void fail_基于错误码_code与msg取自枚举() {
        R<Void> r = R.fail(UserErrorCode.USER_NOT_FOUND);
        assertEquals(UserErrorCode.USER_NOT_FOUND.getCode(), r.getCode());
        assertEquals(UserErrorCode.USER_NOT_FOUND.getMessage(), r.getMsg());
        assertNull(r.getData());
    }

    @Test
    void fail_错误码加数据() {
        R<Map<String, Object>> r = R.fail(UserErrorCode.TOKEN_INVALID, Map.of("reason", "expired"));
        assertEquals(UserErrorCode.TOKEN_INVALID.getCode(), r.getCode());
        assertEquals("expired", r.getData().get("reason"));
    }
}

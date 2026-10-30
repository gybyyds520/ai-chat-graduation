package com.example.aichat.dto;

import com.example.aichat.common.exception.ErrorCode;
import lombok.Data;

@Data
public class R<T> {
    private int code;
    private String msg;
    private T data;

    private R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> R<T> success(T data) {
        return new R<>(200, "success", data);
    }

    public static <T> R<T> success() {
        return new R<>(200, "success", null);
    }

    public static <T> R<T> error(String msg) {
        return new R<>(500, msg, null);
    }

    public static <T> R<T> error(int code, String msg) {
        return new R<>(code, msg, null);
    }

    public static <T> R<T> error(String msg, T data) {
        return new R<>(500, msg, data);
    }

    public static <T> R<T> error(int code, String msg, T data) {
        return new R<>(code, msg, data);
    }

    public static <T> R<T> unauthorized(String msg) {
        return new R<>(401, msg, null);
    }

    public static <T> R<T> badRequest(String msg) {
        return new R<>(400, msg, null);
    }

    /**
     * 基于统一错误码构造失败响应。
     * 重构目的：让 Controller/Service 抛出 BusinessException 后，
     * GlobalExceptionHandler 可直接通过此方法生成标准化 R 响应。
     */
    public static <T> R<T> fail(ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> R<T> fail(ErrorCode errorCode, T data) {
        return new R<>(errorCode.getCode(), errorCode.getMessage(), data);
    }
}

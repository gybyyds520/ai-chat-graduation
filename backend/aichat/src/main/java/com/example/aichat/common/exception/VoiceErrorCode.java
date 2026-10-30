package com.example.aichat.common.exception;

/**
 * 实时语音通话错误码（前缀 3050x）。
 *
 * 覆盖会话生命周期：会话不存在 / 越权访问 / 空音频 / 音频超限。
 */
public enum VoiceErrorCode implements ErrorCode {

    VOICE_SESSION_NOT_FOUND(30500, "语音会话不存在或已结束", 404),
    VOICE_SESSION_UNAUTHORIZED(30501, "无权操作该语音会话", 403),
    VOICE_AUDIO_EMPTY(30502, "音频数据为空"),
    VOICE_AUDIO_TOO_LARGE(30503, "音频文件不能超过10MB");

    private final int code;
    private final String message;
    private final int httpStatus;

    VoiceErrorCode(int code, String message) {
        this(code, message, 400);
    }

    VoiceErrorCode(int code, String message, int httpStatus) {
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

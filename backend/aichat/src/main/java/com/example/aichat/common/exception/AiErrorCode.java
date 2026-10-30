package com.example.aichat.common.exception;

/**
 * AI 域错误码（前缀 3000x）。
 *
 * Phase 3 (P1-1) 引入：覆盖聊天/角色/记忆/模型路由/图像生成/语音等 AI 场景，
 * 替代 ChatController 与 ModelRouter 里散落的 R.error("模型不可用") 等手写判断。
 */
public enum AiErrorCode implements ErrorCode {

    // 聊天相关（30001-30099）
    CHAT_MESSAGE_BLANK(30001, "聊天消息内容不能为空"),
    CHAT_HISTORY_NOT_FOUND(30002, "聊天历史不存在", 404),
    CHARACTER_NOT_FOUND(30003, "AI 角色不存在", 404),
    CONVERSATION_NOT_FOUND(30004, "对话不存在", 404),
    // AI 精灵归属校验（P0 越权修复）
    ELF_NOT_FOUND(30005, "精灵不存在", 404),
    ELF_FORBIDDEN(30006, "无权操作该精灵", 403),

    // AI 记忆归属校验（IDOR 防护）
    MEMORY_NOT_FOUND(30007, "记忆不存在", 404),
    MEMORY_FORBIDDEN(30008, "无权操作该记忆", 403),
    MEMORY_CONTENT_BLANK(30009, "记忆内容不能为空", 400),

    // 模型路由相关（30100-30199）
    MODEL_NOT_FOUND(30100, "指定的模型不存在"),
    MODEL_UNAVAILABLE(30101, "模型当前不可用，请稍后重试", 503),
    MODEL_TIMEOUT(30102, "模型响应超时", 504),
    MODEL_RATE_LIMITED(30103, "模型调用频率过高，请稍后重试", 429),
    API_KEY_NOT_CONFIGURED(30104, "云端模型 API Key 未配置"),

    // 图像生成相关（30200-30299）
    IMAGE_PROMPT_BLANK(30200, "图像生成提示词不能为空"),
    IMAGE_GENERATION_FAILED(30201, "图像生成失败", 500),
    IMAGE_GENERATION_DISABLED(30202, "图像生成功能未启用", 503),

    // 语音相关（30300-30399）
    AUDIO_FILE_EMPTY(30300, "音频文件为空"),
    STT_FAILED(30301, "语音识别失败", 500),
    TTS_FAILED(30302, "语音合成失败", 500),
    VOICE_SERVICE_DISABLED(30303, "语音服务未启用", 503),

    // 网络搜索相关（30400-30499）
    SEARCH_QUERY_BLANK(30400, "搜索关键词不能为空"),
    SEARCH_FAILED(30401, "搜索服务暂时不可用", 503);

    private final int code;
    private final String message;
    private final int httpStatus;

    AiErrorCode(int code, String message) {
        this(code, message, 400);
    }

    AiErrorCode(int code, String message, int httpStatus) {
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

package com.example.aichat.common.exception;

/**
 * 知识库域错误码（前缀 3050x）。
 *
 * 覆盖文档上传/解析/检索/问答场景，替代散落的 R.error("文档不存在") 等手写判断，
 * 并与现有 user(1000x)/ai(3000x) 错误码体系保持一致。
 */
public enum KnowledgeErrorCode implements ErrorCode {

    // 参数与文件类（30500-30509）
    KB_FILE_EMPTY(30500, "上传文档不能为空"),
    KB_FILE_TYPE_UNSUPPORTED(30501, "当前版本仅支持 txt 文本文档，PDF/Word 解析需安装 PDFBox/POI 依赖"),
    KB_QUERY_BLANK(30504, "问题内容不能为空"),

    // 文档归属类（30510-30519）
    KB_DOC_NOT_FOUND(30502, "文档不存在", 404),
    KB_DOC_FORBIDDEN(30503, "无权操作该文档", 403),

    // 解析类（30520-30529）
    KB_PARSE_FAILED(30505, "文档解析失败", 500);

    private final int code;
    private final String message;
    private final int httpStatus;

    KnowledgeErrorCode(int code, String message) {
        this(code, message, 400);
    }

    KnowledgeErrorCode(int code, String message, int httpStatus) {
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

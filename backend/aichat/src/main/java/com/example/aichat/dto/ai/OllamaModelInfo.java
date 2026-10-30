package com.example.aichat.dto.ai;

import lombok.Data;

/**
 * Ollama 模型信息（GET /api/tags 返回的单个模型项）。
 */
@Data
public class OllamaModelInfo {

    /** 模型名称，如 llama3.2:3b */
    private String name;

    /** 模型大小（字节） */
    private Long size;

    /** 最后修改时间（Ollama 返回的原始时间字符串） */
    private String modifiedAt;

    /** 模型摘要 */
    private String digest;
}

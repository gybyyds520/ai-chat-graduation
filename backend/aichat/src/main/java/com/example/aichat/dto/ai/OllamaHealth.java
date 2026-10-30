package com.example.aichat.dto.ai;

import lombok.Data;

import java.util.List;

/**
 * Ollama 服务健康信息。
 * available=false 时 models 为空、version 为 null。
 */
@Data
public class OllamaHealth {

    /** Ollama 服务是否可用 */
    private boolean available;

    /** Ollama 版本号（来自 GET /api/version），不可用时为 null */
    private String version;

    /** 已安装模型数量 */
    private int modelCount;

    /** 已安装模型列表 */
    private List<OllamaModelInfo> models;
}

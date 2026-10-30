package com.example.aichat.dto.ai;

import lombok.Data;

/**
 * AI 全局运行配置（管理端读写）。
 * 全部配置项以键值对持久化在 ai_config 表，此 DTO 为面向管理端的视图。
 */
@Data
public class AiConfigDTO {

    /** 默认模型名，如 llama3.2:3b */
    private String defaultModel;

    /** 采样温度 */
    private Double temperature;

    /** 单次请求最大 token 数 */
    private Integer maxTokens;

    /** 请求超时（秒） */
    private Integer requestTimeoutSec;

    /** AI 对话总开关 */
    private Boolean aiChatEnabled;

    /** 维护公告文案（为空表示不展示） */
    private String maintenanceNotice;
}

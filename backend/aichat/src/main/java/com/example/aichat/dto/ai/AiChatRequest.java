package com.example.aichat.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 聊天请求 DTO。
 *
 * Phase 3 (P1-2) 引入：替代 ChatController 接收 Map<String, String> + 手写校验，
 * 配合 AiErrorCode.CHAT_MESSAGE_BLANK / MODEL_NOT_FOUND 标准化错误响应。
 *
 * 字段说明：
 *   message：用户输入的对话内容（必填，最多 8000 字符防止单次请求消耗过多 token）
 *   model：模型标识，支持 provider:modelId 格式（如 ollama:llama3, openai:gpt-4o）
 *   conversationId：会话 ID（可选，用于关联历史对话上下文）
 *   characterId：AI 角色 ID（可选，决定 system prompt 与人格设定）
 */
@Data
public class AiChatRequest {

    @NotBlank(message = "聊天消息内容不能为空")
    @Size(max = 8000, message = "单次消息内容不能超过8000个字符")
    private String message;

    /** 模型标识：provider:modelId 或裸 modelId（兼容历史前端） */
    @Size(max = 100, message = "模型标识长度超限")
    private String model;

    /** 会话 ID（可选） */
    @Positive
    private Long conversationId;

    /** AI 角色 ID（可选，0 或 null 表示默认助手） */
    @Positive
    private Long characterId;
}

package com.example.aichat.service.voice.adapter;

import java.util.List;

/**
 * 大语言模型（LLM）适配器接口。
 *
 * 设计目的：把"文本 -> 回复文本"的具体实现抽成可插拔接口，
 * 支持 Mock 固定回复与 Ollama / 云端模型两种生产形态。
 */
public interface LlmAdapter {

    /**
     * 根据用户输入生成回复。
     *
     * @param userText    用户文本
     * @param systemPrompt 系统提示词（可为 null）
     * @param history      历史对话（最近若干轮，可为空）
     * @return 回复文本
     */
    String generateReply(String userText, String systemPrompt, List<String> history);

    /** 当前适配器是否可用。 */
    boolean isAvailable();

    /** 适配器名称。 */
    String getName();
}

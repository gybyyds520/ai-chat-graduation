package com.example.aichat.service.voice.adapter;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mock 大模型适配器。
 *
 * 默认返回固定回复并把用户原文拼接到回复中，便于在测试中断言 ASR -> LLM 链路贯通。
 * 开启 echo 模式后原样返回用户文本，用于验证对话历史透传与回声场景。
 */
@Component
public class MockLlmAdapter implements LlmAdapter {

    private volatile boolean echo = false;

    /** 切换 echo 模式（默认关闭）。 */
    public void setEcho(boolean echo) {
        this.echo = echo;
    }

    public boolean isEcho() {
        return echo;
    }

    @Override
    public String generateReply(String userText, String systemPrompt, List<String> history) {
        String text = userText == null ? "" : userText;
        if (echo) {
            return text;
        }
        return "【模拟回复】你好，我是AI助手。你刚才说的是：" + text;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getName() {
        return "mock-llm";
    }
}

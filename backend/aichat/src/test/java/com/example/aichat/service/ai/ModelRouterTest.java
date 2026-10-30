package com.example.aichat.service.ai;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI 模块 - 模型路由器单元测试。
 *
 * 覆盖点：
 *   1. null / 空模型名 -> 默认 ollama；
 *   2. ollama: 前缀 -> 路由到 ollama 提供者；
 *   3. openai: 前缀 / gpt- / deepseek 无前缀 -> 路由到 openai 提供者；
 *   4. 未知无前缀模型 -> 向后兼容回退 ollama。
 */
class ModelRouterTest {

    /** 记录路由目的地的伪 Provider */
    private static class FakeProvider implements ModelProvider {
        private final String name;
        String routedModelId;
        FakeProvider(String name) { this.name = name; }
        @Override public String getProviderName() { return name; }
        @Override public Flux<String> chatStream(String modelId, String prompt, String systemPrompt) {
            this.routedModelId = modelId;
            return Flux.just("[" + name + ":" + modelId + "]");
        }
        @Override public boolean isAvailable() { return true; }
    }

    private final FakeProvider ollama = new FakeProvider("ollama");
    private final FakeProvider openai = new FakeProvider("openai");
    private final ModelRouter router = new ModelRouter(List.of(ollama, openai));

    @Test
    void null或空模型_应走默认ollama() {
        String out = router.chatStream(null, "hi", null).blockFirst();
        assertEquals("ollama", ollama.getProviderName());
        assertTrue(out != null && out.startsWith("[ollama:"), "应路由到 ollama，实际=" + out);
    }

    @Test
    void ollama前缀_路由到ollama且剥离前缀() {
        router.chatStream("ollama:llama3.2:3b", "hi", null).blockFirst();
        assertEquals("llama3.2:3b", ollama.routedModelId);
    }

    @Test
    void openai前缀_路由到openai() {
        router.chatStream("openai:gpt-4o", "hi", null).blockFirst();
        assertEquals("gpt-4o", openai.routedModelId);
    }

    @Test
    void 无前缀但gpt前缀_路由到openai() {
        router.chatStream("gpt-4o-mini", "hi", null).blockFirst();
        assertEquals("gpt-4o-mini", openai.routedModelId);
    }

    @Test
    void 无前缀但deepseek前缀_路由到openai兼容() {
        router.chatStream("deepseek-chat", "hi", null).blockFirst();
        assertEquals("deepseek-chat", openai.routedModelId);
    }

    @Test
    void 未知无前缀模型_向后兼容回退ollama() {
        router.chatStream("some-local-model", "hi", null).blockFirst();
        assertEquals("some-local-model", ollama.routedModelId);
    }
}

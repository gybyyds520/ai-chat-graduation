package com.example.aichat.service.impl;

import com.example.aichat.service.VisionService;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VisionService 单元测试（M4 ⑫）。
 *
 * <p>默认 {@code aichat.vision.enabled=false}，Spring 未注入时 boolean 字段保持默认 false，
 * 因此无需启动 Spring 容器即可验证"离线基线"行为：接口可达但返回未启用提示，不发起 Ollama 调用。</p>
 */
class VisionServiceTest {

    private VisionService newService() {
        // WebClient.Builder 在测试环境无真实连接；disabled 路径不会真正发起请求
        return new VisionServiceImpl(WebClient.builder());
    }

    @Test
    void 未启用时isEnabled返回false() {
        assertFalse(newService().isEnabled());
    }

    @Test
    void 未启用时分析返回未启用提示且不抛异常() {
        VisionService.VisionAnalysis r = newService().analyze(
                new byte[]{1, 2, 3}, "test.png", null);
        assertFalse(r.enabled());
        assertFalse(r.success());
        assertTrue(r.description().isEmpty());
        assertTrue(r.message().contains("未启用"));
    }

    @Test
    void 未启用时空输入也不抛异常() {
        VisionService.VisionAnalysis r = newService().analyze(null, null, null);
        assertFalse(r.enabled());
        assertFalse(r.success());
    }
}

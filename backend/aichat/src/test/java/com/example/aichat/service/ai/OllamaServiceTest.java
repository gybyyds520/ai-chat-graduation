package com.example.aichat.service.ai;

import com.example.aichat.dto.ai.OllamaHealth;
import com.example.aichat.dto.ai.OllamaModelInfo;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * OllamaService 模型管理方法单元测试（mock WebClient）。
 * 覆盖：列出模型（不可用/可用）、删除模型、健康检查（不可用）。
 */
class OllamaServiceTest {

    @SuppressWarnings("unchecked")
    private WebClient mockClientReturning(Mono<?> mono) {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(String.class))).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(Class.class))).thenReturn((Mono) mono);
        return webClient;
    }

    @Test
    void listModels_ollama不可用_应返回空列表不抛异常() {
        WebClient webClient = mockClientReturning(Mono.error(new RuntimeException("connect refused")));
        OllamaService service = new OllamaService(webClient);

        assertDoesNotThrow(() -> {
            List<OllamaModelInfo> models = service.listModels();
            assertNotNull(models);
            assertTrue(models.isEmpty());
        });
    }

    @Test
    void listModels_ollama可用_应返回模型列表() {
        Map<String, Object> tagResp = Map.of("models", List.of(
                Map.of("name", "llama3.2:3b", "size", 12345L,
                        "modified_at", "2024-01-01T00:00:00Z", "digest", "abc123")
        ));
        WebClient webClient = mockClientReturning(Mono.just(tagResp));
        OllamaService service = new OllamaService(webClient);

        List<OllamaModelInfo> models = service.listModels();

        assertEquals(1, models.size());
        assertEquals("llama3.2:3b", models.get(0).getName());
        assertEquals(12345L, models.get(0).getSize());
        assertEquals("abc123", models.get(0).getDigest());
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteModel_成功应返回success() {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec bodySpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.method(any(org.springframework.http.HttpMethod.class))).thenReturn(bodySpec);
        when(bodySpec.uri(any(String.class))).thenReturn(bodySpec);
        when(bodySpec.body(any(BodyInserter.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.just(ResponseEntity.ok().build()));

        OllamaService service = new OllamaService(webClient);
        OllamaService.OllamaResult result = service.deleteModel("llama3.2:3b");

        assertTrue(result.success());
    }

    @Test
    void getOllamaHealth_ollama不可用_应available为false() {
        WebClient webClient = mockClientReturning(Mono.error(new RuntimeException("connect refused")));
        OllamaService service = new OllamaService(webClient);

        OllamaHealth health = service.getOllamaHealth();

        assertFalse(health.isAvailable());
        assertEquals(0, health.getModelCount());
        assertNotNull(health.getModels());
        assertTrue(health.getModels().isEmpty());
    }
}

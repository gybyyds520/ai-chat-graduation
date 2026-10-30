package com.example.aichat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 记忆向量召回 sidecar 客户端（市场级升级 3：记忆检索升级）。
 *
 * <p>复用 C4 已部署的 Chroma + Ollama nomic-embed-text sidecar（scripts/vector_service.py），
 * 使用独立集合 {@code ai_memory}，按 user_id 过滤。
 *
 * <p>仅当 {@code aichat.memory.vector.enabled=true} 时装配（默认 false，保离线基线）；
 * sidecar 不可达时所有方法静默返回空，由调用方降级到本地关键词召回，
 * 不影响 898 基线用例（离线测试环境该 Bean 不存在）。
 */
@Component
@ConditionalOnProperty(name = "aichat.memory.vector.enabled", havingValue = "true")
public class MemoryVectorClient {

    private static final Logger log = LoggerFactory.getLogger(MemoryVectorClient.class);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String endpoint;

    public MemoryVectorClient(@Value("${aichat.vector.endpoint:http://127.0.0.1:11435}") String endpoint) {
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        log.info("MemoryVectorClient 已启用, endpoint={}", this.endpoint);
    }

    /** 向量召回命中项。 */
    public static class MemoryHit {
        public final String memoryId;
        public final double score;
        public final String content;

        public MemoryHit(String memoryId, double score, String content) {
            this.memoryId = memoryId;
            this.score = score;
            this.content = content;
        }
    }

    /** 记忆写入时旁路向量化入库（失败不阻断主流程）。 */
    public void ingestMemory(Long userId, Long memoryId, String content) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            body.put("memoryId", String.valueOf(memoryId));
            body.put("content", content);
            post("/memory/ingest", body);
        } catch (Exception e) {
            log.warn("记忆向量化入库失败(降级忽略) memoryId={}: {}", memoryId, e.getMessage());
        }
    }

    /** 语义召回 topK；sidecar 不可用返回空列表（调用方降级）。 */
    public List<MemoryHit> queryMemories(Long userId, String query, int topK) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            body.put("query", query);
            body.put("topK", topK);
            JsonNode node = post("/memory/query", body);
            List<MemoryHit> hits = new ArrayList<>();
            JsonNode arr = node.path("results");
            if (arr.isArray()) {
                for (JsonNode h : arr) {
                    hits.add(new MemoryHit(
                            h.path("memoryId").asText(),
                            h.path("score").asDouble(),
                            h.path("content").asText()));
                }
            }
            return hits;
        } catch (Exception e) {
            log.warn("记忆向量召回失败(降级关键词) userId={}: {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private JsonNode post(String path, Object body) throws Exception {
        String json = mapper.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + path))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 300) {
            throw new IllegalStateException("memory vector sidecar status=" + resp.statusCode());
        }
        return mapper.readTree(resp.body());
    }
}

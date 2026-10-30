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
 * 向量检索 sidecar 客户端（免费/开源：ChromaDB BSD + Ollama MIT）。
 *
 * <p>仅当 {@code aichat.vector.enabled=true} 时装配；sidecar 不可达时所有方法静默返回空，
 * 由调用方降级到原有 2-gram 关键词检索，保证 877 基线用例（离线）不受影响。
 *
 * <p>sidecar 实现见 scripts/vector_service.py，负责调用 Ollama(nomic-embed-text) 生成向量并写入 Chroma。
 */
@Component
@ConditionalOnProperty(name = "aichat.vector.enabled", havingValue = "true")
public class VectorSearchClient {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchClient.class);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String endpoint;

    public VectorSearchClient(@Value("${aichat.vector.endpoint:http://127.0.0.1:11435}") String endpoint) {
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        log.info("VectorSearchClient 已启用, endpoint={}", this.endpoint);
    }

    /** 向量召回结果项。 */
    public static class Hit {
        public final String chunkId;
        public final int chunkIndex;
        public final double score;
        public final String content;

        public Hit(String chunkId, int chunkIndex, double score, String content) {
            this.chunkId = chunkId;
            this.chunkIndex = chunkIndex;
            this.score = score;
            this.content = content;
        }
    }

    /** 文档分块入库后异步旁路写入向量库（失败不阻断主流程）。 */
    public void ingest(Long userId, Long documentId, List<long[]> idIndexPair, List<String> contents) {
        try {
            List<Map<String, Object>> chunks = new ArrayList<>();
            for (int i = 0; i < contents.size(); i++) {
                Map<String, Object> c = new HashMap<>();
                c.put("chunkId", String.valueOf(idIndexPair.get(i)[0]));
                c.put("chunkIndex", (int) idIndexPair.get(i)[1]);
                c.put("content", contents.get(i));
                chunks.add(c);
            }
            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            body.put("documentId", documentId);
            body.put("chunks", chunks);
            post("/ingest", body);
            log.info("向量入库成功 docId={}, chunks={}", documentId, chunks.size());
        } catch (Exception e) {
            log.warn("向量入库失败(降级忽略) docId={}: {}", documentId, e.getMessage());
        }
    }

    /** 向量召回 query，返回按相似度降序的命中；sidecar 不可用返回空列表。 */
    public List<Hit> query(Long userId, Long documentId, String query, int topK) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            body.put("documentId", documentId);
            body.put("query", query);
            body.put("topK", topK);
            JsonNode node = post("/query", body);
            List<Hit> hits = new ArrayList<>();
            JsonNode arr = node.path("results");
            if (arr.isArray()) {
                for (JsonNode h : arr) {
                    hits.add(new Hit(
                            h.path("chunkId").asText(),
                            h.path("chunkIndex").asInt(),
                            h.path("score").asDouble(),
                            h.path("content").asText()));
                }
            }
            return hits;
        } catch (Exception e) {
            log.warn("向量召回失败(降级到2-gram) docId={}: {}", documentId, e.getMessage());
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
            throw new IllegalStateException("vector sidecar status=" + resp.statusCode() + " body=" + resp.body());
        }
        return mapper.readTree(resp.body());
    }
}

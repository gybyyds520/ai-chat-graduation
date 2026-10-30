package com.example.aichat.service.impl;

import com.example.aichat.service.ContextCompressionService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI 模块 - 上下文压缩服务单元测试。
 *
 * 重点验证本轮 P0 修复：
 *   1. pruneContext 在 targetSize<=2 时不得除零（原 messages.size()/(targetSize-2)）；
 *   2. generateTopicSummary 在 content 为 null 时不得 NPE；
 *   3. 超长历史被截断到 targetSize 附近；
 *   4. 空历史 / null 历史安全返回。
 */
class ContextCompressionServiceTest {

    private final ContextCompressionService service = new ContextCompressionServiceImpl();

    private Map<String, Object> msg(String role, String content) {
        Map<String, Object> m = new HashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private List<Map<String, Object>> build(int n) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(msg(i % 2 == 0 ? "user" : "assistant", "消息内容" + i));
        }
        return list;
    }

    @Test
    void pruneContext_目标为1_不应除零且至少返回1条() {
        List<Map<String, Object>> result = service.pruneContext(build(20), 1);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void pruneContext_目标为2_不应除零() {
        List<Map<String, Object>> result = service.pruneContext(build(20), 2);
        assertNotNull(result);
        assertTrue(result.size() <= 2);
        assertFalse(result.isEmpty());
    }

    @Test
    void pruneContext_超长历史_应截断到目标数量附近() {
        List<Map<String, Object>> result = service.pruneContext(build(50), 10);
        assertNotNull(result);
        assertTrue(result.size() <= 12, "压缩后不应显著超过目标数量");
        assertTrue(result.size() <= 10 || result.size() <= 12);
        // 首尾消息应保留
        assertEquals("消息内容0", result.get(0).get("content"));
        assertEquals("消息内容49", result.get(result.size() - 1).get("content"));
    }

    @Test
    void pruneContext_短于目标_原样返回() {
        List<Map<String, Object>> small = build(3);
        List<Map<String, Object>> result = service.pruneContext(small, 10);
        assertSame(small, result);
    }

    @Test
    void pruneContext_空与null_安全返回不抛异常() {
        assertTrue(service.pruneContext(new ArrayList<>(), 10).isEmpty());
        assertTrue(service.pruneContext(null, 10).isEmpty());
    }

    @Test
    void generateTopicSummary_content为null_不应NPE() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(msg("user", null));
        list.add(msg("assistant", "回复"));
        assertDoesNotThrow(() -> service.generateTopicSummary(list));
    }

    @Test
    void generateTopicSummary_空与null_返回空串() {
        assertEquals("", service.generateTopicSummary(new ArrayList<>()));
        assertEquals("", service.generateTopicSummary(null));
    }

    @Test
    void compressContext_拼接角色与内容() {
        String out = service.compressContext(build(2), 10);
        assertTrue(out.contains("user: "));
        assertTrue(out.contains("assistant: "));
    }

    @Test
    void extractKeyPoints_不抛异常且包含entities集合() {
        Map<String, Object> kp = service.extractKeyPoints(build(5));
        assertNotNull(kp.get("entities"));
        assertNotNull(kp.get("actions"));
    }
}

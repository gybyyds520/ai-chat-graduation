package com.example.aichat.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 对话上下文相关性压缩纯函数测试（≥2 用例）。
 */
class ContextRelevanceCompressorTest {

    private Map<String, Object> msg(String role, String content) {
        Map<String, Object> m = new HashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    @Test
    void compress_保留系统消息与最近轮_丢弃无关中间轮() {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(msg("system", "你是小助手，性格温和"));
        messages.add(msg("user", "今天天气真好我们去爬山了"));   // 中间，与 query 无关
        messages.add(msg("assistant", "哈哈那你一定很开心"));
        messages.add(msg("user", "我喜欢喝咖啡"));              // 中间
        messages.add(msg("assistant", "好的记住了"));
        messages.add(msg("user", "帮我写一段 Java 排序算法"));   // 最新 query
        List<Map<String, Object>> out =
                ContextRelevanceCompressor.compressContext(messages, "帮我写一段 Java 排序算法", 10000);

        // system 一定保留
        assertTrue(out.stream().anyMatch(m -> "system".equals(m.get("role"))), "系统消息必须保留");
        // 最近 4 条保留：含"帮我写一段Java排序算法"
        assertTrue(out.contains(messages.get(5)), "最新 query 必须保留");
        // 无关的早期闲聊轮被裁剪（与 query bigram 无重叠，且非最近4条、无关键标记）
        assertFalse(out.contains(messages.get(1)), "无关中间轮应被裁剪");
    }

    @Test
    void compress_关键记忆消息强制保留() {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(msg("system", "你是小助手"));
        messages.add(msg("assistant", "【关于用户的长期记忆】用户喜欢喝咖啡")); // 含"记忆"
        messages.add(msg("user", "今天随便聊点别的话题"));
        messages.add(msg("user", "再聊一句无关的"));
        messages.add(msg("assistant", "好的"));
        messages.add(msg("user", "继续问问题"));
        List<Map<String, Object>> out =
                ContextRelevanceCompressor.compressContext(messages, "继续问问题", 10000);
        assertTrue(out.contains(messages.get(1)), "含'记忆'标记的关键消息必须保留");
    }

    @Test
    void compress_空与短消息原样返回() {
        assertTrue(ContextRelevanceCompressor.compressContext(null, "q", 100).isEmpty());
        List<Map<String, Object>> one = List.of(msg("user", "hi"));
        assertEquals(1, ContextRelevanceCompressor.compressContext(one, "hi", 100).size());
    }

    @Test
    void compress_budget截断_不裁系统与最近() {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(msg("system", "你是助手"));
        for (int i = 0; i < 10; i++) {
            messages.add(msg("user", "无关内容编号" + i + "填充" + i));
        }
        messages.add(msg("user", "最新问题"));
        // 极小预算：仅够系统 + 最近一条
        List<Map<String, Object>> out =
                ContextRelevanceCompressor.compressContext(messages, "最新问题", 30);
        // 系统保留，最新一条保留
        assertTrue(out.stream().anyMatch(m -> "system".equals(m.get("role"))));
        assertTrue(out.contains(messages.get(messages.size() - 1)));
    }
}

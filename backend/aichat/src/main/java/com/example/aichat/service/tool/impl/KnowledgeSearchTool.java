package com.example.aichat.service.tool.impl;

import com.example.aichat.service.tool.Tool;
import com.example.aichat.service.tool.ToolResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识搜索工具（mock，离线可用）。
 *
 * 返回固定的 mock 搜索结果，不接真实知识库，用于验证工具调用链路。
 */
@Component
public class KnowledgeSearchTool implements Tool {

    @Override
    public String getName() {
        return "knowledge_search";
    }

    @Override
    public String getDescription() {
        return "在知识库中搜索相关内容（mock 数据，离线可用）";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("type", "string");
        query.put("description", "搜索关键词");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("query", query);
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("query"));
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        Object q = parameters.get("query");
        String keyword = q == null ? "" : q.toString();
        Map<String, Object> data = new HashMap<>();
        data.put("keyword", keyword);
        data.put("total", 2);
        data.put("results", List.of(
                Map.of("title", "示例知识条目 A", "snippet", "这是与「" + keyword + "」相关的 mock 结果 A。"),
                Map.of("title", "示例知识条目 B", "snippet", "这是与「" + keyword + "」相关的 mock 结果 B。")
        ));
        data.put("note", "mock 数据");
        return ToolResult.success(data);
    }
}

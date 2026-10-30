package com.example.aichat.service.tool.impl;

import com.example.aichat.entity.SearchResult;
import com.example.aichat.service.WebSearchService;
import com.example.aichat.service.tool.Tool;
import com.example.aichat.service.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 网络搜索工具（复用现有 WebSearchService）。
 *
 * 入参 query 为搜索关键词，最多返回 5 条摘要结果。
 * WebSearchService 不可用 / 网络失败时降级返回友好错误，不抛堆栈。
 */
@Component
public class WebSearchTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(WebSearchTool.class);
    private static final int MAX_RESULTS = 5;

    private final WebSearchService webSearchService;

    public WebSearchTool(WebSearchService webSearchService) {
        this.webSearchService = webSearchService;
    }

    @Override
    public String getName() {
        return "web_search";
    }

    @Override
    public String getDescription() {
        return "联网搜索互联网信息，入参 query 为搜索关键词";
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
        String keyword = q == null ? "" : q.toString().trim();
        if (keyword.isEmpty()) {
            return ToolResult.failure("搜索关键词不能为空");
        }
        try {
            List<SearchResult> results = webSearchService.search(keyword, MAX_RESULTS);
            List<Map<String, Object>> brief = new ArrayList<>();
            if (results != null) {
                for (SearchResult r : results) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("title", r.getTitle());
                    item.put("url", r.getUrl());
                    item.put("snippet", r.getSnippet());
                    brief.add(item);
                }
            }
            Map<String, Object> data = new HashMap<>();
            data.put("query", keyword);
            data.put("total", brief.size());
            data.put("results", brief);
            return ToolResult.success(data);
        } catch (Exception e) {
            log.warn("web_search 执行失败 keyword={}: {}", keyword, e.getMessage());
            return ToolResult.failure("网络搜索暂时不可用，请稍后重试");
        }
    }
}

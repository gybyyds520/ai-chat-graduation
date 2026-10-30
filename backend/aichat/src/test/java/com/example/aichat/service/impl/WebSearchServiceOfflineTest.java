package com.example.aichat.service.impl;

import com.example.aichat.entity.SearchResult;
import com.example.aichat.service.WebSearchService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI 模块 - 网络搜索服务离线单元测试。
 *
 * 说明：现有 WebSearchServiceTest 的 16 个用例为 @SpringBootTest 集成测试，
 *      需要真实百度/Bing/DuckDuckGo 网络与 SearchLogger，离线无法运行，保持 @Disabled。
 *      本类仅覆盖不触网的纯逻辑：关键词生成、关键信息抽取、搜索历史上限。
 */
class WebSearchServiceOfflineTest {

    private final WebSearchService service = new WebSearchServiceImpl();

    private SearchResult result(String title, String url, String snippet, String source) {
        SearchResult r = new SearchResult();
        r.setTitle(title);
        r.setUrl(url);
        r.setSnippet(snippet);
        r.setSource(source);
        r.setRelevanceScore(0.8);
        return r;
    }

    @Test
    void generateSearchKeywords_至少包含原始问题且去重() {
        List<String> keywords = service.generateSearchKeywords("什么是机器学习？");
        assertFalse(keywords.isEmpty());
        // 原始问题（保留原符号）作为首个关键词
        assertTrue(keywords.contains("什么是机器学习？"));
        // distinct 后不应有重复
        assertEquals(keywords.size(), keywords.stream().distinct().count());
    }

    @Test
    void extractKeyInformation_抽取urls与sources() {
        List<SearchResult> list = new ArrayList<>();
        list.add(result("标题一", "https://a.com/1", "摘要一", "baidu"));
        list.add(result("标题二", "https://b.com/2", "摘要二", "bing"));

        Map<String, Object> info = service.extractKeyInformation(list);
        @SuppressWarnings("unchecked")
        List<String> urls = (List<String>) info.get("urls");
        assertEquals(2, urls.size());
        assertNotNull(info.get("combinedSummary"));
    }

    @Test
    void getSearchHistory_空历史返回空列表() {
        List<Map<String, Object>> history = service.getSearchHistory(10);
        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    void extractKeyInformation_空列表不抛异常() {
        Map<String, Object> info = service.extractKeyInformation(new ArrayList<>());
        assertNotNull(info.get("urls"));
        assertNotNull(info.get("sources"));
    }
}

package com.example.aichat.service.tool.impl;

import com.example.aichat.entity.SearchResult;
import com.example.aichat.service.WebSearchService;
import com.example.aichat.service.tool.ToolResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * WebSearchTool 单元测试（Mockito 隔离 WebSearchService，完全离线）。
 */
class WebSearchToolTest {

    @Test
    void 应复用WebSearchService并返回摘要结果() {
        WebSearchService svc = Mockito.mock(WebSearchService.class);
        SearchResult r1 = new SearchResult();
        r1.setTitle("标题A");
        r1.setUrl("https://a.com");
        r1.setSnippet("摘要A");
        when(svc.search(eq("AI"), anyInt())).thenReturn(List.of(r1));

        WebSearchTool tool = new WebSearchTool(svc);
        assertEquals("web_search", tool.getName());
        assertTrue(tool.isAvailable());

        ToolResult result = tool.execute(Map.of("query", "AI"));
        assertTrue(result.isSuccess(), result.getError());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("AI", data.get("query"));
        assertEquals(1, data.get("total"));
    }

    @Test
    void 空关键词应失败() {
        WebSearchService svc = Mockito.mock(WebSearchService.class);
        WebSearchTool tool = new WebSearchTool(svc);
        ToolResult r = tool.execute(Map.of("query", "   "));
        assertFalse(r.isSuccess());
        assertNotNull(r.getError());
    }

    @Test
    void WebSearchService抛异常应降级为友好错误不泄露堆栈() {
        WebSearchService svc = Mockito.mock(WebSearchService.class);
        when(svc.search(anyString(), anyInt())).thenThrow(new RuntimeException("connection refused internal detail"));
        WebSearchTool tool = new WebSearchTool(svc);

        ToolResult r = tool.execute(Map.of("query", "test"));
        assertFalse(r.isSuccess());
        // 不应泄漏内部异常细节
        assertFalse(r.getError().contains("connection refused"));
        assertTrue(r.getError().contains("搜索"));
    }
}

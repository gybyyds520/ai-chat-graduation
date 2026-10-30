package com.example.aichat.service.tool.impl;

import com.example.aichat.service.tool.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mock 工具离线测试：Time / Calculator / Weather / KnowledgeSearch。
 */
class MockToolsTest {

    @Test
    void timeTool_应返回当前时间且成功() {
        TimeTool tool = new TimeTool();
        assertEquals("get_current_time", tool.getName());
        assertTrue(tool.isAvailable());

        ToolResult r = tool.execute(new HashMap<>());
        assertTrue(r.isSuccess());
        assertNotNull(r.getData());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) r.getData();
        assertNotNull(data.get("time"));
        assertNotNull(data.get("zone"));
    }

    @Test
    void calculatorTool_应支持加减乘除与括号() {
        CalculatorTool tool = new CalculatorTool();
        assertEquals("calculator", tool.getName());

        assertEquals(3.0, eval(tool, "1+2"), 1e-9);
        assertEquals(1.0, eval(tool, "3-2"), 1e-9);
        assertEquals(6.0, eval(tool, "2*3"), 1e-9);
        assertEquals(2.5, eval(tool, "5/2"), 1e-9);
        // 优先级与括号
        assertEquals(14.0, eval(tool, "2+3*4"), 1e-9);
        assertEquals(20.0, eval(tool, "(2+3)*4"), 1e-9);
    }

    @Test
    void calculatorTool_除零应失败() {
        CalculatorTool tool = new CalculatorTool();
        ToolResult r = tool.execute(Map.of("expression", "1/0"));
        assertFalse(r.isSuccess());
        assertNotNull(r.getError());
    }

    @Test
    void weatherTool_应返回mock天气数据() {
        WeatherTool tool = new WeatherTool();
        assertEquals("get_weather", tool.getName());
        assertTrue(tool.isAvailable());

        ToolResult r = tool.execute(Map.of("city", "北京"));
        assertTrue(r.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) r.getData();
        assertEquals("北京", data.get("city"));
        assertEquals("晴", data.get("weather"));
    }

    @Test
    void knowledgeSearchTool_应返回mock结果() {
        KnowledgeSearchTool tool = new KnowledgeSearchTool();
        assertEquals("knowledge_search", tool.getName());

        ToolResult r = tool.execute(Map.of("query", "Spring"));
        assertTrue(r.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) r.getData();
        assertEquals("Spring", data.get("keyword"));
        assertEquals(2, data.get("total"));
    }

    private double eval(CalculatorTool tool, String expr) {
        ToolResult r = tool.execute(Map.of("expression", expr));
        assertTrue(r.isSuccess(), r.getError());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) r.getData();
        return ((Number) data.get("result")).doubleValue();
    }
}

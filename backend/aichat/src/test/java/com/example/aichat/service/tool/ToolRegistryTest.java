package com.example.aichat.service.tool;

import com.example.aichat.service.tool.impl.CalculatorTool;
import com.example.aichat.service.tool.impl.TimeTool;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ToolRegistry 注册/获取/列出 测试。
 */
class ToolRegistryTest {

    @Test
    void 注册后应能按名称获取并列出全部工具() {
        ToolRegistry registry = new ToolRegistry(List.of(new TimeTool(), new CalculatorTool()));
        registry.init();

        Optional<Tool> time = registry.getTool("get_current_time");
        assertTrue(time.isPresent());
        assertEquals("get_current_time", time.get().getName());

        assertEquals(2, registry.listTools().size());
        assertEquals(2, registry.listAvailableTools().size());
    }

    @Test
    void 获取不存在的工具应返回空() {
        ToolRegistry registry = new ToolRegistry(List.of(new TimeTool()));
        registry.init();

        assertTrue(registry.getTool("not_exist").isEmpty());
        assertTrue(registry.getTool(null).isEmpty());
    }
}

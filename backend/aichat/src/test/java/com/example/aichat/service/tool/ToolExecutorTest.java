package com.example.aichat.service.tool;

import com.example.aichat.service.tool.impl.CalculatorTool;
import com.example.aichat.service.tool.impl.WeatherTool;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ToolExecutor 执行/错误/参数校验 测试。
 */
class ToolExecutorTest {

    private ToolExecutor newExecutor() {
        ToolRegistry registry = new ToolRegistry(List.of(new CalculatorTool(), new WeatherTool()));
        registry.init();
        return new ToolExecutor(registry);
    }

    @Test
    void 执行存在的工具应成功() {
        ToolExecutor executor = newExecutor();
        ToolResult r = executor.execute("calculator", Map.of("expression", "1+1"));
        assertTrue(r.isSuccess());
    }

    @Test
    void 执行不存在的工具应返回失败() {
        ToolExecutor executor = newExecutor();
        ToolResult r = executor.execute("ghost_tool", new HashMap<>());
        assertFalse(r.isSuccess());
        assertNotNull(r.getError());
    }

    @Test
    void 必填参数缺失应返回失败() {
        ToolExecutor executor = newExecutor();
        // get_weather 必填 city
        ToolResult r = executor.execute("get_weather", new HashMap<>());
        assertFalse(r.isSuccess());
        assertTrue(r.getError().contains("city"));
    }

    @Test
    void 空工具名应返回失败() {
        ToolExecutor executor = newExecutor();
        assertFalse(executor.execute(" ", new HashMap<>()).isSuccess());
    }

    @Test
    void 工具内部抛异常应被捕获为友好失败不向上抛出() {
        // 注册一个会抛异常的工具
        Tool boom = new Tool() {
            @Override public String getName() { return "boom"; }
            @Override public String getDescription() { return "always throws"; }
            @Override public java.util.Map<String, Object> getParameters() { return new HashMap<>(); }
            @Override public ToolResult execute(java.util.Map<String, Object> p) {
                throw new IllegalStateException("simulated internal failure");
            }
        };
        ToolRegistry registry = new ToolRegistry(List.of(boom));
        registry.init();
        ToolExecutor executor = new ToolExecutor(registry);

        // 不应向上抛出异常，而是返回 failure 结果
        ToolResult r = executor.execute("boom", new HashMap<>());
        assertFalse(r.isSuccess());
        assertNotNull(r.getError());
        // 不应泄漏异常类名 / 堆栈信息
        assertFalse(r.getError().contains("IllegalStateException"));
        assertFalse(r.getError().contains("\tat "));
    }
}

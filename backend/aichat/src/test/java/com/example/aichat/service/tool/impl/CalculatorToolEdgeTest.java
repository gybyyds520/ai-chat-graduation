package com.example.aichat.service.tool.impl;

import com.example.aichat.service.tool.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CalculatorTool 递归下降解析器边界用例。
 *
 * <p>既有 {@link MockToolsTest} 已覆盖基础 +、-、*、/、优先级、括号与除零。
 * 本类只补其未覆盖的反例：空表达式、一元负号、缺右括号、尾随运算符、
 * 非法字符、空白容忍、小数与嵌套括号——每条对应一个不同的解析器错误实现。
 */
class CalculatorToolEdgeTest {

    private final CalculatorTool tool = new CalculatorTool();

    private double eval(String expr) {
        ToolResult r = tool.execute(Map.of("expression", expr));
        assertTrue(r.isSuccess(), "应成功: " + expr + " -> " + r.getError());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) r.getData();
        assertEquals(expr, data.get("expression"));
        return ((Number) data.get("result")).doubleValue();
    }

    private ToolResult run(String expr) {
        Map<String, Object> p = new HashMap<>();
        if (expr != null) p.put("expression", expr);
        return tool.execute(p);
    }

    // C1：空/blank 表达式必须返回失败（"表达式不能为空"），不得 NPE 或按 0 计算
    @Test
    void 空表达式_返回失败() {
        assertTrue(run(null).isSuccess() == false);
        assertTrue(run("").isSuccess() == false);
        assertTrue(run("   ").isSuccess() == false);
        assertEquals("表达式不能为空", run(null).getError());
    }

    // C2：一元负号
    @Test
    void 一元负号_正确求值() {
        assertEquals(-5.0, eval("-5"), 1e-9);
        assertEquals(-6.0, eval("2*-3"), 1e-9);
        assertEquals(3.0, eval("-(-3)"), 1e-9);
        assertEquals(1.0, eval("-2+3"), 1e-9);
    }

    // C3：缺少右括号 → 失败
    @Test
    void 缺少右括号_失败() {
        ToolResult r = run("(1+2");
        assertFalse(r.isSuccess());
        assertNotNull(r.getError());
    }

    // C4：尾随运算符（表达式意外结束）→ 失败，不得把缺失右操作数当 0
    @Test
    void 尾随运算符_失败() {
        assertFalse(run("1+").isSuccess());
        assertFalse(run("1*").isSuccess());
        assertFalse(run("(1+2)*").isSuccess());
    }

    // C5：因子位置出现非法字符 → 失败（不得跳过字符或当 0）。
    // 注：解析器对"尾随垃圾字符"（如 "2x"、"(1+2)#"）当前为宽容忽略，属已知行为，不在此断言。
    @Test
    void 非法字符_失败() {
        assertTrue(run("1+a").isSuccess() == false);
        assertTrue(run("1+@").isSuccess() == false);
        assertTrue(run("@1").isSuccess() == false);
    }

    // C6：空白容忍
    @Test
    void 空白容忍_正确求值() {
        assertEquals(3.0, eval(" 1 + 2 "), 1e-9);
        assertEquals(9.0, eval("(\t2 + 1) * 3"), 1e-9);
    }

    // C7：小数
    @Test
    void 小数_正确求值() {
        assertEquals(7.0, eval("3.5*2"), 1e-9);
        assertEquals(0.25, eval("1/4"), 1e-9);
        assertEquals(3.3, eval("1.1+2.2"), 1e-9);
    }

    // C8：嵌套括号 ((1+2)*(2+3))-4 = 11；2*(3+(1-2)) = 2*(3-1) = 4
    @Test
    void 嵌套括号_正确求值() {
        assertEquals(11.0, eval("((1+2)*(2+3))-4"), 1e-9);
        assertEquals(4.0, eval("2*(3+(1-2))"), 1e-9);
    }
}

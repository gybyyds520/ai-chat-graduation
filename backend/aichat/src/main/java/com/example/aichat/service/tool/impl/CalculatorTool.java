package com.example.aichat.service.tool.impl;

import com.example.aichat.service.tool.Tool;
import com.example.aichat.service.tool.ToolResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 计算器工具（离线可用）。
 *
 * 支持加(+)、减(-)、乘(*)、除(/)，遵循标准四则运算优先级与括号，
 * 入参 expression 为字符串，如 "1+1"、"(2+3)*4"。
 * 自实现递归下降解析器，不依赖脚本引擎，安全可测。
 */
@Component
public class CalculatorTool implements Tool {

    @Override
    public String getName() {
        return "calculator";
    }

    @Override
    public String getDescription() {
        return "简单计算器，支持加减乘除与括号，入参 expression 如 1+1";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> expression = new LinkedHashMap<>();
        expression.put("type", "string");
        expression.put("description", "数学表达式，如 1+2*3");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("expression", expression);
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("expression"));
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        Object expr = parameters.get("expression");
        if (expr == null || expr.toString().isBlank()) {
            return ToolResult.failure("表达式不能为空");
        }
        try {
            double value = new Parser(expr.toString()).parse();
            Map<String, Object> data = new HashMap<>();
            data.put("expression", expr.toString());
            data.put("result", value);
            return ToolResult.success(data);
        } catch (Exception e) {
            return ToolResult.failure("表达式计算失败: " + e.getMessage());
        }
    }

    /** 递归下降解析器：expression = term (('+'|'-') term)* */
    private static class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
            this.pos = 0;
        }

        double parse() {
            double v = parseTerm();
            skipSpaces();
            while (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) {
                char op = s.charAt(pos++);
                double rhs = parseTerm();
                v = op == '+' ? v + rhs : v - rhs;
                skipSpaces();
            }
            return v;
        }

        private double parseTerm() {
            double v = parseFactor();
            skipSpaces();
            while (pos < s.length() && (s.charAt(pos) == '*' || s.charAt(pos) == '/')) {
                char op = s.charAt(pos++);
                double rhs = parseFactor();
                if (op == '/') {
                    if (rhs == 0) {
                        throw new IllegalArgumentException("除数不能为零");
                    }
                    v = v / rhs;
                } else {
                    v = v * rhs;
                }
                skipSpaces();
            }
            return v;
        }

        private double parseFactor() {
            skipSpaces();
            if (pos >= s.length()) {
                throw new IllegalArgumentException("表达式意外结束");
            }
            char c = s.charAt(pos);
            if (c == '(') {
                pos++;
                double v = parse();
                skipSpaces();
                if (pos >= s.length() || s.charAt(pos) != ')') {
                    throw new IllegalArgumentException("缺少右括号");
                }
                pos++;
                return v;
            }
            if (c == '-') {
                pos++;
                return -parseFactor();
            }
            // number
            int start = pos;
            while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) {
                pos++;
            }
            if (start == pos) {
                throw new IllegalArgumentException("无法解析的字符: " + c);
            }
            return Double.parseDouble(s.substring(start, pos));
        }

        private void skipSpaces() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }
    }
}

package com.example.aichat.service.tool;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 工具执行器。
 *
 * 根据工具名从注册中心查找工具，做存在性 / 可用性 / 必填参数校验后执行，
 * 并捕获工具内部异常，统一封装为 ToolResult（不向调用方泄漏堆栈）。
 */
@Component
public class ToolExecutor {

    private final ToolRegistry registry;

    public ToolExecutor(ToolRegistry registry) {
        this.registry = registry;
    }

    public ToolResult execute(String toolName, Map<String, Object> parameters) {
        if (toolName == null || toolName.isBlank()) {
            return ToolResult.failure("工具名称不能为空");
        }
        Optional<Tool> opt = registry.getTool(toolName);
        if (opt.isEmpty()) {
            return ToolResult.failure("工具不存在: " + toolName);
        }
        Tool tool = opt.get();
        if (!tool.isAvailable()) {
            return ToolResult.failure("工具当前不可用: " + toolName);
        }
        Map<String, Object> params = parameters == null ? new HashMap<>() : parameters;

        String missing = validateRequired(tool, params);
        if (missing != null) {
            return ToolResult.failure("缺少必要参数: " + missing);
        }
        try {
            return tool.execute(params);
        } catch (Exception e) {
            return ToolResult.failure("工具执行失败: " + e.getMessage());
        }
    }

    /**
     * 依据工具参数 schema 中的 required 列表做必填校验。
     */
    @SuppressWarnings("unchecked")
    private String validateRequired(Tool tool, Map<String, Object> params) {
        Map<String, Object> schema = tool.getParameters();
        if (schema == null) {
            return null;
        }
        Object required = schema.get("required");
        if (!(required instanceof List)) {
            return null;
        }
        for (Object r : (List<Object>) required) {
            String key = String.valueOf(r);
            if (!params.containsKey(key) || params.get(key) == null) {
                return key;
            }
        }
        return null;
    }
}

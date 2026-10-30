package com.example.aichat.service.tool;

import java.util.Map;

/**
 * 工具调用（Function Calling）适配器接口。
 *
 * 每个工具（如时间、计算器、天气、知识搜索）实现本接口，
 * 由 ToolRegistry 统一注册、ToolExecutor 统一调度执行。
 * 设计目标：新增工具只需实现本接口并声明为 Spring Bean，即可被自动发现与调用。
 */
public interface Tool {

    /**
     * 工具名称（唯一标识，如 "get_weather"、"calculator"、"get_current_time"）。
     */
    String getName();

    /**
     * 工具描述，供 AI 模型 / 用户理解该工具用途。
     */
    String getDescription();

    /**
     * 参数定义，JSON Schema 风格：
     * { "type":"object", "properties":{...}, "required":[...] }
     */
    Map<String, Object> getParameters();

    /**
     * 执行工具并返回结构化结果。
     */
    ToolResult execute(Map<String, Object> parameters);

    /**
     * 工具是否当前可用（离线 mock 工具恒为 true；外部服务工具可按探活结果返回）。
     */
    default boolean isAvailable() {
        return true;
    }
}

package com.example.aichat.service.tool.impl;

import com.example.aichat.service.tool.Tool;
import com.example.aichat.service.tool.ToolResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 天气工具（mock，离线可用）。
 *
 * isAvailable 恒为 true，但返回的是固定 mock 数据，
 * 用于验证工具调用链路，不接真实天气服务。
 */
@Component
public class WeatherTool implements Tool {

    @Override
    public String getName() {
        return "get_weather";
    }

    @Override
    public String getDescription() {
        return "查询城市天气（mock 数据，离线可用）";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> city = new LinkedHashMap<>();
        city.put("type", "string");
        city.put("description", "城市名称，如 北京");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("city", city);
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("city"));
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        Object cityObj = parameters.get("city");
        String city = cityObj == null || cityObj.toString().isBlank() ? "北京" : cityObj.toString();
        Map<String, Object> data = new HashMap<>();
        data.put("city", city);
        data.put("weather", "晴");
        data.put("temperature", "25°C");
        data.put("humidity", "45%");
        data.put("note", "mock 数据");
        return ToolResult.success(data);
    }
}

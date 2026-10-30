package com.example.aichat.service.tool.impl;

import com.example.aichat.service.tool.Tool;
import com.example.aichat.service.tool.ToolResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * 时间工具（离线可用，返回系统当前时间）。
 */
@Component
public class TimeTool implements Tool {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String getName() {
        return "get_current_time";
    }

    @Override
    public String getDescription() {
        return "获取当前系统时间（离线可用）";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> props = new HashMap<>();
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of());
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        Map<String, Object> data = new HashMap<>();
        data.put("time", LocalDateTime.now().format(FMT));
        data.put("zone", TimeZone.getDefault().getID());
        return ToolResult.success(data);
    }
}

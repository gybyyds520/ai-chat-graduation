package com.example.aichat.service.tool;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 工具注册中心。
 *
 * Spring 自动收集所有 Tool 实现 Bean，按名称建立索引，
 * 提供注册 / 获取 / 列出可用工具的能力。
 */
@Component
public class ToolRegistry {

    private final List<Tool> tools;
    private final Map<String, Tool> toolMap = new HashMap<>();

    public ToolRegistry(List<Tool> tools) {
        this.tools = tools;
    }

    @PostConstruct
    public void init() {
        toolMap.clear();
        for (Tool tool : tools) {
            toolMap.put(tool.getName(), tool);
        }
    }

    /** 按名称获取工具。 */
    public Optional<Tool> getTool(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(toolMap.get(name));
    }

    /** 列出全部已注册工具。 */
    public List<Tool> listTools() {
        return new ArrayList<>(tools);
    }

    /** 列出当前可用（isAvailable=true）的工具。 */
    public List<Tool> listAvailableTools() {
        List<Tool> available = new ArrayList<>();
        for (Tool tool : tools) {
            if (tool.isAvailable()) {
                available.add(tool);
            }
        }
        return available;
    }
}

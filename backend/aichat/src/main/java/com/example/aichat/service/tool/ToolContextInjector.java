package com.example.aichat.service.tool;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工具上下文注入器。
 *
 * 将当前可用工具列表格式化为 systemPrompt 前缀，供 AI 模型参考。
 * 简化整合方案：不强制改写 ChatController，提供方法即可在构建 systemPrompt 时拼接。
 */
@Component
public class ToolContextInjector {

    private final ToolRegistry registry;

    public ToolContextInjector(ToolRegistry registry) {
        this.registry = registry;
    }

    /**
     * 生成可供 AI 模型参考的可用工具说明文本。
     */
    public String buildSystemPromptPrefix() {
        List<Tool> tools = registry.listAvailableTools();
        StringBuilder sb = new StringBuilder();
        sb.append("你可以使用以下工具来辅助回答。当需要调用工具时，请使用 @tool:<工具名> 参数 的格式输出指令。\n");
        for (Tool tool : tools) {
            sb.append("- ").append(tool.getName()).append("：").append(tool.getDescription()).append("\n");
        }
        return sb.toString();
    }
}

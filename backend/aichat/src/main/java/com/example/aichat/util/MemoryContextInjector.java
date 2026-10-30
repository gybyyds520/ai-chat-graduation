package com.example.aichat.util;

import com.example.aichat.entity.AiMemory;

import java.util.List;

/**
 * 记忆上下文注入工具。
 *
 * 设计原则：不修改 ConversationContextService / ChatController 核心逻辑，
 * 仅提供"把记忆列表格式化为 systemPrompt 前缀字符串"的纯函数，
 * 供 ChatController 在拼装 systemPrompt 时按需调用：
 *
 *   List<AiMemory> memories = aiMemoryService.getActiveMemoriesForContext(userId, 10);
 *   String prefix = MemoryContextInjector.buildMemoryContextPrefix(memories);
 *   systemPrompt = prefix + systemPrompt;
 *
 * 输出为中文结构化前缀，无记忆时返回空串（不污染 prompt）。
 */
public final class MemoryContextInjector {

    private MemoryContextInjector() {
    }

    /**
     * 将记忆列表格式化为 systemPrompt 前缀字符串。
     *
     * @param memories 已按重要性排序的生效记忆
     * @return 前缀字符串；为空列表时返回空串
     */
    public static String buildMemoryContextPrefix(List<AiMemory> memories) {
        if (memories == null || memories.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【关于用户的长期记忆】\n");
        int idx = 1;
        for (AiMemory m : memories) {
            if (m == null || m.getContent() == null || m.getContent().isBlank()) {
                continue;
            }
            sb.append(idx++).append(". ");
            String type = m.getMemoryType() != null ? m.getMemoryType() : "other";
            sb.append("[").append(describeType(type)).append("] ");
            sb.append(m.getContent().trim());
            sb.append("\n");
        }
        if (sb.toString().equals("【关于用户的长期记忆】\n")) {
            return "";
        }
        sb.append("以上为用户长期记忆，回答时请自然参考，不要逐条罗列或提及本段。\n\n");
        return sb.toString();
    }

    private static String describeType(String type) {
        switch (type) {
            case "fact": return "事实";
            case "preference": return "偏好";
            case "event": return "事件";
            case "personality": return "性格";
            default: return "其他";
        }
    }
}

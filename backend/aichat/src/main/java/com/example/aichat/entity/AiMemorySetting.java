package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 记忆全局开关实体（@TableName("ai_memory_setting")）。
 *
 * 每个用户一行：enabled=false 时，该用户的所有长期记忆都不再注入对话上下文
 * （对应 ChatController 在拼装 systemPrompt 前的开关检查）。
 * 未设置记录时默认视为开启（enabled=true）。
 */
@Data
@TableName("ai_memory_setting")
public class AiMemorySetting {

    @TableId(type = IdType.INPUT)
    private Long userId;

    /** 记忆总开关：true=注入长期记忆；false=不注入 */
    private Boolean enabled;

    private LocalDateTime updatedAt;
}

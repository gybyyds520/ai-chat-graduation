package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 灵魂经历流水（@TableName("ai_soul_experience")）。
 *
 * 灵魂算法中"经历"层的持久化：
 *   - experience：刚发生的经历（对话内容摘要）
 *   - feeling：如何感受（情绪反应文本）—— I(t) 的感受部分
 *   - understanding：如何理解（认知评价文本）—— I(t) 的理解部分
 *   - memoryRef：沉淀进长期记忆的内容（与 ai_memory 联动）
 *   - impact：这次经历对"下一刻的自己"的影响摘要
 */
@Data
@TableName("ai_soul_experience")
public class AiSoulExperience {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户 */
    private Long userId;

    /** AI 角色 ID */
    private Long characterId;

    /** 关联 AI 精灵 ID（可空） */
    private Long elfId;

    /** 刚发生的经历（对话内容摘要） */
    private String experience;

    /** 如何感受（情绪反应文本） */
    private String feeling;

    /** 如何理解（认知评价文本） */
    private String understanding;

    /** 沉淀的记忆内容（写入 ai_memory 的摘要） */
    private String memoryRef;

    /** 对下一刻自己的影响摘要 */
    private String impact;

    private LocalDateTime createdAt;
}

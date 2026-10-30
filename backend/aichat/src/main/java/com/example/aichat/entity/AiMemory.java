package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 记忆实体（@TableName("ai_memory")）。
 *
 * 用于"AI 记忆可视化"功能：持久化用户与 AI 交互过程中沉淀下来的事实、偏好、
 * 事件、性格等长期记忆，支持在聊天上下文注入（见 MemoryContextInjector）。
 *
 * 字段说明：
 *   - source:      记忆来源 chat/manual/profile/system
 *   - memoryType:  记忆类型 fact/preference/event/personality/other
 *   - importance:  重要性 1-5，默认 3
 *   - isActive:    是否生效（false 时不注入上下文，仅历史保留）
 *   - lastUsedAt:  最近一次注入上下文的时间，用于排序与召回
 */
@Data
@TableName("ai_memory")
public class AiMemory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 记忆内容（TEXT） */
    private String content;

    /** 来源：chat/manual/profile/system */
    private String source;

    /** 类型：fact/preference/event/personality/other */
    private String memoryType;

    /** 重要性 1-5，默认 3 */
    private Integer importance;

    /** 分类（可空）。市场级升级后，自动落库时写入 MemoryCategory 枚举值（FACT/PREFERENCE/EVENT/RELATIONSHIP）；
     *  用户显式传入的自由文本标签（如"饮食""钉选记忆"）仍原样保留。 */
    private String category;

    /** 是否生效，默认 true */
    private Boolean isActive;

    /**
     * 被对话上下文引用的次数。常引用记忆（>5）在排序时加权 ×1.5。
     * 市场级升级新增字段，默认 0。
     */
    private Integer referencedCount;

    /**
     * 是否钉选。钉选记忆不参与时间衰减，召回时 ×1.5 加成。默认 false。
     * 市场级升级新增字段。
     */
    @TableField("is_pinned")
    private Boolean pinned;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 最近使用（注入上下文）时间，可空。
     * 语义即"最后引用时间 lastReferencedAt"：记忆衰减以距此刻的天数为衰减窗口。
     */
    private LocalDateTime lastUsedAt;
}

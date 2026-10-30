package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 灵魂状态快照（@TableName("ai_soul_state")）。
 *
 * 灵魂算法（用户原创）：
 *   这一刻的自己(S) + 发生的经历(E) + 如何感受和理解(I) + 过去留下的记忆(M)
 *   = 下一刻的自己(S')
 *
 * 本实体承载 S（此刻的自我快照）：
 *   - currentEmotion / moodScore / attitudeScore：情绪与态度层
 *   - personalityWeights：性格权重（JSON），随经历缓慢演化
 *   - coreBeliefs：核心信念/价值观摘要（"我是谁"的沉淀）
 *   - selfSummary：可直接注入 systemPrompt 的自我描述
 */
@Data
@TableName("ai_soul_state")
public class AiSoulState {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户 */
    private Long userId;

    /** AI 角色 ID（灵魂载体） */
    private Long characterId;

    /** 关联 AI 精灵 ID（可空，精灵可复用同一灵魂引擎） */
    private Long elfId;

    /** 当前情绪：neutral/happy/sad/angry/surprised/fearful/love */
    private String currentEmotion;

    /** 心情值 0-100 */
    private Integer moodScore;

    /** 对用户态度温度 -100~100（亲近度） */
    private Integer attitudeScore;

    /** 性格权重 JSON：{"温柔":0.8,"活泼":0.6,...} */
    private String personalityWeights;

    /** 核心信念/价值观摘要 */
    private String coreBeliefs;

    /** 此刻的自我描述（注入 prompt 用） */
    private String selfSummary;

    /** 演化版本号（每次经历 +1） */
    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 精灵互动日志（@TableName("ai_elf_interaction_log")）。
 *
 * 记录每次与精灵的互动（聊天/投喂/玩耍/睡觉）及本次获得的好感度，
 * 用于互动历史时间线与每日互动次数统计。
 */
@Data
@TableName("ai_elf_interaction_log")
public class AiElfInteractionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long elfId;

    private Long userId;

    /** 互动类型：chat/feed/play/sleep */
    private String interactionType;

    /** 本次获得的好感度 */
    private Integer pointsGained;

    private LocalDateTime createdAt;
}

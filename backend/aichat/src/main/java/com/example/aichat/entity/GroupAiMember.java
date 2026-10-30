package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群聊中的 AI 角色成员（多 AI 角色同场群聊）。
 *
 * 一个群可添加多个公开/自有 AI 角色作为成员；发言编排见 GroupAiChatService。
 */
@Data
@TableName("group_ai_member")
public class GroupAiMember {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("group_id")
    private Long groupId;

    @TableField("character_id")
    private Long characterId;

    @TableField("added_by")
    private Long addedBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 是否在群内激活（false 表示被移除/停用）。 */
    @TableField("is_active")
    private Boolean isActive;
}

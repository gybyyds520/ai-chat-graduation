package com.example.aichat.service;

import com.example.aichat.entity.AiCharacter;
import com.example.aichat.entity.GroupAiMember;
import com.example.aichat.entity.GroupMessage;

import java.util.List;
import java.util.Map;

/**
 * 多 AI 角色同场群聊 - 轮番发言编排（简化版）。
 *
 * 不依赖真实 LLM：基于角色人设 + 触发消息，用模板生成不同风格的 mock 回复，
 * 每个激活的 AI 角色在一轮中依次发言，发言作为群消息落库。
 */
public interface GroupAiChatService {

    /**
     * 触发一轮 AI 发言：群内所有激活 AI 角色依次生成一条回复并存为群消息。
     *
     * @param groupId       群 ID
     * @param userId        触发者（必须为群成员）
     * @param triggerMessage 触发消息
     * @return 本轮所有 AI 回复（characterId / characterName / content）
     */
    List<Map<String, Object>> triggerAiRound(Long groupId, Long userId, String triggerMessage);

    /** 获取下一个应该发言的 AI 角色（轮流制）。 */
    GroupAiMember getNextAiCharacter(Long groupId);

    /** 生成 AI 回复（mock：基于角色 name/personality 用模板拼接）。 */
    String generateAiReply(AiCharacter character, String triggerMessage, List<GroupMessage> history);
}

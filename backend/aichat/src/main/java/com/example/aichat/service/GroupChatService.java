package com.example.aichat.service;

import com.example.aichat.entity.GroupChat;
import com.example.aichat.entity.GroupMember;
import com.example.aichat.entity.GroupMessage;

import java.util.List;
import java.util.Map;

public interface GroupChatService {
    
    GroupChat createGroup(Long userId, String name, String description);
    
    GroupChat getGroupById(Long groupId);
    
    List<GroupChat> getUserGroups(Long userId);
    
    List<GroupChat> getPublicGroups();
    
    void joinGroup(Long groupId, Long userId);
    
    void leaveGroup(Long groupId, Long userId);
    
    List<GroupMember> getGroupMembers(Long groupId);
    
    void sendMessage(Long groupId, Long userId, String userName, String content);
    
    List<GroupMessage> getGroupMessages(Long groupId, Long userId, int limit);
    
    void deleteGroup(Long groupId, Long userId);

    List<Map<String, Object>> recommendFriends(Long userId);

    GroupChat updateAnnouncement(Long groupId, Long userId, String announcement);

    GroupChat updateSettings(Long groupId, Long userId, String settings);
    
    GroupChat getGroupWithDetails(Long groupId);
    
    List<Map<String, Object>> getCommunityActivityRanking(int limit);
    
    void updateActivityScore(Long groupId);
    
    com.example.aichat.entity.GroupBot createGroupBot(Long groupId, String botName, String description);
    
    List<com.example.aichat.entity.GroupBot> getGroupBots(Long groupId);

    /** 按 botId 查询机器人（用于更新/删除前反查所属群，做群主归属校验）。 */
    com.example.aichat.entity.GroupBot getGroupBotById(Long botId);

    com.example.aichat.entity.GroupBot updateGroupBot(Long botId, Map<String, Object> updates);
    
    void deleteGroupBot(Long botId);
    
    String processBotResponse(Long groupId, String message);
    
    List<Map<String, Object>> recommendFriendsAdvanced(Long userId);

    // ===== 多 AI 角色同场群聊：群成员管理 =====

    /** 添加 AI 角色到群（校验群存在 + 操作者为群成员）。 */
    void addAiCharacterToGroup(Long groupId, Long userId, Long characterId);

    /** 移除群内 AI 角色（校验群主）。 */
    void removeAiCharacterFromGroup(Long groupId, Long userId, Long characterId);

    /** 列出群内激活的 AI 角色成员。 */
    List<com.example.aichat.entity.GroupAiMember> listAiCharactersInGroup(Long groupId);
}
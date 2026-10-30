package com.example.aichat.service.impl;

import com.example.aichat.entity.GroupAiMember;
import com.example.aichat.entity.GroupBot;
import com.example.aichat.entity.GroupChat;
import com.example.aichat.entity.GroupMember;
import com.example.aichat.entity.GroupMessage;
import com.example.aichat.entity.AiCharacter;
import com.example.aichat.entity.UserInterest;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.SocialErrorCode;
import com.example.aichat.dto.audit.AuditResult;
import com.example.aichat.event.UserDataCaptureEvent;
import com.example.aichat.mapper.AiCharacterMapper;
import com.example.aichat.mapper.GroupAiMemberMapper;
import com.example.aichat.mapper.GroupBotMapper;
import com.example.aichat.mapper.GroupChatMapper;
import com.example.aichat.mapper.GroupMemberMapper;
import com.example.aichat.mapper.GroupMessageMapper;
import com.example.aichat.mapper.UserInterestMapper;
import com.example.aichat.service.ContentAuditService;
import com.example.aichat.service.GroupChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupChatServiceImpl implements GroupChatService {

    private static final Logger logger = LoggerFactory.getLogger(GroupChatServiceImpl.class);

    /** 群消息内容长度上限（与 WebSocket / 私聊 REST 双路保持一致）。 */
    private static final int MAX_MESSAGE_LENGTH = 8000;
    /** 群名称长度上限。 */
    private static final int MAX_GROUP_NAME_LENGTH = 50;

    private final GroupChatMapper groupChatMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final UserInterestMapper userInterestMapper;
    private final GroupBotMapper groupBotMapper;
    private final ContentAuditService contentAuditService;
    private final GroupAiMemberMapper groupAiMemberMapper;
    private final AiCharacterMapper aiCharacterMapper;

    /** 数据池自动采集事件发布器。 */
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public GroupChatServiceImpl(GroupChatMapper groupChatMapper,
                               GroupMemberMapper groupMemberMapper,
                               GroupMessageMapper groupMessageMapper,
                               UserInterestMapper userInterestMapper,
                               GroupBotMapper groupBotMapper,
                               ContentAuditService contentAuditService,
                               GroupAiMemberMapper groupAiMemberMapper,
                               AiCharacterMapper aiCharacterMapper) {
        this.groupChatMapper = groupChatMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.groupMessageMapper = groupMessageMapper;
        this.userInterestMapper = userInterestMapper;
        this.groupBotMapper = groupBotMapper;
        this.contentAuditService = contentAuditService;
        this.groupAiMemberMapper = groupAiMemberMapper;
        this.aiCharacterMapper = aiCharacterMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupChat createGroup(Long userId, String name, String description) {
        // 参数校验（P1）：群名不能为空、不能过长，避免脏数据落库
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(SocialErrorCode.GROUP_NAME_BLANK);
        }
        if (name.trim().length() > MAX_GROUP_NAME_LENGTH) {
            throw new BusinessException(SocialErrorCode.GROUP_NAME_TOO_LONG);
        }

        GroupChat group = new GroupChat();
        group.setName(name.trim());
        group.setDescription(description);
        group.setOwnerId(userId);
        group.setMemberCount(1);
        group.setIsPublic(true);
        group.setActivityScore(0);
        group.setCreatedAt(LocalDateTime.now());

        groupChatMapper.insert(group);

        GroupMember member = new GroupMember();
        member.setGroupId(group.getId());
        member.setUserId(userId);
        member.setRole("owner");
        member.setJoinedAt(LocalDateTime.now());
        groupMemberMapper.insert(member);

        logger.info("[群聊] 用户 {} 创建群聊: {}", userId, name);
        return group;
    }

    @Override
    public GroupChat getGroupById(Long groupId) {
        return groupChatMapper.selectById(groupId);
    }

    @Override
    public List<GroupChat> getUserGroups(Long userId) {
        return groupChatMapper.getGroupsByUserId(userId);
    }

    @Override
    public List<GroupChat> getPublicGroups() {
        return groupChatMapper.getPublicGroups();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinGroup(Long groupId, Long userId) {
        // 先校验群存在（P0）：原实现未校验即插入成员，会产生悬挂成员记录
        GroupChat group = groupChatMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(SocialErrorCode.GROUP_NOT_FOUND);
        }

        GroupMember existing = groupMemberMapper.findMember(groupId, userId);
        if (existing != null) {
            // 幂等：已是成员直接返回，不重复计数
            return;
        }

        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole("member");
        member.setJoinedAt(LocalDateTime.now());
        groupMemberMapper.insert(member);

        group.setMemberCount((group.getMemberCount() == null ? 0 : group.getMemberCount()) + 1);
        groupChatMapper.updateById(group);

        logger.info("[群聊] 用户 {} 加入群聊: {}", userId, groupId);
    }

    @Override
    public void leaveGroup(Long groupId, Long userId) {
        GroupMember member = groupMemberMapper.findMember(groupId, userId);
        if (member != null) {
            groupMemberMapper.deleteById(member.getId());
            
            GroupChat group = groupChatMapper.selectById(groupId);
            if (group != null && group.getMemberCount() > 0) {
                group.setMemberCount(group.getMemberCount() - 1);
                groupChatMapper.updateById(group);
            }
        }
    }

    @Override
    public List<GroupMember> getGroupMembers(Long groupId) {
        return groupMemberMapper.getMembersByGroupId(groupId);
    }

    @Override
    public void sendMessage(Long groupId, Long userId, String userName, String content) {
        // 参数校验（P1）：空/超长消息直接拒绝
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(SocialErrorCode.GROUP_MESSAGE_BLANK);
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new BusinessException(SocialErrorCode.GROUP_MESSAGE_TOO_LONG);
        }
        // 权限校验（P0）：仅群成员可发言，与 WebSocket 路径保持一致
        GroupMember member = groupMemberMapper.findMember(groupId, userId);
        if (member == null) {
            throw new BusinessException(SocialErrorCode.NOT_GROUP_MEMBER);
        }

        // 内容审核：命中敏感词则记录日志并抛 CONTENT_VIOLATION（不入库）
        AuditResult auditResult = contentAuditService.audit(content, "group_chat", userId);
        contentAuditService.recordAuditLog(userId, "group_chat", null, content, auditResult);
        if (!auditResult.isPassed()) {
            throw new BusinessException(SocialErrorCode.CONTENT_VIOLATION);
        }

        GroupMessage message = new GroupMessage();
        message.setGroupId(groupId);
        message.setUserId(userId);
        message.setUserName(userName);
        message.setContent(content);
        message.setMediaType("text");
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());

        groupMessageMapper.insert(message);

        // 数据池自动采集：群聊消息，metadata 记录 groupId
        if (eventPublisher != null) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("groupId", groupId);
            eventPublisher.publishEvent(new UserDataCaptureEvent(
                    userId, "group_chat", content, meta,
                    "gmsg:" + message.getId(), message.getCreatedAt()));
        }
    }

    @Override
    public List<GroupMessage> getGroupMessages(Long groupId, Long userId, int limit) {
        // P1-4 修复：仅群成员可读取群历史消息。原实现直接按 groupId 返回，
        // 任意登录用户遍历 groupId 即可读取非公开群的聊天记录。
        if (groupMemberMapper.findMember(groupId, userId) == null) {
            throw new BusinessException(SocialErrorCode.NOT_GROUP_MEMBER);
        }
        return groupMessageMapper.getMessagesByGroupId(groupId, limit);
    }

    @Override
    public void deleteGroup(Long groupId, Long userId) {
        // P0 安全修复：先校验群存在，再校验操作者为群主
        GroupChat group = groupChatMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(SocialErrorCode.GROUP_NOT_FOUND);
        }
        if (!group.getOwnerId().equals(userId)) {
            throw new BusinessException(SocialErrorCode.NOT_GROUP_OWNER);
        }

        groupMessageMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<GroupMessage>()
                .eq("group_id", groupId)
        );
        groupMemberMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<GroupMember>()
                .eq("group_id", groupId)
        );
        groupChatMapper.deleteById(groupId);
        logger.info("[群聊] 群主 {} 删除群聊: {}", userId, groupId);
    }

    @Override
    public List<Map<String, Object>> recommendFriends(Long userId) {
        List<UserInterest> userInterests = userInterestMapper.getInterestsByUserId(userId);
        Set<String> userInterestSet = userInterests.stream()
            .map(UserInterest::getInterest)
            .collect(Collectors.toSet());
        
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        Map<Long, Double> scoreMap = new HashMap<>();
        
        List<UserInterest> allInterests = userInterestMapper.selectList(null);
        for (UserInterest interest : allInterests) {
            if (!interest.getUserId().equals(userId)) {
                if (userInterestSet.contains(interest.getInterest())) {
                    scoreMap.merge(interest.getUserId(), interest.getWeight().doubleValue(), Double::sum);
                }
            }
        }
        
        scoreMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(10)
            .forEach(entry -> {
                Map<String, Object> rec = new HashMap<>();
                rec.put("userId", entry.getKey());
                rec.put("score", entry.getValue());
                rec.put("reason", "共同兴趣");
                recommendations.add(rec);
            });
        
        return recommendations;
    }

    @Override
    public GroupChat updateAnnouncement(Long groupId, Long userId, String announcement) {
        // P0 安全修复：群不存在抛 GROUP_NOT_FOUND；非群主抛 NOT_GROUP_OWNER
        GroupChat group = groupChatMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(SocialErrorCode.GROUP_NOT_FOUND);
        }
        if (!group.getOwnerId().equals(userId)) {
            throw new BusinessException(SocialErrorCode.NOT_GROUP_OWNER);
        }
        group.setAnnouncement(announcement);
        groupChatMapper.updateById(group);
        logger.info("[群聊] 群主 {} 更新群公告 - groupId: {}", userId, groupId);
        return group;
    }

    @Override
    public GroupChat updateSettings(Long groupId, Long userId, String settings) {
        // P0 安全修复：群不存在抛 GROUP_NOT_FOUND；非群主抛 NOT_GROUP_OWNER
        GroupChat group = groupChatMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(SocialErrorCode.GROUP_NOT_FOUND);
        }
        if (!group.getOwnerId().equals(userId)) {
            throw new BusinessException(SocialErrorCode.NOT_GROUP_OWNER);
        }
        group.setSettings(settings);
        groupChatMapper.updateById(group);
        logger.info("[群聊] 群主 {} 更新群设置 - groupId: {}", userId, groupId);
        return group;
    }

    @Override
    public GroupChat getGroupWithDetails(Long groupId) {
        return groupChatMapper.selectById(groupId);
    }

    @Override
    public List<Map<String, Object>> getCommunityActivityRanking(int limit) {
        // 夹取 limit，防止越界/超大分页参数
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<GroupChat> groups = groupChatMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<GroupChat>()
                .orderByDesc("activity_score")
                .last("LIMIT " + safeLimit)
        );
        
        return groups.stream().map(group -> {
            Map<String, Object> item = new HashMap<>();
            item.put("groupId", group.getId());
            item.put("groupName", group.getName());
            item.put("memberCount", group.getMemberCount());
            item.put("activityScore", group.getActivityScore());
            item.put("lastActivityTime", group.getLastActivityTime());
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    public void updateActivityScore(Long groupId) {
        GroupChat group = groupChatMapper.selectById(groupId);
        if (group != null) {
            int currentScore = group.getActivityScore() != null ? group.getActivityScore() : 0;
            group.setActivityScore(currentScore + 1);
            group.setLastActivityTime(LocalDateTime.now());
            groupChatMapper.updateById(group);
        }
    }

    @Override
    public GroupBot createGroupBot(Long groupId, String botName, String description) {
        GroupBot bot = new GroupBot();
        bot.setGroupId(groupId);
        bot.setBotName(botName);
        bot.setBotDescription(description);
        bot.setEnabled(true);
        bot.setCreatedAt(LocalDateTime.now());
        
        groupBotMapper.insert(bot);
        logger.info("[群机器人] 创建群机器人 - groupId: {}, botName: {}", groupId, botName);
        return bot;
    }

    @Override
    public List<GroupBot> getGroupBots(Long groupId) {
        return groupBotMapper.findEnabledBotsByGroupId(groupId);
    }

    @Override
    public GroupBot getGroupBotById(Long botId) {
        return groupBotMapper.selectById(botId);
    }

    @Override
    public GroupBot updateGroupBot(Long botId, Map<String, Object> updates) {
        GroupBot bot = groupBotMapper.selectById(botId);
        if (bot != null) {
            updates.forEach((key, value) -> {
                switch (key) {
                    case "botName": bot.setBotName((String) value); break;
                    case "botDescription": bot.setBotDescription((String) value); break;
                    case "enabled": bot.setEnabled((Boolean) value); break;
                    case "triggerKeywords": bot.setTriggerKeywords((String) value); break;
                    case "autoReplyRules": bot.setAutoReplyRules((String) value); break;
                }
            });
            groupBotMapper.updateById(bot);
        }
        return bot;
    }

    @Override
    public void deleteGroupBot(Long botId) {
        groupBotMapper.deleteById(botId);
    }

    @Override
    public String processBotResponse(Long groupId, String message) {
        List<GroupBot> bots = groupBotMapper.findEnabledBotsByGroupId(groupId);
        
        for (GroupBot bot : bots) {
            String keywords = bot.getTriggerKeywords();
            if (keywords != null && !keywords.isEmpty()) {
                String[] keywordArray = keywords.split(",");
                for (String keyword : keywordArray) {
                    if (message.contains(keyword.trim())) {
                        return "【" + bot.getBotName() + "】: " + generateAutoReply(bot, message);
                    }
                }
            }
        }
        return null;
    }

    private String generateAutoReply(GroupBot bot, String message) {
        String rules = bot.getAutoReplyRules();
        if (rules != null && !rules.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, String> ruleMap = mapper.readValue(rules, Map.class);
                for (Map.Entry<String, String> entry : ruleMap.entrySet()) {
                    if (message.contains(entry.getKey())) {
                        return entry.getValue();
                    }
                }
            } catch (Exception e) {
                logger.warn("解析自动回复规则失败", e);
            }
        }
        
        return "你好！我是" + bot.getBotName() + "，很高兴为你服务！";
    }

    @Override
    public List<Map<String, Object>> recommendFriendsAdvanced(Long userId) {
        List<UserInterest> userInterests = userInterestMapper.getInterestsByUserId(userId);
        Set<String> userInterestSet = userInterests.stream()
            .map(UserInterest::getInterest)
            .collect(Collectors.toSet());
        
        List<Map<String, Object>> recommendations = new ArrayList<>();
        Map<Long, Double> scoreMap = new HashMap<>();
        
        List<UserInterest> allInterests = userInterestMapper.selectList(null);
        for (UserInterest interest : allInterests) {
            if (!interest.getUserId().equals(userId)) {
                if (userInterestSet.contains(interest.getInterest())) {
                    scoreMap.merge(interest.getUserId(), interest.getWeight().doubleValue() * 2, Double::sum);
                }
            }
        }
        
        List<GroupMember> userGroups = groupMemberMapper.getMembersByUserId(userId);
        Set<Long> userGroupIds = userGroups.stream()
            .map(GroupMember::getGroupId)
            .collect(Collectors.toSet());
        
        for (GroupMember member : groupMemberMapper.selectList(null)) {
            if (!member.getUserId().equals(userId) && userGroupIds.contains(member.getGroupId())) {
                scoreMap.merge(member.getUserId(), 5.0, Double::sum);
            }
        }
        
        scoreMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(10)
            .forEach(entry -> {
                Map<String, Object> rec = new HashMap<>();
                rec.put("userId", entry.getKey());
                rec.put("score", entry.getValue());
                rec.put("reason", entry.getValue() > 10 ? "共同群聊和兴趣" : "共同兴趣");
                recommendations.add(rec);
            });
        
        return recommendations;
    }

    // ===== 多 AI 角色同场群聊：群成员管理 =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addAiCharacterToGroup(Long groupId, Long userId, Long characterId) {
        GroupChat group = groupChatMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(SocialErrorCode.GROUP_NOT_FOUND);
        }
        // 校验操作者为群成员
        GroupMember member = groupMemberMapper.findMember(groupId, userId);
        if (member == null) {
            throw new BusinessException(SocialErrorCode.NOT_GROUP_MEMBER);
        }
        AiCharacter character = aiCharacterMapper.selectById(characterId);
        if (character == null) {
            throw new BusinessException(SocialErrorCode.GROUP_NOT_FOUND);
        }

        GroupAiMember existing = groupAiMemberMapper.findByGroupAndCharacter(groupId, characterId);
        if (existing != null && Boolean.TRUE.equals(existing.getIsActive())) {
            // 幂等：已在群内则直接返回
            return;
        }
        if (existing != null) {
            // 曾被移除，重新激活
            existing.setIsActive(true);
            existing.setAddedBy(userId);
            existing.setCreatedAt(LocalDateTime.now());
            groupAiMemberMapper.updateById(existing);
            return;
        }

        GroupAiMember aiMember = new GroupAiMember();
        aiMember.setGroupId(groupId);
        aiMember.setCharacterId(characterId);
        aiMember.setAddedBy(userId);
        aiMember.setCreatedAt(LocalDateTime.now());
        aiMember.setIsActive(true);
        groupAiMemberMapper.insert(aiMember);
        logger.info("[群聊AI] 用户 {} 将角色 {} 加入群 {}", userId, characterId, groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAiCharacterFromGroup(Long groupId, Long userId, Long characterId) {
        GroupChat group = groupChatMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(SocialErrorCode.GROUP_NOT_FOUND);
        }
        // 校验群主
        if (!group.getOwnerId().equals(userId)) {
            throw new BusinessException(SocialErrorCode.NOT_GROUP_OWNER);
        }
        GroupAiMember existing = groupAiMemberMapper.findByGroupAndCharacter(groupId, characterId);
        if (existing == null) {
            return;
        }
        existing.setIsActive(false);
        groupAiMemberMapper.updateById(existing);
        logger.info("[群聊AI] 群主 {} 将角色 {} 移出群 {}", userId, characterId, groupId);
    }

    @Override
    public List<GroupAiMember> listAiCharactersInGroup(Long groupId) {
        return groupAiMemberMapper.findActiveByGroupId(groupId);
    }
}
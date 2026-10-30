package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.SocialErrorCode;
import com.example.aichat.entity.AiCharacter;
import com.example.aichat.entity.GroupAiMember;
import com.example.aichat.entity.GroupChat;
import com.example.aichat.entity.GroupMember;
import com.example.aichat.entity.GroupMessage;
import com.example.aichat.mapper.AiCharacterMapper;
import com.example.aichat.mapper.GroupAiMemberMapper;
import com.example.aichat.mapper.GroupChatMapper;
import com.example.aichat.mapper.GroupMemberMapper;
import com.example.aichat.mapper.GroupMessageMapper;
import com.example.aichat.service.GroupAiChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多 AI 角色同场群聊编排实现（简化版，mock 回复）。
 *
 * 轮流制：每个群维护一个发言游标（内存态），triggerAiRound 从当前游标开始，
 * 让所有激活 AI 角色各发言一条，游标后移一位，下一轮从下一个角色开始。
 */
@Service
public class GroupAiChatServiceImpl implements GroupAiChatService {

    private static final Logger logger = LoggerFactory.getLogger(GroupAiChatServiceImpl.class);

    private final GroupAiMemberMapper groupAiMemberMapper;
    private final AiCharacterMapper aiCharacterMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final GroupChatMapper groupChatMapper;
    private final GroupMemberMapper groupMemberMapper;

    /** 每群发言游标（简化版内存态，重启后从 0 开始，不影响正确性）。 */
    private final ConcurrentHashMap<Long, AtomicInteger> roundPointer = new ConcurrentHashMap<>();

    public GroupAiChatServiceImpl(GroupAiMemberMapper groupAiMemberMapper,
                                  AiCharacterMapper aiCharacterMapper,
                                  GroupMessageMapper groupMessageMapper,
                                  GroupChatMapper groupChatMapper,
                                  GroupMemberMapper groupMemberMapper) {
        this.groupAiMemberMapper = groupAiMemberMapper;
        this.aiCharacterMapper = aiCharacterMapper;
        this.groupMessageMapper = groupMessageMapper;
        this.groupChatMapper = groupChatMapper;
        this.groupMemberMapper = groupMemberMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> triggerAiRound(Long groupId, Long userId, String triggerMessage) {
        GroupChat group = groupChatMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(SocialErrorCode.GROUP_NOT_FOUND);
        }
        GroupMember member = groupMemberMapper.findMember(groupId, userId);
        if (member == null) {
            throw new BusinessException(SocialErrorCode.NOT_GROUP_MEMBER);
        }

        List<GroupAiMember> active = groupAiMemberMapper.findActiveByGroupId(groupId);
        List<Map<String, Object>> replies = new ArrayList<>();
        if (active.isEmpty()) {
            return replies;
        }

        int size = active.size();
        int start = currentPointer(groupId).get() % size;

        List<GroupMessage> history = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            GroupAiMember aiMember = active.get((start + i) % size);
            AiCharacter character = aiCharacterMapper.selectById(aiMember.getCharacterId());
            if (character == null) {
                continue;
            }
            String content = generateAiReply(character, triggerMessage, history);

            GroupMessage message = new GroupMessage();
            message.setGroupId(groupId);
            message.setUserId(character.getId());          // senderId = characterId
            message.setUserName(character.getName());       // senderType=ai 由前端按 userId 归属识别
            message.setContent(content);
            message.setMediaType("text");
            message.setIsRead(false);
            message.setCreatedAt(LocalDateTime.now());
            groupMessageMapper.insert(message);
            history.add(message);

            Map<String, Object> reply = new HashMap<>();
            reply.put("characterId", character.getId());
            reply.put("characterName", character.getName());
            reply.put("content", content);
            replies.add(reply);
        }

        // 游标后移一位：下一轮从下一个 AI 角色开始
        currentPointer(groupId).incrementAndGet();
        logger.info("[群聊AI] 群 {} 触发一轮发言，共 {} 条 AI 回复（触发者 {}）", groupId, replies.size(), userId);
        return replies;
    }

    @Override
    public GroupAiMember getNextAiCharacter(Long groupId) {
        List<GroupAiMember> active = groupAiMemberMapper.findActiveByGroupId(groupId);
        if (active.isEmpty()) {
            return null;
        }
        int idx = currentPointer(groupId).get() % active.size();
        return active.get(idx);
    }

    @Override
    public String generateAiReply(AiCharacter character, String triggerMessage, List<GroupMessage> history) {
        String name = character.getName() == null ? "神秘角色" : character.getName();
        String personality = character.getPersonality() == null ? "" : character.getPersonality().trim();
        String topic = (triggerMessage == null || triggerMessage.isBlank()) ? "大家的话题" : triggerMessage.trim();

        String opinion = pickOpinion(name, personality);
        StringBuilder sb = new StringBuilder();
        sb.append("我是").append(name);
        if (!personality.isEmpty()) {
            sb.append("（").append(personality).append("）");
        }
        sb.append("，关于「").append(topic).append("」，").append(opinion);

        if (history != null && !history.isEmpty()) {
            GroupMessage last = history.get(history.size() - 1);
            if (last != null && last.getUserName() != null) {
                sb.append(" 顺便回应一下 ").append(last.getUserName()).append(" 的看法。");
            }
        }
        return sb.toString();
    }

    /** 根据角色名+性格确定性地挑选一种口吻，保证不同角色风格不同。 */
    private String pickOpinion(String name, String personality) {
        String[] opinions = {
                "我觉得这件事很值得认真聊聊，我的立场是支持积极尝试。",
                "从我的角度看，需要权衡利弊，稳妥推进会更好。",
                "哈哈，这个话题有意思，我倾向于轻松一点看待它。",
                "让我仔细分析一下，关键在于把细节做到位。",
                "凭直觉说，我更相信第一印象和情感的力量。",
                "理性地讲，我们应该用事实和数据来支撑结论。"
        };
        int h = Math.abs((name + "|" + personality).hashCode());
        return opinions[h % opinions.length];
    }

    private AtomicInteger currentPointer(Long groupId) {
        return roundPointer.computeIfAbsent(groupId, k -> new AtomicInteger(0));
    }
}

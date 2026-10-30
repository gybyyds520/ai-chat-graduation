package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aichat.entity.ChatMessage;
import com.example.aichat.mapper.ChatMessageMapper;
import com.example.aichat.service.ChatMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageServiceImpl.class);

    /** 消息撤回占位文案。 */
    public static final String RECALLED_PLACEHOLDER = "消息已撤回";

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Override
    public void saveMessage(String sessionId, String senderType, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSenderType(senderType);
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(message);
        logger.debug("保存消息 - sessionId: {}, senderType: {}", sessionId, senderType);
    }

    @Override
    public void saveMessage(String sessionId, Long userId, Long characterId, String senderType, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setCharacterId(characterId);
        message.setSenderType(senderType);
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(message);
        logger.debug("保存消息 - sessionId: {}, userId: {}, characterId: {}, senderType: {}",
                     sessionId, userId, characterId, senderType);
    }

    @Override
    public List<ChatMessage> getRecentMessages(String sessionId, int limit) {
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId)
               .orderByDesc("create_time")
               .last("LIMIT " + limit);
        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);
        logger.debug("获取聊天历史 - sessionId: {}, 消息数量: {}", sessionId, messages.size());
        return messages;
    }

    @Override
    public int deleteSessionMessages(String sessionId, Long userId) {
        // 仅删除归属当前用户的该会话消息，防止越权清空他人会话
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId).eq("user_id", userId);
        int deleted = chatMessageMapper.delete(wrapper);
        logger.info("删除会话消息 - sessionId: {}, userId: {}, 删除条数: {}", sessionId, userId, deleted);
        return deleted;
    }

    @Override
    public boolean deleteMessage(Long messageId, Long userId) {
        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null || message.getUserId() == null || !userId.equals(message.getUserId())) {
            return false;
        }
        return chatMessageMapper.deleteById(messageId) > 0;
    }

    // ==================== M1 社交聊天体验 ====================

    @Override
    public ChatMessage findById(Long messageId) {
        if (messageId == null) {
            return null;
        }
        return chatMessageMapper.selectById(messageId);
    }

    @Override
    public ChatMessage findOwnedMessage(Long messageId, Long userId) {
        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null || message.getUserId() == null || !userId.equals(message.getUserId())) {
            return null;
        }
        return message;
    }

    @Override
    public boolean updateContent(Long messageId, String content) {
        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null) {
            return false;
        }
        message.setContent(content);
        return chatMessageMapper.updateById(message) > 0;
    }

    @Override
    public int deleteAfterMessage(String sessionId, Long messageId) {
        ChatMessage anchor = chatMessageMapper.selectById(messageId);
        if (anchor == null) {
            return 0;
        }
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId)
               .gt("create_time", anchor.getCreateTime());
        int deleted = chatMessageMapper.delete(wrapper);
        logger.info("删除下游消息 - sessionId: {}, anchorMessageId: {}, 删除条数: {}", sessionId, messageId, deleted);
        return deleted;
    }

    @Override
    public void deleteById(Long messageId) {
        chatMessageMapper.deleteById(messageId);
    }

    @Override
    public boolean recallMessage(Long messageId) {
        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null) {
            return false;
        }
        message.setContent(RECALLED_PLACEHOLDER);
        return chatMessageMapper.updateById(message) > 0;
    }
}

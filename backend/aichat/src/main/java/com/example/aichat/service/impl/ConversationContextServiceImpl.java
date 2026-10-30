package com.example.aichat.service.impl;

import com.example.aichat.entity.ConversationContext;
import com.example.aichat.mapper.ConversationContextMapper;
import com.example.aichat.service.ConversationContextService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConversationContextServiceImpl implements ConversationContextService {

    private final ConversationContextMapper conversationContextMapper;

    public ConversationContextServiceImpl(ConversationContextMapper conversationContextMapper) {
        this.conversationContextMapper = conversationContextMapper;
    }

    @Override
    public void setContext(String sessionId, Long userId, Long characterId, String key, String value) {
        ConversationContext existing = conversationContextMapper.getContextByKey(sessionId, key);
        
        if (existing != null) {
            existing.setContextValue(value);
            existing.setExpireTime(LocalDateTime.now().plusHours(2));
            conversationContextMapper.updateById(existing);
        } else {
            ConversationContext context = new ConversationContext();
            context.setSessionId(sessionId);
            context.setUserId(userId);
            context.setCharacterId(characterId);
            context.setContextKey(key);
            context.setContextValue(value);
            context.setExpireTime(LocalDateTime.now().plusHours(2));
            context.setCreatedAt(LocalDateTime.now());
            conversationContextMapper.insert(context);
        }
    }

    @Override
    public String getContext(String sessionId, String key) {
        ConversationContext context = conversationContextMapper.getContextByKey(sessionId, key);
        return context != null ? context.getContextValue() : null;
    }

    @Override
    public Map<String, String> getAllContext(String sessionId) {
        Map<String, String> contextMap = new HashMap<>();
        List<ConversationContext> contexts = conversationContextMapper.getContextBySessionId(sessionId);
        contexts.forEach(c -> contextMap.put(c.getContextKey(), c.getContextValue()));
        return contextMap;
    }

    @Override
    public void removeContext(String sessionId, String key) {
        ConversationContext context = conversationContextMapper.getContextByKey(sessionId, key);
        if (context != null) {
            conversationContextMapper.deleteById(context.getId());
        }
    }

    @Override
    public void clearSessionContext(String sessionId) {
        conversationContextMapper.deleteBySessionId(sessionId);
    }

    @Override
    public String buildContextPrompt(String sessionId) {
        Map<String, String> contextMap = getAllContext(sessionId);
        if (contextMap.isEmpty()) {
            return "";
        }
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("\n\n对话上下文信息：\n");
        
        contextMap.forEach((key, value) -> {
            prompt.append("- ").append(key).append(": ").append(value).append("\n");
        });
        
        prompt.append("\n请根据以上上下文信息进行回答，保持对话的连贯性。");
        return prompt.toString();
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanExpiredContext() {
        conversationContextMapper.deleteExpired(LocalDateTime.now());
    }
}
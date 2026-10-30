package com.example.aichat.service.impl;

import com.example.aichat.entity.ConversationSummary;
import com.example.aichat.entity.ChatMessage;
import com.example.aichat.mapper.ConversationSummaryMapper;
import com.example.aichat.mapper.ChatMessageMapper;
import com.example.aichat.service.ConversationSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ConversationSummaryServiceImpl implements ConversationSummaryService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationSummaryServiceImpl.class);
    
    private final ConversationSummaryMapper conversationSummaryMapper;
    private final ChatMessageMapper chatMessageMapper;
    
    private static final Pattern TOPIC_PATTERN = Pattern.compile("(关于|讨论|聊聊|谈谈|话题|主题)([^，。！？]+)");
    private static final Set<String> COMMON_TOPICS = Set.of(
        "天气", "时间", "日期", "工作", "学习", "生活", "健康", "娱乐", "科技", "新闻", "美食", "旅行"
    );

    public ConversationSummaryServiceImpl(ConversationSummaryMapper conversationSummaryMapper,
                                          ChatMessageMapper chatMessageMapper) {
        this.conversationSummaryMapper = conversationSummaryMapper;
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    public String generateSummary(String sessionId, Long userId, Long characterId) {
        List<ChatMessage> messages = chatMessageMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMessage>()
                .eq("session_id", sessionId)
                .orderByAsc("created_at")
        );
        
        if (messages.isEmpty()) {
            return "";
        }
        
        StringBuilder summary = new StringBuilder();
        List<String> topics = new ArrayList<>();
        
        Map<String, Integer> topicCounts = new HashMap<>();
        
        for (ChatMessage message : messages) {
            String content = message.getContent();
            
            Matcher topicMatcher = TOPIC_PATTERN.matcher(content);
            if (topicMatcher.find()) {
                String topic = topicMatcher.group(2).trim();
                topicCounts.merge(topic, 1, Integer::sum);
            }
            
            for (String commonTopic : COMMON_TOPICS) {
                if (content.contains(commonTopic)) {
                    topicCounts.merge(commonTopic, 1, Integer::sum);
                }
            }
        }
        
        List<String> sortedTopics = topicCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .toList();
        
        topics.addAll(sortedTopics);
        
        if (messages.size() <= 5) {
            for (ChatMessage message : messages) {
                summary.append(message.getSenderType()).append(": ").append(message.getContent()).append("\n");
            }
        } else {
            ChatMessage first = messages.get(0);
            ChatMessage last = messages.get(messages.size() - 1);
            
            summary.append("对话开始于").append(first.getCreateTime()).append("\n");
            summary.append("首次消息: ").append(first.getContent()).append("\n");
            summary.append("...\n");
            summary.append("最后消息: ").append(last.getContent()).append("\n");
            summary.append("共").append(messages.size()).append("条消息");
        }
        
        ConversationSummary convSummary = new ConversationSummary();
        convSummary.setSessionId(sessionId);
        convSummary.setUserId(userId);
        convSummary.setCharacterId(characterId);
        convSummary.setSummary(summary.toString());
        convSummary.setTopics(String.join(",", topics));
        convSummary.setMessageCount(messages.size());
        convSummary.setCreatedAt(LocalDateTime.now());
        
        conversationSummaryMapper.insert(convSummary);
        
        logger.info("[对话摘要] 生成摘要 - sessionId: {}, topics: {}", sessionId, topics);
        return summary.toString();
    }

    @Override
    public String getLatestSummary(String sessionId) {
        ConversationSummary summary = conversationSummaryMapper.getLatestBySessionId(sessionId);
        return summary != null ? summary.getSummary() : "";
    }

    @Override
    public List<Map<String, Object>> getSummariesByUserId(Long userId, int limit) {
        List<ConversationSummary> summaries = conversationSummaryMapper.getByUserId(userId, limit);
        return summaries.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("sessionId", s.getSessionId());
            map.put("summary", s.getSummary());
            map.put("topics", Arrays.asList(s.getTopics().split(",")));
            map.put("messageCount", s.getMessageCount());
            map.put("createdAt", s.getCreatedAt());
            return map;
        }).toList();
    }

    @Override
    public List<String> extractTopics(String sessionId) {
        ConversationSummary summary = conversationSummaryMapper.getLatestBySessionId(sessionId);
        if (summary != null && summary.getTopics() != null) {
            return Arrays.asList(summary.getTopics().split(","));
        }
        return Collections.emptyList();
    }

    @Override
    public void updateTopics(String sessionId, List<String> topics) {
        ConversationSummary summary = conversationSummaryMapper.getLatestBySessionId(sessionId);
        if (summary != null) {
            summary.setTopics(String.join(",", topics));
            conversationSummaryMapper.updateById(summary);
        }
    }
}
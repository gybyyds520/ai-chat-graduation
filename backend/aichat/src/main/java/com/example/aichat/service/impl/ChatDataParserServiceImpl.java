package com.example.aichat.service.impl;

import com.example.aichat.service.ChatDataParserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatDataParserServiceImpl implements ChatDataParserService {

    private static final Logger logger = LoggerFactory.getLogger(ChatDataParserServiceImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern TXT_PATTERN1 = Pattern.compile("([^:：]+)[：:](.+)");
    private static final Pattern TXT_PATTERN2 = Pattern.compile("(\\[.+?\\])\\s*(.+)");
    private static final Pattern TXT_PATTERN3 = Pattern.compile("(\\(.+?\\))\\s*(.+)");

    @Override
    public List<Map<String, Object>> parse(String content, String format) {
        if (format == null || format.isEmpty()) {
            format = detectFormat(content);
        }
        
        logger.info("解析聊天数据，格式: {}", format);
        
        switch (format.toLowerCase()) {
            case "json":
                return parseJson(content);
            case "txt":
            default:
                return parseTxt(content);
        }
    }

    @Override
    public List<Map<String, Object>> parseTxt(String content) {
        List<Map<String, Object>> messages = new ArrayList<>();
        String[] lines = content.split("\\r?\\n");
        
        String currentSpeaker = "未知";
        StringBuilder currentContent = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            Matcher matcher = TXT_PATTERN1.matcher(line);
            if (matcher.matches()) {
                if (currentContent.length() > 0) {
                    addMessage(messages, currentSpeaker, currentContent.toString().trim());
                    currentContent = new StringBuilder();
                }
                currentSpeaker = matcher.group(1).trim();
                currentContent.append(matcher.group(2).trim());
            } else {
                matcher = TXT_PATTERN2.matcher(line);
                if (matcher.matches()) {
                    if (currentContent.length() > 0) {
                        addMessage(messages, currentSpeaker, currentContent.toString().trim());
                        currentContent = new StringBuilder();
                    }
                    currentSpeaker = matcher.group(1).replace("[", "").replace("]", "").trim();
                    currentContent.append(matcher.group(2).trim());
                } else {
                    matcher = TXT_PATTERN3.matcher(line);
                    if (matcher.matches()) {
                        if (currentContent.length() > 0) {
                            addMessage(messages, currentSpeaker, currentContent.toString().trim());
                            currentContent = new StringBuilder();
                        }
                        currentSpeaker = matcher.group(1).replace("(", "").replace(")", "").trim();
                        currentContent.append(matcher.group(2).trim());
                    } else {
                        if (currentContent.length() > 0) {
                            currentContent.append(" ").append(line);
                        } else {
                            currentSpeaker = "未知";
                            currentContent.append(line);
                        }
                    }
                }
            }
        }
        
        if (currentContent.length() > 0) {
            addMessage(messages, currentSpeaker, currentContent.toString().trim());
        }
        
        logger.info("TXT解析完成，共解析 {} 条消息", messages.size());
        return messages;
    }

    @Override
    public List<Map<String, Object>> parseJson(String content) {
        List<Map<String, Object>> messages = new ArrayList<>();
        
        try {
            Object parsed = objectMapper.readValue(content, Object.class);
            
            if (parsed instanceof List) {
                List<?> list = (List<?>) parsed;
                for (Object item : list) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) item;
                        Map<String, Object> message = new HashMap<>();
                        message.put("sender", map.getOrDefault("sender", map.get("username")));
                        message.put("content", map.get("content"));
                        message.put("timestamp", LocalDateTime.now().toString());
                        messages.add(message);
                    }
                }
            } else if (parsed instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) parsed;
                if (map.containsKey("messages") && map.get("messages") instanceof List) {
                    List<?> list = (List<?>) map.get("messages");
                    for (Object item : list) {
                        if (item instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> itemMap = (Map<String, Object>) item;
                            Map<String, Object> message = new HashMap<>();
                            message.put("sender", itemMap.getOrDefault("sender", itemMap.get("username")));
                            message.put("content", itemMap.get("content"));
                            message.put("timestamp", LocalDateTime.now().toString());
                            messages.add(message);
                        }
                    }
                }
            }
            
            logger.info("JSON解析完成，共解析 {} 条消息", messages.size());
        } catch (Exception e) {
            logger.error("JSON解析失败: {}", e.getMessage());
            return parseTxt(content);
        }
        
        return messages;
    }

    private void addMessage(List<Map<String, Object>> messages, String speaker, String content) {
        if (speaker == null || speaker.isEmpty()) speaker = "未知";
        if (content == null || content.isEmpty()) return;
        
        Map<String, Object> message = new HashMap<>();
        message.put("sender", speaker);
        message.put("content", content);
        message.put("timestamp", LocalDateTime.now().toString());
        messages.add(message);
    }

    private String detectFormat(String content) {
        if (content.trim().startsWith("{")) return "json";
        if (content.trim().startsWith("[")) return "json";
        return "txt";
    }
}
package com.example.aichat.service.impl;

import com.example.aichat.entity.CharacterMemory;
import com.example.aichat.mapper.CharacterMemoryMapper;
import com.example.aichat.service.CharacterMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CharacterMemoryServiceImpl implements CharacterMemoryService {

    private static final Logger logger = LoggerFactory.getLogger(CharacterMemoryServiceImpl.class);
    
    private final CharacterMemoryMapper characterMemoryMapper;
    
    private static final Pattern NAME_PATTERN = Pattern.compile("我叫([^，。！？\\s]+)");
    private static final Pattern BIRTHDAY_PATTERN = Pattern.compile("(生日|出生|生日是)(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2})");
    private static final Pattern LIKES_PATTERN = Pattern.compile("(喜欢|爱好|兴趣)([^，。！？]+)");
    private static final Pattern DISLIKES_PATTERN = Pattern.compile("(讨厌|不喜欢)([^，。！？]+)");
    private static final Pattern RELATIONSHIP_PATTERN = Pattern.compile("(我们是|我想让你做|你是我的)([^，。！？]+)");

    public CharacterMemoryServiceImpl(CharacterMemoryMapper characterMemoryMapper) {
        this.characterMemoryMapper = characterMemoryMapper;
    }

    @Override
    public void addMemory(Long characterId, Long userId, String key, String value, String type, double importance) {
        CharacterMemory existing = characterMemoryMapper.findByKey(characterId, key);
        
        if (existing != null) {
            existing.setMemoryValue(value);
            existing.setImportance(Math.max(existing.getImportance(), importance));
            existing.setLastAccessed(LocalDateTime.now());
            characterMemoryMapper.updateById(existing);
        } else {
            CharacterMemory memory = new CharacterMemory();
            memory.setCharacterId(characterId);
            memory.setUserId(userId);
            memory.setMemoryKey(key);
            memory.setMemoryValue(value);
            memory.setMemoryType(type);
            memory.setImportance(importance);
            memory.setLastAccessed(LocalDateTime.now());
            memory.setCreatedAt(LocalDateTime.now());
            characterMemoryMapper.insert(memory);
        }
        logger.info("[角色记忆] 添加记忆 - characterId: {}, key: {}, value: {}", characterId, key, value);
    }

    @Override
    public String getMemory(Long characterId, String key) {
        CharacterMemory memory = characterMemoryMapper.findByKey(characterId, key);
        if (memory != null) {
            memory.setLastAccessed(LocalDateTime.now());
            characterMemoryMapper.updateById(memory);
            return memory.getMemoryValue();
        }
        return null;
    }

    @Override
    public List<Map<String, Object>> getImportantMemories(Long characterId, int limit) {
        List<CharacterMemory> memories = characterMemoryMapper.getImportantMemories(characterId, limit);
        return memories.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("key", m.getMemoryKey());
            map.put("value", m.getMemoryValue());
            map.put("type", m.getMemoryType());
            map.put("importance", m.getImportance());
            return map;
        }).toList();
    }

    @Override
    public void updateMemory(Long characterId, String key, String value) {
        CharacterMemory memory = characterMemoryMapper.findByKey(characterId, key);
        if (memory != null) {
            memory.setMemoryValue(value);
            memory.setLastAccessed(LocalDateTime.now());
            characterMemoryMapper.updateById(memory);
        }
    }

    @Override
    public void removeMemory(Long characterId, String key) {
        CharacterMemory memory = characterMemoryMapper.findByKey(characterId, key);
        if (memory != null) {
            characterMemoryMapper.deleteById(memory.getId());
        }
    }

    @Override
    public String buildMemoryPrompt(Long characterId) {
        List<CharacterMemory> memories = characterMemoryMapper.getImportantMemories(characterId, 20);
        
        if (memories.isEmpty()) {
            return "";
        }
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("\n\n你与用户的记忆：\n");
        
        Map<String, StringBuilder> memoriesByType = new HashMap<>();
        
        memories.forEach(m -> {
            memoriesByType.computeIfAbsent(m.getMemoryType(), k -> new StringBuilder())
                .append("- ").append(m.getMemoryValue()).append("\n");
        });
        
        if (memoriesByType.containsKey("identity")) {
            prompt.append("【用户身份】\n").append(memoriesByType.get("identity"));
        }
        if (memoriesByType.containsKey("preference")) {
            prompt.append("【用户喜好】\n").append(memoriesByType.get("preference"));
        }
        if (memoriesByType.containsKey("event")) {
            prompt.append("【重要事件】\n").append(memoriesByType.get("event"));
        }
        if (memoriesByType.containsKey("relationship")) {
            prompt.append("【关系设定】\n").append(memoriesByType.get("relationship"));
        }
        
        prompt.append("\n请根据以上记忆与用户互动，保持一致性和亲切感。");
        return prompt.toString();
    }

    @Override
    public void extractMemoriesFromMessage(Long characterId, Long userId, String message) {
        Matcher nameMatcher = NAME_PATTERN.matcher(message);
        if (nameMatcher.find()) {
            addMemory(characterId, userId, "user_name", nameMatcher.group(1), "identity", 0.9);
        }
        
        Matcher birthdayMatcher = BIRTHDAY_PATTERN.matcher(message);
        if (birthdayMatcher.find()) {
            addMemory(characterId, userId, "birthday", birthdayMatcher.group(2), "event", 0.8);
        }
        
        Matcher likesMatcher = LIKES_PATTERN.matcher(message);
        if (likesMatcher.find()) {
            String existingLikes = getMemory(characterId, "likes");
            String newLikes = existingLikes != null ? existingLikes + "、" + likesMatcher.group(2) : likesMatcher.group(2);
            addMemory(characterId, userId, "likes", newLikes, "preference", 0.7);
        }
        
        Matcher dislikesMatcher = DISLIKES_PATTERN.matcher(message);
        if (dislikesMatcher.find()) {
            String existingDislikes = getMemory(characterId, "dislikes");
            String newDislikes = existingDislikes != null ? existingDislikes + "、" + dislikesMatcher.group(2) : dislikesMatcher.group(2);
            addMemory(characterId, userId, "dislikes", newDislikes, "preference", 0.6);
        }
        
        Matcher relationshipMatcher = RELATIONSHIP_PATTERN.matcher(message);
        if (relationshipMatcher.find()) {
            addMemory(characterId, userId, "relationship", relationshipMatcher.group(2), "relationship", 0.9);
        }
    }
}
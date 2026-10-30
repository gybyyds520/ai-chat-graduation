package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aichat.entity.CharacterPreference;
import com.example.aichat.mapper.CharacterPreferenceMapper;
import com.example.aichat.service.CharacterPreferenceService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CharacterPreferenceServiceImpl implements CharacterPreferenceService {

    @Autowired
    private CharacterPreferenceMapper characterPreferenceMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public CharacterPreference getPreferences(Long userId, Long characterId) {
        return characterPreferenceMapper.findByUserIdAndCharacterId(userId, characterId);
    }

    @Override
    @Transactional
    public CharacterPreference savePreferences(Long userId, Long characterId, String preferences) {
        CharacterPreference existing = characterPreferenceMapper.findByUserIdAndCharacterId(userId, characterId);

        if (existing != null) {
            existing.setPreferences(preferences);
            existing.setUpdatedAt(LocalDateTime.now());
            characterPreferenceMapper.updateById(existing);
            return existing;
        }

        CharacterPreference pref = new CharacterPreference();
        pref.setUserId(userId);
        pref.setCharacterId(characterId);
        pref.setPreferences(preferences);
        pref.setCreatedAt(LocalDateTime.now());
        pref.setUpdatedAt(LocalDateTime.now());

        characterPreferenceMapper.insert(pref);
        return pref;
    }

    @Override
    @Transactional
    public CharacterPreference updatePreferences(Long userId, Long characterId, Map<String, Object> updates) {
        CharacterPreference existing = characterPreferenceMapper.findByUserIdAndCharacterId(userId, characterId);

        if (existing == null) {
            existing = new CharacterPreference();
            existing.setUserId(userId);
            existing.setCharacterId(characterId);
            existing.setPreferences("{}");
            existing.setCreatedAt(LocalDateTime.now());
        }

        try {
            Map<String, Object> preferences = objectMapper.readValue(
                existing.getPreferences() != null ? existing.getPreferences() : "{}",
                new TypeReference<Map<String, Object>>() {}
            );
            preferences.putAll(updates);
            existing.setPreferences(objectMapper.writeValueAsString(preferences));
        } catch (Exception e) {
            existing.setPreferences("{}");
        }

        existing.setUpdatedAt(LocalDateTime.now());

        if (existing.getId() != null) {
            characterPreferenceMapper.updateById(existing);
        } else {
            characterPreferenceMapper.insert(existing);
        }

        return existing;
    }

    @Override
    @Transactional
    public void recordUsage(Long userId, Long characterId) {
        CharacterPreference existing = characterPreferenceMapper.findByUserIdAndCharacterId(userId, characterId);

        if (existing != null) {
            existing.setLastUsedAt(LocalDateTime.now());
            existing.setUpdatedAt(LocalDateTime.now());
            characterPreferenceMapper.updateById(existing);
        } else {
            CharacterPreference pref = new CharacterPreference();
            pref.setUserId(userId);
            pref.setCharacterId(characterId);
            pref.setPreferences("{}");
            pref.setLastUsedAt(LocalDateTime.now());
            pref.setCreatedAt(LocalDateTime.now());
            pref.setUpdatedAt(LocalDateTime.now());
            characterPreferenceMapper.insert(pref);
        }
    }

    @Override
    public List<Long> getRecentCharacterIds(Long userId, int limit) {
        return characterPreferenceMapper.findRecentCharacterIds(userId, limit);
    }

    @Override
    @Transactional
    public boolean deletePreferences(Long userId, Long characterId) {
        CharacterPreference pref = characterPreferenceMapper.findByUserIdAndCharacterId(userId, characterId);
        if (pref != null) {
            characterPreferenceMapper.deleteById(pref.getId());
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean deleteAllByUserId(Long userId) {
        QueryWrapper<CharacterPreference> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return characterPreferenceMapper.delete(wrapper) > 0;
    }

    @Override
    @Transactional
    public boolean deleteAllByCharacterId(Long characterId) {
        QueryWrapper<CharacterPreference> wrapper = new QueryWrapper<>();
        wrapper.eq("character_id", characterId);
        return characterPreferenceMapper.delete(wrapper) > 0;
    }
}
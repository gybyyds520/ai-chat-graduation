package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aichat.entity.CharacterInteraction;
import com.example.aichat.mapper.CharacterInteractionMapper;
import com.example.aichat.service.CharacterInteractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CharacterInteractionServiceImpl implements CharacterInteractionService {

    @Autowired
    private CharacterInteractionMapper characterInteractionMapper;

    @Override
    @Transactional
    public CharacterInteraction recordInteraction(Long userId, Long characterId) {
        CharacterInteraction existing = characterInteractionMapper.findByUserIdAndCharacterId(userId, characterId);

        if (existing != null) {
            existing.setInteractionCount(existing.getInteractionCount() != null ? existing.getInteractionCount() + 1 : 1);
            existing.setTotalMessages(existing.getTotalMessages() != null ? existing.getTotalMessages() + 2 : 2);
            existing.setLastInteractionAt(LocalDateTime.now());
            existing.setUpdatedAt(LocalDateTime.now());
            characterInteractionMapper.updateById(existing);
            return existing;
        }

        CharacterInteraction interaction = new CharacterInteraction();
        interaction.setUserId(userId);
        interaction.setCharacterId(characterId);
        interaction.setInteractionCount(1);
        interaction.setTotalMessages(2);
        interaction.setFirstInteractionAt(LocalDateTime.now());
        interaction.setLastInteractionAt(LocalDateTime.now());
        interaction.setCreatedAt(LocalDateTime.now());
        interaction.setUpdatedAt(LocalDateTime.now());

        characterInteractionMapper.insert(interaction);
        return interaction;
    }

    @Override
    public CharacterInteraction getInteraction(Long userId, Long characterId) {
        return characterInteractionMapper.findByUserIdAndCharacterId(userId, characterId);
    }

    @Override
    public List<CharacterInteraction> getUserInteractions(Long userId) {
        return characterInteractionMapper.findByUserId(userId);
    }

    @Override
    public List<Long> getRecentCharacterIds(Long userId, int limit) {
        return characterInteractionMapper.findRecentCharacterIds(userId, limit);
    }

    @Override
    public int getUserInteractionCount(Long userId) {
        QueryWrapper<CharacterInteraction> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<CharacterInteraction> interactions = characterInteractionMapper.selectList(wrapper);
        return interactions.stream().mapToInt(i -> i.getInteractionCount() != null ? i.getInteractionCount() : 0).sum();
    }

    @Override
    public int getCharacterInteractionCount(Long characterId) {
        QueryWrapper<CharacterInteraction> wrapper = new QueryWrapper<>();
        wrapper.eq("character_id", characterId);
        List<CharacterInteraction> interactions = characterInteractionMapper.selectList(wrapper);
        return interactions.stream().mapToInt(i -> i.getInteractionCount() != null ? i.getInteractionCount() : 0).sum();
    }

    @Override
    @Transactional
    public boolean deleteInteraction(Long userId, Long characterId) {
        CharacterInteraction interaction = characterInteractionMapper.findByUserIdAndCharacterId(userId, characterId);
        if (interaction != null) {
            characterInteractionMapper.deleteById(interaction.getId());
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean deleteAllByUserId(Long userId) {
        QueryWrapper<CharacterInteraction> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return characterInteractionMapper.delete(wrapper) > 0;
    }

    @Override
    @Transactional
    public boolean deleteAllByCharacterId(Long characterId) {
        QueryWrapper<CharacterInteraction> wrapper = new QueryWrapper<>();
        wrapper.eq("character_id", characterId);
        return characterInteractionMapper.delete(wrapper) > 0;
    }
}
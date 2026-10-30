package com.example.aichat.service;

import com.example.aichat.entity.CharacterInteraction;

import java.util.List;

public interface CharacterInteractionService {
    
    CharacterInteraction recordInteraction(Long userId, Long characterId);
    
    CharacterInteraction getInteraction(Long userId, Long characterId);
    
    List<CharacterInteraction> getUserInteractions(Long userId);
    
    List<Long> getRecentCharacterIds(Long userId, int limit);
    
    int getUserInteractionCount(Long userId);
    
    int getCharacterInteractionCount(Long characterId);
    
    boolean deleteInteraction(Long userId, Long characterId);
    
    boolean deleteAllByUserId(Long userId);
    
    boolean deleteAllByCharacterId(Long characterId);
}
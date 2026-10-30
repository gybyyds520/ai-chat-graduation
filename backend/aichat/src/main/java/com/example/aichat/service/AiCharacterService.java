package com.example.aichat.service;

import com.example.aichat.entity.AiCharacter;

import java.util.List;

public interface AiCharacterService {
    List<AiCharacter> getAllCharacters();
    List<AiCharacter> getCharactersByUserId(Long userId);
    AiCharacter getCharacterById(Long id);
    AiCharacter getCharacterByIdWithAccess(Long id, Long userId);
    AiCharacter createCharacter(AiCharacter character);
    AiCharacter updateCharacter(AiCharacter character, Long userId);
    boolean deleteCharacter(Long id, Long userId);
    boolean shareCharacter(Long id, boolean isShared);
    boolean isOwner(Long userId, Long characterId);
    boolean canAccess(Long userId, Long characterId);
}
package com.example.aichat.service;

import com.example.aichat.entity.AiCharacter;

import java.util.List;

public interface CharacterGeneratorService {
    AiCharacter generateCharacter(String theme);
    List<AiCharacter> generateMultipleCharacters(String theme, int count);
}
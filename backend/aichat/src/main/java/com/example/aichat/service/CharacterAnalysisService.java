package com.example.aichat.service;

import java.util.List;
import java.util.Map;

public interface CharacterAnalysisService {
    Map<String, Object> analyzeCharacter(List<Map<String, Object>> messages, String targetSpeaker);
    Map<String, Object> analyzeAllCharacters(List<Map<String, Object>> messages);
}
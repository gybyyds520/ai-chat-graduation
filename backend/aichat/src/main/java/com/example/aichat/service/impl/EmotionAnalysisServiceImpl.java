package com.example.aichat.service.impl;

import com.example.aichat.service.EmotionAnalysisService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class EmotionAnalysisServiceImpl implements EmotionAnalysisService {

    private static final Pattern HAPPY_PATTERN = Pattern.compile("(开心|高兴|快乐|兴奋|幸福|笑|喜|好棒|太棒|nice|good|happy)");
    private static final Pattern SAD_PATTERN = Pattern.compile("(难过|伤心|悲伤|失落|哭|愁|沮丧|失望|bad|sad)");
    private static final Pattern ANGRY_PATTERN = Pattern.compile("(生气|愤怒|烦|火|讨厌|恨|怒|mad|angry)");
    private static final Pattern SURPRISED_PATTERN = Pattern.compile("(惊讶|震惊|哇|天呐|居然|没想到|surprised|amazing)");
    private static final Pattern FEARFUL_PATTERN = Pattern.compile("(害怕|恐惧|怕|吓|担心|担忧|fear|scared)");
    private static final Pattern DISGUSTED_PATTERN = Pattern.compile("(恶心|讨厌|反感|厌恶|disgust)");
    private static final Pattern LOVE_PATTERN = Pattern.compile("(爱|喜欢|想你|想念|love|miss)");

    private final Map<String, String> emotionExpressions = new HashMap<>();
    private final Map<String, String> emotionActions = new HashMap<>();

    public EmotionAnalysisServiceImpl() {
        emotionExpressions.put("happy", "smile");
        emotionExpressions.put("sad", "cry");
        emotionExpressions.put("angry", "frown");
        emotionExpressions.put("surprised", "eyes_open");
        emotionExpressions.put("fearful", "shocked");
        emotionExpressions.put("disgusted", "pout");
        emotionExpressions.put("love", "heart");
        emotionExpressions.put("neutral", "normal");

        emotionActions.put("happy", "jump");
        emotionActions.put("sad", "droop");
        emotionActions.put("angry", "stamp");
        emotionExpressions.put("surprised", "gasps");
        emotionActions.put("fearful", "tremble");
        emotionActions.put("disgusted", "turn_away");
        emotionActions.put("love", "wink");
        emotionActions.put("neutral", "idle");
    }

    @Override
    public String analyzeEmotion(String text) {
        if (text == null || text.isEmpty()) {
            return "neutral";
        }
        
        String lowerText = text.toLowerCase();
        
        if (HAPPY_PATTERN.matcher(lowerText).find()) {
            return "happy";
        }
        if (LOVE_PATTERN.matcher(lowerText).find()) {
            return "love";
        }
        if (ANGRY_PATTERN.matcher(lowerText).find()) {
            return "angry";
        }
        if (SAD_PATTERN.matcher(lowerText).find()) {
            return "sad";
        }
        if (SURPRISED_PATTERN.matcher(lowerText).find()) {
            return "surprised";
        }
        if (FEARFUL_PATTERN.matcher(lowerText).find()) {
            return "fearful";
        }
        if (DISGUSTED_PATTERN.matcher(lowerText).find()) {
            return "disgusted";
        }
        
        int exclamationCount = countOccurrences(text, '!');
        int questionCount = countOccurrences(text, '?');
        
        if (exclamationCount >= 2) {
            return "surprised";
        }
        if (text.contains("哈哈") || text.contains("呵呵") || text.contains("😀") || text.contains("😄")) {
            return "happy";
        }
        if (text.contains("😭") || text.contains("😢") || text.contains("难过")) {
            return "sad";
        }
        
        return "neutral";
    }

    @Override
    public double analyzeIntensity(String text, String emotionType) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        
        double intensity = 0.3;
        
        int exclamationCount = countOccurrences(text, '!');
        int uppercaseCount = countUppercase(text);
        
        intensity += exclamationCount * 0.2;
        intensity += (double) uppercaseCount / text.length() * 0.3;
        
        if (text.contains("非常") || text.contains("特别") || text.contains("太")) {
            intensity += 0.2;
        }
        
        return Math.min(intensity, 1.0);
    }

    @Override
    public Map<String, Object> analyzeFull(String text) {
        Map<String, Object> result = new HashMap<>();
        String emotion = analyzeEmotion(text);
        double intensity = analyzeIntensity(text, emotion);
        
        result.put("emotion", emotion);
        result.put("intensity", intensity);
        result.put("expression", getExpressionForEmotion(emotion));
        result.put("action", getActionForEmotion(emotion));
        
        return result;
    }

    @Override
    public String getExpressionForEmotion(String emotionType) {
        return emotionExpressions.getOrDefault(emotionType, "normal");
    }

    @Override
    public String getActionForEmotion(String emotionType) {
        return emotionActions.getOrDefault(emotionType, "idle");
    }

    private int countOccurrences(String text, char c) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    private int countUppercase(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isUpperCase(text.charAt(i))) {
                count++;
            }
        }
        return count;
    }
}
package com.example.aichat.service.impl;

import com.example.aichat.service.CharacterAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class CharacterAnalysisServiceImpl implements CharacterAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(CharacterAnalysisServiceImpl.class);
    
    private static final Pattern EMOJI_PATTERN = Pattern.compile("[\\uD83C][\\uDC00-\\uDFFF]|[\\uD83D][\\uDC00-\\uDEFF]|[\\uD83E][\\uDC00-\\uDFFF]");
    private static final Pattern QUESTION_PATTERN = Pattern.compile("[？?]");
    private static final Pattern EXCLAMATION_PATTERN = Pattern.compile("[！!]");
    
    private static final String[] FORMAL_WORDS = {"您好", "请问", "感谢", "抱歉", "尊敬的", "贵公司", "特此", "函告", "知悉", "遵照"};
    private static final String[] CASUAL_WORDS = {"哈哈", "嘿嘿", "嗯嗯", "哦哦", "嘛", "呗", "啦", "呢", "哟", "哒"};
    private static final String[] HUMOR_WORDS = {"笑死", "绝了", "离谱", "神仙", "YYDS", "绝绝子", "666", "哈哈哈"};

    @Override
    public Map<String, Object> analyzeAllCharacters(List<Map<String, Object>> messages) {
        Map<String, List<Map<String, Object>>> speakerMessages = groupBySpeaker(messages);
        Map<String, Object> results = new HashMap<>();
        
        for (Map.Entry<String, List<Map<String, Object>>> entry : speakerMessages.entrySet()) {
            String speaker = entry.getKey();
            if (!speaker.equalsIgnoreCase("未知") && !speaker.equalsIgnoreCase("system")) {
                Map<String, Object> analysis = analyzeCharacter(entry.getValue(), speaker);
                results.put(speaker, analysis);
            }
        }
        
        return results;
    }

    @Override
    public Map<String, Object> analyzeCharacter(List<Map<String, Object>> messages, String targetSpeaker) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("【角色分析开始】目标角色: {}", targetSpeaker);
        logger.info("【角色分析】输入消息总数: {}", messages.size());
        
        List<Map<String, Object>> targetMessages = new ArrayList<>();
        for (Map<String, Object> msg : messages) {
            String sender = String.valueOf(msg.getOrDefault("sender", "")).trim();
            if (sender.equalsIgnoreCase(targetSpeaker)) {
                targetMessages.add(msg);
            }
        }
        
        if (targetMessages.isEmpty()) {
            logger.warn("【角色分析警告】未找到角色 {} 的消息，返回默认分析结果", targetSpeaker);
            return createEmptyAnalysis(targetSpeaker);
        }
        
        logger.info("【角色分析】目标角色消息数: {}", targetMessages.size());
        
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("speaker", targetSpeaker);
        analysis.put("messageCount", targetMessages.size());
        
        List<String> contents = new ArrayList<>();
        for (Map<String, Object> msg : targetMessages) {
            String content = String.valueOf(msg.getOrDefault("content", ""));
            contents.add(content);
            logger.debug("【消息内容】{}", content.length() > 50 ? content.substring(0, 50) + "..." : content);
        }
        
        logger.info("【开始分析】语言风格分析...");
        Map<String, Object> languageStyle = analyzeLanguageStyle(contents);
        analysis.put("languageStyle", languageStyle);
        logger.info("【分析结果】语言风格: {} - {}", languageStyle.get("type"), languageStyle.get("description"));
        
        logger.info("【开始分析】词汇分析...");
        Map<String, Object> vocabulary = analyzeVocabulary(contents);
        analysis.put("vocabulary", vocabulary);
        logger.info("【分析结果】词汇量: {}, 独特词汇数: {}", vocabulary.get("wordCount"), vocabulary.get("uniqueWordCount"));
        
        logger.info("【开始分析】对话模式分析...");
        Map<String, Object> conversationPattern = analyzeConversationPattern(targetMessages);
        analysis.put("conversationPattern", conversationPattern);
        logger.info("【分析结果】平均消息长度: {}, 回复率: {}", conversationPattern.get("avgLength"), conversationPattern.get("replyRate"));
        
        logger.info("【开始分析】性格特征分析...");
        Map<String, Object> personality = analyzePersonality(contents, targetMessages);
        analysis.put("personality", personality);
        logger.info("【分析结果】性格描述: {}", personality.get("description"));
        logger.info("【分析结果】性格标签: {}", personality.get("traits"));
        
        logger.info("【角色分析完成】目标角色: {}", targetSpeaker);
        logger.info("═══════════════════════════════════════════════════════════════");
        
        return analysis;
    }

    private Map<String, Object> createEmptyAnalysis(String speaker) {
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("speaker", speaker);
        analysis.put("messageCount", 0);
        analysis.put("languageStyle", Map.of("type", "neutral", "description", "中性风格"));
        analysis.put("vocabulary", Map.of("topWords", List.of(), "wordCount", 0));
        analysis.put("conversationPattern", Map.of("avgLength", 0, "replyRate", 0));
        analysis.put("personality", Map.of("traits", List.of("温和"), "description", "性格温和"));
        return analysis;
    }

    private Map<String, Object> analyzeLanguageStyle(List<String> contents) {
        Map<String, Object> style = new HashMap<>();
        
        int formalCount = 0, casualCount = 0, humorCount = 0;
        int emojiCount = 0, questionCount = 0, exclamationCount = 0;
        int totalChars = 0, lineCount = 0;
        
        for (String content : contents) {
            totalChars += content.length();
            lineCount++;
            
            for (String word : FORMAL_WORDS) {
                if (content.contains(word)) formalCount++;
            }
            for (String word : CASUAL_WORDS) {
                if (content.contains(word)) casualCount++;
            }
            for (String word : HUMOR_WORDS) {
                if (content.contains(word)) humorCount++;
            }
            
            emojiCount += countMatches(EMOJI_PATTERN, content);
            questionCount += countMatches(QUESTION_PATTERN, content);
            exclamationCount += countMatches(EXCLAMATION_PATTERN, content);
        }
        
        String styleType;
        String description;
        
        if (formalCount > casualCount * 2) {
            styleType = "formal";
            description = "正式严谨";
        } else if (casualCount > formalCount * 2 || emojiCount > lineCount / 2) {
            styleType = "casual";
            description = "轻松活泼";
        } else if (humorCount > lineCount / 3) {
            styleType = "humorous";
            description = "幽默风趣";
        } else {
            styleType = "neutral";
            description = "中性适中";
        }
        
        style.put("type", styleType);
        style.put("description", description);
        style.put("formalScore", formalCount);
        style.put("casualScore", casualCount);
        style.put("humorScore", humorCount);
        style.put("emojiCount", emojiCount);
        style.put("questionCount", questionCount);
        style.put("exclamationCount", exclamationCount);
        style.put("avgLength", lineCount > 0 ? totalChars / lineCount : 0);
        
        return style;
    }

    private Map<String, Object> analyzeVocabulary(List<String> contents) {
        Map<String, Integer> wordFrequency = new HashMap<>();
        int totalWords = 0;
        
        for (String content : contents) {
            String[] words = content.split("[\\s,，.。!！?？、;；:：]+");
            for (String word : words) {
                word = word.trim();
                if (word.length() >= 2) {
                    wordFrequency.merge(word, 1, Integer::sum);
                    totalWords++;
                }
            }
        }
        
        List<String> topWords = wordFrequency.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .toList();
        
        Map<String, Object> vocab = new HashMap<>();
        vocab.put("topWords", topWords);
        vocab.put("wordCount", totalWords);
        vocab.put("uniqueWordCount", wordFrequency.size());
        
        return vocab;
    }

    private Map<String, Object> analyzeConversationPattern(List<Map<String, Object>> messages) {
        Map<String, Object> pattern = new HashMap<>();
        
        int totalLength = 0;
        int shortMessages = 0, mediumMessages = 0, longMessages = 0;
        
        for (Map<String, Object> msg : messages) {
            String content = String.valueOf(msg.getOrDefault("content", ""));
            int len = content.length();
            totalLength += len;
            
            if (len <= 20) shortMessages++;
            else if (len <= 100) mediumMessages++;
            else longMessages++;
        }
        
        pattern.put("avgLength", messages.isEmpty() ? 0 : totalLength / messages.size());
        pattern.put("shortMessages", shortMessages);
        pattern.put("mediumMessages", mediumMessages);
        pattern.put("longMessages", longMessages);
        pattern.put("replyRate", messages.isEmpty() ? 0 : Math.min(1.0, messages.size() / 10.0));
        
        return pattern;
    }

    private Map<String, Object> analyzePersonality(List<String> contents, List<Map<String, Object>> messages) {
        Map<String, Object> personality = new HashMap<>();
        List<String> traits = new ArrayList<>();
        StringBuilder description = new StringBuilder();
        
        int agreementCount = 0, disagreementCount = 0;
        int positiveCount = 0, negativeCount = 0;
        int suggestionCount = 0, questionCount = 0;
        
        for (String content : contents) {
            if (content.contains("好") || content.contains("可以") || content.contains("不错") || 
                content.contains("同意") || content.contains("OK") || content.contains("行")) {
                agreementCount++;
            }
            if (content.contains("不") && !content.contains("不错") && !content.contains("不是")) {
                disagreementCount++;
            }
            if (content.contains("开心") || content.contains("高兴") || content.contains("喜欢") || 
                content.contains("爱") || content.contains("棒") || content.contains("赞")) {
                positiveCount++;
            }
            if (content.contains("难过") || content.contains("伤心") || content.contains("讨厌") || 
                content.contains("烦") || content.contains("糟糕")) {
                negativeCount++;
            }
            if (content.contains("建议") || content.contains("应该") || content.contains("可以试试")) {
                suggestionCount++;
            }
            questionCount += countMatches(QUESTION_PATTERN, content);
        }
        
        if (agreementCount > disagreementCount * 2) {
            traits.add("随和");
            description.append("性格随和，容易相处。");
        }
        if (disagreementCount > agreementCount) {
            traits.add("直率");
            description.append("性格直率，敢于表达不同意见。");
        }
        if (positiveCount > negativeCount * 2) {
            traits.add("乐观");
            description.append("态度乐观积极。");
        }
        if (questionCount > contents.size() / 2) {
            traits.add("好奇");
            description.append("好奇心强，善于提问。");
        }
        if (suggestionCount > contents.size() / 3) {
            traits.add("乐于助人");
            description.append("乐于助人，经常给出建议。");
        }
        
        if (traits.isEmpty()) {
            traits.add("温和");
            description.append("性格温和，待人友善。");
        }
        
        personality.put("traits", traits);
        personality.put("description", description.toString().isEmpty() ? "性格温和，待人友善。" : description.toString());
        
        return personality;
    }

    private Map<String, List<Map<String, Object>>> groupBySpeaker(List<Map<String, Object>> messages) {
        Map<String, List<Map<String, Object>>> groups = new HashMap<>();
        for (Map<String, Object> msg : messages) {
            String speaker = String.valueOf(msg.getOrDefault("sender", "未知")).trim();
            groups.computeIfAbsent(speaker, k -> new ArrayList<>()).add(msg);
        }
        return groups;
    }

    private int countMatches(Pattern pattern, String text) {
        return (int) pattern.matcher(text).results().count();
    }
}
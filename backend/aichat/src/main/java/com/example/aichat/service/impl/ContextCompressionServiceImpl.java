package com.example.aichat.service.impl;

import com.example.aichat.service.ContextCompressionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ContextCompressionServiceImpl implements ContextCompressionService {

    private static final Logger logger = LoggerFactory.getLogger(ContextCompressionServiceImpl.class);
    
    private static final Pattern KEYWORD_PATTERN = Pattern.compile(
        "(姓名|名字|生日|爱好|喜欢|讨厌|职业|学校|工作|地址|电话|邮箱|手机|年龄|性别)"
    );
    
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    @Override
    public String compressContext(List<Map<String, Object>> messages, int maxTokens) {
        List<Map<String, Object>> pruned = pruneContext(messages, maxTokens);
        
        StringBuilder compressed = new StringBuilder();
        for (Map<String, Object> message : pruned) {
            String role = (String) message.get("role");
            String content = (String) message.get("content");
            compressed.append(role).append(": ").append(content).append("\n");
        }
        
        return compressed.toString();
    }

    @Override
    public Map<String, Object> extractKeyPoints(List<Map<String, Object>> messages) {
        Map<String, Object> keyPoints = new HashMap<>();
        Set<String> entities = new HashSet<>();
        Set<String> relationships = new HashSet<>();
        List<String> actions = new ArrayList<>();
        
        for (Map<String, Object> message : messages) {
            String content = (String) message.get("content");
            if (content == null) continue;
            
            extractEntities(content, entities);
            extractRelationships(content, relationships);
            extractActions(content, actions);
        }
        
        keyPoints.put("entities", new ArrayList<>(entities));
        keyPoints.put("relationships", new ArrayList<>(relationships));
        keyPoints.put("actions", actions);
        keyPoints.put("summary", generateTopicSummary(messages));
        
        return keyPoints;
    }

    private void extractEntities(String content, Set<String> entities) {
        Matcher matcher = KEYWORD_PATTERN.matcher(content);
        while (matcher.find()) {
            int start = matcher.end();
            int end = Math.min(start + 30, content.length());
            String entity = content.substring(start).trim().split("[，。！？、]")[0].trim();
            if (!entity.isEmpty()) {
                entities.add(matcher.group(1) + ": " + entity);
            }
        }
        
        Matcher numMatcher = NUMBER_PATTERN.matcher(content);
        while (numMatcher.find()) {
            entities.add("数字: " + numMatcher.group());
        }
    }

    private void extractRelationships(String content, Set<String> relationships) {
        String[] relationPatterns = {"是", "属于", "喜欢", "讨厌", "认识", "朋友", "同事", "同学"};
        for (String pattern : relationPatterns) {
            int idx = content.indexOf(pattern);
            if (idx > 0 && idx < content.length() - 1) {
                String before = content.substring(Math.max(0, idx - 5), idx).trim();
                String after = content.substring(idx + 1, Math.min(idx + 6, content.length())).trim();
                if (!before.isEmpty() && !after.isEmpty()) {
                    relationships.add(before + pattern + after);
                }
            }
        }
    }

    private void extractActions(String content, List<String> actions) {
        String[] actionVerbs = {"想", "要", "需要", "希望", "打算", "计划", "开始", "结束"};
        for (String verb : actionVerbs) {
            int idx = content.indexOf(verb);
            if (idx > 0) {
                int end = content.indexOf("。", idx);
                if (end == -1) end = content.indexOf("！", idx);
                if (end == -1) end = content.indexOf("？", idx);
                if (end == -1) end = Math.min(idx + 30, content.length());
                
                String action = content.substring(idx, end).trim();
                if (!action.isEmpty()) {
                    actions.add(action);
                }
            }
        }
    }

    @Override
    public String generateTopicSummary(List<Map<String, Object>> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        StringBuilder summary = new StringBuilder();

        Map<String, Object> first = messages.get(0);
        String firstContent = contentOrEmpty(first);
        summary.append("对话开始: ").append(firstContent.substring(0, Math.min(20, firstContent.length())));

        Map<String, Object> last = messages.get(messages.size() - 1);
        String lastContent = contentOrEmpty(last);
        summary.append(" | 当前: ").append(lastContent.substring(0, Math.min(20, lastContent.length())));

        summary.append(" | 共").append(messages.size()).append("条消息");

        return summary.toString();
    }

    /**
     * 安全读取消息内容，避免 content 为 null 时 NPE。
     */
    private String contentOrEmpty(Map<String, Object> message) {
        if (message == null) {
            return "";
        }
        Object content = message.get("content");
        return content != null ? content.toString() : "";
    }

    @Override
    public List<Map<String, Object>> pruneContext(List<Map<String, Object>> messages, int targetSize) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }
        // 健壮性：targetSize 至少为 1，避免下面 messages.size()/(targetSize-2) 除零或负步长
        int safeTarget = Math.max(1, targetSize);
        if (messages.size() <= safeTarget) {
            return messages;
        }

        // 目标极小（1 或 2）时直接返回首尾各一条，避免除零
        if (safeTarget <= 2) {
            List<Map<String, Object>> edge = new ArrayList<>();
            edge.add(messages.get(0));
            if (safeTarget == 2 && messages.size() > 1) {
                edge.add(messages.get(messages.size() - 1));
            }
            return edge;
        }

        List<Map<String, Object>> pruned = new ArrayList<>();

        pruned.add(messages.get(0));

        int step = messages.size() / (safeTarget - 2);
        if (step < 1) step = 1;
        for (int i = step; i < messages.size() - 1; i += step) {
            pruned.add(messages.get(i));
        }

        pruned.add(messages.get(messages.size() - 1));

        while (pruned.size() > safeTarget) {
            pruned.remove(pruned.size() - 2);
        }

        logger.info("[上下文压缩] 原始消息数: {}, 压缩后: {}", messages.size(), pruned.size());
        return pruned;
    }
}

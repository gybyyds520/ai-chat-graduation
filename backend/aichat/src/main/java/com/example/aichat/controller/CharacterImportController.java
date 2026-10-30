package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.AiCharacter;
import com.example.aichat.mapper.AiCharacterMapper;
import com.example.aichat.service.CharacterAnalysisService;
import com.example.aichat.service.ChatDataParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/character/import")
public class CharacterImportController {

    private static final Logger logger = LoggerFactory.getLogger(CharacterImportController.class);
    
    private static final List<String> AVATAR_STYLES = List.of(
        "bottts", "avataaars", "jdenticon", "gridy", "micah"
    );

    @Autowired
    private ChatDataParserService chatDataParserService;
    
    @Autowired
    private CharacterAnalysisService characterAnalysisService;
    
    @Autowired
    private AiCharacterMapper aiCharacterMapper;

    @PostMapping("/upload")
    public R uploadChatData(@RequestParam("file") MultipartFile file, 
                           @RequestParam(value = "format", required = false) String format) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("【文件上传开始】文件名: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());
        
        try {
            logger.info("【步骤1/3】读取文件内容...");
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            logger.info("【文件内容】长度: {} 字符", content.length());
            
            logger.info("【步骤2/3】检测文件格式...");
            String detectedFormat = format;
            if (detectedFormat == null || detectedFormat.isEmpty()) {
                String filename = file.getOriginalFilename();
                if (filename != null && filename.toLowerCase().endsWith(".json")) {
                    detectedFormat = "json";
                } else {
                    detectedFormat = "txt";
                }
            }
            logger.info("【检测格式】{}", detectedFormat);
            
            logger.info("【步骤3/3】解析聊天数据...");
            List<Map<String, Object>> messages = chatDataParserService.parse(content, detectedFormat);
            logger.info("【解析结果】成功解析 {} 条消息", messages.size());
            
            Map<String, Object> result = new HashMap<>();
            result.put("messages", messages);
            result.put("messageCount", messages.size());
            result.put("format", detectedFormat);
            
            logger.info("【文件上传完成】成功处理文件: {}", file.getOriginalFilename());
            logger.info("═══════════════════════════════════════════════════════════════");
            
            return R.success(result);
        } catch (IOException e) {
            logger.error("【文件上传失败】文件读取异常: {}", e.getMessage());
            logger.error("【堆栈跟踪】", e);
            return R.error("文件读取失败");
        }
    }

    @PostMapping("/analyze")
    public R analyzeCharacters(@RequestBody Map<String, Object> request) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("【角色分析开始】");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) request.get("messages");
        
        if (messages == null || messages.isEmpty()) {
            logger.warn("【角色分析失败】消息数据为空");
            return R.error("消息数据为空");
        }
        
        logger.info("【分析输入】消息数量: {}", messages.size());
        
        Map<String, Object> analysis = characterAnalysisService.analyzeAllCharacters(messages);
        logger.info("【分析结果】识别到 {} 个角色", analysis.size());
        
        for (Object key : analysis.keySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> charAnalysis = (Map<String, Object>) analysis.get(key);
            String speaker = String.valueOf(key);
            int msgCount = (int) charAnalysis.getOrDefault("messageCount", 0);
            logger.info("  - 角色: {}, 消息数: {}", speaker, msgCount);
        }
        
        logger.info("【角色分析完成】");
        logger.info("═══════════════════════════════════════════════════════════════");
        
        return R.success(analysis);
    }

    @PostMapping("/generate")
    public R generateCharacter(@RequestBody Map<String, Object> request) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("【角色生成开始】");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> analysis = (Map<String, Object>) request.get("analysis");
        String speaker = (String) request.get("speaker");
        String customName = (String) request.get("customName");
        String customDescription = (String) request.get("customDescription");
        
        logger.info("【输入参数】speaker: {}, customName: {}, customDescription: {}", 
                   speaker, customName, customDescription != null ? "已提供" : "未提供");
        
        if (analysis == null || speaker == null) {
            logger.warn("【角色生成失败】参数不完整");
            return R.error("参数不完整");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> characterAnalysis = (Map<String, Object>) analysis.get(speaker);
        if (characterAnalysis == null) {
            logger.warn("【角色生成失败】未找到角色分析数据: {}", speaker);
            return R.error("未找到角色分析数据");
        }
        
        logger.info("【开始创建角色】基于分析数据生成角色...");
        AiCharacter character = createCharacterFromAnalysis(characterAnalysis, speaker);
        logger.info("【角色创建成功】名称: {}, Prompt长度: {} 字符", 
                   character.getName(), character.getPrompt().length());
        
        if (customName != null && !customName.isEmpty()) {
            logger.info("【自定义名称】从 '{}' 修改为 '{}'", character.getName(), customName);
            character.setName(customName);
        }
        if (customDescription != null && !customDescription.isEmpty()) {
            logger.info("【自定义描述】使用用户提供的描述");
            character.setPrompt(customDescription);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("character", character);
        result.put("analysis", characterAnalysis);
        
        logger.info("【角色生成完成】");
        logger.info("═══════════════════════════════════════════════════════════════");
        
        return R.success(result);
    }

    @PostMapping("/preview")
    public R previewCharacter(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String avatar = (String) request.get("avatar");
        String prompt = (String) request.get("prompt");
        
        Map<String, Object> preview = new HashMap<>();
        preview.put("name", name);
        preview.put("avatar", avatar);
        preview.put("prompt", prompt);
        preview.put("previewText", generatePreviewText(prompt));
        
        return R.success(preview);
    }

    @PostMapping("/save")
    public R saveCharacter(@RequestBody Map<String, Object> request) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("【角色保存开始】");
        
        String name = (String) request.get("name");
        String avatar = (String) request.get("avatar");
        String prompt = (String) request.get("prompt");
        // 安全修复（P0）：角色归属强制取当前登录用户
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                request.get("userId") != null ? ((Number) request.get("userId")).longValue() : null);
        
        if (avatar != null && avatar.length() > 255) {
            avatar = avatar.substring(0, 255);
            logger.info("【头像截断】头像URL过长，已截断到255字符");
        }
        
        logger.info("【保存参数】name: {}, avatar: {}, prompt长度: {} 字符, userId: {}", 
                   name, avatar != null ? "已设置" : "未设置", 
                   prompt != null ? prompt.length() : 0, userId);
        
        AiCharacter character = new AiCharacter();
        character.setName(name);
        character.setAvatar(avatar);
        character.setPrompt(prompt);
        character.setUserId(userId);
        character.setIsSystem(false);
        character.setIsShared(false);
        
        logger.info("【执行保存】插入角色到数据库...");
        int result = aiCharacterMapper.insert(character);
        
        if (result > 0) {
            logger.info("【保存成功】角色ID: {}, 名称: {}", character.getId(), character.getName());
            logger.info("【角色保存完成】");
            logger.info("═══════════════════════════════════════════════════════════════");
            return R.success(character);
        }
        
        logger.error("【保存失败】数据库插入返回: {}", result);
        logger.info("═══════════════════════════════════════════════════════════════");
        return R.error("保存失败");
    }

    @PostMapping("/batch-generate")
    public R batchGenerateCharacters(@RequestBody Map<String, Object> request) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("【批量角色生成开始】");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> analysis = (Map<String, Object>) request.get("analysis");
        // 安全修复（P0）：角色归属强制取当前登录用户
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                request.get("userId") != null ? ((Number) request.get("userId")).longValue() : null);
        
        if (analysis == null) {
            logger.warn("【批量生成失败】分析数据为空");
            return R.error("分析数据为空");
        }
        
        logger.info("【批量生成】待生成角色数量: {}, userId: {}", analysis.size(), userId);
        
        List<AiCharacter> characters = new ArrayList<>();
        int index = 0;
        for (Object key : analysis.keySet()) {
            String speaker = String.valueOf(key);
            logger.info("【批量生成】处理第 {}/{} 个角色: {}", index + 1, analysis.size(), speaker);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> charAnalysis = (Map<String, Object>) analysis.get(speaker);
            AiCharacter character = createCharacterFromAnalysis(charAnalysis, speaker);
            character.setUserId(userId);
            character.setIsShared(false);
            
            if (character.getAvatar() != null && character.getAvatar().length() > 255) {
                character.setAvatar(character.getAvatar().substring(0, 255));
            }
            
            aiCharacterMapper.insert(character);
            characters.add(character);
            
            logger.info("【批量生成】角色 '{}' 创建成功，ID: {}", speaker, character.getId());
            index++;
        }
        
        logger.info("【批量生成完成】成功生成 {}/{} 个角色", characters.size(), analysis.size());
        logger.info("═══════════════════════════════════════════════════════════════");
        
        return R.success(characters);
    }

    private AiCharacter createCharacterFromAnalysis(Map<String, Object> analysis, String speaker) {
        AiCharacter character = new AiCharacter();
        
        character.setName(speaker);
        
        String avatarStyle = AVATAR_STYLES.get(new Random().nextInt(AVATAR_STYLES.size()));
        character.setAvatar("https://api.dicebear.com/7.x/" + avatarStyle + "/svg?seed=" + speaker);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> languageStyle = (Map<String, Object>) analysis.get("languageStyle");
        @SuppressWarnings("unchecked")
        Map<String, Object> personality = (Map<String, Object>) analysis.get("personality");
        @SuppressWarnings("unchecked")
        Map<String, Object> vocabulary = (Map<String, Object>) analysis.get("vocabulary");
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个名为「").append(speaker).append("」的角色。\n\n");
        
        String styleDesc = (String) languageStyle.getOrDefault("description", "中性风格");
        prompt.append("语言风格：").append(styleDesc).append("。\n");
        
        String personalityDesc = (String) personality.getOrDefault("description", "性格温和");
        prompt.append("性格特点：").append(personalityDesc).append("\n");
        
        @SuppressWarnings("unchecked")
        List<String> traits = (List<String>) personality.getOrDefault("traits", List.of());
        if (!traits.isEmpty()) {
            prompt.append("性格标签：").append(String.join("、", traits)).append("。\n");
        }
        
        @SuppressWarnings("unchecked")
        List<String> topWords = (List<String>) vocabulary.getOrDefault("topWords", List.of());
        if (!topWords.isEmpty()) {
            prompt.append("常用词汇：").append(String.join("、", topWords.subList(0, Math.min(5, topWords.size())))).append("。\n");
        }
        
        prompt.append("\n请根据以上设定，以").append(speaker).append("的身份进行对话。");
        
        character.setPrompt(prompt.toString());
        character.setIsSystem(false);
        
        return character;
    }

    private String generatePreviewText(String prompt) {
        if (prompt == null || prompt.isEmpty()) {
            return "你好！我是一个AI助手。";
        }
        
        if (prompt.contains("幽默")) {
            return "哈哈，你好呀！今天有什么好玩的事吗？😆";
        } else if (prompt.contains("正式")) {
            return "您好，很高兴为您服务。请问有什么可以帮助您的？";
        } else if (prompt.contains("活泼")) {
            return "嗨嗨！今天天气真好呢！我们聊点什么吧~";
        } else {
            return "你好！很高兴认识你。";
        }
    }
}
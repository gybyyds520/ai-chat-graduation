package com.example.aichat.service.impl;

import com.example.aichat.entity.AiCharacter;
import com.example.aichat.mapper.AiCharacterMapper;
import com.example.aichat.service.CharacterGeneratorService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class CharacterGeneratorServiceImpl implements CharacterGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(CharacterGeneratorServiceImpl.class);
    private final WebClient webClient;
    private final AiCharacterMapper characterMapper;
    private final ObjectMapper objectMapper;

    private static final String[] ROLE_TYPES = {
        "有趣的历史人物", "科幻角色", "动漫角色", "神话生物", 
        "职业专家", "动物朋友", "虚拟助手", "奇幻生物"
    };

    private static final String[] AVATAR_STYLES = {
        "avataaars", "bottts", "jdenticon", "gridy", "micah"
    };

    public CharacterGeneratorServiceImpl(AiCharacterMapper characterMapper) {
        this.webClient = WebClient.create("http://localhost:11434");
        this.characterMapper = characterMapper;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public AiCharacter generateCharacter(String theme) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("【角色生成开始】主题: {}", theme);
        logger.info("【时间戳】{}", System.currentTimeMillis());
        
        logger.info("【步骤1/4】调用AI模型生成角色数据...");
        String characterJson = generateCharacterJson(theme);
        logger.info("【AI响应】{}", characterJson.length() > 100 ? characterJson.substring(0, 100) + "..." : characterJson);
        
        try {
            logger.info("【步骤2/4】解析AI响应JSON...");
            JsonNode node = objectMapper.readTree(characterJson);
            
            AiCharacter character = new AiCharacter();
            
            String name = node.has("name") ? node.get("name").asText() : generateRandomName();
            logger.info("【角色名称】{}", name);
            
            String roleDescription = node.has("description") ? node.get("description").asText() : "一个有趣的AI角色";
            logger.info("【角色描述】{}", roleDescription);
            
            String prompt = node.has("prompt") ? node.get("prompt").asText() : generateDefaultPrompt(name, roleDescription);
            logger.info("【生成Prompt】长度: {} 字符", prompt.length());
            logger.debug("【Prompt内容】{}", prompt);
            
            logger.info("【步骤3/4】生成头像URL...");
            String avatarUrl = generateAvatarUrl();
            character.setAvatar(avatarUrl);
            logger.info("【头像URL】{}", avatarUrl);
            
            character.setName(name);
            character.setPrompt(prompt);
            character.setIsSystem(false);
            
            logger.info("【步骤4/4】保存角色到数据库...");
            characterMapper.insert(character);
            
            logger.info("【角色生成成功】ID: {}, 名称: {}", character.getId(), character.getName());
            logger.info("═══════════════════════════════════════════════════════════════");
            
            return character;
        } catch (Exception e) {
            logger.error("【角色生成失败】错误信息: {}", e.getMessage());
            logger.error("【堆栈跟踪】", e);
            logger.warn("【降级处理】使用本地备用生成方案...");
            AiCharacter fallback = generateFallbackCharacter(theme);
            logger.info("【备用生成成功】名称: {}", fallback.getName());
            return fallback;
        }
    }

    @Override
    public List<AiCharacter> generateMultipleCharacters(String theme, int count) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("【批量角色生成开始】主题: {}, 数量: {}", theme, count);
        
        List<AiCharacter> characters = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            logger.info("【批量生成】正在生成第 {}/{} 个角色...", i + 1, count);
            AiCharacter character = generateCharacter(theme);
            if (character != null) {
                characters.add(character);
                logger.info("【批量生成】第 {}/{} 个角色生成成功: {}", i + 1, count, character.getName());
            } else {
                logger.warn("【批量生成】第 {}/{} 个角色生成失败", i + 1, count);
            }
        }
        
        logger.info("【批量角色生成完成】成功生成: {}/{} 个角色", characters.size(), count);
        logger.info("═══════════════════════════════════════════════════════════════");
        
        return characters;
    }

    private String generateCharacterJson(String theme) {
        logger.debug("【调用Ollama】开始请求AI模型...");
        
        try {
            String prompt = String.format("请为主题'%s'生成一个有趣的AI聊天角色，输出JSON格式：{\"name\": \"角色名称\", \"description\": \"角色描述\", \"prompt\": \"角色的系统提示词\"}", theme);
            logger.debug("【Ollama Prompt】{}", prompt.length() > 100 ? prompt.substring(0, 100) + "..." : prompt);
            
            logger.debug("【Ollama请求】POST http://localhost:11434/api/generate");
            String response = webClient.post()
                    .uri("/api/generate")
                    .bodyValue("{\"model\": \"llama3.2:3b\", \"prompt\": \"" + escapeJson(prompt) + "\", \"stream\": false}")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            if (response != null) {
                logger.debug("【Ollama响应】长度: {} 字符", response.length());
                JsonNode node = objectMapper.readTree(response);
                if (node.has("response")) {
                    String result = node.get("response").asText();
                    logger.debug("【提取响应】成功提取角色JSON");
                    return result;
                } else {
                    logger.warn("【Ollama响应】响应格式不正确，缺少'response'字段");
                }
            } else {
                logger.warn("【Ollama响应】响应为空");
            }
        } catch (Exception e) {
            logger.warn("【Ollama调用失败】原因: {}", e.getMessage());
            logger.debug("【Ollama调用失败】详细堆栈:", e);
        }
        
        logger.debug("【Ollama降级】返回空JSON，将使用本地生成");
        return "{}";
    }

    private AiCharacter generateFallbackCharacter(String theme) {
        logger.info("【备用生成】开始本地角色生成...");
        
        AiCharacter character = new AiCharacter();
        
        String name = generateRandomName();
        logger.info("【备用生成】生成名称: {}", name);
        character.setName(name);
        
        String avatarUrl = generateAvatarUrl();
        logger.info("【备用生成】生成头像: {}", avatarUrl);
        character.setAvatar(avatarUrl);
        
        String prompt = generateDefaultPrompt(name, theme);
        logger.info("【备用生成】生成Prompt: {} 字符", prompt.length());
        character.setPrompt(prompt);
        
        character.setIsSystem(false);
        
        characterMapper.insert(character);
        logger.info("【备用生成】保存到数据库成功，ID: {}", character.getId());
        
        return character;
    }

    private String generateRandomName() {
        String[] prefixes = {"智慧", "快乐", "神秘", "勇敢", "可爱", "聪明", "温柔", "活泼"};
        String[] suffixes = {"小猫", "博士", "精灵", "探险家", "守护者", "魔法师", "旅行者", "助手"};
        Random random = new Random();
        return prefixes[random.nextInt(prefixes.length)] + suffixes[random.nextInt(suffixes.length)];
    }

    private String generateAvatarUrl() {
        Random random = new Random();
        String style = AVATAR_STYLES[random.nextInt(AVATAR_STYLES.length)];
        String seed = "char_" + System.currentTimeMillis() + "_" + random.nextInt(1000);
        return "https://api.dicebear.com/7.x/" + style + "/svg?seed=" + seed;
    }

    private String generateDefaultPrompt(String name, String description) {
        return String.format("你是%s，%s。请用友好、有趣的方式与用户对话。", name, description);
    }

    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}
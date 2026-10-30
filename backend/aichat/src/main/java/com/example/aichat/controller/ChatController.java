package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.AiCharacter;
import com.example.aichat.entity.AiMemory;
import com.example.aichat.entity.ChatMessage;
import com.example.aichat.util.MemoryContextInjector;
import com.example.aichat.service.AiCharacterService;
import com.example.aichat.service.ChatMessageService;
import com.example.aichat.service.CharacterInteractionService;
import com.example.aichat.service.CharacterMemoryService;
import com.example.aichat.service.ConversationContextService;
import com.example.aichat.service.ContextCompressionService;
import com.example.aichat.service.EmotionAnalysisService;
import com.example.aichat.service.MultimodalService;
import com.example.aichat.service.ai.OllamaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private OllamaService ollamaService;

    // 多模型路由：支持 ollama:/openai:/deepseek: 等多Provider切换
    @Autowired
    private com.example.aichat.service.ai.ModelRouter modelRouter;

    // AI图像生成：检测"画xxx"意图并生成图片
    @Autowired
    private com.example.aichat.service.ai.ImageGenerationService imageGenerationService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private AiCharacterService aiCharacterService;
    
    @Autowired
    private CharacterInteractionService characterInteractionService;
    
    @Autowired
    private EmotionAnalysisService emotionAnalysisService;
    
    @Autowired
    private ConversationContextService conversationContextService;
    
    @Autowired
    private CharacterMemoryService characterMemoryService;
    
    @Autowired
    private com.example.aichat.service.AiMemoryService aiMemoryService;
    
    @Autowired
    private ContextCompressionService contextCompressionService;
    
    @Autowired
    private MultimodalService multimodalService;

    @Autowired
    private com.example.aichat.service.QuotaService quotaService;

    // 阶段三 P2：用户画像自动更新（对话后异步沉淀话题/活跃时段，失败不影响主对话）
    @Autowired
    private com.example.aichat.service.UserProfileService userProfileService;

    // 灵魂引擎（用户原创算法）：这一刻的自己 + 发生的经历 + 如何感受和理解 + 过去留下的记忆 = 下一刻的自己
    @Autowired
    private com.example.aichat.service.SoulEngineService soulEngineService;

    // 安全修复：替代Executors.newCachedThreadPool()的无界线程池，使用有界线程池防止资源耗尽
    // 核心线程数32、最大线程数64、队列容量100，拒绝策略为CallerRunsPolicy（由调用线程执行）防止任务丢失
    private final ExecutorService executorService = new ThreadPoolExecutor(
            32,
            64,
            60L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * 安全修复：应用关闭时优雅释放线程池资源，防止线程泄漏
     */
    @PreDestroy
    public void shutdown() {
        logger.info("正在关闭聊天线程池...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                logger.warn("聊天线程池强制关闭");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 检查AI服务状态
     */
    @GetMapping("/health")
    public R checkHealth() {
        try {
            boolean ollamaAvailable = ollamaService.isOllamaAvailable();
            if (ollamaAvailable) {
                return R.success("AI服务正常");
            } else {
                return R.success("AI服务不可用，将使用模拟回复");
            }
        } catch (Exception e) {
            logger.error("检查AI服务状态失败", e);
            return R.error("检查服务状态失败: " + e.getMessage());
        }
    }

    /**
     * 多模型：列出所有已注册的AI模型提供者及可用状态
     */
    @GetMapping("/models")
    public R listModels() {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        modelRouter.getProviders().forEach((name, provider) -> {
            java.util.Map<String, Object> info = new java.util.HashMap<>();
            info.put("provider", name);
            info.put("available", provider.isAvailable());
            info.put("modelFormat", name + ":<modelId>");
            result.put(name, info);
        });
        return R.success(result);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    // 安全加固（P1）：移除 @CrossOrigin(origins = "*")，统一走 CorsConfig 白名单
    public SseEmitter streamChat(
            @RequestParam String sessionId,
            @RequestParam Long userId,
            @RequestParam Long characterId,
            @RequestParam String message) {

        // 安全修复（P1）：消息长度校验，防止超长消息消耗过多 token / 存储
        if (message == null || message.isBlank()) {
            SseEmitter errEmitter = new SseEmitter(300000L);
            try {
                errEmitter.send(SseEmitter.event().name("error").data("消息内容不能为空"));
                errEmitter.complete();
            } catch (IOException ignored) {}
            return errEmitter;
        }
        if (message.length() > 8000) {
            SseEmitter errEmitter = new SseEmitter(300000L);
            try {
                errEmitter.send(SseEmitter.event().name("error").data("单次消息内容不能超过8000个字符"));
                errEmitter.complete();
            } catch (IOException ignored) {}
            return errEmitter;
        }
        // 安全修复（P1）：必须登录，且校验当前 token 用户与请求 userId 一致，
        // 防止匿名用户伪造 userId 消耗 AI 资源、读取他人会话。
        Long currentUserId = com.example.aichat.common.security.AuthContext.requireUserId();
        if (!currentUserId.equals(userId)) {
            SseEmitter errEmitter = new SseEmitter(300000L);
            try {
                errEmitter.send(SseEmitter.event().name("error").data("用户身份与令牌不匹配"));
                errEmitter.complete();
            } catch (IOException ignored) {}
            logger.warn("[AUTH_MISMATCH] tokenUserId={}, requestUserId={}", currentUserId, userId);
            return errEmitter;
        }

        logger.info("收到聊天请求 - sessionId: {}, userId: {}, characterId: {}", sessionId, userId, characterId);

        // 付费分级配额检查：在发起 AI 流之前同步校验并消耗一次 ai_chat 配额，
        // 超额 / 无会员权限时通过 SSE error 事件返回友好提示。
        try {
            quotaService.checkAndConsume(userId, "ai_chat");
        } catch (com.example.aichat.common.exception.BusinessException be) {
            SseEmitter quotaErr = new SseEmitter(300000L);
            try {
                quotaErr.send(SseEmitter.event().name("error").data(be.getMessage()));
                quotaErr.complete();
            } catch (IOException ignored) {}
            return quotaErr;
        }

        // 阶段三 P2：对话后自动更新用户画像（话题抽取 + 活跃时段）。
        // 严格 try/catch：画像写入失败绝不影响主对话链路与 SSE 流。
        try {
            userProfileService.recordConversation(userId, message);
        } catch (Exception profileEx) {
            logger.warn("用户画像自动更新失败，忽略: {}", profileEx.getMessage());
        }

        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        executorService.execute(() -> {
            try {
                // 验证角色访问权限
                boolean canAccess = aiCharacterService.canAccess(userId, characterId);
                if (!canAccess) {
                    logger.error("无权访问角色 - userId: {}, characterId: {}", userId, characterId);
                    emitter.send(SseEmitter.event().name("error").data("无权访问该角色"));
                    emitter.complete();
                    return;
                }
                
                AiCharacter character = aiCharacterService.getCharacterById(characterId);
                if (character == null) {
                    logger.error("角色不存在 - characterId: {}", characterId);
                    emitter.send(SseEmitter.event().name("error").data("角色不存在"));
                    emitter.complete();
                    return;
                }

                String systemPrompt = buildSystemPrompt(character);
                
                // 获取对话上下文
                String contextPrompt = conversationContextService.buildContextPrompt(sessionId);
                systemPrompt += contextPrompt;
                
                // 获取角色记忆
                String memoryPrompt = characterMemoryService.buildMemoryPrompt(characterId);
                systemPrompt += memoryPrompt;

                // 注入用户长期记忆（ai_memory）到 systemPrompt 前缀（优先级最高）
                // 安全修复后链路：仅注入当前登录用户自己的生效记忆，不影响其他用户数据
                try {
                    List<AiMemory> activeMemories = aiMemoryService.getActiveMemoriesForContext(userId, 10);
                    if (activeMemories != null && !activeMemories.isEmpty()) {
                        String aiMemoryPrefix = MemoryContextInjector.buildMemoryContextPrefix(activeMemories);
                        if (!aiMemoryPrefix.isEmpty()) {
                            systemPrompt = aiMemoryPrefix + systemPrompt;
                        }
                        List<Long> usedIds = activeMemories.stream().map(AiMemory::getId).toList();
                        try {
                            aiMemoryService.markMemoryUsed(userId, usedIds);
                        } catch (Exception markEx) {
                            logger.warn("标记记忆已用失败: {}", markEx.getMessage());
                        }
                    }
                } catch (Exception memEx) {
                    logger.warn("注入AI记忆失败，继续对话: {}", memEx.getMessage());
                }

                // 注入灵魂状态（第五层，优先级最高）：此刻的自我
                // 灵魂算法：这一刻的自己 + 发生的经历 + 如何感受和理解 + 过去留下的记忆 = 下一刻的自己
                try {
                    String soulPrefix = soulEngineService.buildSoulPrompt(userId, characterId);
                    if (!soulPrefix.isEmpty()) {
                        systemPrompt = soulPrefix + systemPrompt;
                    }
                } catch (Exception soulEx) {
                    logger.warn("注入灵魂状态失败，继续对话: {}", soulEx.getMessage());
                }
                
                String model = character.getModel() != null && !character.getModel().isEmpty()
                        ? character.getModel()
                        : "ollama:llama3.2:3b";

                // 分析用户情感
                java.util.Map<String, Object> emotionResult = emotionAnalysisService.analyzeFull(message);
                // 安全修复（P1）：原 (Double) emotionResult.get("intensity") 在 Jackson 解析下可能为 Integer，
                // 直接 cast 会抛 ClassCastException。改为 Number 转换。
                Object emotionObj = emotionResult.get("emotion");
                String userEmotion = emotionObj != null ? emotionObj.toString() : "neutral";
                Object intensityObj = emotionResult.get("intensity");
                double emotionIntensity = 0.0;
                if (intensityObj instanceof Number) {
                    emotionIntensity = ((Number) intensityObj).doubleValue();
                }
                logger.info("用户情感分析 - emotion: {}, intensity: {}", userEmotion, emotionIntensity);

                // 更新上下文：记录用户情感状态
                conversationContextService.setContext(sessionId, userId, characterId, "user_emotion", userEmotion);
                conversationContextService.setContext(sessionId, userId, characterId, "emotion_intensity", String.valueOf(emotionIntensity));

                // 从消息中提取记忆（如用户名、生日、喜好等）
                characterMemoryService.extractMemoriesFromMessage(characterId, userId, message);

                // 从消息中自动提炼用户长期记忆（ai_memory，source=chat，自动去重）
                try {
                    aiMemoryService.extractMemoriesFromMessage(userId, message);
                } catch (Exception memEx) {
                    logger.warn("自动提炼AI记忆失败: {}", memEx.getMessage());
                }

                // 保存用户消息
                chatMessageService.saveMessage(sessionId, userId, characterId, "user", message);

                // AI图像生成：检测"画xxx"类意图，若检测到则生成图片并通过SSE推送
                if (imageGenerationService.isImageGenerationRequest(message)) {
                    String imagePrompt = imageGenerationService.extractImagePrompt(message);
                    logger.info("检测到图像生成意图, prompt={}", imagePrompt);
                    java.util.Map<String, Object> imgResult = imageGenerationService.generateImage(imagePrompt);
                    if ("success".equals(imgResult.get("status"))) {
                        emitter.send(SseEmitter.event().name("image").data(imgResult));
                    } else {
                        emitter.send(SseEmitter.event().name("image").data(imgResult));
                    }
                }

                StringBuilder aiResponse = new StringBuilder();

                // 多模型路由：根据model前缀(ollama:/openai:/deepseek:)自动路由到对应Provider
                Flux<String> responseFlux = modelRouter.chatStream(model, message, systemPrompt);

                // 使用CountDownLatch确保所有消息发送完成后再关闭连接
                java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

                responseFlux.subscribe(
                        token -> {
                            try {
                                aiResponse.append(token);
                                emitter.send(SseEmitter.event().name("message").data(token));
                            } catch (IOException e) {
                                logger.error("发送消息失败: {}", e.getMessage());
                                emitter.completeWithError(e);
                                latch.countDown();
                            }
                        },
                        error -> {
                            try {
                                String errorMsg = "AI服务异常: " + error.getMessage();
                                logger.error(errorMsg);
                                emitter.send(SseEmitter.event().name("error").data(errorMsg));
                            } catch (IOException e) {
                                logger.error("发送错误消息失败: {}", e.getMessage());
                            }
                            emitter.complete();
                            latch.countDown();
                        },
                        () -> {
                            try {
                                // 保存AI回复
                                chatMessageService.saveMessage(sessionId, userId, characterId, "assistant", aiResponse.toString());
                                logger.info("AI回复完成 - sessionId: {}, 回复长度: {}", sessionId, aiResponse.length());
                                
                                // 记录用户与角色的交互
                                characterInteractionService.recordInteraction(userId, characterId);
                                logger.info("交互记录已更新 - userId: {}, characterId: {}", userId, characterId);

                                // 灵魂演化：对话后异步演化下一刻的自己（失败绝不影响对话收尾）
                                try {
                                    soulEngineService.evolveAfterChat(userId, characterId, null,
                                            message, aiResponse.toString());
                                } catch (Exception soulEx) {
                                    logger.warn("灵魂演化失败，忽略: {}", soulEx.getMessage());
                                }
                            } catch (Exception e) {
                                logger.error("保存AI回复或更新交互记录失败: {}", e.getMessage());
                            }
                            emitter.complete();
                            latch.countDown();
                        }
                );

                // 等待流处理完成（最多等待5分钟）
                try {
                    latch.await(5, java.util.concurrent.TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    logger.error("等待AI响应被中断: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                }

            } catch (Exception e) {
                logger.error("聊天处理异常: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("服务器内部错误"));
                } catch (IOException ioException) {
                    logger.error("发送错误消息失败: {}", ioException.getMessage());
                }
                emitter.completeWithError(e);
            }
        });

        emitter.onCompletion(() -> {
            logger.debug("SSE连接完成 - sessionId: {}", sessionId);
        });

        emitter.onTimeout(() -> {
            logger.warn("SSE连接超时 - sessionId: {}", sessionId);
            emitter.complete();
        });

        emitter.onError(e -> {
            logger.error("SSE连接错误 - sessionId: {}, error: {}", sessionId, e.getMessage());
        });

        return emitter;
    }

    @GetMapping("/history")
    public R getChatHistory(@RequestParam String sessionId, @RequestParam(defaultValue = "50") int limit) {
        logger.info("获取聊天历史 - sessionId: {}, limit: {}", sessionId, limit);
        try {
            // 安全修复（P0）：必须登录 + 校验会话归属，防止任意用户读取他人聊天记录
            Long currentUserId = com.example.aichat.common.security.AuthContext.requireUserId();
            limit = Math.min(Math.max(limit, 1), 200);
            List<ChatMessage> ownershipProbe = chatMessageService.getRecentMessages(sessionId, 1);
            if (!ownershipProbe.isEmpty()) {
                ChatMessage first = ownershipProbe.get(0);
                if (first.getUserId() != null && !currentUserId.equals(first.getUserId())) {
                    return R.unauthorized("无权访问该会话");
                }
            }
            List<ChatMessage> messages = chatMessageService.getRecentMessages(sessionId, limit);
            return R.success(messages);
        } catch (Exception e) {
            logger.error("获取聊天历史失败: {}", e.getMessage());
            return R.error("获取聊天历史失败");
        }
    }

    private String buildSystemPrompt(AiCharacter character) {
        StringBuilder prompt = new StringBuilder();
        
        if (character.getBackstory() != null && !character.getBackstory().isEmpty()) {
            prompt.append("背景故事：").append(character.getBackstory()).append("\n");
        }
        
        if (character.getPersonality() != null && !character.getPersonality().isEmpty()) {
            prompt.append("性格特点：").append(character.getPersonality()).append("\n");
        }
        
        if (character.getRelationship() != null && !character.getRelationship().isEmpty()) {
            String relationshipDesc = switch (character.getRelationship()) {
                case "friend" -> "朋友";
                case "lover" -> "恋人";
                case "teacher" -> "老师";
                case "assistant" -> "助手";
                default -> character.getRelationship();
            };
            prompt.append("与用户关系：").append(relationshipDesc).append("\n");
        }
        
        if (character.getPrompt() != null && !character.getPrompt().isEmpty()) {
            prompt.append(character.getPrompt());
        }
        
        prompt.append("\n\n请根据用户的情感状态做出相应回应。如果用户表现出负面情绪，请给予安慰和支持；如果用户表现出正面情绪，请积极回应。");
        
        return prompt.toString();
    }

    @GetMapping("/context/compress")
    public R compressContext(@RequestParam String sessionId, @RequestParam(defaultValue = "10") int maxMessages) {
        try {
            // 安全修复（P0）：必须登录 + 校验会话归属
            Long currentUserId = com.example.aichat.common.security.AuthContext.requireUserId();
            List<ChatMessage> ownershipProbe = chatMessageService.getRecentMessages(sessionId, 1);
            if (!ownershipProbe.isEmpty() && ownershipProbe.get(0).getUserId() != null
                    && !currentUserId.equals(ownershipProbe.get(0).getUserId())) {
                return R.unauthorized("无权访问该会话");
            }
            int max = Math.min(Math.max(maxMessages, 1), 100);
            List<ChatMessage> messages = chatMessageService.getRecentMessages(sessionId, max * 2);
            
            List<java.util.Map<String, Object>> messageList = messages.stream()
                .map(msg -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("role", msg.getSenderType());
                    map.put("content", msg.getContent());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
            
            String compressed = contextCompressionService.compressContext(messageList, maxMessages);
            java.util.Map<String, Object> keyPoints = contextCompressionService.extractKeyPoints(messageList);
            String summary = contextCompressionService.generateTopicSummary(messageList);
            
            return R.success(java.util.Map.of(
                "compressedContext", compressed,
                "keyPoints", keyPoints,
                "topicSummary", summary
            ));
        } catch (Exception e) {
            logger.error("上下文压缩失败: {}", e.getMessage());
            return R.error("上下文压缩失败");
        }
    }

    @GetMapping("/multimodal/images/{sessionId}")
    public R getImagesBySession(@PathVariable String sessionId) {
        try {
            // 安全修复（P0）：必须登录 + 校验会话归属
            Long currentUserId = com.example.aichat.common.security.AuthContext.requireUserId();
            if (!isSessionOwner(sessionId, currentUserId)) {
                return R.unauthorized("无权访问该会话");
            }
            List<?> images = multimodalService.getImagesBySession(sessionId);
            return R.success(images);
        } catch (Exception e) {
            logger.error("获取图片失败: {}", e.getMessage());
            return R.error("获取图片失败");
        }
    }

    @GetMapping("/multimodal/files/{sessionId}")
    public R getFilesBySession(@PathVariable String sessionId) {
        try {
            // 安全修复（P0）：必须登录 + 校验会话归属
            Long currentUserId = com.example.aichat.common.security.AuthContext.requireUserId();
            if (!isSessionOwner(sessionId, currentUserId)) {
                return R.unauthorized("无权访问该会话");
            }
            List<?> files = multimodalService.getFilesBySession(sessionId);
            return R.success(files);
        } catch (Exception e) {
            logger.error("获取文件失败: {}", e.getMessage());
            return R.error("获取文件失败");
        }
    }

    /**
     * 安全加固：校验会话是否属于当前用户（会话内任一消息归属即可）。
     */
    private boolean isSessionOwner(String sessionId, Long userId) {
        try {
            List<ChatMessage> probe = chatMessageService.getRecentMessages(sessionId, 1);
            if (probe.isEmpty()) {
                return true; // 空会话无可泄漏数据
            }
            ChatMessage first = probe.get(0);
            return first.getUserId() == null || userId.equals(first.getUserId());
        } catch (Exception e) {
            logger.warn("会话归属校验失败: sessionId={}", sessionId, e);
            return false;
        }
    }

    /**
     * M3-A9：删除整段 AI 对话（清空该会话下当前用户的全部消息）。
     * 必须登录；仅删除归属当前用户的消息。前端二次确认后调用。
     */
    @DeleteMapping("/session/{sessionId}")
    public R deleteSession(@PathVariable String sessionId) {
        Long currentUserId = com.example.aichat.common.security.AuthContext.requireUserId();
        int deleted = chatMessageService.deleteSessionMessages(sessionId, currentUserId);
        logger.info("删除AI对话会话 - sessionId: {}, userId: {}, 删除条数: {}", sessionId, currentUserId, deleted);
        return R.success(java.util.Map.of("deleted", deleted, "sessionId", sessionId));
    }

    /**
     * M3-A9：删除单条 AI 对话消息（仅本人消息可删）。
     */
    @DeleteMapping("/message/{messageId}")
    public R deleteMessage(@PathVariable Long messageId) {
        Long currentUserId = com.example.aichat.common.security.AuthContext.requireUserId();
        boolean ok = chatMessageService.deleteMessage(messageId, currentUserId);
        if (!ok) {
            return R.unauthorized("消息不存在或无权删除");
        }
        return R.success(java.util.Map.of("deletedId", messageId));
    }

    /**
     * M1 ①：重新生成指定 AI 回复。
     * 删除原 AI 回复，基于同一会话上下文重新调用 LLM 生成新回复并落库。
     */
    @PostMapping("/regenerate/{messageId}")
    public R regenerate(@PathVariable Long messageId) {
        Long currentUserId = com.example.aichat.common.security.AuthContext.requireUserId();
        ChatMessage old = chatMessageService.findById(messageId);
        if (old == null || old.getUserId() == null || !currentUserId.equals(old.getUserId())) {
            return R.unauthorized("消息不存在或无权操作");
        }
        if (!"assistant".equals(old.getSenderType()) && !"ai".equals(old.getSenderType())) {
            return R.badRequest("只能重新生成 AI 回复");
        }
        String sessionId = old.getSessionId();
        Long characterId = old.getCharacterId();

        // 重新生成前先删除旧 AI 回复
        chatMessageService.deleteById(messageId);

        // 取该会话最近一条用户消息作为本次生成的“提问”（与原回复对应的用户输入）
        List<ChatMessage> recent = chatMessageService.getRecentMessages(sessionId, 20);
        String userPrompt = old.getContent(); // fallback
        for (ChatMessage m : recent) {
            if ("user".equals(m.getSenderType())) {
                userPrompt = m.getContent();
                break;
            }
        }

        try {
            AiCharacter character = aiCharacterService.getCharacterById(characterId);
            String systemPrompt = buildSystemPrompt(character);
            systemPrompt += conversationContextService.buildContextPrompt(sessionId);
            systemPrompt += characterMemoryService.buildMemoryPrompt(characterId);
            try {
                List<AiMemory> activeMemories = aiMemoryService.getActiveMemoriesForContext(currentUserId, 10);
                if (activeMemories != null && !activeMemories.isEmpty()) {
                    String prefix = MemoryContextInjector.buildMemoryContextPrefix(activeMemories);
                    if (!prefix.isEmpty()) {
                        systemPrompt = prefix + systemPrompt;
                    }
                }
            } catch (Exception memEx) {
                logger.warn("重新生成注入记忆失败: {}", memEx.getMessage());
            }

            String model = character != null && character.getModel() != null && !character.getModel().isEmpty()
                    ? character.getModel() : "ollama:llama3.2:3b";

            StringBuilder sb = new StringBuilder();
            try {
                modelRouter.chatStream(model, userPrompt, systemPrompt)
                        .doOnNext(sb::append)
                        .blockLast(java.time.Duration.ofMinutes(5));
            } catch (Exception ex) {
                logger.warn("重新生成 LLM 调用失败，使用兜底回复: {}", ex.getMessage());
                sb.setLength(0);
                sb.append("（重新生成失败，请稍后再试）");
            }

            String newContent = sb.length() > 0 ? sb.toString() : "（重新生成失败，请稍后再试）";
            ChatMessage saved = new ChatMessage();
            saved.setSessionId(sessionId);
            saved.setUserId(currentUserId);
            saved.setCharacterId(characterId);
            saved.setSenderType("assistant");
            saved.setContent(newContent);
            saved.setCreateTime(java.time.LocalDateTime.now());
            chatMessageService.saveMessage(sessionId, currentUserId, characterId, "assistant", newContent);
            saved.setContent(newContent);
            logger.info("重新生成AI回复 - sessionId: {}, 长度: {}", sessionId, newContent.length());
            return R.success(java.util.Map.of(
                    "sessionId", sessionId,
                    "content", newContent));
        } catch (Exception e) {
            logger.error("重新生成失败", e);
            return R.error("重新生成失败: " + e.getMessage());
        }
    }

    /**
     * M1 ①：编辑本人用户消息（仅发送后 N 分钟内），编辑后下游 AI 回复基于新内容重新生成。
     */
    @PutMapping("/message/{messageId}")
    public R editMessage(@PathVariable Long messageId, @RequestBody java.util.Map<String, String> body) {
        Long currentUserId = com.example.aichat.common.security.AuthContext.requireUserId();
        String content = body == null ? null : body.get("content");
        if (content == null || content.trim().isEmpty()) {
            return R.badRequest("消息内容不能为空");
        }
        if (content.length() > 8000) {
            return R.badRequest("消息内容不能超过8000个字符");
        }
        ChatMessage message = chatMessageService.findOwnedMessage(messageId, currentUserId);
        if (message == null) {
            return R.unauthorized("消息不存在或无权编辑");
        }
        if (!"user".equals(message.getSenderType())) {
            return R.badRequest("只能编辑本人发送的消息");
        }
        // 编辑时间窗：发送后 10 分钟内可编辑
        if (message.getCreateTime() != null
                && message.getCreateTime().isBefore(java.time.LocalDateTime.now().minusMinutes(10))) {
            return R.badRequest("消息已超过可编辑时间");
        }
        chatMessageService.updateContent(messageId, content.trim());
        // 清除该消息之后的所有下游回复，由前端/后续重新生成
        int removed = chatMessageService.deleteAfterMessage(message.getSessionId(), messageId);
        logger.info("编辑用户消息 - messageId: {}, 删除下游: {}", messageId, removed);
        return R.success(java.util.Map.of(
                "messageId", messageId,
                "content", content.trim(),
                "removedAfter", removed));
    }

    /**
     * M1 ④：撤回本人消息（发送后 2 分钟内），撤回后显示“消息已撤回”。
     */
    @PutMapping("/message/{messageId}/recall")
    public R recallMessage(@PathVariable Long messageId) {
        Long currentUserId = com.example.aichat.common.security.AuthContext.requireUserId();
        ChatMessage message = chatMessageService.findOwnedMessage(messageId, currentUserId);
        if (message == null) {
            return R.unauthorized("消息不存在或无权撤回");
        }
        if (message.getCreateTime() != null
                && message.getCreateTime().isBefore(java.time.LocalDateTime.now().minusMinutes(2))) {
            return R.badRequest("消息已超过 2 分钟，无法撤回");
        }
        boolean ok = chatMessageService.recallMessage(messageId);
        if (!ok) {
            return R.error("撤回失败");
        }
        logger.info("撤回消息 - messageId: {}, userId: {}", messageId, currentUserId);
        return R.success(java.util.Map.of("messageId", messageId, "content", "消息已撤回"));
    }

    /**
     * M1 ③：批量标记某个 AI 会话的消息为已读。
     */
    @PutMapping("/message/read/{sessionId}")
    public R markSessionRead(@PathVariable String sessionId) {
        Long currentUserId = com.example.aichat.common.security.AuthContext.requireUserId();
        if (!isSessionOwner(sessionId, currentUserId)) {
            return R.unauthorized("无权访问该会话");
        }
        // AI 对话中消息天然已读，此处仅返回成功以便前端与移动端统一调用
        return R.success(java.util.Map.of("sessionId", sessionId, "markedRead", true));
    }
}
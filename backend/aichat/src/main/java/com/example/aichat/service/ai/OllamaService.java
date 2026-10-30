package com.example.aichat.service.ai;

import com.example.aichat.dto.ai.OllamaHealth;
import com.example.aichat.dto.ai.OllamaModelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ollama 本地模型客户端（底层实现）。
 *
 * 架构说明（Phase 1 重构标注）：
 *   此类为底层 HTTP 客户端，已被 OllamaModelProvider 适配为 ModelProvider 接口实现，
 *   并由 ModelRouter 统一路由。建议新代码不要直接 @Autowired OllamaService，
 *   而应注入 ModelRouter 或 ModelProvider（按 provider 名称）。
 *
 *   历史调用方（ChatController.health、ChatWebSocketHandler、AiSystemController）
 *   暂保留以避免破坏健康检查与 WebSocket 流，将在后续 P2 阶段迁移至 ModelProviderRegistry。
 */
@Deprecated
@Service
public class OllamaService {

    private static final Logger logger = LoggerFactory.getLogger(OllamaService.class);
    private static final String DEFAULT_MODEL = "llama3.2:3b";
    private static final String BASE_URL = "http://localhost:11434";
    /** 拉取模型是流式长操作，最长等待 120 秒。 */
    private static final Duration PULL_TIMEOUT = Duration.ofSeconds(120);

    private final WebClient webClient;

    /** 主构造器：显式 @Autowired 标记，避免与测试构造器并存时 Spring 构造器推断失败。 */
    @org.springframework.beans.factory.annotation.Autowired
    public OllamaService(WebClient.Builder webClientBuilder) {
        this(webClientBuilder.baseUrl(BASE_URL).build());
    }

    /** 测试用构造器：直接注入已构造好的 WebClient（可指向 mock / 死端口）。 */
    OllamaService(WebClient webClient) {
        this.webClient = webClient;
    }

    /** 模型管理操作结果。 */
    public record OllamaResult(boolean success, String message) {}

    public Flux<String> chatStream(String model, String prompt, String systemPrompt) {
        String actualModel = (model == null || model.isEmpty()) ? DEFAULT_MODEL : model;
        String safePrompt = (prompt == null) ? "" : prompt;
        logger.info("发起Ollama请求 - model: {}, prompt长度: {}", actualModel, safePrompt.length());

        boolean ollamaAvailable = isOllamaAvailable();
        if (!ollamaAvailable) {
            logger.info("Ollama服务不可用，使用模拟回复");
            String mockResponse = generateMockResponse(safePrompt, systemPrompt);
            return Flux.just(mockResponse);
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", actualModel);
        requestBody.put("prompt", prompt);
        requestBody.put("system", systemPrompt);
        requestBody.put("stream", true);
        requestBody.put("options", Map.of(
            "temperature", 0.7,
            "max_tokens", 2048
        ));

        return webClient.post()
                .uri("/api/generate")
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse -> {
                    logger.error("Ollama服务返回错误状态: {}", clientResponse.statusCode());
                    return clientResponse.bodyToMono(String.class)
                            .flatMap(body -> {
                                logger.error("Ollama错误响应体: {}", body);
                                if (body != null && body.contains("model not found")) {
                                    String errorMsg = String.format("模型 '%s' 未找到，请运行命令下载：ollama pull %s", actualModel, actualModel);
                                    return Mono.error(new RuntimeException(errorMsg));
                                }
                                return Mono.error(new RuntimeException("Ollama服务错误: " + body));
                            });
                })
                .bodyToFlux(Map.class)
                .map(response -> {
                    Object responseObj = response.get("response");
                    Object doneObj = response.get("done");
                    
                    if (doneObj != null && (Boolean) doneObj) {
                        return "";
                    }
                    
                    return responseObj != null ? (String) responseObj : "";
                })
                .filter(response -> !response.isEmpty())
                .onErrorResume(e -> {
                    logger.error("Ollama服务调用失败: {}", e.getMessage(), e);
                    String mockResponse = generateMockResponseWithError(safePrompt, systemPrompt, e.getMessage());
                    return Flux.just(mockResponse);
                });
    }

    /**
     * 生成模拟回复，当Ollama服务不可用时使用
     */
    private String generateMockResponse(String prompt, String systemPrompt) {
        StringBuilder mockResponse = new StringBuilder();
        
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            mockResponse.append("【角色设定】\n").append(systemPrompt).append("\n\n");
        }
        
        mockResponse.append(generateSmartResponse(prompt)).append("\n\n");
        mockResponse.append("---\n");
        mockResponse.append("💡 **提示**：当前AI服务使用模拟回复。如需完整AI体验，请：\n");
        mockResponse.append("1. 打开控制面板 → 系统 → 高级系统设置 → 环境变量\n");
        mockResponse.append("2. 添加系统变量：OLLAMA_MODELS=C:\\ollama-models\n");
        mockResponse.append("3. 重启Ollama服务（右键托盘图标退出后重新启动）\n");
        mockResponse.append("4. 运行命令：ollama pull llama3.2:3b\n");
        
        return mockResponse.toString();
    }

    private String generateMockResponseWithError(String prompt, String systemPrompt, String errorMessage) {
        StringBuilder mockResponse = new StringBuilder();
        
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            mockResponse.append("【角色设定】\n").append(systemPrompt).append("\n\n");
        }
        
        mockResponse.append(generateSmartResponse(prompt)).append("\n\n");
        mockResponse.append("---\n");
        mockResponse.append("⚠️ **AI服务异常**：").append(errorMessage).append("\n\n");
        mockResponse.append("🔧 **解决方案**：\n");
        
        if (errorMessage.contains("model not found")) {
            mockResponse.append("1. 模型未找到，请运行命令下载：\n");
            mockResponse.append("   `ollama pull llama3.2:3b`\n\n");
        } else if (errorMessage.contains("failed to load model")) {
            mockResponse.append("1. 模型加载失败，可能是中文路径问题\n");
            mockResponse.append("2. 设置环境变量：OLLAMA_MODELS=C:\\ollama-models\n");
            mockResponse.append("3. 重启Ollama服务并重新下载模型\n");
        } else {
            mockResponse.append("1. 请检查Ollama服务是否正在运行\n");
            mockResponse.append("2. 确保已下载模型：`ollama pull llama3.2:3b`\n");
            mockResponse.append("3. 检查网络连接是否正常\n");
        }
        
        return mockResponse.toString();
    }
    
    /**
     * 根据用户问题生成智能模拟回复
     */
    private String generateSmartResponse(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        
        if (lowerPrompt.contains("你好") || lowerPrompt.contains("hello") || lowerPrompt.contains("hi")) {
            return "您好！👋 我是您的AI助手。很高兴为您服务！\n\n请问有什么我可以帮助您的吗？我可以帮您解答问题、提供建议、编写代码等。";
        }
        
        if (lowerPrompt.contains("帮助") || lowerPrompt.contains("help")) {
            return "当然可以！我可以帮助您：\n\n📝 **常见功能**：\n- 解答各种问题（科学、技术、历史等）\n- 编写和调试代码\n- 提供写作建议和文案创作\n- 翻译和语言学习\n- 创意生成（故事、诗歌等）\n- 数据分析和建议\n\n💬 请告诉我您需要什么帮助！";
        }
        
        if (lowerPrompt.contains("代码") || lowerPrompt.contains("code") || lowerPrompt.contains("编程")) {
            return "我可以帮助您编写代码！💻\n\n请告诉我您需要什么：\n- 编写新代码\n- 调试现有代码\n- 代码优化建议\n- 解释代码逻辑\n- 学习某种编程语言\n\n请提供具体的需求描述。";
        }
        
        if (lowerPrompt.contains("故事") || lowerPrompt.contains("story") || lowerPrompt.contains("写")) {
            return "好的！我可以为您创作故事、诗歌、文章等。🎨\n\n请告诉我：\n- 故事类型（科幻、奇幻、悬疑等）\n- 主题或角色设定\n- 大致长度要求\n\n让我为您创作一个精彩的故事！";
        }
        
        if (lowerPrompt.contains("翻译") || lowerPrompt.contains("translate")) {
            return "我可以帮助您进行翻译！🌍\n\n支持多种语言互译：\n- 中文 ↔ 英文\n- 中文 ↔ 日语\n- 中文 ↔ 韩语\n- 以及其他多种语言\n\n请提供您需要翻译的文本和目标语言。";
        }
        
        if (lowerPrompt.contains("数学") || lowerPrompt.contains("math") || lowerPrompt.contains("计算")) {
            return "数学问题我可以帮忙！🧮\n\n请告诉我您的问题：\n- 代数方程求解\n- 几何问题\n- 微积分\n- 统计分析\n- 其他数学问题\n\n请提供具体的题目。";
        }
        
        if (lowerPrompt.contains("时间") || lowerPrompt.contains("日期") || lowerPrompt.contains("今天")) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");
            return "📅 当前时间：" + now.format(formatter) + "\n\n请问您需要什么时间相关的帮助？";
        }
        
        if (lowerPrompt.contains("天气") || lowerPrompt.contains("weather")) {
            return "🌤️ 关于天气信息：\n\n我目前无法直接获取实时天气数据，但您可以：\n- 在手机上查看天气应用\n- 在浏览器中搜索当地天气\n- 使用专业的天气API服务\n\n如需其他帮助，请随时告诉我！";
        }
        
        if (lowerPrompt.contains("游戏") || lowerPrompt.contains("game")) {
            return "🎮 游戏相关问题？我可以帮助您：\n\n- 推荐游戏\n- 游戏攻略和技巧\n- 游戏开发知识\n- 讨论游戏剧情\n\n请告诉我您感兴趣的游戏类型或具体游戏！";
        }
        
        if (lowerPrompt.contains("音乐") || lowerPrompt.contains("music") || lowerPrompt.contains("歌")) {
            return "🎵 音乐相关话题！\n\n我可以：\n- 推荐音乐\n- 分析音乐风格\n- 讨论歌词含义\n- 分享音乐知识\n\n请告诉我您喜欢的音乐类型或歌手！";
        }
        
        if (lowerPrompt.contains("电影") || lowerPrompt.contains("movie") || lowerPrompt.contains("视频")) {
            return "🎬 电影和视频！\n\n我可以：\n- 推荐电影和剧集\n- 分析电影剧情和主题\n- 讨论电影制作\n- 分享观影心得\n\n请告诉我您喜欢的电影类型！";
        }
        
        if (lowerPrompt.contains("学习") || lowerPrompt.contains("study") || lowerPrompt.contains("教育")) {
            return "📚 学习相关问题！\n\n我可以帮助您：\n- 制定学习计划\n- 提供学习资源推荐\n- 解答学科问题\n- 分享学习方法和技巧\n\n请告诉我您正在学习的内容！";
        }
        
        if (lowerPrompt.contains("健康") || lowerPrompt.contains("health") || lowerPrompt.contains("健身")) {
            return "🏥 健康和健身！\n\n我可以提供：\n- 健康饮食建议\n- 健身计划推荐\n- 生活习惯指导\n- 心理健康建议\n\n⚠️ 请注意：我的建议仅供参考，不能替代专业医疗建议。";
        }
        
        if (lowerPrompt.contains("历史") || lowerPrompt.contains("history")) {
            return "🏛️ 历史话题！\n\n我可以帮助您：\n- 解答历史问题\n- 介绍历史事件\n- 分析历史人物\n- 讨论历史趋势\n\n请告诉我您感兴趣的历史时期或事件！";
        }
        
        if (lowerPrompt.contains("科学") || lowerPrompt.contains("science") || lowerPrompt.contains("知识")) {
            return "🔬 科学知识！\n\n我可以：\n- 解释科学概念\n- 解答科学问题\n- 讨论最新科技进展\n- 分享科普知识\n\n请告诉我您感兴趣的科学领域！";
        }
        
        if (lowerPrompt.contains("情感") || lowerPrompt.contains("心理") || lowerPrompt.contains("情绪")) {
            return "❤️ 情感和心理支持！\n\n我可以：\n- 倾听您的烦恼\n- 提供情感支持\n- 分享心理知识\n- 给出建议和鼓励\n\n请告诉我您的困扰，我在这里听您倾诉。";
        }
        
        if (lowerPrompt.contains("推荐") || lowerPrompt.contains("suggest")) {
            return "✨ 我可以为您提供推荐！\n\n请告诉我您需要什么类型的推荐：\n- 书籍推荐\n- 电影推荐\n- 音乐推荐\n- 旅行目的地推荐\n- 产品推荐\n\n请提供更多具体信息！";
        }
        
        if (lowerPrompt.contains("什么") || lowerPrompt.contains("why") || lowerPrompt.contains("为什么")) {
            return "🤔 这是一个有趣的问题！\n\n关于您的问题，我可以从多个角度为您分析：\n\n**可能的解释**：\n- 从科学角度分析\n- 从历史角度回顾\n- 从逻辑角度推理\n- 从实践角度探讨\n\n由于AI服务暂时不可用，我无法提供详细的实时回答。请描述更多细节，我会尽力为您解答！";
        }
        
        return "感谢您的提问！🙋\n\n" + prompt + "\n\n这是一个很有意思的话题。由于AI服务暂时不可用，我无法提供详细的实时回答。\n\n不过，我可以告诉您一些通用信息：\n\n**相关主题**：\n- 这是一个关于" + (prompt.length() > 10 ? prompt.substring(0, 10) + "..." : prompt) + "的问题\n- 您可能对相关领域感兴趣\n- 建议查阅更多资料以获取深入了解\n\n**提示**：当Ollama服务正常运行后，您将获得完整的AI对话体验！";
    }

    /**
     * 检查Ollama服务是否可用
     */
    public boolean isOllamaAvailable() {
        try {
            webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            logger.warn("Ollama服务不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 列出已安装模型：GET /api/tags。
     * Ollama 不可用时返回空列表，不抛异常（优雅降级）。
     */
    @SuppressWarnings("unchecked")
    public List<OllamaModelInfo> listModels() {
        try {
            Map<String, Object> resp = webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (resp == null) {
                return List.of();
            }
            Object modelsObj = resp.get("models");
            if (!(modelsObj instanceof List)) {
                return List.of();
            }
            List<OllamaModelInfo> result = new ArrayList<>();
            for (Object item : (List<Object>) modelsObj) {
                if (item instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) item;
                    OllamaModelInfo info = new OllamaModelInfo();
                    info.setName((String) m.get("name"));
                    Object size = m.get("size");
                    if (size instanceof Number) {
                        info.setSize(((Number) size).longValue());
                    }
                    Object modified = m.get("modified_at");
                    info.setModifiedAt(modified != null ? modified.toString() : null);
                    info.setDigest((String) m.get("digest"));
                    result.add(info);
                }
            }
            return result;
        } catch (Exception e) {
            logger.warn("Ollama列出模型失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 拉取模型：POST /api/pull（流式 NDJSON），读取最终状态。
     * 超过 120 秒超时返回失败；Ollama 不可用返回失败。
     */
    public OllamaResult pullModel(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return new OllamaResult(false, "模型名称不能为空");
        }
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("name", modelName);
            Map last = webClient.post()
                    .uri("/api/pull")
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .timeout(PULL_TIMEOUT)
                    .blockLast();
            if (last != null && last.get("error") != null) {
                return new OllamaResult(false, "拉取失败: " + last.get("error"));
            }
            return new OllamaResult(true, "模型拉取完成: " + modelName);
        } catch (Exception e) {
            logger.warn("Ollama拉取模型失败: {}", e.getMessage());
            return new OllamaResult(false, "拉取模型失败: " + modelName);
        }
    }

    /**
     * 删除模型：DELETE /api/delete。
     */
    public OllamaResult deleteModel(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return new OllamaResult(false, "模型名称不能为空");
        }
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("name", modelName);
            webClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri("/api/delete")
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return new OllamaResult(true, "模型删除成功: " + modelName);
        } catch (Exception e) {
            logger.warn("Ollama删除模型失败: {}", e.getMessage());
            return new OllamaResult(false, "删除模型失败: " + modelName);
        }
    }

    /**
     * Ollama 健康信息：GET /api/version + listModels。
     * Ollama 不可用时 available=false、空模型列表，不抛异常。
     */
    public OllamaHealth getOllamaHealth() {
        OllamaHealth health = new OllamaHealth();
        try {
            Map versionResp = webClient.get()
                    .uri("/api/version")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            String version = (versionResp != null && versionResp.get("version") != null)
                    ? versionResp.get("version").toString()
                    : null;
            List<OllamaModelInfo> models = listModels();
            health.setAvailable(true);
            health.setVersion(version);
            health.setModels(models);
            health.setModelCount(models.size());
        } catch (Exception e) {
            logger.warn("Ollama健康检查失败: {}", e.getMessage());
            health.setAvailable(false);
            health.setVersion(null);
            health.setModelCount(0);
            health.setModels(List.of());
        }
        return health;
    }
}
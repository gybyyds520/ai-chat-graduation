package com.example.aichat.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.aichat.entity.AiMemory;

import java.util.List;
import java.util.Map;

/**
 * AI 记忆服务。
 *
 * 设计要点：
 *   1. 所有方法都以 userId 为归属维度，Service 层在读取/修改/删除前校验记忆归属，
 *      杜绝横向越权（IDOR）；Controller 仅负责取当前登录用户并传入。
 *   2. 记忆注入上下文方法（getActiveMemoriesForContext / markMemoryUsed）
 *      不修改 ConversationContextService 核心逻辑，由调用方在拼装 systemPrompt 时使用。
 */
public interface AiMemoryService {

    /** 创建记忆。source 默认 manual，importance 默认 3，isActive 默认 true。 */
    AiMemory createMemory(Long userId, String content, String source,
                          String memoryType, Integer importance, String category);

    /** 更新记忆（校验归属）。仅更新传入的可空字段，null 表示不修改。 */
    AiMemory updateMemory(Long userId, Long memoryId, String content,
                         String memoryType, Integer importance, String category, Boolean isActive);

    /** 删除记忆（校验归属）。 */
    void deleteMemory(Long userId, Long memoryId);

    /** 查询记忆详情（校验归属）。不存在或非本人抛出业务异常。 */
    AiMemory getMemory(Long userId, Long memoryId);

    /** 分页+过滤查询当前用户记忆。memoryType/category/isActive/keyword/source 为可空过滤条件。 */
    IPage<AiMemory> listMemories(Long userId, int page, int size,
                                 String memoryType, String category, Boolean isActive,
                                 String keyword, String source);

    /** 统计：总数、各类型数量、重要性分布。 */
    Map<String, Object> getMemoryStats(Long userId);

    /** 切换记忆生效状态（isActive 取反，校验归属）。 */
    AiMemory toggleMemory(Long userId, Long memoryId);

    /**
     * 取当前用户可注入上下文的记忆：isActive=true，按 importance 降序 + lastUsedAt 降序。
     * 不修改 ConversationContextService，仅供 ChatController 在拼装 systemPrompt 前缀时调用。
     */
    List<AiMemory> getActiveMemoriesForContext(Long userId, int limit);

    /** 标记记忆已被使用：批量更新 lastUsedAt 为当前时间（仅本人记忆）。 */
    void markMemoryUsed(Long userId, List<Long> memoryIds);

    /**
     * 从用户聊天消息中自动提炼长期记忆（规则式）并去重落库。
     * 匹配"我叫/我喜欢/我讨厌/我住在/我今年X岁/生日/职业"等模式，
     * 提炼内容写入 ai_memory（source=chat），相同或高度相似内容自动跳过。
     * 返回本次新增记忆条数。
     */
    int extractMemoriesFromMessage(Long userId, String message);

    /**
     * 读取记忆全局开关。无记录时默认开启（enabled=true）。
     * @return Map{ enabled: boolean }
     */
    Map<String, Object> getMemorySetting(Long userId);

    /**
     * 更新记忆全局开关（enabled/disabled）。开关关闭后不再向对话注入长期记忆。
     */
    void updateMemorySetting(Long userId, boolean enabled);

    /**
     * 对话中“钉选”某条消息加入长期记忆（source=pinned，importance=5）。
     * 内容为空抛业务异常；归属当前用户。
     */
    AiMemory pinMessageToMemory(Long userId, String content, String source);

    // ================= 市场级升级：批量删除 / 导出 / 语义召回 / 主动引用 =================

    /**
     * 批量删除记忆（逐条校验归属，非本人/不存在的 id 跳过，不中断整批）。
     *
     * @return 实际删除条数
     */
    int batchDeleteMemories(Long userId, List<Long> memoryIds);

    /**
     * 导出当前用户全部生效记忆（JSON 导出用）。按评分（重要度*衰减）降序。
     */
    List<AiMemory> exportAllMemories(Long userId);

    /**
     * 语义召回相关记忆（市场级升级 3/4）：
     * <ul>
     *   <li>向量开关开启时：query embedding → Chroma 召回 topK → 融合时间衰减+重要度排序</li>
     *   <li>向量开关关闭 / sidecar 不可用时：降级为本地关键词召回，同样按融合评分排序</li>
     * </ul>
     * 命中的记忆会更新 lastUsedAt + referencedCount。
     *
     * @param query 用户最新消息
     * @param topK  召回条数
     * @return 相关记忆（按融合分降序）
     */
    List<AiMemory> recallRelevantMemories(Long userId, String query, int topK);

    /**
     * 主动引用：取出与 query 最相关的 topK 条记忆内容，格式化为可注入 prompt 的片段。
     * 同时更新这些记忆的 lastUsedAt 与 referencedCount。
     *
     * @return 形如 "用户记得：{content1}；{content2}" 的注入串；无相关记忆返回空串
     */
    String buildActiveReferenceContext(Long userId, String query, int topK);
}

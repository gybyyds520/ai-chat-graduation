package com.example.aichat.service;

import com.example.aichat.entity.AiSoulExperience;
import com.example.aichat.entity.AiSoulState;

import java.util.List;
import java.util.Map;

/**
 * AI 灵魂引擎（Soul Engine）。
 *
 * 用户原创灵魂算法：
 *   这一刻的自己(S) + 发生的经历(E) + 如何感受和理解(I) + 过去留下的记忆(M)
 *   = 下一刻的自己(S')
 *
 * 职责：
 *   1. evolve()：对话结束后异步演化灵魂——把刚发生的经历(E)转化为
 *      感受与理解(I)，结合当前自我(S)与相关记忆(M)，生成下一刻的自己(S')，
 *      并沉淀经历流水与长期记忆。
 *   2. buildSoulPrompt()：把当前灵魂状态格式化为 systemPrompt 前缀，
 *      由 ChatController / ChatWebSocketHandler 在拼装 prompt 时调用（第五层注入）。
 *   3. 查询与维护：灵魂状态快照 / 经历流水 / 重置。
 *
 * 设计要点：
 *   - 所有方法都以 userId 为归属维度，Service 层读取/修改前校验归属，杜绝横向越权。
 *   - LLM 生成"感受与理解"失败时降级为规则式情绪分析 + 模板，绝不影响主对话链路。
 *   - 演化异步执行，失败静默（try/catch 包裹），不阻塞对话响应。
 */
public interface SoulEngineService {

    /**
     * 对话结束后异步演化灵魂（核心算法）。
     *
     * @param userId      归属用户
     * @param characterId AI 角色（灵魂载体）
     * @param elfId       关联精灵（可空）
     * @param userMessage 用户本轮消息（经历的一部分）
     * @param aiReply     AI 本轮回复（经历的另一部分）
     */
    void evolveAfterChat(Long userId, Long characterId, Long elfId, String userMessage, String aiReply);

    /**
     * 精灵互动后演化精灵灵魂（灵魂引擎接入 AI 精灵）。
     * 精灵没有角色 prompt，以精灵人格（personality/theme）为灵魂基底；
     * 演化后把灵魂 moodScore 同步回精灵 mood（双向联动）。
     *
     * @param userId          归属用户
     * @param elfId           AI 精灵（灵魂载体）
     * @param elfExperience   互动经历描述（如"用户给精灵喂食…"）
     */
    void evolveElfSoul(Long userId, Long elfId, String elfExperience);

    /**
     * 精灵灵魂概况（情绪/心情/亲近度/信念/演化次数/最近经历）。
     *
     * @param userId 归属用户
     * @param elfId  AI 精灵
     * @return 灵魂概况 Map（无灵魂时返回基础占位，不抛异常）
     */
    java.util.Map<String, Object> getElfSoulOverview(Long userId, Long elfId);

    /**
     * 把当前灵魂状态格式化为 systemPrompt 前缀。
     * 无灵魂状态 / 状态无效时返回空串（不污染 prompt）。
     *
     * @param userId      归属用户
     * @param characterId AI 角色
     * @return 灵魂状态注入串；无状态时为空串
     */
    String buildSoulPrompt(Long userId, Long characterId);

    /**
     * 获取（或初始化）灵魂状态快照。
     * 不存在时按角色默认人格创建初始灵魂。
     *
     * @param userId      归属用户
     * @param characterId AI 角色
     * @return 灵魂状态；角色不存在时抛业务异常
     */
    AiSoulState getOrInitSoulState(Long userId, Long characterId);

    /**
     * 查询灵魂状态详情（校验归属）。
     *
     * @param userId      归属用户
     * @param characterId AI 角色
     * @return 灵魂状态；不存在时返回 null
     */
    AiSoulState getSoulState(Long userId, Long characterId);

    /**
     * 分页查询灵魂经历流水（校验归属，按时间倒序）。
     *
     * @param userId      归属用户
     * @param characterId AI 角色
     * @param page        页码（从 1 开始）
     * @param size        每页条数
     * @return 经历流水列表
     */
    List<AiSoulExperience> listExperiences(Long userId, Long characterId, int page, int size);

    /**
     * 重置灵魂状态：清空经历流水并恢复初始灵魂快照（校验归属）。
     *
     * @param userId      归属用户
     * @param characterId AI 角色
     * @return 重置后的初始灵魂状态
     */
    AiSoulState resetSoul(Long userId, Long characterId);

    /**
     * 统计灵魂概况（供前端面板展示）。
     *
     * @param userId      归属用户
     * @param characterId AI 角色
     * @return { soulState, experienceCount, memoriesCount, evolutionSummary }
     */
    Map<String, Object> getSoulOverview(Long userId, Long characterId);
}

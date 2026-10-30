package com.example.aichat.service;

import com.example.aichat.dto.search.DeepResearchReport;

import java.util.List;
import java.util.Map;

/**
 * 深度研究服务（简化版）。
 *
 * 多轮自主搜索 + 结果整合，离线可测（搜索调用 WebSearchService，测试时 mock）。
 * 研究状态存内存，不依赖数据库。
 */
public interface DeepResearchService {

    /**
     * 启动研究任务。
     *
     * @param userId    用户 ID
     * @param query     研究问题
     * @param maxRounds 最大搜索轮次，默认 3
     * @return 研究任务 ID
     */
    String startResearch(Long userId, String query, Integer maxRounds);

    /**
     * 获取研究状态（含轮次与进度）。
     *
     * @param userId     当前用户 ID（用于越权校验）
     * @param researchId 研究任务 ID
     * @return 状态信息
     */
    Map<String, Object> getResearchStatus(Long userId, String researchId);

    /**
     * 获取研究报告。
     *
     * @param userId     当前用户 ID（用于越权校验）
     * @param researchId 研究任务 ID
     * @return 研究报告
     */
    DeepResearchReport getResearchResult(Long userId, String researchId);

    /**
     * 分页查询用户研究历史。
     *
     * @param userId 用户 ID
     * @param page   页码，从 1 开始
     * @param size   每页大小
     * @return 历史报告列表（新的在前）
     */
    List<DeepResearchReport> listResearches(Long userId, int page, int size);

    /**
     * 研究进度回调接口（A8：SSE 流式输出）。
     * 由 Controller 层实现，每轮搜索/整合时回调推送进度事件。
     */
    interface ProgressListener {
        /** 推送一个进度事件。eventName 如 "round"、"finding"、"complete"、"error"。 */
        void onProgress(String eventName, Object data);
    }

    /**
     * 启动研究任务并通过 ProgressListener 流式推送进度（A8：SSE）。
     *
     * @param userId    用户 ID
     * @param query     研究问题
     * @param maxRounds 最大搜索轮次
     * @param listener  进度回调
     * @return 研究任务 ID
     */
    String startResearchWithListener(Long userId, String query, Integer maxRounds, ProgressListener listener);
}

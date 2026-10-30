package com.example.aichat.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 深度研究报告 DTO（简化版，离线可测）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeepResearchReport {

    /**
     * 研究任务 ID。
     */
    private String researchId;

    /**
     * 原始研究问题。
     */
    private String query;

    /**
     * 状态：pending/searching/round2/integrating/completed/failed。
     */
    private String status;

    /**
     * 当前轮次。
     */
    private Integer currentRound;

    /**
     * 进度 0-100。
     */
    private Integer progress;

    /**
     * 整合摘要。
     */
    private String summary;

    /**
     * 关键发现列表。
     */
    private List<String> keyFindings;

    /**
     * 所有引用来源（按 url 去重，编号 1..M）。
     */
    private List<SourceCitation> sources;

    /**
     * 每轮搜索详情。
     */
    private List<RoundDetail> roundDetails;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 完成时间。
     */
    private LocalDateTime completedAt;

    /**
     * 单轮搜索详情。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundDetail {
        /**
         * 轮次号，从 1 开始。
         */
        private Integer round;

        /**
         * 本轮查询词。
         */
        private String query;

        /**
         * 本轮结果数。
         */
        private Integer resultCount;

        /**
         * top3 结果标题。
         */
        private List<String> topTitles;
    }
}

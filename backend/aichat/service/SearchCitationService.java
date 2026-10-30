package com.example.aichat.service;

import com.example.aichat.dto.search.CitedAnswer;
import com.example.aichat.dto.search.SourceCitation;
import com.example.aichat.entity.SearchResult;

import java.util.List;

/**
 * 搜索结果引用标注服务。
 *
 * 负责：
 * 1. 从搜索结果构建结构化来源引用列表；
 * 2. 在回答文本中按句子插入引用序号 [index]；
 * 3. 一站式生成带引用标注的回答。
 */
public interface SearchCitationService {

    /**
     * 从搜索结果构建引用列表，index 从 1 开始递增。
     *
     * @param results 搜索结果列表
     * @return 结构化引用列表
     */
    List<SourceCitation> buildCitations(List<SearchResult> results);

    /**
     * 在回答文本中插入引用序号。
     *
     * 按句子切分回答，逐句与每个 citation 的 snippet 计算关键词重叠率
     * （中文 2-gram + 英文词），超过阈值则在句末插入 [index]；
     * 若无任何句子命中，则在回答末尾追加 "参考来源：[1] [2] ..."。
     *
     * @param answer    原始回答文本
     * @param citations 引用列表
     * @return 带引用标注的回答文本
     */
    String annotateAnswerWithCitations(String answer, List<SourceCitation> citations);

    /**
     * 一站式：由原始回答 + 搜索结果生成带引用标注的回答与引用列表。
     *
     * @param answer  原始回答文本
     * @param results 搜索结果列表
     * @return 带引用标注的回答结果
     */
    CitedAnswer buildAnswerWithSources(String answer, List<SearchResult> results);
}

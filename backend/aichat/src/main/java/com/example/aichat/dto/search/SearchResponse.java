package com.example.aichat.dto.search;

import com.example.aichat.entity.SearchResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 结构化搜索响应 DTO。
 *
 * 在既有搜索结果（results，保持对前端兼容）基础上新增结构化来源引用 citations。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {

    /**
     * 搜索词。
     */
    private String query;

    /**
     * 结果总数。
     */
    private Integer totalResults;

    /**
     * 搜索结果列表（保持既有结构，前端可继续使用）。
     */
    private List<SearchResult> results;

    /**
     * 结构化来源引用列表（新增）。
     */
    private List<SourceCitation> citations;

    /**
     * 搜索耗时（毫秒）。
     */
    private Long searchTime;
}

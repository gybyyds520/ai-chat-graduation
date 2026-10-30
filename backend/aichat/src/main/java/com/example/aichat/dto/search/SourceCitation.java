package com.example.aichat.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 结构化来源引用 DTO。
 *
 * 用于在搜索回答中以 [index] 形式标注信息来源，前端据此渲染可点击的来源列表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceCitation {

    /**
     * 引用序号，从 1 开始，与回答文本中的 [index] 对应。
     */
    private Integer index;

    /**
     * 来源标题。
     */
    private String title;

    /**
     * 来源链接 URL。
     */
    private String url;

    /**
     * 来源摘要片段。
     */
    private String snippet;

    /**
     * 来源网站（如 baidu / bing / 域名）。
     */
    private String source;

    /**
     * 发布时间（可空）。
     */
    private String publishTime;

    /**
     * 相关度评分 0-1（可空）。
     */
    private Double relevanceScore;
}

package com.example.aichat.dto.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 带引用标注的回答结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitedAnswer {

    /**
     * 插入引用序号 [index] 后的回答文本。
     */
    private String answerWithCitations;

    /**
     * 引用来源列表。
     */
    private List<SourceCitation> citations;
}

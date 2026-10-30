package com.example.aichat.dto.knowledge;

import lombok.Data;

/**
 * 问答引用片段摘要（用于前端展示来源）。
 */
@Data
public class ChunkInfo {

    /** 分块序号 */
    private Integer chunkIndex;

    /** 分块内容（可截断显示） */
    private String content;

    /** 与问题的相似度分数 [0,1] */
    private double similarity;

    public ChunkInfo() {
    }

    public ChunkInfo(Integer chunkIndex, String content, double similarity) {
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.similarity = similarity;
    }
}

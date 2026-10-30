package com.example.aichat.dto.knowledge;

import lombok.Data;

import java.util.List;

/**
 * 知识库问答响应体。
 */
@Data
public class AskResponse {

    /** 回答内容（当前为离线抽取式回答） */
    private String answer;

    /** 引用的片段列表 */
    private List<ChunkInfo> sourceChunks;

    /** 提问所基于的文档 ID */
    private Long documentId;

    /** 用户问题 */
    private String query;

    public AskResponse() {
    }

    public AskResponse(String answer, List<ChunkInfo> sourceChunks, Long documentId, String query) {
        this.answer = answer;
        this.sourceChunks = sourceChunks;
        this.documentId = documentId;
        this.query = query;
    }
}

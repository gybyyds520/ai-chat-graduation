package com.example.aichat.dto.knowledge;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 知识库问答请求体。
 *
 * 阶段二 P1：补全 Bean Validation。query 必填且限长，topK 限定合理区间，
 * 配合 KnowledgeBaseController.ask 上的 @Valid 在 HTTP 层自动拦截非法入参。
 */
@Data
public class AskRequest {

    /** 用户问题 */
    @NotBlank(message = "问题不能为空")
    @Size(max = 2000, message = "问题长度不能超过2000个字符")
    private String query;

    /** 检索返回的 top-k 片段数量（可选，缺省由服务端配置） */
    @Min(value = 1, message = "topK 最小为1")
    @Max(value = 50, message = "topK 最大为50")
    private Integer topK;

    /**
     * 多文档问答范围：指定文档 ID 列表。
     * 为空 / null 表示在当前用户全部文档中检索（全部文档问答）。
     */
    private List<Long> documentIds;
}

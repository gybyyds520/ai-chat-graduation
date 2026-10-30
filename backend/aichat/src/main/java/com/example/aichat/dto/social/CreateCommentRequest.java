package com.example.aichat.dto.social;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建评论请求 DTO。
 *
 * Phase 3 (P1-2) 引入：替代 SocialController.commentPost() 接收 Map + 手写校验，
 * 配合 SocialErrorCode.POST_NOT_FOUND / COMMENT_CONTENT_BLANK 标准化错误响应。
 */
@Data
public class CreateCommentRequest {

    @NotNull(message = "动态 ID 不能为空")
    @Positive(message = "动态 ID 必须为正数")
    private Long postId;

    @NotNull(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过500个字符")
    private String content;

    /** 父评论 ID（可选，存在则为回复评论） */
    @Positive(message = "父评论 ID 必须为正数")
    private Long parentId;
}

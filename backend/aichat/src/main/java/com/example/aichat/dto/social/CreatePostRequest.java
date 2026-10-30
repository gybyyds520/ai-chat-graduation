package com.example.aichat.dto.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布动态请求 DTO。
 *
 * Phase 3 (P1-2) 引入：替代 SocialController.createPost() 接收 Map<String, String> + 手写校验，
 * 配合 SocialErrorCode.POST_CONTENT_BLANK / POST_CONTENT_TOO_LONG 标准化错误响应。
 */
@Data
public class CreatePostRequest {

    @NotBlank(message = "动态内容不能为空")
    @Size(max = 2000, message = "动态内容不能超过2000个字符")
    private String content;

    /** 图片 URL 列表（可选，逗号分隔或后续扩展为数组） */
    @Size(max = 2000, message = "图片字段长度超限")
    private String images;

    /** 标签（可选） */
    @Size(max = 200, message = "标签长度超限")
    private String tags;

    /** 是否公开：0=公开，1=仅好友可见，2=私密 */
    private Integer visibility = 0;
}

package com.example.aichat.dto.search;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * M3-B1：搜索请求 DTO，统一 Bean Validation。
 */
@Data
public class SearchRequest {

    @NotBlank(message = "搜索关键词不能为空")
    @Size(max = 100, message = "搜索关键词不能超过100个字符")
    private String keyword;
}

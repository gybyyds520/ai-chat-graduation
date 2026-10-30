package com.example.aichat.dto.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * M3-B1：创建兴趣社群请求 DTO，统一 Bean Validation。
 */
@Data
public class CreateCommunityRequest {

    @NotBlank(message = "社群名称不能为空")
    @Size(max = 50, message = "社群名称不能超过50个字符")
    private String name;

    @Size(max = 500, message = "社群描述不能超过500个字符")
    private String description;

    @Size(max = 50, message = "分类名称过长")
    private String category;
}

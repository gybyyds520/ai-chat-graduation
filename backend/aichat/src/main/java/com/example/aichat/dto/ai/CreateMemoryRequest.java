package com.example.aichat.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * M3-B1：新建 AI 记忆请求 DTO，统一 Bean Validation。
 */
@Data
public class CreateMemoryRequest {

    @NotBlank(message = "记忆内容不能为空")
    @Size(max = 1000, message = "记忆内容不能超过1000个字符")
    private String content;

    /** 记忆类型（可选）。 */
    @Size(max = 50, message = "记忆类型过长")
    private String type;
}

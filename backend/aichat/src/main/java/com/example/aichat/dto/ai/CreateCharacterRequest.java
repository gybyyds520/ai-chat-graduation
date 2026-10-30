package com.example.aichat.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * M3-B1：创建 AI 角色（角色）请求 DTO，统一 Bean Validation。
 *
 * <p>替代直接用实体 / Map 接收 body，缺名等非法请求由 GlobalExceptionHandler
 * 统一转 400。</p>
 */
@Data
public class CreateCharacterRequest {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称不能超过50个字符")
    private String name;

    @Size(max = 2000, message = "角色背景不能超过2000个字符")
    private String backstory;

    @Size(max = 50, message = "性格描述不能超过50个字符")
    private String personality;

    @Size(max = 8000, message = "角色 prompt 过长")
    private String prompt;
}

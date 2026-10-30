package com.example.aichat.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求 DTO。
 *
 * 重构目的：替代原 Map<String, String> 接收方式，统一参数校验。
 * 兼容性：字段名与原前端 POST body 完全一致（username/password）。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}

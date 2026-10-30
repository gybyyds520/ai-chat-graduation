package com.example.aichat.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员登录请求 DTO。
 *
 * Phase 3 (P1-2) 引入：替代 AdminController.login() 接收 Map + 手写校验，
 * 配合 AdminErrorCode.ADMIN_LOGIN_FAILED 标准化错误响应。
 */
@Data
public class AdminLoginRequest {

    @NotBlank(message = "管理员账号不能为空")
    @Size(min = 3, max = 50, message = "管理员账号长度必须在3-50个字符之间")
    private String username;

    @NotBlank(message = "管理员密码不能为空")
    @Size(min = 6, max = 50, message = "管理员密码长度必须在6-50个字符之间")
    private String password;
}

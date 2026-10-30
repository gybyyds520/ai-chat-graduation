package com.example.aichat.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求 DTO。
 *
 * 重构目的（Phase 1 缺陷修复）：
 *   原 UserController.register() 接收 Map<String, String> 并手写 6 段 if/else 校验，
 *   代码冗长且无标准错误码。改为使用 Bean Validation 注解 + GlobalExceptionHandler 统一处理。
 *
 * 兼容性：字段名与原前端 POST body 完全一致（username/password/nickname/email）。
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String password;

    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过50个字符")
    private String email;

    /**
     * 自声明年龄（可选）。
     * - 不传 / null / 0：表示未声明，按成年用户处理（不触发未成年保护）；
     * - 1-17：判定为未成年人，自动开启未成年保护模式；
     * - 18-120：判定为成年人。
     * 用于未成年保护模式的年龄门槛（自声明简化方案）。
     */
    @Min(value = 0, message = "年龄不能为负数")
    @Max(value = 120, message = "年龄输入无效")
    private Integer age;
}

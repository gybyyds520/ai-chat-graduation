package com.example.aichat.dto;

import com.example.aichat.dto.user.LoginRequest;
import com.example.aichat.dto.user.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 缺陷修复验证测试：Bean Validation。
 *
 * 验证目的：
 *   原 UserController 手写 6 段 if/else 校验（用户名长度/格式、密码长度等），
 *   重构后改为 RegisterRequest DTO 上的注解校验。本测试验证：
 *   1. 合法请求应通过校验（0 violation）；
 *   2. 用户名为空 / 过短 / 过长 / 格式不符应触发对应注解的 violation；
 *   3. 密码为空 / 过短应触发 violation；
 *   4. 邮箱格式不正确应触发 @Email 注解。
 */
class UserDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void validRegisterRequest_passesValidation() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice_01");
        req.setPassword("securePwd123");
        req.setNickname("Alice");
        req.setEmail("alice@example.com");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "合法请求不应有任何校验错误，实际: " + violations);
    }

    @Test
    void blankUsername_triggersNotBlankViolation() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("");
        req.setPassword("securePwd123");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        // 空字符串 "" 会同时触发 @NotBlank、@Size(min=3)、@Pattern 三个约束
        // 因此 violations 数量 >= 1，且应包含"用户名不能为空"消息
        assertFalse(violations.isEmpty(), "空用户名应触发至少一个约束违反");
        Set<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
        assertTrue(messages.contains("用户名不能为空"),
                "空用户名应触发 @NotBlank，实际消息: " + messages);
    }

    @Test
    void shortUsername_triggersSizeViolation() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("ab");  // 长度 2 < min 3
        req.setPassword("securePwd123");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        Set<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
        assertTrue(messages.contains("用户名长度必须在3-20个字符之间"));
    }

    @Test
    void longUsername_triggersSizeViolation() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("a".repeat(21));  // 长度 21 > max 20
        req.setPassword("securePwd123");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        Set<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
        assertTrue(messages.contains("用户名长度必须在3-20个字符之间"));
    }

    @Test
    void invalidUsernameFormat_triggersPatternViolation() {
        RegisterRequest req = new RegisterRequest();
        // 包含特殊字符 ! 与空格，不匹配 ^[a-zA-Z0-9_]+$
        req.setUsername("alice! 01");
        req.setPassword("securePwd123");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        Set<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
        assertTrue(messages.contains("用户名只能包含字母、数字和下划线"));
    }

    @Test
    void shortPassword_triggersSizeViolation() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice_01");
        req.setPassword("123");  // 长度 3 < min 6
        req.setEmail("alice@example.com");  // email 合法，仅密码长度触发违反

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertEquals(1, violations.size());
        assertEquals("密码长度必须在6-20个字符之间", violations.iterator().next().getMessage());
    }

    @Test
    void blankPassword_triggersNotBlankViolation() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice_01");
        req.setPassword(null);  // null

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        Set<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
        assertTrue(messages.contains("密码不能为空"));
    }

    @Test
    void invalidEmail_triggersEmailViolation() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice_01");
        req.setPassword("securePwd123");
        req.setEmail("not-an-email");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        Set<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
        assertTrue(messages.contains("邮箱格式不正确"));
    }

    @Test
    void nullNickname_passesValidation() {
        // nickname 是可选字段（无 @NotBlank），允许 null；email 为必填，需提供合法值
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice_01");
        req.setPassword("securePwd123");
        req.setNickname(null);
        req.setEmail("alice@example.com");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    void blankEmail_triggersNotBlankViolation() {
        // 阶段二 P1：email 改为必填，空字符串应触发 @NotBlank
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice_01");
        req.setPassword("securePwd123");
        req.setEmail("");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        Set<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
        assertTrue(messages.contains("邮箱不能为空"),
                "空邮箱应触发 @NotBlank，实际消息: " + messages);
    }

    @Test
    void nullEmail_triggersNotBlankViolation() {
        // 阶段二 P1：email 为必填，null 应触发 @NotBlank
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice_01");
        req.setPassword("securePwd123");
        req.setEmail(null);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        Set<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
        assertTrue(messages.contains("邮箱不能为空"));
    }

    @Test
    void blankLoginFields_triggerViolations() {
        LoginRequest req = new LoginRequest();
        req.setUsername(null);
        req.setPassword("");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertEquals(2, violations.size());
        Set<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
        assertTrue(messages.contains("用户名不能为空"));
        assertTrue(messages.contains("密码不能为空"));
    }

    @Test
    void validLoginRequest_passesValidation() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice_01");
        req.setPassword("securePwd123");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }
}

package com.example.aichat.dto;

import com.example.aichat.dto.knowledge.AskRequest;
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
 * 阶段二 P1：AskRequest Bean Validation 补全验证。
 *
 * 验证：
 *   1. 合法请求（query 非空、topK 在区间内）通过校验；
 *   2. query 为空 / 超长触发对应约束；
 *   3. topK 越界（<1 或 >50）触发 @Min/@Max。
 */
class AskRequestValidationTest {

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
    void validRequest_passesValidation() {
        AskRequest req = new AskRequest();
        req.setQuery("什么是人工智能");
        req.setTopK(3);

        Set<ConstraintViolation<AskRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "合法请求不应有任何校验错误，实际: " + violations);
    }

    @Test
    void blankQuery_triggersNotBlankViolation() {
        AskRequest req = new AskRequest();
        req.setQuery("");
        req.setTopK(3);

        Set<ConstraintViolation<AskRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        Set<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
        assertTrue(messages.contains("问题不能为空"),
                "空问题应触发 @NotBlank，实际消息: " + messages);
    }

    @Test
    void longQuery_triggersSizeViolation() {
        AskRequest req = new AskRequest();
        req.setQuery("你".repeat(2001));
        req.setTopK(3);

        Set<ConstraintViolation<AskRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        Set<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
        assertTrue(messages.contains("问题长度不能超过2000个字符"));
    }

    @Test
    void topKTooSmall_triggersMinViolation() {
        AskRequest req = new AskRequest();
        req.setQuery("什么是人工智能");
        req.setTopK(0);

        Set<ConstraintViolation<AskRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        Set<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
        assertTrue(messages.contains("topK 最小为1"),
                "topK=0 应触发 @Min，实际消息: " + messages);
    }

    @Test
    void topKTooLarge_triggersMaxViolation() {
        AskRequest req = new AskRequest();
        req.setQuery("什么是人工智能");
        req.setTopK(100);

        Set<ConstraintViolation<AskRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        Set<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
        assertTrue(messages.contains("topK 最大为50"),
                "topK=100 应触发 @Max，实际消息: " + messages);
    }

    @Test
    void nullTopK_passesValidation() {
        // topK 为可选字段，缺省由服务端配置
        AskRequest req = new AskRequest();
        req.setQuery("什么是人工智能");
        req.setTopK(null);

        Set<ConstraintViolation<AskRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }
}

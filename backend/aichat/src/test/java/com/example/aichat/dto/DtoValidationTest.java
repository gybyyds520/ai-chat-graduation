package com.example.aichat.dto;

import com.example.aichat.dto.ai.CreateCharacterRequest;
import com.example.aichat.dto.ai.CreateMemoryRequest;
import com.example.aichat.dto.payment.CreatePaymentRequest;
import com.example.aichat.dto.search.SearchRequest;
import com.example.aichat.dto.social.CreateCommunityRequest;
import com.example.aichat.dto.user.UpdateNotificationSettingRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M3-B1：DTO Bean Validation 校验测试。
 *
 * <p>校验缺失必填字段时产生约束违规（等价于控制器层 @Valid 返回 400）。
 * 覆盖：角色创建、社群创建、支付参数、记忆内容、搜索关键词、通知设置。</p>
 */
class DtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setup() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private boolean hasViolationFor(Object dto, String field) {
        Set<ConstraintViolation<Object>> violations = validator.validate(dto);
        return violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }

    @Test
    void 角色创建缺少名称应报校验错误() {
        CreateCharacterRequest req = new CreateCharacterRequest();
        // name 留空
        assertTrue(hasViolationFor(req, "name"));
    }

    @Test
    void 社群创建缺少名称应报校验错误() {
        CreateCommunityRequest req = new CreateCommunityRequest();
        assertTrue(hasViolationFor(req, "name"));
    }

    @Test
    void 支付参数缺失应报校验错误() {
        CreatePaymentRequest req = new CreatePaymentRequest();
        // amount / paymentMethod / subject 全空
        assertTrue(hasViolationFor(req, "amount"));
        assertTrue(hasViolationFor(req, "paymentMethod"));
        assertTrue(hasViolationFor(req, "subject"));
    }

    @Test
    void 记忆内容为空应报校验错误() {
        CreateMemoryRequest req = new CreateMemoryRequest();
        assertTrue(hasViolationFor(req, "content"));
    }

    @Test
    void 搜索关键词为空应报校验错误() {
        SearchRequest req = new SearchRequest();
        assertTrue(hasViolationFor(req, "keyword"));
    }

    @Test
    void 通知设置开关缺失应报校验错误() {
        UpdateNotificationSettingRequest req = new UpdateNotificationSettingRequest();
        assertTrue(hasViolationFor(req, "inAppEnabled"));
        assertTrue(hasViolationFor(req, "emailEnabled"));
    }

    @Test
    void 合法角色创建请求应无校验错误() {
        CreateCharacterRequest req = new CreateCharacterRequest();
        req.setName("小助");
        assertEquals(0, validator.validate(req).size());
    }

    @Test
    void 合法支付请求应无校验错误() {
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setAmount(new BigDecimal("19.90"));
        req.setPaymentMethod("WECHAT");
        req.setSubject("会员月卡");
        assertEquals(0, validator.validate(req).size());
    }
}

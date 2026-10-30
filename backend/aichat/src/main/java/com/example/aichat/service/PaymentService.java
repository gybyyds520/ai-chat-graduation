package com.example.aichat.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.aichat.entity.PaymentRecord;

import java.util.List;
import java.util.Map;

public interface PaymentService {

    PaymentRecord createPayment(Map<String, Object> request);

    PaymentRecord reviewPayment(Long id, Map<String, Object> request);

    PaymentRecord getPaymentById(Long id);

    IPage<PaymentRecord> getPaymentPage(Integer page, Integer size, String reviewStatus);

    List<PaymentRecord> getPaymentByUserId(Long userId);

    List<PaymentRecord> getPendingReviews();

    /**
     * 发起会员分级升级（FREE->BASIC->PREMIUM，模拟/审核制）。
     * 创建一条 PENDING 支付记录，管理员审核通过后同步会员等级。
     */
    PaymentRecord requestMembershipUpgrade(Long userId, String targetLevel, Integer durationDays, String paymentMethod);
}
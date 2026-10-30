package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.PaymentErrorCode;
import com.example.aichat.entity.PaymentRecord;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.PayAccountMapper;
import com.example.aichat.mapper.PaymentRecordMapper;
import com.example.aichat.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * P1-2 回归：支付审核必须 (1) APPROVED 时同步 payment_status=SUCCESS；
 * (2) 用条件 UPDATE 抢锁，并发/重复审核只能成功一次，会员时长不重复叠加。
 */
@ExtendWith(MockitoExtension.class)
class PaymentReviewCasTest {

    @Mock private PaymentRecordMapper paymentRecordMapper;
    @Mock private UserMapper userMapper;
    @Mock private PayAccountMapper payAccountMapper;

    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentServiceImpl();
        ReflectionTestUtils.setField(service, "paymentRecordMapper", paymentRecordMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "payAccountMapper", payAccountMapper);
    }

    private PaymentRecord pendingRecord() {
        PaymentRecord r = new PaymentRecord();
        r.setId(1L);
        r.setUserId(7L);
        r.setVersionType("PRO");
        r.setDurationDays(30);
        r.setPaymentStatus("PENDING");
        r.setReviewStatus("PENDING");
        r.setPaymentTime(LocalDateTime.now());
        return r;
    }

    @Test
    void 审核通过_应把paymentStatus置为SUCCESS() {
        PaymentRecord rec = pendingRecord();
        when(paymentRecordMapper.selectById(1L)).thenReturn(rec);
        // 条件 UPDATE 抢到审核权
        when(paymentRecordMapper.update(any(PaymentRecord.class), any())).thenReturn(1);
        when(userMapper.selectById(7L)).thenReturn(new User());

        Map<String, Object> req = new HashMap<>();
        req.put("reviewStatus", "APPROVED");
        service.reviewPayment(1L, req);

        ArgumentCaptor<PaymentRecord> captor = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(paymentRecordMapper).update(captor.capture(), any());
        assertEquals("SUCCESS", captor.getValue().getPaymentStatus(),
                "APPROVED 时支付流水状态必须同步为 SUCCESS，不能停留在 PENDING");
        assertEquals("APPROVED", captor.getValue().getReviewStatus());
    }

    @Test
    void 并发审核_条件更新影响0行_抛已审核且不升级() {
        PaymentRecord rec = pendingRecord();
        when(paymentRecordMapper.selectById(1L)).thenReturn(rec);
        // 另一并发事务已抢先把 review_status 改为非 PENDING
        when(paymentRecordMapper.update(any(PaymentRecord.class), any())).thenReturn(0);

        Map<String, Object> req = new HashMap<>();
        req.put("reviewStatus", "APPROVED");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.reviewPayment(1L, req));
        assertEquals(PaymentErrorCode.PAYMENT_ALREADY_REVIEWED, ex.getErrorCode());
        // 未抢到锁，绝不能升级会员（避免时长叠加两次）
        verify(userMapper, never()).updateById(any(User.class));
    }
}

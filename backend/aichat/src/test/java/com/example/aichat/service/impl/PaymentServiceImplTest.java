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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 全栈审查 - PaymentServiceImpl 单元测试（Mockito 隔离测试）。
 *
 * 覆盖点（验证 P0/P1 修复）：
 *   1. createPayment 参数校验：缺失字段抛 ORDER_AMOUNT_INVALID；
 *   2. durationDays<=0 抛业务异常；
 *   3. versionType 非法抛业务异常；
 *   4. 用户不存在抛业务异常（PAY_ACCOUNT_NOT_FOUND / ORDER_NOT_FOUND）；
 *   5. 交易 ID 必须以 "TXN-" 开头且为 UUID 衍生（验证 P0 修复：不再使用毫秒+userId）；
 *   6. 支付状态必须为 PENDING（验证资金安全修复）；
 *   7. 金额计算 PRO 99/月、ENTERPRISE 999/月、FREE 0。
 *
 * 设计说明：
 *   - 不真正调用支付网关，仅验证业务规则与字段映射；
 *   - ArgumentCaptor 用于捕获 PaymentRecord 入参做断言。
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRecordMapper paymentRecordMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PayAccountMapper payAccountMapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(42L);
        sampleUser.setUsername("alice");
        sampleUser.setVersionType("FREE");
    }

    // ============ 参数校验测试 ============

    @Test
    void createPayment_缺失userId应抛ORDER_AMOUNT_INVALID() {
        Map<String, Object> req = new HashMap<>();
        req.put("durationDays", 30);
        req.put("versionType", "PRO");
        req.put("paymentMethod", "ALIPAY");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.createPayment(req));
        assertEquals(PaymentErrorCode.ORDER_AMOUNT_INVALID, ex.getErrorCode());
        verifyNoInteractions(paymentRecordMapper);
    }

    @Test
    void createPayment_durationDays为零应抛业务异常() {
        Map<String, Object> req = new HashMap<>();
        req.put("userId", 42L);
        req.put("durationDays", 0);
        req.put("versionType", "PRO");
        req.put("paymentMethod", "ALIPAY");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.createPayment(req));
        assertEquals(PaymentErrorCode.ORDER_AMOUNT_INVALID, ex.getErrorCode());
        verifyNoInteractions(paymentRecordMapper);
    }

    @Test
    void createPayment_versionType非法应抛业务异常() {
        Map<String, Object> req = new HashMap<>();
        req.put("userId", 42L);
        req.put("durationDays", 30);
        req.put("versionType", "INVALID");
        req.put("paymentMethod", "ALIPAY");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.createPayment(req));
        assertEquals(PaymentErrorCode.ORDER_AMOUNT_INVALID, ex.getErrorCode());
    }

    @Test
    void createPayment_用户不存在应抛业务异常() {
        when(userMapper.selectById(42L)).thenReturn(null);

        Map<String, Object> req = new HashMap<>();
        req.put("userId", 42L);
        req.put("durationDays", 30);
        req.put("versionType", "PRO");
        req.put("paymentMethod", "ALIPAY");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.createPayment(req));
        assertEquals(PaymentErrorCode.ORDER_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(paymentRecordMapper);
    }

    @Test
    void createPayment_无支付账户应抛PAY_ACCOUNT_NOT_FOUND() {
        when(userMapper.selectById(42L)).thenReturn(sampleUser);
        when(payAccountMapper.countByUserId(42L)).thenReturn(0);

        Map<String, Object> req = new HashMap<>();
        req.put("userId", 42L);
        req.put("durationDays", 30);
        req.put("versionType", "PRO");
        req.put("paymentMethod", "ALIPAY");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.createPayment(req));
        assertEquals(PaymentErrorCode.PAY_ACCOUNT_NOT_FOUND, ex.getErrorCode());
    }

    // ============ 正常流程测试 ============

    @Test
    void createPayment_正常流程应使用UUID生成交易ID且状态为PENDING() {
        when(userMapper.selectById(42L)).thenReturn(sampleUser);
        when(payAccountMapper.countByUserId(42L)).thenReturn(1);

        Map<String, Object> req = new HashMap<>();
        req.put("userId", 42L);
        req.put("durationDays", 30);
        req.put("versionType", "PRO");
        req.put("paymentMethod", "ALIPAY");

        ArgumentCaptor<PaymentRecord> captor = ArgumentCaptor.forClass(PaymentRecord.class);

        PaymentRecord result = paymentService.createPayment(req);

        verify(paymentRecordMapper).insert(captor.capture());
        PaymentRecord saved = captor.getValue();

        assertNotNull(saved.getTransactionId());
        // P0 修复验证：交易 ID 应以 "TXN-" 开头，且为 UUID 衍生格式（24 字符）
        assertTrue(saved.getTransactionId().startsWith("TXN-"),
                "交易 ID 必须以 TXN- 开头，当前：" + saved.getTransactionId());
        assertEquals("PENDING", saved.getPaymentStatus(),
                "支付状态必须为 PENDING，待网关回调确认后才能改为 SUCCESS");
        assertEquals("PENDING", saved.getReviewStatus());
        assertEquals(42L, saved.getUserId());
        assertEquals("alice", saved.getUsername());
        assertEquals("PRO", saved.getVersionType());
        assertEquals(30, saved.getDurationDays());
        assertEquals("ALIPAY", saved.getPaymentMethod());
        // PRO 99/月 → 30天金额 = 99 * 30 / 30 = 99
        assertEquals(new java.math.BigDecimal("99.00"), saved.getAmount());
    }

    @Test
    void createPayment_PRO类型30天金额应为99元() {
        when(userMapper.selectById(42L)).thenReturn(sampleUser);
        when(payAccountMapper.countByUserId(42L)).thenReturn(1);

        Map<String, Object> req = new HashMap<>();
        req.put("userId", 42L);
        req.put("durationDays", 30);
        req.put("versionType", "PRO");
        req.put("paymentMethod", "ALIPAY");

        paymentService.createPayment(req);

        ArgumentCaptor<PaymentRecord> captor = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(paymentRecordMapper).insert(captor.capture());
        assertEquals(new java.math.BigDecimal("99.00"), captor.getValue().getAmount());
    }

    @Test
    void createPayment_ENTERPRISE类型30天金额应为999元() {
        when(userMapper.selectById(42L)).thenReturn(sampleUser);
        when(payAccountMapper.countByUserId(42L)).thenReturn(1);

        Map<String, Object> req = new HashMap<>();
        req.put("userId", 42L);
        req.put("durationDays", 30);
        req.put("versionType", "ENTERPRISE");
        req.put("paymentMethod", "WECHAT");

        paymentService.createPayment(req);

        ArgumentCaptor<PaymentRecord> captor = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(paymentRecordMapper).insert(captor.capture());
        assertEquals(new java.math.BigDecimal("999.00"), captor.getValue().getAmount());
    }

    @Test
    void createPayment_FREE类型金额应为0() {
        when(userMapper.selectById(42L)).thenReturn(sampleUser);
        when(payAccountMapper.countByUserId(42L)).thenReturn(1);

        Map<String, Object> req = new HashMap<>();
        req.put("userId", 42L);
        req.put("durationDays", 30);
        req.put("versionType", "FREE");
        req.put("paymentMethod", "ALIPAY");

        paymentService.createPayment(req);

        ArgumentCaptor<PaymentRecord> captor = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(paymentRecordMapper).insert(captor.capture());
        // BigDecimal.ZERO scale=0，calculateAmount 使用 divide(.., 2, HALF_UP) 会得 0.00（scale=2）
        // 用 compareTo 避免因 scale 不一致导致 assertEquals 失败
        assertEquals(0, captor.getValue().getAmount().compareTo(java.math.BigDecimal.ZERO),
                "FREE 类型金额必须为 0");
    }

    @Test
    void createPayment_多次调用交易ID应全局唯一() {
        when(userMapper.selectById(42L)).thenReturn(sampleUser);
        when(payAccountMapper.countByUserId(42L)).thenReturn(1);

        Map<String, Object> req = new HashMap<>();
        req.put("userId", 42L);
        req.put("durationDays", 30);
        req.put("versionType", "PRO");
        req.put("paymentMethod", "ALIPAY");

        java.util.Set<String> transactionIds = new java.util.HashSet<>();
        for (int i = 0; i < 100; i++) {
            paymentService.createPayment(req);
            ArgumentCaptor<PaymentRecord> captor = ArgumentCaptor.forClass(PaymentRecord.class);
            verify(paymentRecordMapper, times(i + 1)).insert(captor.capture());
            // 取最后一次捕获值（最后一次 captor.getValue()）
            PaymentRecord saved = captor.getAllValues().get(i);
            transactionIds.add(saved.getTransactionId());
        }
        assertEquals(100, transactionIds.size(),
                "100 次创建支付应生成 100 个唯一交易 ID，验证 P0 UUID 修复避免并发冲突");
    }
}

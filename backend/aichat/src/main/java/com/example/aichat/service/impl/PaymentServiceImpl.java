package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.PaymentErrorCode;
import com.example.aichat.entity.PaymentRecord;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.PaymentRecordMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.mapper.PayAccountMapper;
import com.example.aichat.service.PaymentService;
import com.example.aichat.service.quota.MembershipLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PayAccountMapper payAccountMapper;

    @Override
    @Transactional
    // 安全修复：补充 @Transactional 保证 insert 原子化；参数校验防止异常输入
    public PaymentRecord createPayment(Map<String, Object> request) {
        // 参数校验：避免 ClassCastException 与 NPE
        Object userIdObj = request.get("userId");
        Object durationDaysObj = request.get("durationDays");
        String versionType = (String) request.get("versionType");
        String paymentMethod = (String) request.get("paymentMethod");

        if (userIdObj == null || durationDaysObj == null || versionType == null
                || paymentMethod == null || versionType.isBlank()) {
            throw new BusinessException(PaymentErrorCode.ORDER_AMOUNT_INVALID, "支付参数不完整");
        }

        Long userId = ((Number) userIdObj).longValue();
        Integer durationDays = ((Number) durationDaysObj).intValue();

        // 业务校验：durationDays 必须为正，versionType 必须合法，paymentMethod 必须合法
        if (durationDays == null || durationDays <= 0) {
            throw new BusinessException(PaymentErrorCode.ORDER_AMOUNT_INVALID, "订阅天数必须大于 0");
        }
        if (!"PRO".equals(versionType) && !"ENTERPRISE".equals(versionType) && !"FREE".equals(versionType)) {
            throw new BusinessException(PaymentErrorCode.ORDER_AMOUNT_INVALID, "订阅类型不合法");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(PaymentErrorCode.ORDER_NOT_FOUND, "用户不存在");
        }

        int accountCount = payAccountMapper.countByUserId(userId);
        if (accountCount == 0) {
            throw new BusinessException(PaymentErrorCode.PAY_ACCOUNT_NOT_FOUND);
        }

        BigDecimal amount = calculateAmount(versionType, durationDays);

        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setUserId(userId);
        paymentRecord.setUsername(user.getUsername());
        paymentRecord.setPaymentType("UPGRADE");
        paymentRecord.setVersionType(versionType);
        paymentRecord.setAmount(amount);
        paymentRecord.setDurationDays(durationDays);
        paymentRecord.setPaymentMethod(paymentMethod);
        // Bug 修复（P0）：原 transactionId = "TXN" + System.currentTimeMillis() + userId
        // 在并发下同毫秒同 userId 会冲突。改用 UUID 保证全局唯一。
        paymentRecord.setTransactionId("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase());
        // 业务修复：支付状态改为 PENDING，待支付网关回调确认后改为 SUCCESS。
        // 原实现直接 SUCCESS 存在资金安全风险（用户未实际支付即视为成功）。
        paymentRecord.setPaymentStatus("PENDING");
        paymentRecord.setReviewStatus("PENDING");
        paymentRecord.setPaymentTime(LocalDateTime.now());
        paymentRecord.setCreatedAt(LocalDateTime.now());
        paymentRecord.setUpdatedAt(LocalDateTime.now());

        paymentRecordMapper.insert(paymentRecord);
        return paymentRecord;
    }

    @Override
    @Transactional
    // 安全修复：审核支付涉及两处写操作（更新支付记录 + 升级用户版本），必须原子化，否则可能出现支付已通过但用户未升级的数据不一致
    public PaymentRecord reviewPayment(Long id, Map<String, Object> request) {
        PaymentRecord existing = paymentRecordMapper.selectById(id);
        if (existing == null) {
            return null;
        }

        String reviewStatus = (String) request.get("reviewStatus");

        // P1-2 修复（并发幂等 + 状态流转）：
        // 1) 用条件 UPDATE（WHERE review_status='PENDING'）做乐观抢锁，保证并发审核同一订单时
        //    只有一个事务能 affected 行，另一个 updated=0，避免会员时长被重复叠加。
        // 2) APPROVED 时同步把 payment_status 置为 SUCCESS（原实现只改 review_status，支付流水永远 PENDING）。
        PaymentRecord patch = new PaymentRecord();
        patch.setReviewStatus(reviewStatus);
        patch.setReviewRemark((String) request.get("reviewRemark"));
        patch.setReviewTime(LocalDateTime.now());
        if (request.containsKey("reviewedBy")) {
            patch.setReviewedBy(((Number) request.get("reviewedBy")).longValue());
        }
        patch.setUpdatedAt(LocalDateTime.now());
        if ("APPROVED".equals(reviewStatus)) {
            patch.setPaymentStatus("SUCCESS");
        }

        int updated = paymentRecordMapper.update(patch,
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<PaymentRecord>()
                        .eq("id", id)
                        .eq("review_status", "PENDING"));

        if (updated == 0) {
            // 记录存在（前面已 selectById），走到这里说明已被并发/先前审核置为非 PENDING
            throw new BusinessException(PaymentErrorCode.PAYMENT_ALREADY_REVIEWED);
        }

        if ("APPROVED".equals(reviewStatus)) {
            upgradeUserVersion(existing);
        }

        return paymentRecordMapper.selectById(id);
    }

    private void upgradeUserVersion(PaymentRecord paymentRecord) {
        User user = userMapper.selectById(paymentRecord.getUserId());
        if (user == null) {
            return;
        }

        user.setVersionType(paymentRecord.getVersionType());

        LocalDateTime expireTime = paymentRecord.getPaymentTime();
        if (user.getVersionExpireTime() != null && user.getVersionExpireTime().isAfter(expireTime)) {
            expireTime = user.getVersionExpireTime();
        }
        user.setVersionExpireTime(expireTime.plusDays(paymentRecord.getDurationDays()));
        user.setAccountStatus("ACTIVE");
        // 付费分级：审核通过时同步会员等级（BASIC/PREMIUM）与到期时间
        MembershipLevel level = MembershipLevel.fromString(paymentRecord.getVersionType());
        user.setMembershipLevel(level.name());
        user.setMembershipExpireAt(user.getVersionExpireTime());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.updateById(user);
    }

    private BigDecimal calculateAmount(String versionType, Integer durationDays) {
        BigDecimal monthlyPrice;
        if ("PRO".equals(versionType)) {
            monthlyPrice = new BigDecimal("99");
        } else if ("ENTERPRISE".equals(versionType)) {
            monthlyPrice = new BigDecimal("999");
        } else {
            monthlyPrice = BigDecimal.ZERO;
        }
        return monthlyPrice.multiply(new BigDecimal(durationDays)).divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP);
    }

    @Override
    public PaymentRecord getPaymentById(Long id) {
        return paymentRecordMapper.selectById(id);
    }

    @Override
    public IPage<PaymentRecord> getPaymentPage(Integer page, Integer size, String reviewStatus) {
        Page<PaymentRecord> pageParam = new Page<>(page, size);
        QueryWrapper<PaymentRecord> wrapper = new QueryWrapper<>();
        if (reviewStatus != null && !reviewStatus.isEmpty()) {
            wrapper.eq("review_status", reviewStatus);
        }
        wrapper.orderByDesc("created_at");
        return paymentRecordMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public List<PaymentRecord> getPaymentByUserId(Long userId) {
        QueryWrapper<PaymentRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("created_at");
        return paymentRecordMapper.selectList(wrapper);
    }

    @Override
    public List<PaymentRecord> getPendingReviews() {
        QueryWrapper<PaymentRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("review_status", "PENDING");
        wrapper.orderByAsc("created_at");
        return paymentRecordMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public PaymentRecord requestMembershipUpgrade(Long userId, String targetLevel,
                                                  Integer durationDays, String paymentMethod) {
        if (targetLevel == null || targetLevel.isBlank()) {
            throw new BusinessException(PaymentErrorCode.ORDER_AMOUNT_INVALID, "目标会员等级不能为空");
        }
        MembershipLevel level;
        try {
            level = MembershipLevel.valueOf(targetLevel.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(PaymentErrorCode.ORDER_AMOUNT_INVALID, "目标会员等级不合法");
        }
        if (level == MembershipLevel.FREE) {
            throw new BusinessException(PaymentErrorCode.ORDER_AMOUNT_INVALID, "免费版无需购买");
        }
        if (durationDays == null || durationDays <= 0) {
            throw new BusinessException(PaymentErrorCode.ORDER_AMOUNT_INVALID, "订阅天数必须大于 0");
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new BusinessException(PaymentErrorCode.ORDER_AMOUNT_INVALID, "支付方式不能为空");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(PaymentErrorCode.ORDER_NOT_FOUND, "用户不存在");
        }

        PaymentRecord record = new PaymentRecord();
        record.setUserId(userId);
        record.setUsername(user.getUsername());
        record.setPaymentType("MEMBERSHIP_UPGRADE");
        record.setVersionType(level.name());
        record.setAmount(calculateAmount(level.name(), durationDays));
        record.setDurationDays(durationDays);
        record.setPaymentMethod(paymentMethod);
        record.setTransactionId("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase());
        record.setPaymentStatus("PENDING");
        record.setReviewStatus("PENDING");
        record.setPaymentTime(LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        paymentRecordMapper.insert(record);
        return record;
    }
}
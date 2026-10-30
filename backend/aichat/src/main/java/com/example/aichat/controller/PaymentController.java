package com.example.aichat.controller;

import com.example.aichat.common.audit.Audited;
import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.entity.PaymentRecord;
import com.example.aichat.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 支付接口。
 *
 * Phase 4 (P4-d) 声明式权限示范：
 *   - 创建支付订单：要求登录（isAuthenticated），并自动用 AuthContext 取当前用户 ID；
 *   - 审核支付订单：要求管理员权限（hasRole('ADMIN')），普通用户不能审核自己的支付；
 *   - 查询支付订单：要求登录，但允许查询（不强制 admin，业务层做数据隔离）。
 *
 * 安全意义：支付是高敏感操作，未登录用户不应能创建订单；普通用户不能审核
 * （否则可自行通过审核白嫖付费版本）。
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    @Audited(action = "CREATE_PAYMENT", resourceType = "PAYMENT")
    public R createPayment(@RequestBody Map<String, Object> request) {
        // 安全修复（P0）：强制以当前登录用户为订单归属，忽略/拒绝客户端传入的 userId，
        // 防止任意用户替他人创建支付订单。
        Long currentUserId = AuthContext.requireUserId();
        request.put("userId", currentUserId);
        if (!request.containsKey("versionType") || !request.containsKey("durationDays")) {
            return R.badRequest("缺少必要参数");
        }

        PaymentRecord paymentRecord = paymentService.createPayment(request);
        if (paymentRecord == null) {
            return R.error(403, "请先绑定支付账户，否则无法购买付费版本");
        }
        return R.success(paymentRecord);
    }

    /**
     * 审核支付订单 - 仅管理员可操作。
     * 普通用户不能审核自己的支付，防止绕过付费流程。
     */
    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "REVIEW_PAYMENT", resourceType = "PAYMENT")
    public R reviewPayment(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        if (!request.containsKey("reviewStatus")) {
            return R.badRequest("缺少审核状态参数");
        }

        PaymentRecord paymentRecord = paymentService.reviewPayment(id, request);
        if (paymentRecord == null) {
            return R.error("充值记录不存在");
        }
        return R.success(paymentRecord);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Audited(action = "GET_PAYMENT", resourceType = "PAYMENT", logArgs = false)
    public R getPaymentById(@PathVariable Long id) {
        // 安全修复（P0）：只能查看自己的订单，管理员可查看全部
        PaymentRecord paymentRecord = paymentService.getPaymentById(id);
        if (paymentRecord == null) {
            return R.error("充值记录不存在");
        }
        Long currentUserId = AuthContext.requireUserId();
        boolean isAdmin = AuthContext.hasRole("ADMIN");
        if (!isAdmin && !paymentRecord.getUserId().equals(currentUserId)) {
            return R.unauthorized("无权查看该充值记录");
        }
        return R.success(paymentRecord);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "LIST_PAYMENTS", resourceType = "PAYMENT")
    public R getPayments(@RequestParam(defaultValue = "1") Integer page,
                        @RequestParam(defaultValue = "20") Integer size,
                        @RequestParam(required = false) String reviewStatus) {
        // 列出所有支付订单仅限管理员（普通用户应通过 /api/payment/{id} 查自己订单）
        return R.success(paymentService.getPaymentPage(page, size, reviewStatus));
    }

    /**
     * 安全修复（P0）：查询指定用户订单与待审核列表仅限管理员。
     * 原实现未加任何权限注解，任意登录用户可枚举/查看他人支付记录（含支付账号敏感信息）。
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public R getPaymentsByUser(@PathVariable Long userId) {
        List<PaymentRecord> records = paymentService.getPaymentByUserId(userId);
        return R.success(records);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public R getPendingReviews() {
        List<PaymentRecord> records = paymentService.getPendingReviews();
        return R.success(records);
    }

    /**
     * 发起会员分级升级（FREE->BASIC->PREMIUM，模拟/审核制）。
     * body: { targetLevel: "BASIC"|"PREMIUM", durationDays, paymentMethod }
     */
    @PostMapping("/membership/upgrade")
    @PreAuthorize("isAuthenticated()")
    @Audited(action = "MEMBERSHIP_UPGRADE", resourceType = "PAYMENT")
    public R upgradeMembership(@RequestBody Map<String, Object> request) {
        Long userId = AuthContext.requireUserId();
        String targetLevel = request.get("targetLevel") == null ? null : request.get("targetLevel").toString();
        Object durationDays = request.get("durationDays");
        String paymentMethod = request.get("paymentMethod") == null ? null : request.get("paymentMethod").toString();
        if (targetLevel == null || durationDays == null) {
            return R.badRequest("缺少目标等级或订阅天数");
        }
        PaymentRecord record = paymentService.requestMembershipUpgrade(
                userId, targetLevel, ((Number) durationDays).intValue(), paymentMethod);
        return R.success(record);
    }
}
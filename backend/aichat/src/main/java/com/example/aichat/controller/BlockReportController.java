package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.UserBlock;
import com.example.aichat.entity.UserReport;
import com.example.aichat.service.BlockReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/block-report")
public class BlockReportController {

    @Autowired
    private BlockReportService blockReportService;

    @PostMapping("/block")
    public R blockUser(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        Object blockedUserIdObj = request.get("blockedUserId");
        
        if (blockedUserIdObj == null) {
            return R.error("参数错误");
        }
        
        // 安全修复（P0）：拉黑操作以当前登录用户为准
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());
        Long blockedUserId = ((Number) blockedUserIdObj).longValue();
        
        UserBlock block = blockReportService.blockUser(userId, blockedUserId);
        if (block != null) {
            return R.success(block);
        }
        return R.error("拉黑失败");
    }

    @PostMapping("/unblock")
    public R unblockUser(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        Object blockedUserIdObj = request.get("blockedUserId");
        
        if (blockedUserIdObj == null) {
            return R.error("参数错误");
        }
        
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());
        Long blockedUserId = ((Number) blockedUserIdObj).longValue();
        
        blockReportService.unblockUser(userId, blockedUserId);
        return R.success("取消拉黑成功");
    }

    @GetMapping("/blocked")
    public R getBlockedUsers(@RequestParam Long userId) {
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        List<UserBlock> blocked = blockReportService.getBlockedUsers(current);
        return R.success(blocked);
    }

    @GetMapping("/is-blocked")
    public R isBlocked(@RequestParam Long userId, @RequestParam Long targetUserId) {
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        boolean blocked = blockReportService.isBlocked(current, targetUserId);
        return R.success(Map.of("blocked", blocked));
    }

    @PostMapping("/report")
    public R createReport(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        String targetType = (String) request.get("targetType");
        Object targetIdObj = request.get("targetId");
        String reportReason = (String) request.get("reportReason");
        
        if (targetType == null || targetIdObj == null || reportReason == null) {
            return R.error("参数错误");
        }
        
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());
        Long targetId = ((Number) targetIdObj).longValue();
        
        UserReport report = blockReportService.createReport(userId, targetType, targetId, reportReason);
        return R.success(report);
    }

    /**
     * 安全修复（P0）：待处理举报与举报审核仅管理员可访问。
     */
    @GetMapping("/reports/pending")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public R getPendingReports() {
        List<UserReport> reports = blockReportService.getPendingReports();
        return R.success(reports);
    }

    @GetMapping("/reports/user")
    public R getReportsByUser(@RequestParam Long userId) {
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        List<UserReport> reports = blockReportService.getReportsByUser(current);
        return R.success(reports);
    }

    @PostMapping("/reports/review")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public R reviewReport(@RequestBody Map<String, Object> request) {
        Object reportIdObj = request.get("reportId");
        String status = (String) request.get("status");
        String reviewResult = (String) request.get("reviewResult");
        
        if (reportIdObj == null || status == null) {
            return R.error("参数错误");
        }
        
        Long reportId = ((Number) reportIdObj).longValue();
        
        blockReportService.reviewReport(reportId, status, reviewResult);
        return R.success("审核完成");
    }
}
package com.example.aichat.service.impl;

import com.example.aichat.entity.UserBlock;
import com.example.aichat.entity.UserReport;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.SocialErrorCode;
import com.example.aichat.dto.audit.AuditResult;
import com.example.aichat.mapper.UserBlockMapper;
import com.example.aichat.mapper.UserReportMapper;
import com.example.aichat.service.BlockReportService;
import com.example.aichat.service.ContentAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BlockReportServiceImpl implements BlockReportService {

    private static final Logger logger = LoggerFactory.getLogger(BlockReportServiceImpl.class);

    private final UserBlockMapper userBlockMapper;
    private final UserReportMapper userReportMapper;
    private final ContentAuditService contentAuditService;

    public BlockReportServiceImpl(UserBlockMapper userBlockMapper,
                                  UserReportMapper userReportMapper,
                                  ContentAuditService contentAuditService) {
        this.userBlockMapper = userBlockMapper;
        this.userReportMapper = userReportMapper;
        this.contentAuditService = contentAuditService;
    }

    @Override
    public UserBlock blockUser(Long userId, Long blockedUserId) {
        // 防御：userId 为空直接拒绝（原实现 userId.equals 会 NPE）
        if (userId == null || blockedUserId == null) {
            return null;
        }
        if (userId.equals(blockedUserId)) {
            return null;
        }
        
        UserBlock existing = userBlockMapper.findBlock(userId, blockedUserId);
        if (existing != null) {
            return existing;
        }
        
        UserBlock block = new UserBlock();
        block.setUserId(userId);
        block.setBlockedUserId(blockedUserId);
        block.setBlockedAt(LocalDateTime.now());
        
        userBlockMapper.insert(block);
        logger.info("[用户拉黑] 用户 {} 拉黑用户 {}", userId, blockedUserId);
        return block;
    }

    @Override
    public void unblockUser(Long userId, Long blockedUserId) {
        userBlockMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserBlock>()
                .eq("user_id", userId)
                .eq("blocked_user_id", blockedUserId)
        );
        logger.info("[用户拉黑] 用户 {} 取消拉黑用户 {}", userId, blockedUserId);
    }

    @Override
    public boolean isBlocked(Long userId, Long targetUserId) {
        return userBlockMapper.isBlocked(userId, targetUserId) > 0;
    }

    @Override
    public List<UserBlock> getBlockedUsers(Long userId) {
        return userBlockMapper.getBlockedUsers(userId);
    }

    @Override
    public UserReport createReport(Long userId, String targetType, Long targetId, String reportReason) {
        // 安全修复（P1）：举报滥用防护。同一用户对同一目标已有 PENDING 举报时，
        // 拒绝重复提交，防止短时间内刷爆审核队列、对目标用户进行恶意举报轰炸。
        if (userReportMapper.countPendingReport(userId, targetType, targetId) > 0) {
            throw new BusinessException(SocialErrorCode.REPORT_ALREADY_SUBMITTED);
        }

        UserReport report = new UserReport();
        report.setUserId(userId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReportReason(reportReason);
        report.setStatus("PENDING");
        report.setCreatedAt(LocalDateTime.now());
        
        userReportMapper.insert(report);
        logger.info("[用户举报] 用户 {} 举报 {}: {}", userId, targetType, targetId);

        // 举报闭环整合：对举报理由做一次内容审核并落审核日志（contentType=report），
        // 便于管理员在审核举报时关联查看审核记录。举报理由不强制拦截。
        AuditResult reportAudit = contentAuditService.audit(reportReason, "report", userId);
        contentAuditService.recordAuditLog(userId, "report", targetId, reportReason, reportAudit);
        if (!reportAudit.isPassed()) {
            logger.info("[用户举报] 举报理由命中敏感词 {}，已记录审核日志", reportAudit.getMatchedWords());
        }
        return report;
    }

    @Override
    public List<UserReport> getPendingReports() {
        return userReportMapper.getReportsByStatus("PENDING");
    }

    @Override
    public List<UserReport> getReportsByUser(Long userId) {
        return userReportMapper.getReportsByUser(userId);
    }

    @Override
    public void reviewReport(Long reportId, String status, String reviewResult) {
        userReportMapper.reviewReport(reportId, status, reviewResult, LocalDateTime.now());
        logger.info("[举报审核] 举报 {} 审核结果: {}", reportId, status);
    }
}
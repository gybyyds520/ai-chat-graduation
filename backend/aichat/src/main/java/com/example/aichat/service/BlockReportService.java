package com.example.aichat.service;

import com.example.aichat.entity.UserBlock;
import com.example.aichat.entity.UserReport;

import java.util.List;

public interface BlockReportService {
    
    UserBlock blockUser(Long userId, Long blockedUserId);
    
    void unblockUser(Long userId, Long blockedUserId);
    
    boolean isBlocked(Long userId, Long targetUserId);
    
    List<UserBlock> getBlockedUsers(Long userId);
    
    UserReport createReport(Long userId, String targetType, Long targetId, String reportReason);
    
    List<UserReport> getPendingReports();
    
    List<UserReport> getReportsByUser(Long userId);
    
    void reviewReport(Long reportId, String status, String reviewResult);
}
package com.example.aichat.service;

import com.example.aichat.entity.AccessLog;
import com.example.aichat.mapper.AccessLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 访问日志服务
 * 记录所有数据访问行为，支持审计和监控
 */
@Service
public class AccessLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(AccessLogService.class);
    
    @Autowired
    private AccessLogMapper accessLogMapper;
    
    /**
     * 记录访问日志（异步）
     */
    @Async
    public void logAccessAsync(Long aiSystemId, Long dataId, String accessType, 
                                String requestMethod, String requestPath, String requestParams,
                                Integer responseStatus, Integer responseTime, 
                                String ipAddress, String userAgent, String errorMessage) {
        logAccess(aiSystemId, dataId, accessType, requestMethod, requestPath, requestParams,
                  responseStatus, responseTime, ipAddress, userAgent, errorMessage);
    }
    
    /**
     * 记录访问日志
     */
    public void logAccess(Long aiSystemId, Long dataId, String accessType, 
                          String requestMethod, String requestPath, String requestParams,
                          Integer responseStatus, Integer responseTime, 
                          String ipAddress, String userAgent, String errorMessage) {
        AccessLog log = new AccessLog();
        log.setAiSystemId(aiSystemId);
        log.setDataId(dataId);
        log.setAccessType(accessType);
        log.setRequestMethod(requestMethod);
        log.setRequestPath(requestPath);
        log.setRequestParams(requestParams);
        log.setResponseStatus(responseStatus);
        log.setResponseTime(responseTime);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setErrorMessage(errorMessage);
        log.setAccessedAt(LocalDateTime.now());
        
        accessLogMapper.insert(log);
        
        // 检查异常行为
        checkAnomalousBehavior(log);
    }
    
    /**
     * 查询AI系统的访问日志
     */
    public List<AccessLog> getSystemAccessLogs(Long aiSystemId, int limit) {
        return accessLogMapper.selectByAiSystemId(aiSystemId, limit);
    }
    
    /**
     * 查询数据的访问日志
     */
    public List<AccessLog> getDataAccessLogs(Long dataId) {
        return accessLogMapper.selectByDataId(dataId);
    }
    
    /**
     * 统计访问类型
     */
    public List<Map<String, Object>> getAccessTypeStats(Long aiSystemId) {
        return accessLogMapper.countByAccessType(aiSystemId);
    }
    
    /**
     * 检查异常行为
     */
    private void checkAnomalousBehavior(AccessLog log) {
        // 响应时间过长
        if (log.getResponseTime() != null && log.getResponseTime() > 5000) {
            logger.warn("检测到慢查询: aiSystemId={}, path={}, time={}ms", 
                       log.getAiSystemId(), log.getRequestPath(), log.getResponseTime());
        }
        
        // 错误响应
        if (log.getResponseStatus() != null && log.getResponseStatus() >= 400) {
            logger.warn("检测到错误响应: aiSystemId={}, status={}, path={}", 
                       log.getAiSystemId(), log.getResponseStatus(), log.getRequestPath());
        }
        
        // 可以添加更多异常检测规则
    }
}

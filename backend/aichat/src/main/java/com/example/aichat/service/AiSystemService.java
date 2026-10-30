package com.example.aichat.service;

import com.example.aichat.entity.AiSystem;
import com.example.aichat.mapper.AiSystemMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * AI系统管理服务
 * 提供AI系统的注册、认证、权限管理等功能
 */
@Service
public class AiSystemService {
    
    private static final Logger logger = LoggerFactory.getLogger(AiSystemService.class);
    
    @Autowired
    private AiSystemMapper aiSystemMapper;
    
    /**
     * 注册新的AI系统
     */
    @Transactional
    public AiSystem registerSystem(String systemName, String systemType, String description, Integer rateLimit) {
        logger.info("注册新的AI系统: name={}, type={}", systemName, systemType);
        
        // 检查系统名是否已存在
        if (aiSystemMapper.selectBySystemName(systemName) != null) {
            throw new IllegalArgumentException("系统名称已存在: " + systemName);
        }
        
        AiSystem system = new AiSystem();
        system.setSystemName(systemName);
        system.setSystemType(systemType);
        system.setDescription(description);
        system.setRateLimit(rateLimit != null ? rateLimit : 100);
        system.setStatus(1);
        
        // 生成API密钥和密钥
        system.setApiKey(generateApiKey());
        system.setSecretKey(generateSecretKey());
        
        system.setCreatedAt(LocalDateTime.now());
        system.setUpdatedAt(LocalDateTime.now());
        
        aiSystemMapper.insert(system);
        
        logger.info("AI系统注册成功: id={}, apiKey={}***(masked)", system.getId(), maskSecret(system.getApiKey()));
        return system;
    }
    
    /**
     * 验证API密钥
     */
    public AiSystem authenticate(String apiKey, String secretKey) {
        AiSystem system = aiSystemMapper.selectByApiKey(apiKey);
        
        if (system == null) {
            logger.warn("API密钥无效: {}***(masked)", maskSecret(apiKey));
            return null;
        }
        
        if (!system.getSecretKey().equals(secretKey)) {
            logger.warn("密钥不匹配: systemId={}", system.getId());
            return null;
        }
        
        if (system.getStatus() != 1) {
            logger.warn("系统已禁用: systemId={}", system.getId());
            return null;
        }
        
        // 更新最后访问时间与累计调用次数
        system.setLastAccessAt(LocalDateTime.now());
        int currentCalls = system.getCallCount() == null ? 0 : system.getCallCount();
        system.setCallCount(currentCalls + 1);
        aiSystemMapper.updateById(system);

        return system;
    }
    
    /**
     * 获取系统信息
     */
    public AiSystem getSystemById(Long id) {
        return aiSystemMapper.selectById(id);
    }
    
    /**
     * 获取系统信息（通过API密钥）
     */
    public AiSystem getSystemByApiKey(String apiKey) {
        return aiSystemMapper.selectByApiKey(apiKey);
    }
    
    /**
     * 获取所有系统列表
     */
    public List<AiSystem> getAllSystems() {
        return aiSystemMapper.selectList(null);
    }
    
    /**
     * 获取活跃系统列表
     */
    public List<AiSystem> getActiveSystems() {
        return aiSystemMapper.selectList(null);
    }

    /**
     * 管理端使用统计：总数、活跃数、累计调用、今日调用（暂无按日统计返回 0）、Top5 调用系统。
     */
    public java.util.Map<String, Object> getStats() {
        List<AiSystem> all = aiSystemMapper.selectList(null);

        long totalSystems = all.size();
        long activeSystems = all.stream().filter(s -> s.getStatus() != null && s.getStatus() == 1).count();
        long totalCalls = all.stream()
                .filter(s -> s.getCallCount() != null)
                .mapToLong(AiSystem::getCallCount)
                .sum();

        List<java.util.Map<String, Object>> topSystems = all.stream()
                .sorted((a, b) -> {
                    int ac = a.getCallCount() == null ? 0 : a.getCallCount();
                    int bc = b.getCallCount() == null ? 0 : b.getCallCount();
                    return Integer.compare(bc, ac);
                })
                .limit(5)
                .map(s -> {
                    java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
                    item.put("id", s.getId());
                    item.put("systemName", s.getSystemName());
                    item.put("callCount", s.getCallCount() == null ? 0 : s.getCallCount());
                    item.put("lastAccessAt", s.getLastAccessAt());
                    return item;
                })
                .toList();

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("totalSystems", totalSystems);
        result.put("activeSystems", activeSystems);
        result.put("totalCalls", totalCalls);
        result.put("todayCalls", 0L);
        result.put("topSystems", topSystems);
        return result;
    }
    
    /**
     * 更新系统状态
     */
    @Transactional
    public boolean updateSystemStatus(Long id, Integer status) {
        AiSystem system = aiSystemMapper.selectById(id);
        if (system == null) {
            return false;
        }
        
        system.setStatus(status);
        system.setUpdatedAt(LocalDateTime.now());
        
        return aiSystemMapper.updateById(system) > 0;
    }
    
    /**
     * 更新系统速率限制
     */
    @Transactional
    public boolean updateRateLimit(Long id, Integer rateLimit) {
        AiSystem system = aiSystemMapper.selectById(id);
        if (system == null) {
            return false;
        }
        
        system.setRateLimit(rateLimit);
        system.setUpdatedAt(LocalDateTime.now());
        
        return aiSystemMapper.updateById(system) > 0;
    }
    
    /**
     * 重新生成API密钥
     */
    @Transactional
    public AiSystem regenerateApiKey(Long id) {
        AiSystem system = aiSystemMapper.selectById(id);
        if (system == null) {
            throw new IllegalArgumentException("系统不存在: " + id);
        }
        
        system.setApiKey(generateApiKey());
        system.setSecretKey(generateSecretKey());
        system.setUpdatedAt(LocalDateTime.now());
        
        aiSystemMapper.updateById(system);
        
        logger.info("重新生成API密钥: systemId={}", id);
        return system;
    }
    
    /**
     * 删除系统
     */
    @Transactional
    public boolean deleteSystem(Long id) {
        logger.info("删除AI系统: id={}", id);
        return aiSystemMapper.deleteById(id) > 0;
    }
    
    /**
     * 生成API密钥
     */
    private String generateApiKey() {
        return "ak_" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }
    
    /**
     * 生成密钥
     */
    private String generateSecretKey() {
        return "sk_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 敏感串打码：仅保留前 4 位前缀，防止完整密钥写入日志。
     */
    private String maskSecret(String secret) {
        if (secret == null) return "null";
        if (secret.length() <= 4) return "****";
        return secret.substring(0, 4);
    }
}

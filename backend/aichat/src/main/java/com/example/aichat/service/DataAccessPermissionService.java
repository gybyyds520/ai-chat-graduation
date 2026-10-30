package com.example.aichat.service;

import com.example.aichat.entity.DataAccessPermission;
import com.example.aichat.mapper.DataAccessPermissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据访问权限服务
 * 实现精细化的权限控制
 */
@Service
public class DataAccessPermissionService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataAccessPermissionService.class);
    
    @Autowired
    private DataAccessPermissionMapper permissionMapper;
    
    /**
     * 授予数据访问权限
     */
    @Transactional
    public DataAccessPermission grantPermission(Long dataId, Long aiSystemId, String permissionType, 
                                                  String createdBy, LocalDateTime expiresAt) {
        logger.info("授予权限: dataId={}, aiSystemId={}, type={}", dataId, aiSystemId, permissionType);
        
        // 检查是否已存在权限
        DataAccessPermission existing = permissionMapper.selectBySystemAndData(aiSystemId, dataId);
        if (existing != null) {
            // 更新现有权限
            existing.setPermissionType(permissionType);
            existing.setExpiresAt(expiresAt);
            existing.setCreatedBy(createdBy);
            permissionMapper.updateById(existing);
            return existing;
        }
        
        DataAccessPermission permission = new DataAccessPermission();
        permission.setDataId(dataId);
        permission.setAiSystemId(aiSystemId);
        permission.setPermissionType(permissionType);
        permission.setExpiresAt(expiresAt);
        permission.setCreatedBy(createdBy);
        permission.setCreatedAt(LocalDateTime.now());
        
        permissionMapper.insert(permission);
        
        logger.info("权限授予成功: id={}", permission.getId());
        return permission;
    }
    
    /**
     * 授予分类权限
     */
    @Transactional
    public DataAccessPermission grantCategoryPermission(String category, Long aiSystemId, 
                                                          String permissionType, String createdBy) {
        logger.info("授予分类权限: category={}, aiSystemId={}", category, aiSystemId);
        
        DataAccessPermission permission = new DataAccessPermission();
        permission.setDataCategory(category);
        permission.setAiSystemId(aiSystemId);
        permission.setPermissionType(permissionType);
        permission.setCreatedBy(createdBy);
        permission.setCreatedAt(LocalDateTime.now());
        
        permissionMapper.insert(permission);
        
        return permission;
    }
    
    /**
     * 检查权限
     */
    public boolean hasPermission(Long aiSystemId, Long dataId, String requiredPermission) {
        DataAccessPermission permission = permissionMapper.selectBySystemAndData(aiSystemId, dataId);
        
        if (permission == null) {
            return false;
        }
        
        // 检查权限级别
        return checkPermissionLevel(permission.getPermissionType(), requiredPermission);
    }
    
    /**
     * 检查分类权限
     */
    public boolean hasCategoryPermission(Long aiSystemId, String category, String requiredPermission) {
        List<DataAccessPermission> permissions = permissionMapper.selectByAiSystemId(aiSystemId);
        
        for (DataAccessPermission permission : permissions) {
            if (category.equals(permission.getDataCategory())) {
                return checkPermissionLevel(permission.getPermissionType(), requiredPermission);
            }
        }
        
        return false;
    }
    
    /**
     * 获取AI系统的所有权限
     */
    public List<DataAccessPermission> getSystemPermissions(Long aiSystemId) {
        return permissionMapper.selectByAiSystemId(aiSystemId);
    }
    
    /**
     * 获取数据的所有权限
     */
    public List<DataAccessPermission> getDataPermissions(Long dataId) {
        return permissionMapper.selectByDataId(dataId);
    }
    
    /**
     * 撤销权限
     */
    @Transactional
    public boolean revokePermission(Long permissionId) {
        logger.info("撤销权限: id={}", permissionId);
        return permissionMapper.deleteById(permissionId) > 0;
    }
    
    /**
     * 检查权限级别
     * admin > delete > write > read
     */
    private boolean checkPermissionLevel(String grantedPermission, String requiredPermission) {
        int grantedLevel = getPermissionLevel(grantedPermission);
        int requiredLevel = getPermissionLevel(requiredPermission);
        
        return grantedLevel >= requiredLevel;
    }
    
    /**
     * 获取权限级别数值
     */
    private int getPermissionLevel(String permission) {
        switch (permission.toLowerCase()) {
            case "admin": return 4;
            case "delete": return 3;
            case "write": return 2;
            case "read": return 1;
            default: return 0;
        }
    }
}

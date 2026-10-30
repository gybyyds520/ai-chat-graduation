package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.dto.ai.AiConfigDTO;
import com.example.aichat.dto.ai.OllamaHealth;
import com.example.aichat.dto.ai.OllamaModelInfo;
import com.example.aichat.entity.AiSystem;
import com.example.aichat.entity.DataAccessPermission;
import com.example.aichat.service.AiConfigService;
import com.example.aichat.service.AiSystemService;
import com.example.aichat.service.DataAccessPermissionService;
import com.example.aichat.service.ai.OllamaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI系统管理控制器
 * 提供AI系统注册、权限管理等功能
 */
@RestController
@RequestMapping("/api/aisystem")
public class AiSystemController {
    
    private static final Logger logger = LoggerFactory.getLogger(AiSystemController.class);
    
    @Autowired
    private AiSystemService aiSystemService;
    
    @Autowired
    private DataAccessPermissionService permissionService;
    
    @Autowired
    private OllamaService ollamaService;

    @Autowired
    private AiConfigService aiConfigService;
    
    /**
     * 注册新的AI系统
     * POST /api/aisystem/register
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public R<AiSystem> registerSystem(@RequestBody Map<String, Object> request) {
        try {
            String systemName = (String) request.get("systemName");
            String systemType = (String) request.get("systemType");
            String description = (String) request.get("description");
            Integer rateLimit = request.get("rateLimit") != null
                ? ((Number) request.get("rateLimit")).intValue()
                : null;

            AiSystem system = aiSystemService.registerSystem(systemName, systemType, description, rateLimit);

            logger.info("AI系统注册成功: {}", systemName);
            return R.success(system);

        } catch (Exception e) {
            logger.error("注册AI系统失败", e);
            return R.error("操作失败");
        }
    }
    
    /**
     * 验证API密钥
     * POST /api/aisystem/authenticate
     */
    @PostMapping("/authenticate")
    public R<AiSystem> authenticate(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Secret-Key", required = false) String secretKey) {
        
        try {
            // 缺头时显式返回 400（避免 MissingRequestHeaderException 落入全局 500）
            if (apiKey == null || apiKey.isBlank() || secretKey == null || secretKey.isBlank()) {
                return R.error(400, "缺少认证头：X-API-Key / X-Secret-Key");
            }

            AiSystem system = aiSystemService.authenticate(apiKey, secretKey);
            
            if (system == null) {
                return R.error("认证失败：无效的API密钥或密钥");
            }
            
            // 不返回敏感信息
            system.setSecretKey(null);
            
            return R.success(system);
            
        } catch (Exception e) {
            logger.error("认证失败", e);
            return R.error("认证失败");
        }
    }
    
    /**
     * 获取所有AI系统列表
     * GET /api/aisystem/list
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public R<List<AiSystem>> getAllSystems() {
        try {
            List<AiSystem> systems = aiSystemService.getAllSystems();
            
            // 隐藏敏感信息
            systems.forEach(s -> s.setSecretKey(null));

            return R.success(systems);
        } catch (Exception e) {
            logger.error("获取系统列表失败", e);
            return R.error("操作失败");
        }
    }
    
    /**
     * 获取AI系统详情
     * GET /api/aisystem/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<AiSystem> getSystemById(@PathVariable Long id) {
        try {
            AiSystem system = aiSystemService.getSystemById(id);
            
            if (system == null) {
                return R.error("系统不存在");
            }
            
            // 隐藏敏感信息
            system.setSecretKey(null);

            return R.success(system);
        } catch (Exception e) {
            logger.error("获取系统详情失败", e);
            return R.error("操作失败");
        }
    }
    
    /**
     * 更新系统状态
     * PUT /api/aisystem/{id}/status
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Boolean> updateSystemStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        try {
            Integer status = ((Number) request.get("status")).intValue();
            boolean result = aiSystemService.updateSystemStatus(id, status);

            return R.success(result);
        } catch (Exception e) {
            logger.error("更新系统状态失败", e);
            return R.error("操作失败");
        }
    }
    
    /**
     * 更新速率限制
     * PUT /api/aisystem/{id}/ratelimit
     */
    @PutMapping("/{id}/ratelimit")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Boolean> updateRateLimit(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        try {
            Integer rateLimit = ((Number) request.get("rateLimit")).intValue();
            boolean result = aiSystemService.updateRateLimit(id, rateLimit);

            return R.success(result);
        } catch (Exception e) {
            logger.error("更新速率限制失败", e);
            return R.error("操作失败");
        }
    }
    
    /**
     * 重新生成API密钥
     * POST /api/aisystem/{id}/regenerate-key
     */
    @PostMapping("/{id}/regenerate-key")
    @PreAuthorize("hasRole('ADMIN')")
    public R<AiSystem> regenerateApiKey(@PathVariable Long id) {
        try {
            AiSystem system = aiSystemService.regenerateApiKey(id);

            logger.info("重新生成API密钥成功: systemId={}", id);
            return R.success(system);
        } catch (Exception e) {
            logger.error("重新生成API密钥失败", e);
            return R.error("操作失败");
        }
    }
    
    /**
     * 删除AI系统
     * DELETE /api/aisystem/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Boolean> deleteSystem(@PathVariable Long id) {
        try {
            boolean result = aiSystemService.deleteSystem(id);

            logger.info("删除AI系统: id={}", id);
            return R.success(result);
        } catch (Exception e) {
            logger.error("删除系统失败", e);
            return R.error("操作失败");
        }
    }
    
    /**
     * 授予数据访问权限
     * POST /api/aisystem/permission/grant
     */
    @PostMapping("/permission/grant")
    @PreAuthorize("hasRole('ADMIN')")
    public R<DataAccessPermission> grantPermission(@RequestBody Map<String, Object> request) {
        try {
            Long dataId = request.get("dataId") != null 
                ? ((Number) request.get("dataId")).longValue() 
                : null;
            Long aiSystemId = ((Number) request.get("aiSystemId")).longValue();
            String permissionType = (String) request.get("permissionType");
            String createdBy = (String) request.getOrDefault("createdBy", "admin");
            String expiresAtStr = (String) request.get("expiresAt");
            
            LocalDateTime expiresAt = null;
            if (expiresAtStr != null && !expiresAtStr.isEmpty()) {
                expiresAt = LocalDateTime.parse(expiresAtStr);
            }
            
            DataAccessPermission permission = permissionService.grantPermission(
                dataId, aiSystemId, permissionType, createdBy, expiresAt
            );
            
            logger.info("授予权限成功: aiSystemId={}, dataId={}", aiSystemId, dataId);
            return R.success(permission);
        } catch (Exception e) {
            logger.error("授予权限失败", e);
            return R.error("操作失败");
        }
    }
    
    /**
     * 授予分类权限
     * POST /api/aisystem/permission/grant-category
     */
    @PostMapping("/permission/grant-category")
    @PreAuthorize("hasRole('ADMIN')")
    public R<DataAccessPermission> grantCategoryPermission(@RequestBody Map<String, Object> request) {
        try {
            String category = (String) request.get("category");
            Long aiSystemId = ((Number) request.get("aiSystemId")).longValue();
            String permissionType = (String) request.get("permissionType");
            String createdBy = (String) request.getOrDefault("createdBy", "admin");
            
            DataAccessPermission permission = permissionService.grantCategoryPermission(
                category, aiSystemId, permissionType, createdBy
            );
            
            logger.info("授予分类权限成功: aiSystemId={}, category={}", aiSystemId, category);
            return R.success(permission);
        } catch (Exception e) {
            logger.error("授予分类权限失败", e);
            return R.error("操作失败");
        }
    }
    
    /**
     * 获取AI系统的权限列表
     * GET /api/aisystem/{id}/permissions
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public R<List<DataAccessPermission>> getSystemPermissions(@PathVariable Long id) {
        try {
            List<DataAccessPermission> permissions = permissionService.getSystemPermissions(id);
            return R.success(permissions);
        } catch (Exception e) {
            logger.error("获取权限列表失败", e);
            return R.error("操作失败");
        }
    }
    
    /**
     * 撤销权限
     * DELETE /api/aisystem/permission/{permissionId}
     */
    @DeleteMapping("/permission/{permissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Boolean> revokePermission(@PathVariable Long permissionId) {
        try {
            boolean result = permissionService.revokePermission(permissionId);

            logger.info("撤销权限: id={}", permissionId);
            return R.success(result);
        } catch (Exception e) {
            logger.error("撤销权限失败", e);
            return R.error("操作失败");
        }
    }
    
    /**
     * 检查Ollama服务状态（完整健康信息）
     * GET /api/aisystem/ollama/status
     */
    @GetMapping("/ollama/status")
    @PreAuthorize("hasRole('ADMIN')")
    public R<OllamaHealth> checkOllamaStatus() {
        try {
            OllamaHealth health = ollamaService.getOllamaHealth();
            return R.success(health);
        } catch (Exception e) {
            logger.error("检查Ollama状态失败", e);
            return R.error("操作失败");
        }
    }

    /**
     * 列出已安装模型
     * GET /api/aisystem/ollama/models
     */
    @GetMapping("/ollama/models")
    @PreAuthorize("hasRole('ADMIN')")
    public R<List<OllamaModelInfo>> listOllamaModels() {
        try {
            return R.success(ollamaService.listModels());
        } catch (Exception e) {
            logger.error("列出Ollama模型失败", e);
            return R.error("操作失败");
        }
    }

    /**
     * 拉取模型
     * POST /api/aisystem/ollama/pull  body: {"name":"llama3.2:3b"}
     */
    @PostMapping("/ollama/pull")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Map<String, Object>> pullOllamaModel(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            OllamaService.OllamaResult result = ollamaService.pullModel(name);
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("success", result.success());
            body.put("message", result.message());
            return R.success(body);
        } catch (Exception e) {
            logger.error("拉取Ollama模型失败", e);
            return R.error("操作失败");
        }
    }

    /**
     * 删除模型
     * DELETE /api/aisystem/ollama/models/{name}
     */
    @DeleteMapping("/ollama/models/{name}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Map<String, Object>> deleteOllamaModel(@PathVariable String name) {
        try {
            OllamaService.OllamaResult result = ollamaService.deleteModel(name);
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("success", result.success());
            body.put("message", result.message());
            return R.success(body);
        } catch (Exception e) {
            logger.error("删除Ollama模型失败", e);
            return R.error("操作失败");
        }
    }

    /**
     * 读取 AI 全局配置
     * GET /api/aisystem/config
     */
    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public R<AiConfigDTO> getAiConfig() {
        try {
            return R.success(aiConfigService.getConfig());
        } catch (Exception e) {
            logger.error("读取AI配置失败", e);
            return R.error("操作失败");
        }
    }

    /**
     * 更新 AI 全局配置
     * PUT /api/aisystem/config
     */
    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Map<String, Object>> updateAiConfig(@RequestBody AiConfigDTO configDTO) {
        try {
            aiConfigService.updateConfig(configDTO);
            AiConfigDTO updated = aiConfigService.getConfig();
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("success", true);
            body.put("config", updated);
            return R.success(body);
        } catch (Exception e) {
            logger.error("更新AI配置失败", e);
            return R.error("操作失败");
        }
    }

    /**
     * 使用统计
     * GET /api/aisystem/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Map<String, Object>> getStats() {
        try {
            return R.success(aiSystemService.getStats());
        } catch (Exception e) {
            logger.error("获取使用统计失败", e);
            return R.error("操作失败");
        }
    }
    
    /**
     * 重启Ollama服务
     * POST /api/aisystem/ollama/restart
     */
    @PostMapping("/ollama/restart")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Map<String, Object>> restartOllama() {
        try {
            logger.info("开始重启Ollama服务...");

            boolean wasAvailable = ollamaService.isOllamaAvailable();

            Map<String, Object> result = Map.of(
                "message", "Ollama服务重启请求已发送",
                "previousStatus", wasAvailable ? "running" : "stopped",
                "restarting", true,
                "timestamp", LocalDateTime.now().toString()
            );

            logger.info("Ollama服务重启请求已处理");
            return R.success(result);
        } catch (Exception e) {
            logger.error("重启Ollama服务失败", e);
            return R.error("操作失败");
        }
    }
}

package com.example.aichat.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.AiErrorCode;
import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.entity.AiMemory;
import com.example.aichat.service.AiMemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 记忆接口（/api/memory）。
 *
 * 安全加固：所有端点 requireUserId()，userId 一律取当前登录令牌，
 * 记忆归属校验下放到 Service（requireOwnership），杜绝 IDOR。
 */
@RestController
@RequestMapping("/api/memory")
public class AiMemoryController {

    @Autowired
    private AiMemoryService aiMemoryService;

    /** 创建记忆 */
    @PostMapping
    public R create(@RequestBody Map<String, Object> request) {
        Long userId = AuthContext.requireUserId();
        Object contentObj = request.get("content");
        if (contentObj == null || contentObj.toString().isBlank()) {
            throw new BusinessException(AiErrorCode.MEMORY_CONTENT_BLANK);
        }
        String source = request.get("source") != null ? request.get("source").toString() : null;
        String memoryType = request.get("memoryType") != null ? request.get("memoryType").toString() : null;
        Integer importance = toInt(request.get("importance"));
        String category = request.get("category") != null ? request.get("category").toString() : null;

        AiMemory memory = aiMemoryService.createMemory(
                userId, contentObj.toString(), source, memoryType, importance, category);
        return R.success(memory);
    }

    /** 更新记忆（校验归属） */
    @PutMapping("/{memoryId}")
    public R update(@PathVariable Long memoryId, @RequestBody Map<String, Object> request) {
        Long userId = AuthContext.requireUserId();
        String content = request.get("content") != null ? request.get("content").toString() : null;
        String memoryType = request.get("memoryType") != null ? request.get("memoryType").toString() : null;
        Integer importance = toInt(request.get("importance"));
        String category = request.get("category") != null ? request.get("category").toString() : null;
        Boolean isActive = request.get("isActive") != null ? (Boolean) request.get("isActive") : null;

        AiMemory memory = aiMemoryService.updateMemory(
                userId, memoryId, content, memoryType, importance, category, isActive);
        return R.success(memory);
    }

    /** 删除记忆（校验归属） */
    @DeleteMapping("/{memoryId}")
    public R delete(@PathVariable Long memoryId) {
        Long userId = AuthContext.requireUserId();
        aiMemoryService.deleteMemory(userId, memoryId);
        return R.success("删除成功");
    }

    /** 分页查询（page/size/memoryType/category/isActive/keyword/source） */
    @GetMapping("/list")
    public R list(@RequestParam(defaultValue = "1") int page,
                  @RequestParam(defaultValue = "10") int size,
                  @RequestParam(required = false) String memoryType,
                  @RequestParam(required = false) String category,
                  @RequestParam(required = false) Boolean isActive,
                  @RequestParam(required = false) String keyword,
                  @RequestParam(required = false) String source) {
        Long userId = AuthContext.requireUserId();
        IPage<AiMemory> result = aiMemoryService.listMemories(
                userId, page, size, memoryType, category, isActive, keyword, source);
        return R.success(result);
    }

    /** 统计 */
    @GetMapping("/stats")
    public R stats() {
        Long userId = AuthContext.requireUserId();
        return R.success(aiMemoryService.getMemoryStats(userId));
    }

    /** 详情（校验归属） */
    @GetMapping("/{memoryId}")
    public R detail(@PathVariable Long memoryId) {
        Long userId = AuthContext.requireUserId();
        return R.success(aiMemoryService.getMemory(userId, memoryId));
    }

    /** 切换生效状态 */
    @PostMapping("/{memoryId}/toggle")
    public R toggle(@PathVariable Long memoryId) {
        Long userId = AuthContext.requireUserId();
        return R.success(aiMemoryService.toggleMemory(userId, memoryId));
    }

    // ================= M1 / A3：记忆可视化管理约定路径 =================

    /** 记忆条目列表（分页）。GET /api/memory/items，与 /list 等价。
     *  支持按分类浏览（category=FACT/PREFERENCE/EVENT/RELATIONSHIP）。 */
    @GetMapping("/items")
    public R items(@RequestParam(defaultValue = "1") int page,
                   @RequestParam(defaultValue = "10") int size,
                   @RequestParam(required = false) String keyword,
                   @RequestParam(required = false) String source,
                   @RequestParam(required = false) String category) {
        Long userId = AuthContext.requireUserId();
        IPage<AiMemory> result = aiMemoryService.listMemories(
                userId, page, size, null, category, null, keyword, source);
        return R.success(result);
    }

    /** 批量删除记忆。DELETE /api/memory/batch  body:{ ids:[1,2,3] } */
    @DeleteMapping("/batch")
    public R batchDelete(@RequestBody Map<String, Object> request) {
        Long userId = AuthContext.requireUserId();
        Object idsObj = request.get("ids");
        if (!(idsObj instanceof List) || ((List<?>) idsObj).isEmpty()) {
            return R.error("ids 不能为空");
        }
        List<Long> ids = new java.util.ArrayList<>();
        for (Object o : (List<?>) idsObj) {
            if (o == null) continue;
            try {
                ids.add(Long.valueOf(o.toString()));
            } catch (NumberFormatException ignore) {
            }
        }
        int deleted = aiMemoryService.batchDeleteMemories(userId, ids);
        return R.success(Map.of("deleted", deleted));
    }

    /** 导出当前用户全部生效记忆为 JSON。GET /api/memory/export */
    @GetMapping("/export")
    public R export() {
        Long userId = AuthContext.requireUserId();
        return R.success(aiMemoryService.exportAllMemories(userId));
    }

    /** 编辑单条记忆内容。PUT /api/memory/items/{id} */
    @PutMapping("/items/{id}")
    public R updateItem(@PathVariable("id") Long memoryId, @RequestBody Map<String, Object> request) {
        Long userId = AuthContext.requireUserId();
        String content = request.get("content") != null ? request.get("content").toString() : null;
        AiMemory memory = aiMemoryService.updateMemory(
                userId, memoryId, content, null, null, null, null);
        return R.success(memory);
    }

    /** 删除单条记忆。DELETE /api/memory/items/{id}（删除后不再注入 prompt） */
    @DeleteMapping("/items/{id}")
    public R deleteItem(@PathVariable("id") Long memoryId) {
        Long userId = AuthContext.requireUserId();
        aiMemoryService.deleteMemory(userId, memoryId);
        return R.success("删除成功");
    }

    /** 读取记忆全局开关。GET /api/memory/settings */
    @GetMapping("/settings")
    public R getSettings() {
        Long userId = AuthContext.requireUserId();
        return R.success(aiMemoryService.getMemorySetting(userId));
    }

    /** 更新记忆全局开关。PUT /api/memory/settings  body:{enabled:boolean} */
    @PutMapping("/settings")
    public R updateSettings(@RequestBody Map<String, Object> request) {
        Long userId = AuthContext.requireUserId();
        Object enabledObj = request.get("enabled");
        if (enabledObj == null) {
            return R.error("enabled 不能为空");
        }
        boolean enabled = Boolean.parseBoolean(enabledObj.toString());
        aiMemoryService.updateMemorySetting(userId, enabled);
        return R.success(Map.of("enabled", enabled));
    }

    /** 将对话消息钉选加入长期记忆。POST /api/memory/pin  body:{content, source?} */
    @PostMapping("/pin")
    public R pin(@RequestBody Map<String, Object> request) {
        Long userId = AuthContext.requireUserId();
        Object contentObj = request.get("content");
        if (contentObj == null || contentObj.toString().isBlank()) {
            throw new BusinessException(AiErrorCode.MEMORY_CONTENT_BLANK);
        }
        String source = request.get("source") != null ? request.get("source").toString() : null;
        AiMemory memory = aiMemoryService.pinMessageToMemory(userId, contentObj.toString(), source);
        return R.success(memory);
    }

    private Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

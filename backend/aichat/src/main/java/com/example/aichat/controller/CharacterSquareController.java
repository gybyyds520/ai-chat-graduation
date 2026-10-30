package com.example.aichat.controller;

import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.service.CharacterPlazaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 角色广场（/api/character/square）。
 *
 * 对任务约定路径 /api/character/square 提供公开发现接口，内部复用 CharacterPlazaService
 * （数据来源 ai_character.is_shared = true）。
 *
 * 安全策略：
 *   - GET 浏览类接口匿名可访问（SecurityConfig 已放行 /api/character/*）；
 *   - “使用”动作仍需登录（由 /api/plaza/{id}/use 或开始对话时记录，requireUserId）。
 */
@RestController
@RequestMapping("/api/character")
public class CharacterSquareController {

    private static final Logger logger = LoggerFactory.getLogger(CharacterSquareController.class);

    @Autowired
    private CharacterPlazaService characterPlazaService;

    /**
     * 角色广场分页列表。
     *
     * @param page     页码（从 1 开始）
     * @param size     每页大小
     * @param category 分类筛选（可空）
     * @param sortBy   排序：hot=按使用人数 / created=最新 / rating=评分
     * @param keyword  关键词搜索（可空；非空时走名称/简介/标签模糊搜索）
     */
    @GetMapping("/square")
    public R<Map<String, Object>> square(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "12") int size,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(defaultValue = "hot") String sortBy,
                                         @RequestParam(required = false) String keyword) {
        Map<String, Object> result;
        if (keyword != null && !keyword.isBlank()) {
            result = characterPlazaService.searchPublicCharacters(keyword, page, size);
        } else {
            result = characterPlazaService.listPublicCharacters(page, size, category, sortBy);
        }
        return R.success(result);
    }

    /**
     * 记录“使用此角色”：要求登录（requireUserId），未登录返回 401。
     * 复用 CharacterPlazaService.incrementUseCount。
     */
    @PostMapping("/square/use/{characterId}")
    public R use(@PathVariable Long characterId) {
        Long userId = AuthContext.requireUserId();
        boolean ok = characterPlazaService.incrementUseCount(characterId);
        if (!ok) {
            return R.error("角色不存在");
        }
        logger.info("角色广场使用: userId={}, characterId={}", userId, characterId);
        return R.success("已记录使用");
    }
}

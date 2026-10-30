package com.example.aichat.controller;

import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.entity.AiCharacter;
import com.example.aichat.service.CharacterPlazaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色广场 / 发现页接口（/api/plaza）。
 *
 * 安全策略：
 *   - GET 浏览类接口（list/search/hot/categories/detail）匿名可访问（在 SecurityConfig 中放行）；
 *   - POST /{characterId}/use 要求登录（requireUserId），用于记录使用次数。
 */
@RestController
@RequestMapping("/api/plaza")
public class CharacterPlazaController {

    @Autowired
    private CharacterPlazaService characterPlazaService;

    /** 公开角色列表（分页/分类/排序）。 */
    @GetMapping("/list")
    public R list(@RequestParam(defaultValue = "1") int page,
                  @RequestParam(defaultValue = "12") int size,
                  @RequestParam(required = false) String category,
                  @RequestParam(defaultValue = "hot") String sortBy) {
        return R.success(characterPlazaService.listPublicCharacters(page, size, category, sortBy));
    }

    /** 搜索公开角色。 */
    @GetMapping("/search")
    public R search(@RequestParam String keyword,
                    @RequestParam(defaultValue = "1") int page,
                    @RequestParam(defaultValue = "12") int size) {
        return R.success(characterPlazaService.searchPublicCharacters(keyword, page, size));
    }

    /** 热门角色排行。 */
    @GetMapping("/hot")
    public R hot(@RequestParam(defaultValue = "10") int limit) {
        List<AiCharacter> hot = characterPlazaService.getHotCharacters(limit);
        return R.success(hot);
    }

    /** 分类列表。 */
    @GetMapping("/categories")
    public R categories() {
        return R.success(characterPlazaService.getCategories());
    }

    /** 角色详情（可匿名浏览，userId 可空用于判断是否为所有者）。 */
    @GetMapping("/{characterId}")
    public R detail(@PathVariable Long characterId,
                    @RequestParam(required = false) Long userId) {
        // 匿名浏览：未登录时 userId 为 null；登录时优先取 SecurityContext
        Long current = AuthContext.isAuthenticated() ? AuthContext.requireUserId() : userId;
        Map<String, Object> detail = characterPlazaService.getCharacterDetail(characterId, current);
        if (Boolean.FALSE.equals(detail.get("found"))) {
            return R.error("角色不存在或未公开");
        }
        return R.success(detail);
    }

    /** 记录使用（使用次数 +1，要求登录）。 */
    @PostMapping("/{characterId}/use")
    public R use(@PathVariable Long characterId) {
        // 安全：使用记录必须登录
        AuthContext.requireUserId();
        boolean ok = characterPlazaService.incrementUseCount(characterId);
        if (!ok) {
            return R.error("角色不存在");
        }
        return R.success("已记录使用");
    }

    /**
     * 将自建角色分享到广场（要求登录 + 必须为角色创建者）。
     * 安全：归属校验在 Service 内完成（character.userId == 当前用户），非创建者返回 403。
     */
    @PostMapping("/share/{characterId}")
    public R share(@PathVariable Long characterId) {
        Long userId = AuthContext.requireUserId();
        characterPlazaService.shareToPlaza(characterId, userId);
        return R.success(Map.of("success", true, "isShared", true));
    }

    /**
     * 取消自建角色的广场分享（要求登录 + 必须为角色创建者）。
     */
    @PostMapping("/unshare/{characterId}")
    public R unshare(@PathVariable Long characterId) {
        Long userId = AuthContext.requireUserId();
        characterPlazaService.unshareFromPlaza(characterId, userId);
        return R.success(Map.of("success", true, "isShared", false));
    }
}

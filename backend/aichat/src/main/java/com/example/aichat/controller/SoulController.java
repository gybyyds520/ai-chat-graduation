package com.example.aichat.controller;

import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.entity.AiSoulExperience;
import com.example.aichat.service.SoulEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 灵魂引擎控制器。
 *
 * 灵魂算法（用户原创）：这一刻的自己 + 发生的经历 + 如何感受和理解 + 过去留下的记忆 = 下一刻的自己。
 * 暴露：灵魂状态查询 / 经历流水 / 重置 / 手动触发一次演化（供测试）。
 * 所有接口均以当前登录用户为归属维度（AuthContext.requireUserId()），Service 层再校验归属。
 */
@RestController
@RequestMapping("/api/soul")
public class SoulController {

    private static final Logger logger = LoggerFactory.getLogger(SoulController.class);

    @Autowired
    private SoulEngineService soulEngineService;

    /** 灵魂状态快照（含演化版本/心情/态度/性格权重/信念） */
    @GetMapping("/{characterId}")
    public R getSoul(@PathVariable Long characterId) {
        Long userId = AuthContext.requireUserId();
        return R.success(soulEngineService.getSoulOverview(userId, characterId));
    }

    /** 灵魂经历流水（分页，按时间倒序） */
    @GetMapping("/{characterId}/experiences")
    public R listExperiences(@PathVariable Long characterId,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int size) {
        Long userId = AuthContext.requireUserId();
        List<AiSoulExperience> list = soulEngineService.listExperiences(userId, characterId, page, size);
        return R.success(Map.of("list", list, "page", Math.max(page, 1), "size", size));
    }

    /** 重置灵魂：清空经历并恢复初始状态 */
    @PostMapping("/{characterId}/reset")
    public R resetSoul(@PathVariable Long characterId) {
        Long userId = AuthContext.requireUserId();
        return R.success(soulEngineService.resetSoul(userId, characterId));
    }

    /** 手动触发一次灵魂演化（供测试/演示：传一段用户消息即可演化） */
    @PostMapping("/{characterId}/evolve")
    public R evolve(@PathVariable Long characterId,
                    @RequestBody(required = false) Map<String, String> body) {
        Long userId = AuthContext.requireUserId();
        String message = body != null && body.get("message") != null ? body.get("message") : "今天过得怎么样？";
        soulEngineService.evolveAfterChat(userId, characterId, null, message, "");
        return R.success(Map.of("evolved", true, "message", message));
    }

    /** 灵魂注入预览：查看当前灵魂会以什么形态注入 prompt */
    @GetMapping("/{characterId}/prompt")
    public R soulPrompt(@PathVariable Long characterId) {
        Long userId = AuthContext.requireUserId();
        String prefix = soulEngineService.buildSoulPrompt(userId, characterId);
        return R.success(Map.of("prefix", prefix));
    }
}

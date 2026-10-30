package com.example.aichat.controller;

import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.service.QuotaService;
import org.springframework.web.bind.annotation.*;

/**
 * 配额查询与重置接口（全部要求登录）。
 */
@RestController
@RequestMapping("/api/quota")
public class QuotaController {

    private final QuotaService quotaService;

    public QuotaController(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    /** 当前配额使用情况。 */
    @GetMapping("/current")
    public R current() {
        Long userId = AuthContext.requireUserId();
        return R.success(quotaService.getCurrentQuota(userId));
    }

    /** 重置当日配额（测试 / 自助）。 */
    @PostMapping("/reset")
    public R reset() {
        Long userId = AuthContext.requireUserId();
        quotaService.resetDailyQuotas(userId);
        return R.success();
    }
}

package com.example.aichat.controller;

import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.service.CheckinService;
import org.springframework.web.bind.annotation.*;

/**
 * 每日签到打卡（全部要求登录，操作者强制取当前登录用户，杜绝越权代签）。
 */
@RestController
@RequestMapping("/api/checkin")
public class CheckinController {

    private final CheckinService checkinService;

    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    /** 每日签到。 */
    @PostMapping
    public R checkin() {
        Long userId = AuthContext.requireUserId();
        return R.success(checkinService.checkin(userId));
    }

    /** 查询今日是否已签到 / 连续签到天数 / 奖励预览。 */
    @GetMapping("/status")
    public R status() {
        Long userId = AuthContext.requireUserId();
        return R.success(checkinService.status(userId));
    }
}

package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.PrivacySetting;
import com.example.aichat.service.PrivacyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/privacy")
public class PrivacyController {

    @Autowired
    private PrivacyService privacyService;

    @GetMapping("/settings")
    public R getSettings(@RequestParam Long userId) {
        // 安全修复（P0）：隐私设置仅本人可读
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        Map<String, Object> settings = privacyService.getAllSettings(current);
        return R.success(settings);
    }

    @GetMapping("/setting")
    public R getSetting(@RequestParam Long userId, @RequestParam String key) {
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        PrivacySetting setting = privacyService.getSetting(current, key);
        if (setting != null) {
            return R.success(setting);
        }
        return R.success(null);
    }

    @PostMapping("/setting")
    public R updateSetting(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        String key = (String) request.get("key");
        Object valueObj = request.get("value");
        
        if (key == null) {
            return R.error("参数错误");
        }
        
        // 安全修复（P0）：仅可修改自己的隐私设置
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());
        
        String value;
        if (valueObj instanceof String) {
            value = (String) valueObj;
        } else {
            value = valueObj != null ? valueObj.toString() : null;
        }
        
        PrivacySetting setting = privacyService.updateSetting(userId, key, value);
        return R.success(setting);
    }

    @DeleteMapping("/setting")
    public R deleteSetting(@RequestParam Long userId, @RequestParam String key) {
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        privacyService.deleteSetting(current, key);
        return R.success("删除成功");
    }
}
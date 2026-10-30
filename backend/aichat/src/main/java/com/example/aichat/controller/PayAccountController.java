package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.PayAccount;
import com.example.aichat.service.PayAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pay-account")
public class PayAccountController {

    @Autowired
    private PayAccountService payAccountService;

    @PostMapping("/bind")
    public R bindAccount(@RequestBody Map<String, Object> request) {
        if (!request.containsKey("accountType") || 
            !request.containsKey("accountNumber") || !request.containsKey("accountName")) {
            return R.badRequest("缺少必要参数");
        }

        // 安全修复（P0）：绑定账户强制归属当前登录用户
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                request.get("userId") == null ? null : Long.parseLong(request.get("userId").toString()));
        PayAccount account = payAccountService.bindAccount(userId, request);

        Map<String, Object> result = Map.of(
            "id", account.getId(),
            "accountType", account.getAccountType(),
            "accountNumber", account.getAccountNumber(),
            "status", account.getStatus(),
            "message", "账户绑定成功，验证码已发送"
        );

        return R.success(result);
    }

    @PostMapping("/verify")
    public R verifyAccount(@RequestBody Map<String, Object> request) {
        if (!request.containsKey("id") || !request.containsKey("verificationCode")) {
            return R.badRequest("缺少必要参数");
        }

        Long id = Long.parseLong(request.get("id").toString());
        String verificationCode = request.get("verificationCode").toString();

        PayAccount account = payAccountService.verifyAccount(id, verificationCode);
        if (account == null) {
            return R.error("验证码错误");
        }

        return R.success(Map.of(
            "id", account.getId(),
            "status", account.getStatus(),
            "message", "账户验证成功"
        ));
    }

    @DeleteMapping("/{id}")
    public R unbindAccount(@PathVariable Long id) {
        payAccountService.unbindAccount(id);
        return R.success("账户解绑成功");
    }

    @GetMapping("/user/{userId}")
    public R getAccountsByUser(@PathVariable Long userId) {
        // 安全修复（P0）：支付账户为敏感数据，仅本人可查
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        List<PayAccount> accounts = payAccountService.getAccountsByUserId(current);
        return R.success(accounts);
    }

    @GetMapping("/user/{userId}/has-any")
    public R hasAnyAccount(@PathVariable Long userId) {
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        boolean hasAccount = payAccountService.hasAnyAccount(current);
        return R.success(Map.of("hasAccount", hasAccount));
    }
}
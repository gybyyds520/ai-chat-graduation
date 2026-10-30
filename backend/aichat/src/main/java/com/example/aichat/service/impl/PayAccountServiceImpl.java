package com.example.aichat.service.impl;

import com.example.aichat.entity.PayAccount;
import com.example.aichat.mapper.PayAccountMapper;
import com.example.aichat.service.PayAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class PayAccountServiceImpl implements PayAccountService {

    @Autowired
    private PayAccountMapper payAccountMapper;

    private final Random random = new Random();

    @Override
    @Transactional
    public PayAccount bindAccount(Long userId, Map<String, Object> request) {
        String accountType = request.get("accountType").toString();
        String accountNumber = request.get("accountNumber").toString();
        String accountName = request.get("accountName").toString();

        List<PayAccount> existing = payAccountMapper.selectByUserIdAndType(userId, accountType);
        if (!existing.isEmpty()) {
            PayAccount account = existing.get(0);
            account.setAccountNumber(accountNumber);
            account.setAccountName(accountName);
            account.setStatus("PENDING_VERIFICATION");
            account.setVerificationCode(generateVerificationCode());
            account.setUpdateTime(LocalDateTime.now());
            payAccountMapper.updateById(account);
            return account;
        }

        PayAccount account = new PayAccount();
        account.setUserId(userId);
        account.setAccountType(accountType);
        account.setAccountNumber(maskAccountNumber(accountType, accountNumber));
        account.setAccountName(accountName);
        account.setStatus("PENDING_VERIFICATION");
        account.setVerificationCode(generateVerificationCode());
        account.setCreateTime(LocalDateTime.now());
        account.setUpdateTime(LocalDateTime.now());

        payAccountMapper.insert(account);
        return account;
    }

    @Override
    @Transactional
    public PayAccount verifyAccount(Long id, String verificationCode) {
        PayAccount account = payAccountMapper.selectById(id);
        if (account == null) {
            return null;
        }
        // 安全加固（P0）：仅账户所有者可完成验证
        com.example.aichat.common.security.AuthContext.requireSelf(account.getUserId());

        if ("VERIFIED".equals(account.getStatus())) {
            return account;
        }

        if (verificationCode != null && verificationCode.equals(account.getVerificationCode())) {
            account.setStatus("VERIFIED");
            account.setVerifiedAt(LocalDateTime.now());
            account.setUpdateTime(LocalDateTime.now());
            payAccountMapper.updateById(account);
            return account;
        }

        return null;
    }

    @Override
    @Transactional
    public void unbindAccount(Long id) {
        PayAccount account = payAccountMapper.selectById(id);
        if (account == null) {
            return;
        }
        // 安全加固（P0）：仅账户所有者可解绑
        com.example.aichat.common.security.AuthContext.requireSelf(account.getUserId());
        payAccountMapper.deleteById(id);
    }

    @Override
    public List<PayAccount> getAccountsByUserId(Long userId) {
        return payAccountMapper.selectByUserId(userId);
    }

    @Override
    public boolean hasAnyAccount(Long userId) {
        return countAccounts(userId) > 0;
    }

    @Override
    public int countAccounts(Long userId) {
        return payAccountMapper.countByUserId(userId);
    }

    private String generateVerificationCode() {
        return String.format("%06d", random.nextInt(1000000));
    }

    private String maskAccountNumber(String accountType, String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        int maskLength = accountNumber.length() - 4;
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < maskLength; i++) {
            masked.append("*");
        }
        masked.append(accountNumber.substring(accountNumber.length() - 4));
        return masked.toString();
    }
}
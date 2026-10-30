package com.example.aichat.service;

import com.example.aichat.entity.PayAccount;

import java.util.List;
import java.util.Map;

public interface PayAccountService {

    PayAccount bindAccount(Long userId, Map<String, Object> request);

    PayAccount verifyAccount(Long id, String verificationCode);

    void unbindAccount(Long id);

    List<PayAccount> getAccountsByUserId(Long userId);

    boolean hasAnyAccount(Long userId);

    int countAccounts(Long userId);
}
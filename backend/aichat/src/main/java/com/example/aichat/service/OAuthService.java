package com.example.aichat.service;

import java.util.Map;

public interface OAuthService {

    Map<String, Object> wechatLogin(String code, String deviceId, String ipAddress);

    Map<String, Object> wechatCallback(String code, String state);

    void bindWechat(Long userId, String code);

    void unbindWechat(Long userId);

    boolean isWechatBound(Long userId);
}
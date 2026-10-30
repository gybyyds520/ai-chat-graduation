package com.example.aichat.service;

import com.example.aichat.entity.User;

public interface AdminService {
    User adminLogin(String username, String password);
    boolean isAdmin(Long userId);
}
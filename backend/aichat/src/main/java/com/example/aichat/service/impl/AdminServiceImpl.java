package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);

    private static final String ADMIN_USERNAME = "admin";

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public User adminLogin(String username, String password) {
        logger.info("========== 管理员登录尝试开始 ==========");
        logger.info("登录用户名: {}", username != null ? username.substring(0, Math.min(2, username.length())) + "***" : "null");

        if (username == null || username.isEmpty()) {
            logger.warn("登录失败: 用户名为空");
            return null;
        }

        if (password == null || password.isEmpty()) {
            logger.warn("登录失败: 密码为空");
            return null;
        }

        if (!ADMIN_USERNAME.equals(username)) {
            logger.error("登录失败: 用户名不是管理员");
            logger.info("========== 管理员登录尝试结束 (失败) ==========");
            return null;
        }

        // 从数据库 user 表查询 admin 用户记录
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", ADMIN_USERNAME);
        User admin = userMapper.selectOne(wrapper);

        if (admin == null) {
            logger.error("登录失败: 数据库中未找到 admin 用户记录");
            logger.info("========== 管理员登录尝试结束 (失败) ==========");
            return null;
        }

        String storedPassword = admin.getPassword();
        if (storedPassword == null || storedPassword.isEmpty()) {
            logger.error("登录失败: admin 用户密码字段为空");
            logger.info("========== 管理员登录尝试结束 (失败) ==========");
            return null;
        }

        // 密码验证：统一走 DelegatingPasswordEncoder（AppConfig 配置）。
        // 兼容 `{bcrypt}$2a$...` 前缀哈希与无前缀 `$2a$` 哈希两种存储格式；
        // 明文口令（无算法前缀且非 $2 开头）按默认 bcrypt 编码器校验必然不匹配，
        // 从而杜绝"明文口令可登录管理员"的安全隐患（明文口令种子数据已迁移为哈希）。
        boolean passwordMatch = passwordEncoder.matches(password, storedPassword);

        if (passwordMatch) {
            logger.info("管理员登录成功!");
            // 确保 admin 记录的必要字段完整
            if (admin.getNickname() == null || admin.getNickname().isEmpty()) {
                admin.setNickname("管理员");
            }
            if (admin.getVersionType() == null || admin.getVersionType().isEmpty()) {
                admin.setVersionType("ENTERPRISE");
            }
            if (admin.getAccountStatus() == null || admin.getAccountStatus().isEmpty()) {
                admin.setAccountStatus("ACTIVE");
            }
            logger.info("========== 管理员登录尝试结束 (成功) ==========");
            return admin;
        } else {
            logger.error("登录失败: 密码不正确");
            logger.info("========== 管理员登录尝试结束 (失败) ==========");
            return null;
        }
    }

    @Override
    public boolean isAdmin(Long userId) {
        // 管理员判断：userId 为数据库中 admin 用户记录的 id
        // 登录时 adminLogin 返回数据库中的真实 admin 用户对象（含真实 id）
        if (userId == null) {
            return false;
        }
        // 查询数据库确认该用户是 admin
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("id", userId).eq("username", ADMIN_USERNAME);
        User admin = userMapper.selectOne(wrapper);
        return admin != null;
    }
}

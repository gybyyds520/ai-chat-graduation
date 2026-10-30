package com.example.aichat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * 密码加密器配置。
 *
 * Phase 4 (P4-a) 升级：从单一 BCryptPasswordEncoder 升级为 DelegatingPasswordEncoder，
 * 支持"算法前缀标识"机制（如 `{bcrypt}$2a$10$xxx`），便于：
 *   1. 向前兼容：现有 BCrypt 哈希（无前缀）仍可匹配；
 *   2. 未来迁移：新增 Argon2/scrypt 等更强算法时，新密码带前缀生成，
 *      老密码仍用 BCrypt 验证，逐步轮换；
 *   3. 应急切换：若 BCrypt 被发现漏洞，可快速切换默认算法。
 *
 * 默认算法仍为 BCrypt（保持现有密码哈希格式不变，前端无感知）。
 */
@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 算法 ID 到实现类的映射
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        // Pbkdf2 作为备选算法（无外部依赖，JDK 内置）
        encoders.put("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        // 明文（仅用于历史遗留数据迁移，不推荐生产使用）
        encoders.put("noop", org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance());

        // 默认使用 bcrypt，新密码生成时不带前缀（保持与现有数据库哈希格式一致）
        // 当未来切换到 Argon2 时，改为 new Argon2PasswordEncoder() 并设置默认前缀
        DelegatingPasswordEncoder delegating = new DelegatingPasswordEncoder("bcrypt", encoders);
        // 设置默认未标记前缀的哈希按 BCrypt 解析（兼容现有数据库存储格式）
        delegating.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());
        return delegating;
    }
}

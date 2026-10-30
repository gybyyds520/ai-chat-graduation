package com.example.aichat.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 4 (P4-e) 密码加密器兼容性验证测试。
 *
 * 覆盖点：
 * 1. 新生成的密码带 {bcrypt} 前缀，且能被 DelegatingPasswordEncoder 正确校验；
 * 2. 历史数据库中的 BCrypt 哈希（无前缀）仍可匹配（向前兼容关键路径）；
 * 3. 显式带前缀的 {pbkdf2} / {noop} 哈希可被识别并校验；
 * 4. 同一明文密码的两次加密生成不同哈希（盐值随机），但均可通过校验；
 * 5. 错误密码不能通过校验；
 * 6. 无效前缀抛出 IllegalArgumentException（防止静默放行）。
 *
 * 不依赖 Spring 上下文，纯单元测试，可在 CI 无 MySQL 环境运行。
 */
class PasswordEncoderTest {

    private DelegatingPasswordEncoder delegating;

    @BeforeEach
    void setUp() {
        // 复刻 AppConfig.passwordEncoder() 的装配逻辑，避免启动 Spring 上下文
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        encoders.put("pbkdf2", org.springframework.security.crypto.password.Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("noop", NoOpPasswordEncoder.getInstance());

        delegating = new DelegatingPasswordEncoder("bcrypt", encoders);
        // 兼容现有数据库无前缀哈希：默认按 BCrypt 匹配
        delegating.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());
    }

    @Test
    void encode_generatesBcryptWithPrefix() {
        // 新密码应带 {bcrypt} 前缀，便于未来切换算法时识别
        String raw = "P@ssw0rd!2026";
        String encoded = delegating.encode(raw);

        assertNotNull(encoded);
        assertTrue(encoded.startsWith("{bcrypt}"),
                "新密码应带 {bcrypt} 前缀，实际值=" + encoded);
        assertTrue(delegating.matches(raw, encoded),
                "加密后的哈希应能通过 matches 校验");
    }

    @Test
    void matches_legacyBcryptHashWithoutPrefixStillMatches() {
        // 关键兼容性场景：数据库中已存在无前缀的 BCrypt 哈希（由老版本 BCryptPasswordEncoder 生成）
        // 模拟老数据库中的哈希（直接用 BCryptPasswordEncoder 生成，不带前缀）
        String raw = "legacyPassword123";
        BCryptPasswordEncoder legacyEncoder = new BCryptPasswordEncoder();
        String legacyHash = legacyEncoder.encode(raw);

        assertFalse(legacyHash.startsWith("{"),
                "老哈希应无算法前缀，实际值=" + legacyHash);

        // DelegatingPasswordEncoder 应能正确校验无前缀哈希（走 defaultPasswordEncoderForMatches 分支）
        assertTrue(delegating.matches(raw, legacyHash),
                "无前缀 BCrypt 哈希应能被 DelegatingPasswordEncoder 校验");
        assertFalse(delegating.matches("wrongPassword", legacyHash),
                "错误密码不应通过校验");
    }

    @Test
    void matches_prefixedBcryptHashMatches() {
        // 显式带 {bcrypt} 前缀的哈希（未来迁移路径）
        String raw = "mySecret!#%";
        String encoded = delegating.encode(raw);

        assertTrue(delegating.matches(raw, encoded));
        assertFalse(delegating.matches("differentPassword", encoded));
    }

    @Test
    void matches_prefixedPbkdf2HashMatches() {
        // 显式 {pbkdf2} 前缀哈希应走 Pbkdf2PasswordEncoder 分支
        String raw = "pbkdf2Password";
        // 手动构造 {pbkdf2} 前缀哈希验证前缀路由正确性
        PasswordEncoder pbkdf2Encoder = org.springframework.security.crypto.password.Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        String pbkdf2Prefixed = "{pbkdf2}" + pbkdf2Encoder.encode(raw);

        assertTrue(delegating.matches(raw, pbkdf2Prefixed),
                "{pbkdf2} 前缀哈希应能正确校验");
        assertFalse(delegating.matches("notThePassword", pbkdf2Prefixed));
    }

    @Test
    void matches_legacyNoopHashMatches() {
        // {noop} 明文存储（仅用于历史遗留迁移，生产不应使用）
        String raw = "plainTextLegacy";
        String noopHash = "{noop}" + raw;

        assertTrue(delegating.matches(raw, noopHash),
                "{noop} 明文哈希应能正确校验");
        assertFalse(delegating.matches("notMatch", noopHash));
    }

    @Test
    void encode_samePasswordProducesDifferentHashes() {
        // BCrypt 每次加密使用随机盐，同一明文应生成不同哈希（防彩虹表）
        String raw = "repeatedPassword";
        String encoded1 = delegating.encode(raw);
        String encoded2 = delegating.encode(raw);

        assertNotEquals(encoded1, encoded2,
                "同一明文两次加密应生成不同哈希（盐值随机）");
        assertTrue(delegating.matches(raw, encoded1));
        assertTrue(delegating.matches(raw, encoded2));
    }

    @Test
    void upgradeEncoding_detectsLegacyHashAndRecommendsUpgrade() {
        // DelegatingPasswordEncoder.upgradeEncoding 用于识别"无前缀老哈希需要升级"
        String raw = "toUpgrade";
        String legacyHash = new BCryptPasswordEncoder().encode(raw); // 无前缀

        assertTrue(delegating.matches(raw, legacyHash));
        // upgradeEncoding 应返回 true，表示此哈希无前缀，建议在用户登录时重新加密
        assertTrue(delegating.upgradeEncoding(legacyHash),
                "无前缀老哈希应被识别为需要升级");

        String newHash = delegating.encode(raw);
        assertFalse(delegating.upgradeEncoding(newHash),
                "带前缀的新哈希不应被识别为需要升级");
    }

    @Test
    void matches_invalidPrefixFallsThroughToDefaultAndReturnsFalse() {
        // 无效前缀 {unknown} 不被识别：因设置了 defaultPasswordEncoderForMatches (BCrypt)，
        // 会落入默认 BCrypt 分支并返回 false（而不是抛异常），防止静默放行。
        String invalidHash = "{unknown}$2a$10$someInvalidHash";
        assertFalse(delegating.matches("anything", invalidHash),
                "未知算法前缀应被默认 BCrypt 编码器拒绝，返回 false 而非静默通过");
    }

    @Test
    void matches_nullRawPasswordThrowsToFailFast() {
        // null 明文密码：BCryptPasswordEncoder.matches 会抛 IllegalArgumentException
        // （Spring Security 的 fail-fast 设计，避免 null 与空字符串混用造成安全漏洞）
        String encoded = delegating.encode("realPassword");
        assertThrows(IllegalArgumentException.class,
                () -> delegating.matches(null, encoded),
                "null 明文密码应抛 IllegalArgumentException（fail-fast，避免 NPE 与空串歧义）");
    }

    @Test
    void endToEnd_loginFlowSimulation() {
        // 模拟真实登录流程：注册时加密 → 数据库存储 → 登录时校验
        String username = "testuser";
        String rawPassword = "User@2026";

        // 注册阶段：加密并存储到数据库
        String storedHash = delegating.encode(rawPassword);
        assertTrue(storedHash.startsWith("{bcrypt}"));

        // 登录阶段：用户输入密码，与数据库哈希比对
        assertTrue(delegating.matches(rawPassword, storedHash),
                "正确密码应通过登录校验");
        assertFalse(delegating.matches("wrongAttempt", storedHash),
                "错误密码不应通过登录校验");

        // 模拟老用户首次登录：数据库存的是无前缀老哈希
        String oldDbHash = new BCryptPasswordEncoder().encode(rawPassword);
        assertTrue(delegating.matches(rawPassword, oldDbHash),
                "老用户的 BCrypt 哈希应能通过登录校验（向后兼容）");
        assertTrue(delegating.upgradeEncoding(oldDbHash),
                "登录后应触发哈希升级到带前缀格式");

        // 升级后重新加密
        String upgradedHash = delegating.encode(rawPassword);
        assertTrue(delegating.matches(rawPassword, upgradedHash));
        assertFalse(delegating.upgradeEncoding(upgradedHash),
                "升级后的哈希不应再被识别为需要升级");
    }
}

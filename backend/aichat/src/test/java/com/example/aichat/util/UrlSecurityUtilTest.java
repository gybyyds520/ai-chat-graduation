package com.example.aichat.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UrlSecurityUtil 单元测试。
 *
 * 覆盖点：
 *   1. http:// 合法URL应通过
 *   2. https:// 合法URL应通过
 *   3. null/空URL应抛出异常
 *   4. file:// 协议应被拒绝
 *   5. javascript: 协议应被拒绝
 *   6. ftp:// 协议应被拒绝
 *   7. 回环地址 127.0.0.1 应被拒绝
 *   8. localhost 应被拒绝
 *   9. 内网IP 10.x 应被拒绝
 *  10. 内网IP 192.168.x 应被拒绝
 *  11. 编码IP（纯数字主机名）应被拒绝
 *  12. 无主机名应抛出异常
 */
class UrlSecurityUtilTest {

    @Test
    void validateUrl_httpUrl应通过() {
        assertDoesNotThrow(() -> UrlSecurityUtil.validateUrl("http://example.com/page"));
    }

    @Test
    void validateUrl_httpsUrl应通过() {
        assertDoesNotThrow(() -> UrlSecurityUtil.validateUrl("https://www.example.com/api"));
    }

    @Test
    void validateUrl_nullUrl应抛出异常() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> UrlSecurityUtil.validateUrl(null));
        assertTrue(ex.getMessage().contains("URL不能为空"));
    }

    @Test
    void validateUrl_空Url应抛出异常() {
        assertThrows(IllegalArgumentException.class,
                () -> UrlSecurityUtil.validateUrl(""));
        assertThrows(IllegalArgumentException.class,
                () -> UrlSecurityUtil.validateUrl("   "));
    }

    @Test
    void validateUrl_fileProtocol应被拒绝() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> UrlSecurityUtil.validateUrl("file:///etc/passwd"));
        assertTrue(ex.getMessage().contains("http/https"),
                "错误信息应说明仅允许http/https，实际: " + ex.getMessage());
    }

    @Test
    void validateUrl_javascriptProtocol应被拒绝() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> UrlSecurityUtil.validateUrl("javascript:alert(1)"));
        assertTrue(ex.getMessage().contains("http/https"));
    }

    @Test
    void validateUrl_ftpProtocol应被拒绝() {
        assertThrows(IllegalArgumentException.class,
                () -> UrlSecurityUtil.validateUrl("ftp://ftp.example.com/file"));
    }

    @Test
    void validateUrl_回环地址应被拒绝() {
        // 127.0.0.1 会被 DNS 解析为回环地址
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> UrlSecurityUtil.validateUrl("http://127.0.0.1/admin"));
        assertTrue(ex.getMessage().contains("内网/保留地址") || ex.getMessage().contains("禁止访问"),
                "应拒绝回环地址，实际: " + ex.getMessage());
    }

    @Test
    void validateUrl_localhost应被拒绝() {
        // localhost 解析为 127.0.0.1
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> UrlSecurityUtil.validateUrl("http://localhost:8080/internal"));
        assertTrue(ex.getMessage().contains("内网/保留地址") || ex.getMessage().contains("禁止访问"),
                "应拒绝localhost，实际: " + ex.getMessage());
    }

    @Test
    void validateUrl_内网IP10应被拒绝() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> UrlSecurityUtil.validateUrl("http://10.0.0.1/internal"));
        assertTrue(ex.getMessage().contains("内网/保留地址") || ex.getMessage().contains("禁止访问"));
    }

    @Test
    void validateUrl_内网IP192应被拒绝() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> UrlSecurityUtil.validateUrl("http://192.168.1.1/admin"));
        assertTrue(ex.getMessage().contains("内网/保留地址") || ex.getMessage().contains("禁止访问"));
    }

    @Test
    void validateUrl_纯数字主机名应被拒绝() {
        // 2130706433 是 127.0.0.1 的十进制表示
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> UrlSecurityUtil.validateUrl("http://2130706433/"));
        assertTrue(ex.getMessage().contains("主机名非法") || ex.getMessage().contains("内网/保留地址"),
                "应拒绝编码IP，实际: " + ex.getMessage());
    }

    @Test
    void validateUrl_无主机名应抛出异常() {
        assertThrows(IllegalArgumentException.class,
                () -> UrlSecurityUtil.validateUrl("http://"));
    }

    @Test
    void validateUrl_带端口的公网URL应通过() {
        assertDoesNotThrow(() -> UrlSecurityUtil.validateUrl("https://api.github.com:443/users"));
    }
}

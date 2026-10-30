package com.example.aichat.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全栈审查 - TokenUtils 单元测试。
 *
 * 覆盖点：
 * 1. generateToken 输出三段式 JWT 结构（header.payload.signature）；
 * 2. getUserIdFromToken 正确解析 userId（验证 Phase 3 P3-e 的 + 11 错位修复）；
 * 3. validateToken 完整性校验：合法 token 通过、被篡改的 token 拒绝；
 * 4. 边界值：单字符 userId、多字符 userId、空 token、非法格式 token；
 * 5. 防止 JSON 注入：username 含 " 或 \ 时不应破坏 token 解析。
 *
 * 设计说明：
 *   - 不依赖 Spring ApplicationContext，纯静态方法调用；
 *   - 不直接读取真实环境变量，使用默认 fallback 密钥即可验证签名一致性。
 */
class TokenUtilsTest {

    @Test
    void generateToken_返回三段式JWT结构() {
        String token = TokenUtils.generateToken("42", "alice");
        assertNotNull(token);
        // JWT 必须由 header.payload.signature 三段组成
        assertEquals(3, token.split("\\.").length, "JWT 应为三段式结构");
    }

    @Test
    void getUserIdFromToken_正确解析多字符userId() {
        // 该用例直接验证 Phase 3 P3-e 的 + 11 错位 bug 修复
        // 原 bug：indexOf + 11 会跳过 "userId":" 的最后一个字符，
        //         导致 "42" 被解析为 "2"，"1234" 被解析为 "234"
        String token = TokenUtils.generateToken("42", "alice");
        String userId = TokenUtils.getUserIdFromToken(token);
        assertEquals("42", userId, "多字符 userId 必须完整解析，不能截断首位");
    }

    @Test
    void getUserIdFromToken_正确解析单字符userId() {
        // 原 bug 场景：userId="0" 时 + 11 读取到闭合 "，返回空字符串
        String token = TokenUtils.generateToken("0", "admin");
        String userId = TokenUtils.getUserIdFromToken(token);
        assertEquals("0", userId, "单字符 userId 不应被错位读取为空");
    }

    @Test
    void getUserIdFromToken_正确解析长数字userId() {
        String token = TokenUtils.generateToken("1234567890", "longIdUser");
        String userId = TokenUtils.getUserIdFromToken(token);
        assertEquals("1234567890", userId);
    }

    @Test
    void getUserIdFromToken_空token返回null() {
        assertNull(TokenUtils.getUserIdFromToken(""));
    }

    @Test
    void getUserIdFromToken_非三段式token返回null() {
        assertNull(TokenUtils.getUserIdFromToken("invalid.token"));
        assertNull(TokenUtils.getUserIdFromToken("onlyonesegment"));
    }

    @Test
    void getUserIdFromToken_payload无userId字段返回null() {
        // 构造一个有效 base64 但不含 userId 字段的 token
        String header = java.util.Base64.getUrlEncoder()
                .encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String payload = java.util.Base64.getUrlEncoder()
                .encodeToString("{\"foo\":\"bar\"}".getBytes());
        String fakeSig = java.util.Base64.getUrlEncoder().encodeToString(new byte[]{1, 2, 3});
        assertNull(TokenUtils.getUserIdFromToken(header + "." + payload + "." + fakeSig));
    }

    @Test
    void validateToken_合法token返回true() {
        String token = TokenUtils.generateToken("42", "alice");
        assertTrue(TokenUtils.validateToken(token), "未篡改的 token 必须通过完整性校验");
    }

    @Test
    void validateToken_篡改payload后返回false() {
        String token = TokenUtils.generateToken("42", "alice");
        String[] parts = token.split("\\.");
        // 构造一个篡改后的 payload（替换 userId 为 "999"）
        String tamperedPayload = java.util.Base64.getUrlEncoder()
                .encodeToString("{\"userId\":\"999\",\"username\":\"alice\"}".getBytes());
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];
        assertFalse(TokenUtils.validateToken(tamperedToken), "篡改 payload 后签名不匹配，必须拒绝");
    }

    @Test
    void validateToken_签名错误返回false() {
        String token = TokenUtils.generateToken("42", "alice");
        String[] parts = token.split("\\.");
        // 用错误签名拼接
        String tamperedToken = parts[0] + "." + parts[1] + ".invalid-signature";
        assertFalse(TokenUtils.validateToken(tamperedToken));
    }

    @Test
    void validateToken_非三段式返回false() {
        assertFalse(TokenUtils.validateToken("invalid"));
        assertFalse(TokenUtils.validateToken(""));
    }

    @Test
    void generateToken_username含特殊字符不破坏解析() {
        // 安全修复：toJson 中对 " 和 \ 进行了转义，防止 JSON 注入
        // 这里验证特殊字符 username 不应破坏 token 的可解析性
        String malicious = "alice\"evil";
        String token = TokenUtils.generateToken("42", malicious);
        // token 仍能解析出 userId（username 是否能完整还原不在 TokenUtils 范围）
        assertEquals("42", TokenUtils.getUserIdFromToken(token));
        assertTrue(TokenUtils.validateToken(token), "含特殊字符 username 的 token 签名应一致");
    }

    @Test
    void generateToken_多次生成签名一致() {
        // 同一输入应生成同一签名（HMAC 确定性）—— 注意 claims 中含 iat 时间戳，
        // 所以 token 字符串本身会变，但签名算法本身是确定性的。
        // 这里仅验证两次生成的 token 都能被 validateToken 通过
        String token1 = TokenUtils.generateToken("7", "u1");
        String token2 = TokenUtils.generateToken("7", "u1");
        assertTrue(TokenUtils.validateToken(token1));
        assertTrue(TokenUtils.validateToken(token2));
        assertEquals("7", TokenUtils.getUserIdFromToken(token1));
        assertEquals("7", TokenUtils.getUserIdFromToken(token2));
    }

    // ============ 边界补充用例 ============

    @Test
    void getUserIdFromToken_null入参返回null不抛异常() {
        // 边界：null 不应导致 NPE
        assertNull(TokenUtils.getUserIdFromToken(null));
    }

    @Test
    void validateToken_null入参返回false不抛异常() {
        assertFalse(TokenUtils.validateToken(null));
    }

    @Test
    void validateToken_只有两段返回false() {
        assertFalse(TokenUtils.validateToken("header.payload"));
    }

    @Test
    void getUserIdFromToken_payload中userId为空串返回null() {
        // 构造 userId 为空值的 payload，应被守卫拦截
        String header = java.util.Base64.getUrlEncoder()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());
        String payload = java.util.Base64.getUrlEncoder()
                .encodeToString("{\"userId\":\"\",\"username\":\"a\"}".getBytes());
        String fakeSig = java.util.Base64.getUrlEncoder().encodeToString(new byte[]{9, 9});
        assertNull(TokenUtils.getUserIdFromToken(header + "." + payload + "." + fakeSig));
    }

    // ============ P1-10：exp 为必填，缺失即拒绝 ============

    /** 用当前进程的签名密钥为指定 payload 重新签名，构造"签名合法但 payload 缺 exp"的 token。 */
    private String signWithoutExp() throws Exception {
        java.lang.reflect.Field secretField = TokenUtils.class.getDeclaredField("SECRET_KEY");
        secretField.setAccessible(true);
        String secret = (String) secretField.get(null);

        String header = java.util.Base64.getUrlEncoder()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());
        // 故意不含 exp 字段
        String payload = java.util.Base64.getUrlEncoder()
                .encodeToString("{\"userId\":\"42\",\"username\":\"alice\"}".getBytes());

        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        String sig = java.util.Base64.getUrlEncoder()
                .encodeToString(mac.doFinal((header + "." + payload).getBytes()));
        return header + "." + payload + "." + sig;
    }

    @Test
    void 签名合法但payload缺exp_必须拒绝() throws Exception {
        String token = signWithoutExp();
        assertFalse(TokenUtils.validateToken(token), "无 exp 的 token 不得视为永久有效");
    }

    @Test
    void 合法生成的token含exp_仍通过() {
        String token = TokenUtils.generateToken("42", "alice");
        assertTrue(TokenUtils.validateToken(token));
    }
}

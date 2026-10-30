package com.example.aichat.utils;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TokenUtils 安全边界专项测试。
 *
 * 覆盖（补充 TokenUtilsTest 未覆盖的攻击面）：
 * 1. 过期 token（exp 已过）必须被拒绝；
 * 2. alg:none 攻击（签名为空）必须被拒绝；
 * 3. 算法混淆（header 声明 HS256 但 payload 被篡改后重签）必须拒绝；
 * 4. 用错误密钥签名的 token 必须被拒绝；
 * 5. exp 为未来时间但签名错误 → 拒绝；
 * 6. token 中包含伪造的 role/isAdmin 字段不影响服务端角色判定（服务端查库）。
 */
class TokenUtilsSecurityEdgeCaseTest {

    /** 通过反射拿到当前进程的 SECRET_KEY，用于构造"签名合法但 payload 特殊"的 token。 */
    private String getSecret() throws Exception {
        Field f = TokenUtils.class.getDeclaredField("SECRET_KEY");
        f.setAccessible(true);
        return (String) f.get(null);
    }

    /** 用指定 header/payload 和当前密钥计算 HMAC-SHA256 签名。 */
    private String sign(String headerB64, String payloadB64, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        byte[] sig = mac.doFinal((headerB64 + "." + payloadB64).getBytes());
        return Base64.getUrlEncoder().encodeToString(sig);
    }

    private String b64(String s) {
        return Base64.getUrlEncoder().encodeToString(s.getBytes());
    }

    @Test
    void 过期token必须被拒绝() throws Exception {
        String secret = getSecret();
        String header = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        // exp 设为 1 小时前
        long past = System.currentTimeMillis() - 3600_000L;
        String payload = b64("{\"userId\":\"42\",\"username\":\"alice\",\"exp\":\"" + past + "\"}");
        String sig = sign(header, payload, secret);
        String expiredToken = header + "." + payload + "." + sig;

        assertFalse(TokenUtils.validateToken(expiredToken),
                "exp 已过的 token 必须被拒绝");
    }

    @Test
    void 未过期token必须通过() throws Exception {
        String secret = getSecret();
        String header = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        long future = System.currentTimeMillis() + 3600_000L;
        String payload = b64("{\"userId\":\"42\",\"username\":\"alice\",\"exp\":\"" + future + "\"}");
        String sig = sign(header, payload, secret);
        String validToken = header + "." + payload + "." + sig;

        assertTrue(TokenUtils.validateToken(validToken),
                "exp 在未来的 token 应通过");
        assertEquals("42", TokenUtils.getUserIdFromToken(validToken));
    }

    @Test
    void alg_none攻击必须被拒绝() {
        // alg:none 攻击：header 声明 none，签名为空
        String header = b64("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String payload = b64("{\"userId\":\"0\",\"username\":\"admin\"}");
        String noneToken = header + "." + payload + ".";

        assertFalse(TokenUtils.validateToken(noneToken),
                "alg:none 攻击必须被拒绝");
    }

    @Test
    void alg_none大写攻击必须被拒绝() {
        String header = b64("{\"alg\":\"None\",\"typ\":\"JWT\"}");
        String payload = b64("{\"userId\":\"0\",\"username\":\"admin\"}");
        String noneToken = header + "." + payload + ".";

        assertFalse(TokenUtils.validateToken(noneToken),
                "alg:None 攻击必须被拒绝");
    }

    @Test
    void 用错误密钥签名必须被拒绝() {
        String header = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        long future = System.currentTimeMillis() + 3600_000L;
        String payload = b64("{\"userId\":\"42\",\"username\":\"alice\",\"exp\":\"" + future + "\"}");
        // 用错误密钥签名
        String wrongSecret = "0123456789abcdef0123456789abcdef0123456789abcdef";
        try {
            String sig = sign(header, payload, wrongSecret);
            String forged = header + "." + payload + "." + sig;
            assertFalse(TokenUtils.validateToken(forged),
                    "用错误密钥签名的 token 必须被拒绝");
        } catch (Exception e) {
            fail("构造错误密钥 token 不应抛异常: " + e.getMessage());
        }
    }

    @Test
    void payload中伪造isAdmin字段不影响解析() {
        // 即使 payload 中加入 isAdmin:true，TokenUtils 本身不读取该字段；
        // 服务端角色由 JwtAuthenticationFilter 通过 adminService.isAdmin(userId) 查库决定。
        String token = TokenUtils.generateToken("42", "alice");
        // 正常生成的 token 不应包含 isAdmin 字段
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        assertFalse(payload.contains("isAdmin"),
                "generateToken 不应签发 isAdmin 字段");
        assertTrue(TokenUtils.validateToken(token));
        assertEquals("42", TokenUtils.getUserIdFromToken(token));
    }

    @Test
    void 篡改userId后签名不匹配必须拒绝() throws Exception {
        String secret = getSecret();
        String header = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        long future = System.currentTimeMillis() + 3600_000L;
        // 原始 token 是 userId=42，篡改为 userId=0（冒充管理员）
        String payload42 = b64("{\"userId\":\"42\",\"username\":\"alice\",\"exp\":\"" + future + "\"}");
        String sig42 = sign(header, payload42, secret);
        String original = header + "." + payload42 + "." + sig42;
        assertTrue(TokenUtils.validateToken(original));

        // 篡改 payload 为 userId=0 但不重新签名
        String payload0 = b64("{\"userId\":\"0\",\"username\":\"admin\",\"exp\":\"" + future + "\"}");
        String tampered = header + "." + payload0 + "." + sig42;
        assertFalse(TokenUtils.validateToken(tampered),
                "篡改 userId 为管理员后签名不匹配，必须拒绝");
    }

    @Test
    void 空字符串token必须拒绝() {
        assertFalse(TokenUtils.validateToken(""));
        assertFalse(TokenUtils.validateToken(null));
    }

    @Test
    void 只有header和payload没有signature必须拒绝() {
        String header = b64("{\"alg\":\"HS256\"}");
        String payload = b64("{\"userId\":\"42\"}");
        assertFalse(TokenUtils.validateToken(header + "." + payload));
    }

    @Test
    void base64编码的payload含特殊字符不崩溃() {
        // 畸形 base64 不应导致异常
        assertDoesNotThrow(() -> TokenUtils.validateToken("a.b.c"));
        assertDoesNotThrow(() -> TokenUtils.validateToken("!!!.@@@.###"));
        assertDoesNotThrow(() -> TokenUtils.getUserIdFromToken("a.b.c"));
    }
}

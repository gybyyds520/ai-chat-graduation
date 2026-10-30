package com.example.aichat.utils;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TokenUtils {

    /**
     * 安全修复（加固）：
     * 1) 生产环境必须通过环境变量 JWT_SECRET_KEY 或系统属性 jwt.secret.key 提供 ≥32 字节密钥；
     * 2) 未配置时不再使用可预测的硬编码回退密钥（否则攻击者可伪造任意用户/管理员 token），
     *    而是生成随机会话密钥：本地开发可用，重启后所有旧 token 失效（可接受）；
     * 3) 日志中仅提示未配置，不泄露任何密钥内容。
     */
    private static final String SECRET_KEY;
    private static final long EXPIRATION_TIME = 86400000L;

    static {
        String configured = System.getProperty("jwt.secret.key");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("JWT_SECRET_KEY");
        }
        if (configured != null && !configured.isBlank() && configured.getBytes().length >= 32) {
            SECRET_KEY = configured;
        } else {
            if (configured != null && !configured.isBlank()) {
                System.err.println("[WARN] JWT_SECRET_KEY 长度不足 32 字节，已忽略并改用随机密钥（重启后旧 token 失效）");
            } else {
                System.err.println("[WARN] 未配置 JWT_SECRET_KEY，使用随机生成的会话密钥（重启后所有 token 失效；生产环境请务必配置）");
            }
            SECRET_KEY = generateRandomSecret();
        }
    }

    private static String generateRandomSecret() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String generateToken(String userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("exp", new Date(System.currentTimeMillis() + EXPIRATION_TIME));
        claims.put("iat", new Date());
        
        String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getUrlEncoder().encodeToString(toJson(claims).getBytes());
        String signature = generateSignature(header + "." + payload);
        
        return header + "." + payload + "." + signature;
    }

    private static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof String) {
                // 安全修复：转义字符串中的特殊字符，防止JSON注入
                String escaped = ((String) entry.getValue())
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
                sb.append("\"").append(escaped).append("\"");
            } else if (entry.getValue() instanceof Date) {
                sb.append("\"").append(((Date) entry.getValue()).getTime()).append("\"");
            } else {
                sb.append(entry.getValue());
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private static String generateSignature(String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(SECRET_KEY.getBytes(), "HmacSHA256");
            mac.init(keySpec);
            byte[] signatureBytes = mac.doFinal(data.getBytes());
            return Base64.getUrlEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    public static String getUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            // Bug 修复（Phase 3 P3-e）：原 + 11 错位读取 userId 值，
            // "userId":" 共 10 字符，indexOf 返回匹配开始位置，+ 10 才指向 value 首字符。
            // 原 + 11 会导致 "42" 被解析为 "2"，"0" 被解析为空字符串。
            int userIdStart = payload.indexOf("\"userId\":\"") + 10;
            if (userIdStart < 10) return null;  // 未找到 userId 字段
            int userIdEnd = payload.indexOf("\"", userIdStart);
            if (userIdEnd <= userIdStart) return null;  // value 为空或格式异常
            return payload.substring(userIdStart, userIdEnd);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 安全修复：验证Token完整性 - 检查签名是否匹配（常数时间比较，防时序侧信道）
     */
    public static boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;

            // 安全加固：校验 header 声明的算法必须是 HS256，防止算法混淆/降级攻击
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            if (!headerJson.contains("\"alg\":\"HS256\"") && !headerJson.contains("\"alg\": \"HS256\"")) {
                return false;
            }

            String header = parts[0];
            String payload = parts[1];
            String signature = parts[2];

            // 重新计算签名并比较（常数时间）
            String expectedSignature = generateSignature(header + "." + payload);
            if (!java.security.MessageDigest.isEqual(
                    signature.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    expectedSignature.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                return false;
            }

            // 验证userId是否存在
            String userId = getUserIdFromToken(token);
            if (userId == null) {
                return false;
            }

            // 验证过期时间（P1-10 修复）：exp 为必填。解析不到 exp（expTime<=0）一律拒绝，
            // 防止无 exp 的 token 被视为永久有效。
            String payloadJson = new String(Base64.getUrlDecoder().decode(payload));
            long expTime = extractExpTime(payloadJson);
            if (expTime <= 0 || System.currentTimeMillis() > expTime) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static long extractExpTime(String payloadJson) {
        try {
            int expStart = payloadJson.indexOf("\"exp\":\"") + 7;
            if (expStart < 7) {
                expStart = payloadJson.indexOf("\"exp\":") + 6;
                if (expStart < 6) return 0;
            }
            int expEnd = payloadJson.indexOf("\"", expStart);
            if (expEnd == -1) {
                expEnd = payloadJson.indexOf(",", expStart);
                if (expEnd == -1) expEnd = payloadJson.indexOf("}", expStart);
            }
            return Long.parseLong(payloadJson.substring(expStart, expEnd).replace("\"", "").trim());
        } catch (Exception e) {
            return 0;
        }
    }
}

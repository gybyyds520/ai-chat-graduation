package com.example.aichat.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * 轻量级 TOTP（RFC 6238）校验工具，用于多因素认证（MFA）验证码校验。
 * 兼容 Google Authenticator / 微信小程序 TOTP 生成的 6 位动态码。
 */
public final class TOTPUtil {

    private static final int CODE_DIGITS = 6;
    private static final long TIME_STEP_SECONDS = 30;

    private TOTPUtil() {}

    /**
     * 校验用户输入的动态码是否与 secret 匹配（容忍前后 window 个时间窗的时钟偏移）。
     */
    public static boolean verify(String base32Secret, String code, int window) throws GeneralSecurityException {
        if (base32Secret == null || base32Secret.isBlank() || code == null || code.isBlank()) {
            return false;
        }
        if (!code.matches("\\d{6}")) {
            return false;
        }
        byte[] key = base32Decode(base32Secret.trim());
        long counter = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;
        for (int i = -window; i <= window; i++) {
            String candidate = generateCode(key, counter + i);
            if (constantTimeEquals(candidate, code)) {
                return true;
            }
        }
        return false;
    }

    private static String generateCode(byte[] key, long counter) throws GeneralSecurityException {
        byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);

        int otp = binary % (int) Math.pow(10, CODE_DIGITS);
        return String.format("%0" + CODE_DIGITS + "d", otp);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        return java.security.MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] base32Decode(String input) throws GeneralSecurityException {
        // 标准 Base32 字母表 -> 十六进制 -> 字节，兼容大小写与去空格
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder binary = new StringBuilder();
        for (char c : input.toUpperCase().toCharArray()) {
            int idx = alphabet.indexOf(c);
            if (idx < 0) {
                throw new GeneralSecurityException("非法的Base32字符");
            }
            binary.append(String.format("%5s", Integer.toBinaryString(idx)).replace(' ', '0'));
        }
        int usable = (binary.length() / 8) * 8;
        if (usable == 0) {
            throw new GeneralSecurityException("密钥过短");
        }
        byte[] bytes = new byte[usable / 8];
        for (int i = 0; i < usable; i += 8) {
            bytes[i / 8] = (byte) Integer.parseInt(binary.substring(i, i + 8), 2);
        }
        return bytes;
    }
}

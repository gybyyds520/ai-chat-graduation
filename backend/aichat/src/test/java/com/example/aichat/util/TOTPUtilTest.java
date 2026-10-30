package com.example.aichat.util;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TOTPUtil（RFC 6238 动态口令校验）纯单元测试。
 *
 * <p>本类不加载 Spring 上下文，全部为纯静态方法调用。
 * 正向用例使用独立的 RFC 6238 参考实现（与被测方法同算法但独立编写），
 * 对已知密钥字节（"12345678901234567890"，RFC 6238 官方测试向量密钥）
 * 的 Base32 编码串 {@code GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ} 计算当前时间窗口令，
 * 从而同时校验 Base32 解码、HMAC-SHA1 截断与时间窗容忍三个环节。
 */
class TOTPUtilTest {

    /** RFC 6238 官方测试向量密钥（20 字节 ASCII）。 */
    private static final byte[] RFC_KEY = "12345678901234567890".getBytes(StandardCharsets.UTF_8);
    /** 上述密钥的标准 Base32 编码（独立于被测类硬编码，避免同源错误）。 */
    private static final String BASE32_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    /** 当前时间步计数器（30s/步）。 */
    private static long nowCounter() {
        return System.currentTimeMillis() / 1000 / 30;
    }

    /** 独立参考实现：用已知密钥字节与计数器计算 6 位 TOTP。 */
    private static String referenceCode(byte[] key, long counter) throws Exception {
        byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(data);
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
        int otp = binary % 1_000_000;
        return String.format("%06d", otp);
    }

    // ── T1：空/blank 输入必须返回 false（不得抛 NPE） ──────────────────────
    @Test
    void verify_空密钥或空码_返回false() {
        assertDoesNotThrow(() -> {
            assertFalse(TOTPUtil.verify(null, "123456", 1));
            assertFalse(TOTPUtil.verify("", "123456", 1));
            assertFalse(TOTPUtil.verify("   ", "123456", 1));
            assertFalse(TOTPUtil.verify(BASE32_SECRET, null, 1));
            assertFalse(TOTPUtil.verify(BASE32_SECRET, "", 1));
            assertFalse(TOTPUtil.verify(BASE32_SECRET, "   ", 1));
        });
    }

    // ── T2：非 6 位数字码必须返回 false（不进入解码/HMAC） ──────────────────
    @Test
    void verify_非六位数字码_返回false() {
        assertDoesNotThrow(() -> {
            assertFalse(TOTPUtil.verify(BASE32_SECRET, "12345", 1));      // 5 位
            assertFalse(TOTPUtil.verify(BASE32_SECRET, "1234567", 1));    // 7 位
            assertFalse(TOTPUtil.verify(BASE32_SECRET, "abcdef", 1));    // 非数字
            assertFalse(TOTPUtil.verify(BASE32_SECRET, "12 3456", 1));   // 含空格
            assertFalse(TOTPUtil.verify(BASE32_SECRET, "12345a", 1));     // 字母混合
        });
    }

    // ── T3：非法 Base32 字符必须抛 GeneralSecurityException（不得静默吞错） ──
    @Test
    void verify_非法Base32密钥_抛出异常() {
        // '!' 不在 Base32 字母表中
        assertThrows(GeneralSecurityException.class,
                () -> TOTPUtil.verify("!!!!", "123456", 1));
    }

    // ── T4：合法密钥 + 当前窗正确口令 → true（独立参考实现计算） ──────────
    // window=1 容忍 ±1 个时间窗，规避测试执行瞬间跨过 30s 边界导致的偶发失败。
    @Test
    void verify_正确当前口令_窗口容忍内_返回true() throws Exception {
        long c = nowCounter();
        String code = referenceCode(RFC_KEY, c);
        assertEquals(6, code.length());
        assertTrue(TOTPUtil.verify(BASE32_SECRET, code, 1),
                "当前时间窗口令应在 window=1 容忍内校验通过");
    }

    // ── T5：合法密钥 + 错误口令 → false（不得对任意 6 位码放行） ───────────
    @Test
    void verify_错误口令_返回false() throws Exception {
        long c = nowCounter();
        // 构造一个与当前正确码不同的 6 位码：把正确码每位翻转
        String correct = referenceCode(RFC_KEY, c);
        char[] flipped = correct.toCharArray();
        for (int i = 0; i < flipped.length; i++) {
            flipped[i] = (char) (((flipped[i] - '0' + 5) % 10) + '0');
        }
        String wrong = new String(flipped);
        assertNotEquals(correct, wrong);
        assertFalse(TOTPUtil.verify(BASE32_SECRET, wrong, 0),
                "错误口令即使 window=0 也必须拒绝");
    }

    // ── T6：负 window 不进入循环，即使正确口令也返回 false（边界契约） ────
    // 循环 for(i=-window; i<=window; i++) 在 window<0 时一次都不执行。
    @Test
    void verify_负window_不迭代_返回false() throws Exception {
        long c = nowCounter();
        String code = referenceCode(RFC_KEY, c);
        // window=-1 => -window=1, 1<=-1 为假，循环不执行
        assertFalse(TOTPUtil.verify(BASE32_SECRET, code, -1),
                "负 window 不应匹配任何时间窗");
    }
}

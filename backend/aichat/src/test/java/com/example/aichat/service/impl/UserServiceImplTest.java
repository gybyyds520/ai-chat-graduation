package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.UserErrorCode;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 全栈审查 - UserServiceImpl 单元测试（Mockito 隔离测试）。
 *
 * 覆盖点（验证 P1 安全修复）：
 *   1. register：参数校验（用户名/密码长度）+ 重复用户名抛业务异常；
 *   2. login：用户名密码错误后递增失败计数，5 次锁定 30 分钟；
 *   3. login：成功登录后清理失败计数；
 *   4. login：参数为空直接抛 LOGIN_FAILED（不查询数据库）；
 *   5. changePassword：新密码长度校验 + 旧密码错误抛 OLD_PASSWORD_INCORRECT；
 *   6. register：密码经 BCrypt 加密后才入库（验证 passwordEncoder.encode 被调用）。
 *
 * 设计说明：
 *   - 使用 @Mock 隔离 Mapper/Cache/PasswordEncoder，避免依赖 MySQL/Redis；
 *   - @InjectMocks 自动将 mock 注入 UserServiceImpl 字段；
 *   - CacheService 的 mock 返回 null（即模拟无历史失败计数），覆盖大多数路径。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CacheService cacheService;

    @Mock
    private com.example.aichat.service.MinorProtectionService minorProtectionService;

    @InjectMocks
    private UserServiceImpl userService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("alice");
        existingUser.setPassword("hashed:secret");
        existingUser.setNickname("Alice");
        existingUser.setAccountStatus("ACTIVE");
    }

    // ============ register 测试 ============

    @Test
    void register_用户名过短应抛USERNAME_LENGTH_INVALID() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.register("ab", "p@ssw0rd", "Ab"));
        assertEquals(UserErrorCode.USERNAME_LENGTH_INVALID, ex.getErrorCode());
        verifyNoInteractions(userMapper);
    }

    @Test
    void register_密码过短应抛PASSWORD_LENGTH_INVALID() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.register("alice", "123", "Alice"));
        assertEquals(UserErrorCode.PASSWORD_LENGTH_INVALID, ex.getErrorCode());
        verifyNoInteractions(userMapper);
    }

    @Test
    void register_用户名已存在应抛USERNAME_EXISTS() {
        when(userMapper.selectOne(any())).thenReturn(existingUser);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.register("alice", "p@ssw0rd", "Alice"));
        assertEquals(UserErrorCode.USERNAME_EXISTS, ex.getErrorCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void register_正常注册应加密密码并插入用户() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("p@ssw0rd")).thenReturn("hashed:p@ssw0rd");

        User user = userService.register("newuser", "p@ssw0rd", "New User");

        assertNotNull(user);
        assertEquals("newuser", user.getUsername());
        assertEquals("hashed:p@ssw0rd", user.getPassword());
        verify(userMapper).insert(any(User.class));
        verify(passwordEncoder).encode("p@ssw0rd");
    }

    @Test
    void register_传入年龄应写入User对象() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("p@ssw0rd")).thenReturn("hashed:p@ssw0rd");

        User user = userService.register("teenuser", "p@ssw0rd", "Teen", 15);

        assertNotNull(user);
        assertEquals(15, user.getAge());
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void register_年龄0应视为未声明不写入() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("p@ssw0rd")).thenReturn("hashed:p@ssw0rd");

        User user = userService.register("nouser", "p@ssw0rd", "None", 0);
        assertNull(user.getAge());
    }

    @Test
    void register_年龄超范围应抛MINOR_AGE_INVALID() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.register("newuser", "p@ssw0rd", "N", 999));
        assertEquals(UserErrorCode.MINOR_AGE_INVALID, ex.getErrorCode());
        verifyNoInteractions(userMapper);
    }

    // ============ login 测试 ============

    @Test
    void login_参数为空应抛LOGIN_FAILED不查询DB() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login("", "password"));
        assertEquals(UserErrorCode.LOGIN_FAILED, ex.getErrorCode());
        verifyNoInteractions(userMapper);
    }

    @Test
    void login_用户不存在应递增失败计数并抛LOGIN_FAILED() {
        when(userMapper.selectOne(any())).thenReturn(null);
        // cacheService.get 返回 null 表示无历史失败计数
        when(cacheService.get(eq("login_failed:ghost"), eq(Integer.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login("ghost", "wrong"));
        assertEquals(UserErrorCode.LOGIN_FAILED, ex.getErrorCode());
        // 验证失败计数递增到 1 并写入 Redis
        verify(cacheService).set(eq("login_failed:ghost"), eq(1), eq(1800L));
    }

    @Test
    void login_密码错误应递增失败计数() {
        when(userMapper.selectOne(any())).thenReturn(existingUser);
        when(passwordEncoder.matches("wrong", "hashed:secret")).thenReturn(false);
        when(cacheService.get(eq("login_failed:alice"), eq(Integer.class))).thenReturn(2);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login("alice", "wrong"));
        assertEquals(UserErrorCode.LOGIN_FAILED, ex.getErrorCode());
        // 已有 2 次，递增到 3 次写入
        verify(cacheService).set(eq("login_failed:alice"), eq(3), eq(1800L));
    }

    @Test
    void login_第5次失败应抛ACCOUNT_LOCKED() {
        when(userMapper.selectOne(any())).thenReturn(existingUser);
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        when(cacheService.get(eq("login_failed:alice"), eq(Integer.class))).thenReturn(4);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login("alice", "wrong"));
        assertEquals(UserErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        verify(cacheService).set(eq("login_failed:alice"), eq(5), eq(1800L));
    }

    @Test
    void login_成功登录后应清理失败计数() {
        when(userMapper.selectOne(any())).thenReturn(existingUser);
        when(passwordEncoder.matches("secret", "hashed:secret")).thenReturn(true);

        User result = userService.login("alice", "secret");

        assertNotNull(result);
        assertEquals("alice", result.getUsername());
        verify(cacheService).delete("login_failed:alice");
    }

    @Test
    void login_支持邮箱登录() {
        when(userMapper.selectOne(any())).thenReturn(null).thenReturn(existingUser);
        when(passwordEncoder.matches("secret", "hashed:secret")).thenReturn(true);

        User result = userService.login("alice@example.com", "secret");

        assertNotNull(result);
        // 应清理失败计数
        verify(cacheService).delete("login_failed:alice@example.com");
    }

    @Test
    void login_账号被禁用即使密码正确也拒绝() {
        existingUser.setAccountStatus("disabled");
        when(userMapper.selectOne(any())).thenReturn(existingUser);
        when(passwordEncoder.matches("secret", "hashed:secret")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login("alice", "secret"));
        assertEquals(UserErrorCode.ACCOUNT_DISABLED, ex.getErrorCode());
        // 禁用拒绝不应清理失败计数（也不应发 token）
        verify(cacheService, never()).delete(anyString());
    }

    @Test
    void login_账号被锁定即使密码正确也拒绝() {
        existingUser.setAccountStatus("locked");
        when(userMapper.selectOne(any())).thenReturn(existingUser);
        when(passwordEncoder.matches("secret", "hashed:secret")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login("alice", "secret"));
        assertEquals(UserErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
    }

    @Test
    void login_密码错误时即使账号禁用也不泄露状态() {
        // 安全：密码错误先抛 LOGIN_FAILED，不泄露账号是否被禁用（防用户枚举）
        existingUser.setAccountStatus("disabled");
        when(userMapper.selectOne(any())).thenReturn(existingUser);
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        when(cacheService.get(eq("login_failed:alice"), eq(Integer.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login("alice", "wrong"));
        assertEquals(UserErrorCode.LOGIN_FAILED, ex.getErrorCode());
    }

    // ============ changePassword 测试 ============

    @Test
    void changePassword_新密码过短应抛PASSWORD_LENGTH_INVALID() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePassword(1L, "old", "123"));
        assertEquals(UserErrorCode.PASSWORD_LENGTH_INVALID, ex.getErrorCode());
        verifyNoInteractions(userMapper);
    }

    @Test
    void changePassword_旧密码错误应抛OLD_PASSWORD_INCORRECT() {
        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(passwordEncoder.matches("wrong", "hashed:secret")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePassword(1L, "wrong", "newp@ss"));
        assertEquals(UserErrorCode.OLD_PASSWORD_INCORRECT, ex.getErrorCode());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void changePassword_正常修改应加密新密码() {
        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(passwordEncoder.matches("old", "hashed:secret")).thenReturn(true);
        when(passwordEncoder.encode("newp@ss")).thenReturn("hashed:newp@ss");

        boolean result = userService.changePassword(1L, "old", "newp@ss");

        assertTrue(result);
        assertEquals("hashed:newp@ss", existingUser.getPassword());
        verify(userMapper).updateById(existingUser);
    }

    // ============ resetPasswordByCode 测试 ============

    @Test
    void resetPasswordByCode_新密码过短应抛PASSWORD_LENGTH_INVALID() {
        // 修复：服务层密码长度兜底（与 changePassword 一致）
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.resetPasswordByCode("a@b.com", "123456", "123"));
        assertEquals(UserErrorCode.PASSWORD_LENGTH_INVALID, ex.getErrorCode());
        verifyNoInteractions(userMapper);
    }

    @Test
    void resetPasswordByCode_验证码错误应返回false() {
        // cacheService 未命中 → 内存验证码为空 → code 校验失败
        when(cacheService.get(eq("verify_code:email:a@b.com"), eq(String.class))).thenReturn(null);

        boolean result = userService.resetPasswordByCode("a@b.com", "wrong", "newp@ss");

        assertFalse(result);
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void resetPasswordByCode_验证码正确应加密更新密码() {
        when(cacheService.get(eq("verify_code:email:a@b.com"), eq(String.class))).thenReturn("123456");
        when(userMapper.selectOne(any())).thenReturn(existingUser);
        when(passwordEncoder.encode("newp@ss")).thenReturn("hashed:newp@ss");

        boolean result = userService.resetPasswordByCode("a@b.com", "123456", "newp@ss");

        assertTrue(result);
        assertEquals("hashed:newp@ss", existingUser.getPassword());
        verify(userMapper).updateById(existingUser);
    }

    // ============ 邮箱/短信验证码 测试 ============

    @Test
    void sendVerificationEmail_用户不存在返回false() {
        when(userMapper.selectOne(any())).thenReturn(null);
        assertFalse(userService.sendVerificationEmail("ghost@b.com"));
    }

    @Test
    void sendVerificationEmail_用户存在返回true() {
        when(userMapper.selectOne(any())).thenReturn(existingUser);
        assertTrue(userService.sendVerificationEmail("alice@example.com"));
    }

    @Test
    void verifyEmailCode_Redis命中正确码返回true并失效() {
        when(cacheService.get(eq("verify_code:email:a@b.com"), eq(String.class))).thenReturn("123456");

        assertTrue(userService.verifyEmailCode("a@b.com", "123456"));
        verify(cacheService).delete("verify_code:email:a@b.com");
    }

    @Test
    void verifyEmailCode_Redis命中错误码返回false() {
        when(cacheService.get(eq("verify_code:email:a@b.com"), eq(String.class))).thenReturn("123456");

        assertFalse(userService.verifyEmailCode("a@b.com", "000000"));
        verify(cacheService, never()).delete(anyString());
    }
}

package com.example.aichat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CacheService 缓存层缺口补测。
 *
 * <p>覆盖 CacheService 全部公开方法的三分支：
 *   1. redisTemplate 为 null（未启用 Redis）时全部静默降级；
 *   2. 正常读写路径；
 *   3. Redis 抛异常时静默降级返回 null/false/-1，不影响业务。</p>
 *
 * <p>原项目无任何 CacheService 测试类，本类为新增缺口补测。</p>
 */
class CacheServiceGapTest {

    private CacheService service;
    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOps;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        service = new CacheService();
        redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        injectRedisTemplate(service, redisTemplate);
    }

    private static void injectRedisTemplate(CacheService target, RedisTemplate<String, Object> tpl) throws Exception {
        java.lang.reflect.Field f = CacheService.class.getDeclaredField("redisTemplate");
        f.setAccessible(true);
        f.set(target, tpl);
    }

    private CacheService serviceWithoutRedis() throws Exception {
        CacheService s = new CacheService();
        // 保持 redisTemplate 为 null（@Autowired(required=false) 默认状态）
        return s;
    }

    // ===== set =====

    @Test
    void set_withDuration_callsRedisWithTtl() {
        service.set("k1", "v1", Duration.ofSeconds(30));
        verify(valueOps).set("k1", "v1", 30_000L, TimeUnit.MILLISECONDS);
    }

    @Test
    void set_withSeconds_delegatesToDuration() {
        service.set("k2", 123, 60L);
        verify(valueOps).set("k2", 123, 60_000L, TimeUnit.MILLISECONDS);
    }

    @Test
    void set_redisThrows_doesNotPropagate() {
        doThrow(new RuntimeException("connection lost")).when(valueOps)
                .set(anyString(), any(), anyLong(), any(TimeUnit.class));
        assertDoesNotThrow(() -> service.set("k", "v", Duration.ofSeconds(1)));
    }

    @Test
    void set_redisTemplateNull_doesNothing() throws Exception {
        CacheService s = serviceWithoutRedis();
        assertDoesNotThrow(() -> s.set("k", "v", Duration.ofSeconds(1)));
        // 不应 NPE，也不应调用任何 redis 方法
        verifyNoInteractions(redisTemplate);
    }

    // ===== get =====

    @Test
    void get_returnsStoredValue() {
        when(valueOps.get("k")).thenReturn("hello");
        assertEquals("hello", service.get("k"));
    }

    @Test
    void get_returnsNullWhenAbsent() {
        when(valueOps.get("missing")).thenReturn(null);
        assertNull(service.get("missing"));
    }

    @Test
    void get_redisThrows_returnsNull() {
        when(valueOps.get("k")).thenThrow(new RuntimeException("down"));
        assertNull(service.get("k"));
    }

    @Test
    void get_redisTemplateNull_returnsNull() throws Exception {
        assertNull(serviceWithoutRedis().get("k"));
    }

    @Test
    void get_typed_returnsCastedValue() {
        when(valueOps.get("num")).thenReturn(42);
        assertEquals(42, service.get("num", Integer.class));
    }

    @Test
    void get_typed_nullValue_returnsNull() {
        when(valueOps.get("missing")).thenReturn(null);
        assertNull(service.get("missing", String.class));
    }

    @Test
    void get_typed_type参数不参与实际转换() {
        // 主代码观察（记录，不修改）：因泛型擦除，`return (T) value;` 在字节码层是空操作，
        // 不会抛 ClassCastException；`type` 参数与 catch (ClassCastException) 实际是死代码。
        // 这里按真实行为断言：传入 Integer，即使请求 String.class 也原样返回。
        when(valueOps.get("k")).thenReturn(12345);
        Object result = service.get("k", String.class);
        assertEquals(12345, result, "当前实现不做类型校验，原样返回缓存对象");
    }

    @Test
    void get_typed_nullValueStill_returnsNull() {
        when(valueOps.get("missing")).thenReturn(null);
        assertNull(service.get("missing", Long.class));
    }

    // ===== delete =====

    @Test
    void delete_whenKeyExists_returnsTrue() {
        when(redisTemplate.delete("k")).thenReturn(Boolean.TRUE);
        assertTrue(service.delete("k"));
    }

    @Test
    void delete_whenKeyAbsent_returnsFalse() {
        when(redisTemplate.delete("k")).thenReturn(Boolean.FALSE);
        assertFalse(service.delete("k"));
    }

    @Test
    void delete_whenResultNull_returnsFalse() {
        when(redisTemplate.delete("k")).thenReturn(null);
        assertFalse(service.delete("k"));
    }

    @Test
    void delete_redisThrows_returnsFalse() {
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("down"));
        assertFalse(service.delete("k"));
    }

    @Test
    void delete_redisTemplateNull_returnsFalse() throws Exception {
        assertFalse(serviceWithoutRedis().delete("k"));
    }

    // ===== exists =====

    @Test
    void exists_whenPresent_returnsTrue() {
        when(redisTemplate.hasKey("k")).thenReturn(Boolean.TRUE);
        assertTrue(service.exists("k"));
    }

    @Test
    void exists_whenAbsent_returnsFalse() {
        when(redisTemplate.hasKey("k")).thenReturn(Boolean.FALSE);
        assertFalse(service.exists("k"));
    }

    @Test
    void exists_whenResultNull_returnsFalse() {
        when(redisTemplate.hasKey("k")).thenReturn(null);
        assertFalse(service.exists("k"));
    }

    @Test
    void exists_redisThrows_returnsFalse() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("down"));
        assertFalse(service.exists("k"));
    }

    @Test
    void exists_redisTemplateNull_returnsFalse() throws Exception {
        assertFalse(serviceWithoutRedis().exists("k"));
    }

    // ===== increment =====

    @Test
    void increment_returnsNewCount() {
        when(valueOps.increment("counter", 5L)).thenReturn(15L);
        assertEquals(15L, service.increment("counter", 5L));
    }

    @Test
    void increment_negativeDelta_supported() {
        when(valueOps.increment("counter", -3L)).thenReturn(2L);
        assertEquals(2L, service.increment("counter", -3L));
    }

    @Test
    void increment_redisThrows_returnsMinusOne() {
        when(valueOps.increment(anyString(), anyLong())).thenThrow(new RuntimeException("down"));
        assertEquals(-1L, service.increment("counter", 1L));
    }

    @Test
    void increment_redisTemplateNull_returnsMinusOne() throws Exception {
        assertEquals(-1L, serviceWithoutRedis().increment("counter", 1L));
    }

    // ===== expire =====

    @Test
    void expire_setsTtl_returnsTrue() {
        when(redisTemplate.expire(eq("k"), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(Boolean.TRUE);
        assertTrue(service.expire("k", Duration.ofMinutes(5)));
    }

    @Test
    void expire_whenKeyAbsent_returnsFalse() {
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(Boolean.FALSE);
        assertFalse(service.expire("k", Duration.ofSeconds(10)));
    }

    @Test
    void expire_redisThrows_returnsFalse() {
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class)))
                .thenThrow(new RuntimeException("down"));
        assertFalse(service.expire("k", Duration.ofSeconds(10)));
    }

    @Test
    void expire_redisTemplateNull_returnsFalse() throws Exception {
        assertFalse(serviceWithoutRedis().expire("k", Duration.ofSeconds(10)));
    }
}

package com.example.aichat.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MybatisPlusConfig 单元测试：
 * 验证分页插件已注册（修复"假分页"：total=0 + 全量返回）且单页上限配置生效。
 */
class MybatisPlusConfigTest {

    private final MybatisPlusConfig config = new MybatisPlusConfig();

    @Test
    void 分页插件已注册() {
        MybatisPlusInterceptor interceptor = config.mybatisPlusInterceptor();
        assertNotNull(interceptor);
        assertFalse(interceptor.getInterceptors().isEmpty());
        assertTrue(interceptor.getInterceptors().get(0) instanceof PaginationInnerInterceptor);
    }

    @Test
    void 单页上限为500() {
        MybatisPlusInterceptor interceptor = config.mybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = (PaginationInnerInterceptor) interceptor.getInterceptors().get(0);
        assertEquals(500L, pagination.getMaxLimit());
    }
}

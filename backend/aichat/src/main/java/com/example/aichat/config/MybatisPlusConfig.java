package com.example.aichat.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置。
 *
 * 健壮性修复（真实环境 E2E 发现）：
 * 此前项目未注册分页插件，所有 selectPage 退化为"假分页"——
 * 返回全部记录且 total 恒为 0（前端分页器显示"共 0 条"，大数据量时全量查询存在性能隐患）。
 * 注册 PaginationInnerInterceptor 后，分页查询自动生成 COUNT 与 LIMIT 语句。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 安全上限：单页最多 500 条，防止异常参数导致全量拉取
        pagination.setMaxLimit(500L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}

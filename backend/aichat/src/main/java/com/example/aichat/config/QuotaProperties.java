package com.example.aichat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 配额（额度管理）开关与阈值配置。
 *
 * <p>生产环境默认开启逐次配额扣减；测试 / 集成环境通过 {@code aichat.quota.enabled=false}
 * 跳过所有额度检查，避免用例被每日限额阻断。</p>
 */
@Component
@ConfigurationProperties(prefix = "aichat.quota")
public class QuotaProperties {

    /** 是否启用配额扣减（false 时 checkAndConsume 直接放行）。 */
    private boolean enabled = true;

    /**
     * 测试环境可选：把所有功能每日上限放大到该值（0 表示沿用 MembershipLevel 内置上限）。
     * 仅当 enabled=true 时生效；用于"超大额度"而非完全跳过。
     */
    private long oversizedLimit = 0L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getOversizedLimit() {
        return oversizedLimit;
    }

    public void setOversizedLimit(long oversizedLimit) {
        this.oversizedLimit = oversizedLimit;
    }
}

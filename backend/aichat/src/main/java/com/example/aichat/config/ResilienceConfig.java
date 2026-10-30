package com.example.aichat.config;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * Resilience4j 限流/重试配置 - Phase 3 (P1-3) 引入。
 *
 * 设计原则：
 *   1. 公开 API（register/login/send-verification-code）：限流 10次/分钟/IP，防爆破
 *   2. AI 接口（chat/stream）：限流 30次/分钟/用户，防滥用
 *   3. 图像/语音生成：限流 5次/分钟/用户，资源消耗大
 *   4. 管理员写操作：限流 60次/分钟/用户
 *   5. 外部 API 调用（OpenAI/Ollama/SD）：配置 Retry，3 次重试 + 指数退避
 *
 * 使用方式（在 Controller/Service 方法上叠加注解）：
 *   @RateLimiter(name = "authApi", fallbackMethod = "registerFallback")
 *   public R&lt;User&gt; register(@Valid @RequestBody RegisterRequest req) { ... }
 *
 *   fallback 方法签名须与原方法一致（多一个 Exception 参数）。
 */
@Configuration
public class ResilienceConfig {

    /**
     * 限流器注册表：预创建 4 个共享配置，按 API 类型分级限流。
     */
    @Bean
    @Primary
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig authConfig = RateLimiterConfig.custom()
                .limitForPeriod(10)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build();

        RateLimiterConfig aiConfig = RateLimiterConfig.custom()
                .limitForPeriod(30)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(10))
                .build();

        RateLimiterConfig mediaConfig = RateLimiterConfig.custom()
                .limitForPeriod(5)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build();

        RateLimiterConfig adminConfig = RateLimiterConfig.custom()
                .limitForPeriod(60)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(authConfig);
        // 注册命名配置模板，@RateLimiter(name="authApi", configName="authApi") 时引用
        registry.addConfiguration("authApi", authConfig);
        registry.addConfiguration("aiApi", aiConfig);
        registry.addConfiguration("mediaApi", mediaConfig);
        registry.addConfiguration("adminApi", adminConfig);

        // 预创建限流器实例，避免首次请求触发延迟初始化
        registry.rateLimiter("authApi");
        registry.rateLimiter("aiApi");
        registry.rateLimiter("mediaApi");
        registry.rateLimiter("adminApi");
        return registry;
    }

    /**
     * 外部 API 重试：3 次重试，指数退避 1s/2s/4s，针对 RuntimeException 触发。
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(
                        Duration.ofSeconds(1), 2.0))
                .retryOnException(e -> e instanceof RuntimeException)
                .build();
        RetryRegistry registry = RetryRegistry.of(config);
        registry.retry("externalApi");
        return registry;
    }
}

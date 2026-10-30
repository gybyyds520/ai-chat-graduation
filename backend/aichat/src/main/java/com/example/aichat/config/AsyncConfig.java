package com.example.aichat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务与定时任务配置 - Phase 3 (P2-2) 引入。
 *
 * 设计目标：
 *   1. @Async 标注的方法（如审计日志异步写入 access_log、AI 生成图片异步落库）
 *      走专属线程池，不占用 HTTP 请求线程，避免接口超时；
 *   2. @Scheduled 定时任务（如清理过期 token、统计日活）通过 @EnableScheduling 启用；
 *   3. Quartz 已在 pom.xml 引入，用于需要持久化/集群调度的复杂任务（如定期备份）。
 *
 * 线程池参数：
 *   core=8, max=32, queue=200，拒绝策略 CallerRuns（兜底降级到同步执行）。
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * 业务异步任务线程池。
     * 注解 @Async 默认查找名为 "taskExecutor" 的 Bean，亦可指定 @Async("bizTaskExecutor")。
     */
    @Bean("bizTaskExecutor")
    public Executor bizTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("biz-async-");
        // 拒绝策略：队列满时由调用线程同步执行，降级保护
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务完成再关闭，避免审计日志丢失
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * 默认 taskExecutor - 兼容 @Async 无指定 name 的场景。
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        return bizTaskExecutor();
    }
}

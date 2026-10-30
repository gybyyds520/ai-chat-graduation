package com.example.aichat.common.audit;

import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 审计切面 - 拦截 @Audited 标注的方法，自动记录结构化访问日志。
 *
 * Phase 2 扩展架构目的：
 *   统一替代散落在 Controller/Service 中的 logger.info("[XXX_SUCCESS]...") 手写代码，
 *   通过 AOP 在方法执行前后无侵入收集：
 *   - 方法签名（用于定位代码）
 *   - 执行耗时（毫秒）
 *   - 调用结果（R.code 或抛出异常）
 *   - 参数摘要（toString 截断，防止日志爆炸）
 *
 * Phase 3 增强：
 *   - 通过 AuthContext 从 SecurityContext 自动取当前用户 ID，无需业务传参；
 *   - 通过 RequestContextHolder 取当前请求 IP 与 User-Agent，统一审计字段来源；
 *   - 失败路径也记录审计日志（status=FAILED + exception），便于异常追溯。
 *
 *   日志降级策略：
 *   - AccessLog 表插入失败时仅 warn 输出，不影响业务；
 *   - RequestContextHolder 在异步/定时任务中无上下文，会优雅降级。
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger logger = LoggerFactory.getLogger("AUDIT");
    private static final int ARGS_SUMMARY_MAX_LENGTH = 200;

    @Autowired(required = false)
    private com.example.aichat.service.AccessLogService accessLogService;

    /**
     * 环绕通知：包裹 @Audited 标注方法，前后记日志。
     * 使用 @Around 而非 @AfterReturning 的原因：需要捕获异常路径（login 失败抛 BusinessException），
     * 同时记录耗时。
     */
    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        long startNanos = System.nanoTime();
        String method = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        String argsSummary = formatArgs(joinPoint, audited);

        // Phase 3：在方法执行前从 SecurityContext 取当前用户（执行后可能已被清理）
        Optional<Long> currentUserId = AuthContext.getCurrentUserId();
        Optional<String> currentUsername = AuthContext.getCurrentUsername();
        Optional<HttpServletRequest> requestOpt = getCurrentRequest();

        Throwable thrown = null;
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            thrown = t;
            throw t;  // 重新抛出，不吞业务异常
        } finally {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            int responseCode = extractResponseCode(result, thrown);
            String status = (thrown == null) ? "SUCCESS" : "FAILED";

            // 结构化日志输出，便于 ELK/Loki 等日志系统解析
            logger.info("[AUDIT] action={} resource={} method={} status={} code={} elapsedMs={}ms userId={} username={} ip={} args={}",
                    audited.action(),
                    audited.resourceType(),
                    method,
                    status,
                    responseCode,
                    elapsedMs,
                    currentUserId.orElse(null),
                    currentUsername.orElse(null),
                    requestOpt.map(req -> req.getRemoteAddr()).orElse(null),
                    argsSummary
            );

            // 失败原因记录（异常类名 + message，不输出完整堆栈以控制日志量）
            if (thrown != null) {
                logger.warn("[AUDIT_ERROR] action={} method={} userId={} exception={}: {}",
                        audited.action(),
                        method,
                        currentUserId.orElse(null),
                        thrown.getClass().getSimpleName(),
                        thrown.getMessage()
                );
            }

            // 可选：异步写入 access_log 表（依赖 AccessLogService bean 存在）
            // 此处仅在成功路径记录，避免重复日志；异常路径已由 GlobalExceptionHandler 统一处理
            if (accessLogService != null && thrown == null) {
                try {
                    accessLogService.logAccessAsync(
                            null,                                  // aiSystemId 暂无上下文
                            null,                                  // dataId 暂无上下文
                            audited.action(),
                            requestOpt.map(HttpServletRequest::getMethod).orElse("POST"),
                            method,
                            argsSummary,
                            responseCode,
                            (int) elapsedMs,
                            requestOpt.map(HttpServletRequest::getRemoteAddr).orElse(null),
                            requestOpt.map(req -> req.getHeader("User-Agent")).orElse(null),
                            null                                   // errorMessage
                    );
                } catch (Exception ex) {
                    // 审计日志失败不应影响业务
                    logger.warn("[AUDIT] 写入 access_log 表失败，降级为 logger 输出: {}", ex.getMessage());
                }
            }
        }
    }

    /**
     * 获取当前 HttpServletRequest（仅在 HTTP 请求线程内有效，异步任务返回 empty）。
     */
    private Optional<HttpServletRequest> getCurrentRequest() {
        try {
            Object attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                return Optional.of(sra.getRequest());
            }
        } catch (IllegalStateException ignored) {
            // 非 Web 线程（如 @Async 任务），降级返回 empty
        }
        return Optional.empty();
    }

    /**
     * 格式化方法参数摘要，截断过长内容，避免日志爆炸。
     */
    private String formatArgs(ProceedingJoinPoint joinPoint, Audited audited) {
        if (!audited.logArgs()) {
            return "[args hidden]";
        }
        if (joinPoint.getArgs() == null || joinPoint.getArgs().length == 0) {
            return "[]";
        }
        String raw = Arrays.stream(joinPoint.getArgs())
                .map(arg -> arg == null ? "null" : arg.toString())
                .collect(Collectors.joining(", ", "[", "]"));
        if (raw.length() > ARGS_SUMMARY_MAX_LENGTH) {
            return raw.substring(0, ARGS_SUMMARY_MAX_LENGTH) + "...(truncated)";
        }
        return raw;
    }

    /**
     * 从方法返回值（R 对象）或异常中提取响应码。
     */
    private int extractResponseCode(Object result, Throwable thrown) {
        if (thrown != null) {
            return 500;  // 默认异常视为 500，具体码由 GlobalExceptionHandler 处理
        }
        if (result instanceof R<?> r) {
            return r.getCode();
        }
        return 200;  // 非 R 类型返回视为成功
    }
}

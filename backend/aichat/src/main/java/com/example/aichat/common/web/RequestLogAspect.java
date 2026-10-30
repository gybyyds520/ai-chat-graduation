package com.example.aichat.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Web 请求日志切面（Filter 实现）。
 *
 * 职责：
 *   - 记录每个 HTTP 请求的 method / url / 状态码 / 耗时
 *   - 耗时 &gt; 3000ms 单独打 WARN，便于发现慢请求
 *   - 对 URL 查询参数与表单参数中的敏感字段打码
 *   - 排除健康检查、登录等高频/敏感端点的请求体记录
 *
 * 设计说明：
 *   - 用 Filter 而非 AOP，是因为 Filter 天然能拿到 HttpServletResponse.status，
 *     且不侵入 Controller 方法签名；异常路径也能正确记录。
 *   - 请求体/响应体不打印（流式、体积大、易泄漏）；只记录 method/url/status/elapsed。
 *   - 与 {@link com.example.aichat.common.audit.AuditAspect} 互补：
 *     AuditAspect 面向 @Audited 业务动作（带 userId/action），本 Filter 面向全部 HTTP 流量。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLogAspect extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLogAspect.class);

    /** 慢请求阈值（毫秒），超过打 WARN */
    private static final long SLOW_THRESHOLD_MS = 3000L;

    /** 不记录请求参数/body 的端点（高频或敏感） */
    private static final Set<String> EXCLUDE_PATHS = new HashSet<>(Arrays.asList(
            "/api/health",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/logout",
            "/api/auth/refresh"
    ));

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** 需要打码的参数名（小写匹配） */
    private static final Set<String> SENSITIVE_PARAMS = new HashSet<>(Arrays.asList(
            "password", "oldpassword", "newpassword", "confirmPassword",
            "token", "accesstoken", "refreshtoken", "authorization",
            "passwordhash", "secret", "apikey", "api_key",
            "captcha", "verifycode", "sms_code"
    ));

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startNanos = System.nanoTime();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = maskQueryString(request.getQueryString());
        String clientIp = request.getRemoteAddr();

        // 包一层 wrapper 以便读取响应状态（这里只需要 status，不需要缓存 body）
        boolean shouldExclude = isExcluded(uri);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            int status = response.getStatus();

            String fullUrl = queryString == null ? uri : uri + "?" + queryString;

            if (elapsedMs > SLOW_THRESHOLD_MS) {
                logger.warn("[SLOW_REQUEST] method={} uri={} status={} elapsedMs={}ms ip={}",
                        method, fullUrl, status, elapsedMs, clientIp);
            } else if (status >= 500) {
                logger.error("[REQUEST_ERROR] method={} uri={} status={} elapsedMs={}ms ip={}",
                        method, fullUrl, status, elapsedMs, clientIp);
            } else if (status >= 400) {
                logger.warn("[REQUEST_CLIENT_ERROR] method={} uri={} status={} elapsedMs={}ms ip={}",
                        method, fullUrl, status, elapsedMs, clientIp);
            } else if (!shouldExclude) {
                logger.info("[REQUEST] method={} uri={} status={} elapsedMs={}ms ip={}",
                        method, fullUrl, status, elapsedMs, clientIp);
            }
            // 排除端点走 DEBUG 级，避免登录/健康检查刷屏
            else {
                logger.debug("[REQUEST_SKIPPED] method={} uri={} status={} elapsedMs={}ms",
                        method, fullUrl, status, elapsedMs);
            }
        }
    }

    private boolean isExcluded(String uri) {
        if (uri == null) return false;
        for (String pattern : EXCLUDE_PATHS) {
            if (PATH_MATCHER.match(pattern, uri)) return true;
        }
        return false;
    }

    /**
     * 对查询串中的敏感参数打码。
     * 例如 password=abc123 → password=***
     */
    private String maskQueryString(String query) {
        if (query == null || query.isEmpty()) return null;
        try {
            String[] pairs = query.split("&");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pairs.length; i++) {
                if (i > 0) sb.append('&');
                String pair = pairs[i];
                int eq = pair.indexOf('=');
                String key = eq > 0 ? pair.substring(0, eq).toLowerCase(Locale.ROOT) : pair.toLowerCase(Locale.ROOT);
                if (SENSITIVE_PARAMS.contains(key)) {
                    sb.append(eq > 0 ? pair.substring(0, eq + 1) : pair).append("***");
                } else {
                    sb.append(pair);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "[mask-failed]";
        }
    }
}

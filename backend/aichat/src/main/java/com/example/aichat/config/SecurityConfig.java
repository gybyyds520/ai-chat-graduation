package com.example.aichat.config;

import com.example.aichat.common.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security 配置 - Phase 2 引入，Phase 4 (P4-g) 完成批次切换。
 *
 * 演进历史：
 *   - Phase 2：默认 anyRequest().permitAll()，仅注册 JWT 过滤器，业务自行判断权限；
 *   - Phase 4 (P4-g)：将默认策略改为 anyRequest().authenticated()，关闭未鉴权写操作漏洞。
 *
 * 当前策略（前后端协同批次切换完成）：
 *   1. 公开白名单（无需登录即可访问）：
 *      - 认证相关端点：/api/user/register|login|forgot-password|reset-password*|send-verification-code
 *      - OAuth 端点：/api/auth/**
 *      - 健康检查、监控、API 文档：/api/health、/actuator/**、/docs/**、/v3/api-docs/**、/swagger-ui/**
 *      - 静态资源与上传：/、/index.html、/static/**、/uploads/**
 *      - WebSocket 与 SSE 流式端点：/ws/**、/api/chat/stream/**（鉴权由业务自行处理）
 *      - 匿名浏览类 GET 接口：角色列表/社区帖子/Feed/推荐/搜索/公告/广告/资料查看
 *   2. 管理员专属端点（hasRole('ADMIN')）：
 *      - /api/admin/** 全部管理员接口
 *      - 支付审核、用户管理等敏感写操作由 @PreAuthorize 在方法层控制
 *   3. 其他所有请求（默认 anyRequest().authenticated()）：
 *      - 所有 POST/PUT/DELETE/PATCH 写操作必须携带有效 JWT token；
 *      - 未携带 token 或 token 无效的请求返回 401 Unauthorized；
 *      - 业务层通过 AuthContext.getCurrentUserId() 获取当前用户 ID，不再依赖手写 token 解析。
 *
 * 安全收益：
 *   - 关闭"未登录用户可调用任意写接口"的横向越权漏洞；
 *   - 配合 Phase 1 AuditAspect，所有写操作自动记录操作者身份；
 *   - 前端只需在受影响接口的请求头携带 Authorization: Bearer <token>，无侵入。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // 开启 @PreAuthorize/@PostAuthorize 声明式权限
public class SecurityConfig {

    /**
     * JWT 过滤器作为 Bean，方便注入 SecurityFilterChain。
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 关闭 CSRF：JWT 无状态认证不需要
            .csrf(csrf -> csrf.disable())
            // 无状态会话
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 启用 CORS（复用现有 CorsConfig 的 Bean）
            .cors(cors -> {})
            // 授权规则：批次切换至 authenticated 默认策略
            .authorizeHttpRequests(auth -> auth
                // === 1. 完全公开路径（无登录可访问） ===
                // 认证相关端点（已按 AuthController 实际公开面收紧，仅短信登录相关）
                .requestMatchers(
                    new AntPathRequestMatcher("/api/user/register"),
                    new AntPathRequestMatcher("/api/user/login"),
                    new AntPathRequestMatcher("/api/user/forgot-password"),
                    new AntPathRequestMatcher("/api/user/reset-password"),
                    new AntPathRequestMatcher("/api/user/reset-password-by-code"),
                    new AntPathRequestMatcher("/api/user/send-verification-code"),
                    new AntPathRequestMatcher("/api/auth/sms/send"),
                    new AntPathRequestMatcher("/api/auth/sms/login"),
                    // AI 系统 API Key 认证端点（通过 X-API-Key/X-Secret-Key 头认证，不依赖 JWT）
                    new AntPathRequestMatcher("/api/aisystem/authenticate")
                ).permitAll()
                // 健康检查、API 文档（actuator 仅暴露 health，其余端点要求认证）
                // 阶段二 P1：springdoc 文档统一收敛到 /docs/**（/docs/api-docs JSON、
                // /docs/swagger-ui.html UI），Nginx 反代可按 /docs/** 整段放行；
                // 同时保留 /swagger-ui/**（webjars 静态资源）与 /v3/api-docs/**（默认兜底）。
                .requestMatchers(
                    new AntPathRequestMatcher("/api/health"),
                    new AntPathRequestMatcher("/actuator/health"),
                    new AntPathRequestMatcher("/v3/api-docs/**"),
                    new AntPathRequestMatcher("/docs/**"),
                    new AntPathRequestMatcher("/swagger-ui/**")
                ).permitAll()
                // 监控端点：/actuator/health 已在上方 permitAll。阶段二 P1：
                // Prometheus 抓取 /actuator/prometheus 与 /actuator/metrics 需要无鉴权访问，
                // 故对 /actuator/** 放行。注意 management.endpoints.web.exposure.include 仅开放
                // health,metrics,prometheus 三个端点，其余敏感端点（env/beans 等）不会暴露；
                // 生产环境建议在 Nginx/网络层再限制为仅内网监控网段可达。
                .requestMatchers(new AntPathRequestMatcher("/actuator/**")).permitAll()
                // 静态资源与上传文件
                .requestMatchers(
                    new AntPathRequestMatcher("/"),
                    new AntPathRequestMatcher("/index.html"),
                    new AntPathRequestMatcher("/static/**"),
                    new AntPathRequestMatcher("/uploads/**")
                ).permitAll()
                // WebSocket 与 SSE 端点（鉴权由业务自行处理）
                .requestMatchers(
                    new AntPathRequestMatcher("/ws/**"),
                    new AntPathRequestMatcher("/api/chat/stream/**")
                ).permitAll()

                // === 2. 匿名浏览类 GET 接口（无登录可浏览，提升首次访问体验） ===
                // 角色浏览：列表、详情、按用户列表
                .requestMatchers(HttpMethod.GET,
                    "/api/character/list",
                    "/api/character/list/**",
                    "/api/character/*"
                ).permitAll()
                // 角色广场 / 发现页（匿名可浏览；POST /use 走默认 authenticated 策略）
                .requestMatchers(HttpMethod.GET,
                    "/api/plaza/**"
                ).permitAll()
                // 社区与 Feed 浏览
                .requestMatchers(HttpMethod.GET,
                    "/api/community/**",
                    "/api/feed/**",
                    "/api/recommendation/**"
                ).permitAll()
                // 搜索与公告、广告
                .requestMatchers(HttpMethod.GET,
                    "/api/search/**",
                    "/api/announcement/**",
                    "/api/advertisement/**"
                ).permitAll()
                // 个人资料查看（公开主页）
                .requestMatchers(HttpMethod.GET, "/api/profile/**").permitAll()

                // === 3. 管理员专属端点（必须管理员角色） ===
                // 管理员登录端点必须放行，否则无法获取 ADMIN 角色 JWT
                .requestMatchers(new AntPathRequestMatcher("/api/admin/login")).permitAll()
                // 公告公开读取（管理端写操作仍要求 ADMIN）
                .requestMatchers(HttpMethod.GET, "/api/admin/announcements/public/**").permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/admin/**")).hasRole("ADMIN")

                // === 4. 其他所有请求：必须认证（关闭未鉴权写操作漏洞） ===
                .anyRequest().authenticated()
            )
            // 异常处理：未认证请求返回 401 JSON（便于前端识别并跳转登录）
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"msg\":\"未登录或登录已过期，请重新登录\",\"data\":null}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":403,\"msg\":\"权限不足，无法访问该资源\",\"data\":null}");
                })
            )
            // 注册 JWT 过滤器，在 Spring Security 默认用户名密码过滤器之前
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

package com.example.aichat.common.security;

import com.example.aichat.service.AdminService;
import com.example.aichat.utils.TokenUtils;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.UserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器 - 解析请求头中的 token 并注入 SecurityContext。
 *
 * Phase 2 (P2-2) 引入：替代手写 token 校验 + Controller 内重复解析。
 *
 * 工作流程：
 *   1. 从 Authorization 头或 token 头提取 JWT；
 *   2. 调用 TokenUtils.validateToken 校验签名 + 过期；
 *   3. 解析 userId/username，构造 UserPrincipal；
 *   4. 注入 UsernamePasswordAuthenticationToken 到 SecurityContext；
 *   5. 业务代码可通过 AuthContext.getCurrentUserId() 获取当前用户。
 *
 * 兼容性设计：
 *   - token 无效或缺失时，不抛异常、不阻断请求（仍走 FilterChain），
 *     SecurityContext 保持空，由 SecurityConfig 的 permitAll/allAuthenticated 决定是否放行；
 *   - 同时支持 Authorization: Bearer xxx 和直接 token: xxx 两种头格式，
 *     兼容现有前端发送方式。
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired(required = false)
    private AdminService adminService;

    /**
     * M3 ⑨：用于校验账号是否已被注销。
     * required=false：单元测试中未注入时跳过注销校验，保持向后兼容；
     * 生产环境由 Spring 自动注入。
     */
    @Autowired(required = false)
    private UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (token != null) {
                authenticate(token, request);
            }
        } catch (Exception e) {
            // 解析失败不阻断请求，让 SecurityConfig 的后续规则决定
            logger.warn("[JWT_PARSE_FAILED] path={}, error={}", request.getRequestURI(), e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求提取 token，支持：
     *   - Authorization: Bearer xxx
     *   - token: xxx 请求头
     *   - token 查询参数（仅 GET，供 SSE/EventSource 场景使用）
     */
    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }
        // 兼容历史前端的 token 头
        String tokenHeader = request.getHeader("token");
        if (tokenHeader != null && !tokenHeader.isBlank()) {
            return tokenHeader.trim();
        }
        // SSE（EventSource）无法设置自定义请求头，允许通过查询参数传递 token
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            String tokenParam = request.getParameter("token");
            if (tokenParam != null && !tokenParam.isBlank()) {
                return tokenParam.trim();
            }
        }
        return null;
    }

    /**
     * 校验 token 并注入 SecurityContext。
     *
     * Phase 3 增强：根据 userId 区分角色：
     *   - userId = 0：管理员 token（由 /api/admin/login 颁发），注入 ROLE_ADMIN
     *   - 其他 userId：普通用户 token，注入 ROLE_USER
     *
     * 这避免了修改 TokenUtils（保持向后兼容），同时让 @PreAuthorize("hasRole('ADMIN')")
     * 在 AdminController 上生效。未来 P4 阶段可改造为 token payload 显式携带 role 字段。
     */
    private void authenticate(String token, HttpServletRequest request) {
        if (!TokenUtils.validateToken(token)) {
            logger.debug("[JWT_INVALID] path={}", request.getRequestURI());
            return;
        }
        String userIdStr = TokenUtils.getUserIdFromToken(token);
        if (userIdStr == null) {
            return;
        }
        Long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            logger.warn("[JWT_USERID_INVALID] userId={}", userIdStr);
            return;
        }
        // M3 ⑨：账号已注销（软删除）的用户，其持有的 JWT 立即失效——
        // 不注入 Authentication，后续受保护接口按未认证（401）处理。
        // userMapper 为 null（如纯单元测试）时跳过该校验，保持向后兼容。
        if (userMapper != null) {
            User dbUser = userMapper.selectById(userId);
            if (dbUser == null || "deleted".equals(dbUser.getAccountStatus())) {
                logger.info("[JWT_REJECTED] account deleted or missing, userId={}", userId);
                return;
            }
        }
        // 根据数据库 user 表判断是否为管理员（username='admin'）
        // 管理员token由 /api/admin/login 颁发，使用数据库中 admin 记录的真实 id
        // adminService 为 null 时安全降级：一律视为普通用户，避免 NPE
        boolean isAdmin = adminService != null && adminService.isAdmin(userId);
        String role = isAdmin ? "ROLE_ADMIN" : "ROLE_USER";
        String username = isAdmin ? "admin" : "user-" + userId;
        UserPrincipal principal = new UserPrincipal(userId, username, Collections.singleton(role));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
        logger.debug("[JWT_AUTH_SUCCESS] userId={} role={} path={}", userId, role, request.getRequestURI());
    }
}

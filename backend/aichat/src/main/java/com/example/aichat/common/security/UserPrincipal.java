package com.example.aichat.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 系统认证主体 - 包装当前登录用户信息。
 *
 * Phase 2 (P2-2) 引入：作为 Spring Security 的 UserDetails 实现，
 * 由 JwtAuthenticationFilter 从 token 解析后注入到 SecurityContext。
 *
 * 使用方式：
 *   // 在 Controller/Service 中获取当前用户
 *   UserPrincipal currentUser = (UserPrincipal) SecurityContextHolder.getContext()
 *           .getAuthentication().getPrincipal();
 *   Long userId = currentUser.getUserId();
 *   String username = currentUser.getUsername();
 *
 * 或通过 AuthContext 工具类（更简洁）：
 *   Long userId = AuthContext.getCurrentUserId();
 */
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long userId, String username, Collection<String> roles) {
        this.userId = userId;
        this.username = username;
        Set<GrantedAuthority> auths = new HashSet<>();
        if (roles != null) {
            for (String role : roles) {
                // Spring Security 约定角色前缀为 ROLE_
                String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                auths.add(new SimpleGrantedAuthority(roleWithPrefix));
            }
        }
        this.authorities = Collections.unmodifiableSet(auths);
    }

    /**
     * 简化构造：普通用户（无显式角色，仅基础权限）
     */
    public UserPrincipal(Long userId, String username) {
        this(userId, username, Collections.singleton("ROLE_USER"));
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        // JWT 无状态认证，不返回密码
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

package com.example.aichat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig {

    /**
     * 安全加固（P1）：收紧 CORS 源列表。
     * 原配置 allowCredentials(true) + allowedOriginPatterns("*") 属于高危组合，
     * 任意来源网站都可以携带凭证发起跨域请求。
     * 现改为显式白名单，可通过环境变量 CORS_ALLOWED_ORIGINS 覆盖（逗号分隔）。
     */
    @Bean
    @Primary
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:4173,http://127.0.0.1:5173,http://localhost:8080,http://localhost:5187,http://127.0.0.1:5187}") String allowedOrigins) {
        CorsConfiguration config = buildCorsConfiguration(allowedOrigins);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * CORS 过滤器 - 兼容历史代码，非 Security 链路也生效。
     */
    @Bean
    public CorsFilter corsFilter(CorsConfigurationSource corsConfigurationSource) {
        return new CorsFilter(corsConfigurationSource);
    }

    private CorsConfiguration buildCorsConfiguration(String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();

        // 显式来源白名单，禁止通配符
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        config.setAllowedOrigins(origins);

        // 允许的HTTP方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 允许的请求头（包含SSE所需的Accept头）
        config.addAllowedHeader("*");

        // 预检请求缓存时间
        config.setMaxAge(3600L);

        // 暴露的响应头（包含SSE所需的头）
        config.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Cache-Control",
                "Connection",
                "Transfer-Encoding"
        ));
        config.setAllowCredentials(true);
        return config;
    }
}

package com.example.aichat.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局健康检查端点（匿名可访问，用于 nginx/docker 存活探活）。
 *
 * 安全约定：不返回任何内部细节（版本、路径、异常消息），仅返回
 * status/db/timestamp 三个字段；数据库异常仅记录服务端日志。
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        boolean dbUp = false;
        try (Connection conn = dataSource.getConnection()) {
            dbUp = conn.isValid(2);
        } catch (Exception e) {
            log.warn("[HEALTH] database check failed: {}", e.getMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", dbUp ? "UP" : "DOWN");
        body.put("db", dbUp ? "UP" : "DOWN");
        body.put("timestamp", System.currentTimeMillis());

        return dbUp
                ? ResponseEntity.ok(body)
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}

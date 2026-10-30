package com.example.aichat.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.entity.DataPool;
import com.example.aichat.service.WebCrawlerService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 网页爬虫接口。
 *
 * 安全加固（P0）：爬虫会对外发起批量请求并消耗服务器资源，全部接口限制为管理员可用，
 * 并对触发爬取的接口叠加限流（adminApi：60次/分钟）。
 */
@RestController
@RequestMapping("/api/crawler")
@PreAuthorize("hasRole('ADMIN')")
public class WebCrawlerController {

    private static final Logger logger = LoggerFactory.getLogger(WebCrawlerController.class);

    private final WebCrawlerService webCrawlerService;

    public WebCrawlerController(WebCrawlerService webCrawlerService) {
        this.webCrawlerService = webCrawlerService;
    }

    /**
     * 开始收集AI资料
     */
    @PostMapping("/collect")
    @RateLimiter(name = "adminApi", fallbackMethod = "rateLimited")
    public ResponseEntity<Map<String, Object>> startCollection(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "5") int maxPages) {

        // 安全加固：限制单次爬取页数，防止资源滥用
        if (maxPages < 1 || maxPages > 20) {
            maxPages = Math.min(Math.max(maxPages, 1), 20);
        }

        logger.info("收到收集请求，关键词: {}, 最大页数: {}", keyword, maxPages);

        Map<String, Object> result = webCrawlerService.executeCollection(keyword, maxPages);

        return ResponseEntity.ok(result);
    }

    /**
     * 限流兜底。
     */
    public ResponseEntity<Map<String, Object>> rateLimited(String keyword, int maxPages, Throwable t) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "请求过于频繁，请稍后再试");
        return ResponseEntity.status(429).body(response);
    }

    /**
     * 获取收集统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = webCrawlerService.getCollectionStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * 搜索已收集的资料
     */
    @GetMapping("/search")
    public ResponseEntity<Page<DataPool>> searchData(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<DataPool> results = webCrawlerService.searchCollectedData(keyword, page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * 清除URL缓存
     */
    @PostMapping("/clear-cache")
    public ResponseEntity<Map<String, Object>> clearCache() {
        webCrawlerService.clearVisitedUrlsCache();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "缓存已清除");

        return ResponseEntity.ok(response);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "running");
        health.put("service", "web-crawler");
        return ResponseEntity.ok(health);
    }
}

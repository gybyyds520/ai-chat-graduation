package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.service.ExternalApiService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 外部 API 接口（代理/AI/抓取）。
 *
 * 安全加固（P0）：
 *   - 本控制器全部接口仅允许管理员访问：其中 /proxy、/fetch、/download 是任意 URL 代理能力，
 *     普通用户可借其访问内网（SSRF）或滥用服务器出口流量；/ai/** 会消耗服务器 AI API 密钥额度。
 *   - AI 调用叠加限流（aiApi：30次/分钟），防成本滥用。
 */
@RestController
@RequestMapping("/api/external")
@PreAuthorize("hasRole('ADMIN')")
public class ExternalApiController {

    private static final Logger logger = LoggerFactory.getLogger(ExternalApiController.class);

    @Autowired
    private ExternalApiService externalApiService;

    @GetMapping("/health")
    public R<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> result = externalApiService.healthCheck();
            return R.success(result);
        } catch (Exception e) {
            logger.error("健康检查失败", e);
            return R.error("健康检查失败");
        }
    }

    @GetMapping("/providers")
    public R<List<Map<String, Object>>> getProviders() {
        try {
            List<Map<String, Object>> providers = externalApiService.getAvailableProviders();
            return R.success(providers);
        } catch (Exception e) {
            logger.error("获取服务商列表失败", e);
            return R.error("获取服务商列表失败");
        }
    }

    @PostMapping("/proxy")
    public ResponseEntity<?> proxyRequest(@RequestBody Map<String, Object> requestBody) {
        try {
            String method = (String) requestBody.getOrDefault("method", "GET");
            String url = (String) requestBody.get("url");
            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) requestBody.get("headers");
            Object body = requestBody.get("body");
            Integer timeout = requestBody.get("timeout") != null ? 
                    (Integer) requestBody.get("timeout") : 30;

            if (url == null || url.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(R.error("URL不能为空"));
            }

            logger.info("执行代理请求: method={}, url={}", method, url);

            Map<String, Object> result = externalApiService.proxyRequest(method, url, headers, body, timeout);

            int statusCode = (Integer) result.getOrDefault("statusCode", 200);
            @SuppressWarnings("unchecked")
            Map<String, String> responseHeaders = (Map<String, String>) result.getOrDefault("headers", new HashMap<>());
            Object responseBody = result.get("body");

            HttpHeaders httpHeaders = new HttpHeaders();
            responseHeaders.forEach(httpHeaders::set);

            return ResponseEntity.status(statusCode).headers(httpHeaders).body(responseBody);

        } catch (Exception e) {
            logger.error("代理请求失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "代理请求失败");
            error.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/ai/chat")
    @RateLimiter(name = "aiApi", fallbackMethod = "rateLimited")
    public R<Map<String, Object>> aiChat(@RequestBody Map<String, Object> requestBody) {
        try {
            String provider = (String) requestBody.get("provider");
            String model = (String) requestBody.get("model");
            String prompt = (String) requestBody.get("prompt");
            String systemPrompt = (String) requestBody.get("systemPrompt");
            Boolean stream = requestBody.get("stream") != null ? 
                    (Boolean) requestBody.get("stream") : false;

            if (provider == null || provider.trim().isEmpty()) {
                return R.error("服务商不能为空");
            }
            if (prompt == null || prompt.trim().isEmpty()) {
                return R.error("提示词不能为空");
            }

            logger.info("执行AI对话: provider={}, model={}, promptLength={}", provider, model, prompt.length());

            Map<String, Object> result = externalApiService.aiChat(provider, model, prompt, systemPrompt, stream);
            return R.success(result);

        } catch (Exception e) {
            logger.error("AI对话失败", e);
            return R.error("AI对话失败");
        }
    }

    @PostMapping("/ai/completions")
    @RateLimiter(name = "aiApi", fallbackMethod = "rateLimited")
    public R<Map<String, Object>> aiCompletions(@RequestBody Map<String, Object> requestBody) {
        try {
            String provider = (String) requestBody.get("provider");
            String model = (String) requestBody.get("model");
            String prompt = (String) requestBody.get("prompt");
            Integer maxTokens = requestBody.get("maxTokens") != null ? 
                    (Integer) requestBody.get("maxTokens") : 1024;
            Double temperature = requestBody.get("temperature") != null ? 
                    (Double) requestBody.get("temperature") : 0.7;

            if (provider == null || provider.trim().isEmpty()) {
                return R.error("服务商不能为空");
            }
            if (prompt == null || prompt.trim().isEmpty()) {
                return R.error("提示词不能为空");
            }

            logger.info("执行AI补全: provider={}, model={}, promptLength={}", provider, model, prompt.length());

            Map<String, Object> result = externalApiService.aiCompletions(provider, model, prompt, maxTokens, temperature);
            return R.success(result);

        } catch (Exception e) {
            logger.error("AI补全失败", e);
            return R.error("AI补全失败");
        }
    }

    @GetMapping("/ai/models/{provider}")
    public R<List<Map<String, Object>>> getModels(@PathVariable String provider) {
        try {
            logger.info("获取模型列表: provider={}", provider);
            
            List<Map<String, Object>> models = externalApiService.getProviderModels(provider);
            return R.success(models);

        } catch (Exception e) {
            logger.error("获取模型列表失败", e);
            return R.error("获取模型列表失败");
        }
    }

    @PostMapping("/ai/embeddings")
    public R<Map<String, Object>> aiEmbeddings(@RequestBody Map<String, Object> requestBody) {
        try {
            String provider = (String) requestBody.get("provider");
            String model = (String) requestBody.get("model");
            @SuppressWarnings("unchecked")
            List<String> texts = (List<String>) requestBody.get("texts");

            if (provider == null || provider.trim().isEmpty()) {
                return R.error("服务商不能为空");
            }
            if (texts == null || texts.isEmpty()) {
                return R.error("文本列表不能为空");
            }

            logger.info("执行Embeddings: provider={}, model={}, textCount={}", provider, model, texts.size());

            Map<String, Object> result = externalApiService.aiEmbeddings(provider, model, texts);
            return R.success(result);

        } catch (Exception e) {
            logger.error("Embeddings失败", e);
            return R.error("Embeddings失败");
        }
    }

    @GetMapping("/fetch")
    public R<Map<String, Object>> fetchUrl(
            @RequestParam String url,
            @RequestParam(defaultValue = "30") int timeout,
            @RequestParam(defaultValue = "false") boolean parse) {
        
        try {
            logger.info("获取URL内容: url={}, timeout={}, parse={}", url, timeout, parse);

            Map<String, Object> result = externalApiService.fetchUrl(url, timeout, parse);
            return R.success(result);

        } catch (Exception e) {
            logger.error("获取URL内容失败", e);
            return R.error("获取URL内容失败");
        }
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(@RequestParam String url) {
        try {
            logger.info("下载文件: url={}", url);

            byte[] content = externalApiService.downloadFile(url);
            
            String fileName = url.substring(url.lastIndexOf('/') + 1);
            if (fileName.isEmpty()) {
                fileName = "download";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);

            return ResponseEntity.ok().headers(headers).body(content);

        } catch (Exception e) {
            logger.error("下载文件失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "下载文件失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/search")
    public R<Map<String, Object>> webSearch(@RequestBody Map<String, Object> requestBody) {
        try {
            String query = (String) requestBody.get("query");
            String engine = (String) requestBody.getOrDefault("engine", "baidu");
            Integer maxResults = requestBody.get("maxResults") != null ? 
                    (Integer) requestBody.get("maxResults") : 10;

            if (query == null || query.trim().isEmpty()) {
                return R.error("搜索关键词不能为空");
            }

            logger.info("执行网络搜索: query={}, engine={}, maxResults={}", query, engine, maxResults);

            Map<String, Object> result = externalApiService.webSearch(query, engine, maxResults);
            return R.success(result);

        } catch (Exception e) {
            logger.error("网络搜索失败", e);
            return R.error("网络搜索失败");
        }
    }

    @GetMapping("/status")
    public R<Map<String, Object>> getStatus() {
        try {
            Map<String, Object> status = externalApiService.getSystemStatus();
            return R.success(status);
        } catch (Exception e) {
            logger.error("获取系统状态失败", e);
            return R.error("获取系统状态失败");
        }
    }

    /**
     * 限流兜底：请求过于频繁时统一返回友好提示。
     */
    public R<Map<String, Object>> rateLimited(Map<String, Object> requestBody, Throwable t) {
        return R.error(429, "请求过于频繁，请稍后再试");
    }
}
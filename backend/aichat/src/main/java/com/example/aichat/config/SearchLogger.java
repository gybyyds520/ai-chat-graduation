package com.example.aichat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 搜索日志记录器
 * 提供结构化的日志记录功能，用于跟踪爬虫请求和结果解析过程
 */
@Component
public class SearchLogger {

    private static final Logger logger = LoggerFactory.getLogger(SearchLogger.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 存储请求上下文
     */
    private final ConcurrentHashMap<String, RequestContext> requestContexts = new ConcurrentHashMap<>();

    /**
     * 请求上下文类
     */
    public static class RequestContext {
        public String requestId;
        public String url;
        public String method;
        public Map<String, String> params;
        public long startTime;
        public long endTime;
        public int statusCode;
        public String statusMessage;
        public long responseTimeMs;
        public String error;
        public String phase;
        public int retryCount;
        public String proxyUsed;
        public int resultCount;
        public String parserStatus;
    }

    /**
     * 开始请求日志
     * 
     * @param url 请求URL
     * @param params 请求参数
     * @return 请求ID
     */
    public String logRequestStart(String url, Map<String, String> params) {
        String requestId = generateRequestId();
        RequestContext context = new RequestContext();
        context.requestId = requestId;
        context.url = url;
        context.method = "GET";
        context.params = params != null ? params : new HashMap<>();
        context.startTime = System.currentTimeMillis();
        context.phase = "REQUEST_START";
        context.retryCount = 0;

        requestContexts.put(requestId, context);

        Map<String, Object> logData = new HashMap<>();
        logData.put("requestId", requestId);
        logData.put("url", url);
        logData.put("params", context.params);
        logData.put("timestamp", LocalDateTime.now().format(formatter));
        logData.put("phase", "REQUEST_START");

        logger.info("[SEARCH_REQUEST_START] {}", toJsonString(logData));

        return requestId;
    }

    /**
     * 请求发送后日志
     * 
     * @param requestId 请求ID
     * @param statusCode HTTP状态码
     */
    public void logRequestSent(String requestId, int statusCode) {
        RequestContext context = requestContexts.get(requestId);
        if (context == null) {
            logger.warn("[SEARCH_REQUEST_UNKNOWN] requestId={}", requestId);
            return;
        }

        context.statusCode = statusCode;
        context.phase = "REQUEST_SENT";

        Map<String, Object> logData = new HashMap<>();
        logData.put("requestId", requestId);
        logData.put("url", context.url);
        logData.put("statusCode", statusCode);
        logData.put("timestamp", LocalDateTime.now().format(formatter));
        logData.put("phase", "REQUEST_SENT");
        logData.put("elapsedMs", System.currentTimeMillis() - context.startTime);

        logger.info("[SEARCH_REQUEST_SENT] {}", toJsonString(logData));
    }

    /**
     * 响应接收日志
     * 
     * @param requestId 请求ID
     * @param statusCode HTTP状态码
     * @param responseBody 响应体长度
     */
    public void logResponseReceived(String requestId, int statusCode, int responseBodyLength) {
        RequestContext context = requestContexts.get(requestId);
        if (context == null) {
            logger.warn("[SEARCH_RESPONSE_UNKNOWN] requestId={}", requestId);
            return;
        }

        context.endTime = System.currentTimeMillis();
        context.statusCode = statusCode;
        context.responseTimeMs = context.endTime - context.startTime;
        context.phase = "RESPONSE_RECEIVED";

        Map<String, Object> logData = new HashMap<>();
        logData.put("requestId", requestId);
        logData.put("url", context.url);
        logData.put("statusCode", statusCode);
        logData.put("responseTimeMs", context.responseTimeMs);
        logData.put("responseBodyLength", responseBodyLength);
        logData.put("timestamp", LocalDateTime.now().format(formatter));
        logData.put("phase", "RESPONSE_RECEIVED");

        if (statusCode >= 200 && statusCode < 300) {
            logger.info("[SEARCH_RESPONSE_SUCCESS] {}", toJsonString(logData));
        } else if (statusCode >= 400 && statusCode < 500) {
            logger.warn("[SEARCH_RESPONSE_CLIENT_ERROR] {}", toJsonString(logData));
        } else if (statusCode >= 500) {
            logger.error("[SEARCH_RESPONSE_SERVER_ERROR] {}", toJsonString(logData));
        } else {
            logger.info("[SEARCH_RESPONSE_RECEIVED] {}", toJsonString(logData));
        }
    }

    /**
     * 数据提取开始日志
     * 
     * @param requestId 请求ID
     * @param parserType 解析器类型
     */
    public void logExtractionStart(String requestId, String parserType) {
        RequestContext context = requestContexts.get(requestId);
        if (context == null) {
            context = new RequestContext();
            context.requestId = requestId;
            requestContexts.put(requestId, context);
        }

        context.phase = "EXTRACTION_START";
        context.parserStatus = "STARTED";

        Map<String, Object> logData = new HashMap<>();
        logData.put("requestId", requestId);
        logData.put("parserType", parserType);
        logData.put("timestamp", LocalDateTime.now().format(formatter));
        logData.put("phase", "EXTRACTION_START");

        logger.info("[SEARCH_EXTRACTION_START] {}", toJsonString(logData));
    }

    /**
     * 数据转换过程日志
     * 
     * @param requestId 请求ID
     * @param fieldName 字段名称
     * @param value 字段值
     * @param success 是否成功
     */
    public void logDataTransformation(String requestId, String fieldName, Object value, boolean success) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("requestId", requestId);
        logData.put("fieldName", fieldName);
        logData.put("valuePreview", value != null ? truncateString(value.toString(), 100) : null);
        logData.put("success", success);
        logData.put("timestamp", LocalDateTime.now().format(formatter));
        logData.put("phase", "DATA_TRANSFORMATION");

        if (success) {
            logger.debug("[SEARCH_DATA_TRANSFORM] {}", toJsonString(logData));
        } else {
            logger.warn("[SEARCH_DATA_TRANSFORM_FAILED] {}", toJsonString(logData));
        }
    }

    /**
     * 解析完成日志
     * 
     * @param requestId 请求ID
     * @param resultCount 解析结果数量
     * @param success 是否成功
     */
    public void logParsingComplete(String requestId, int resultCount, boolean success) {
        RequestContext context = requestContexts.get(requestId);
        if (context != null) {
            context.resultCount = resultCount;
            context.parserStatus = success ? "COMPLETED" : "FAILED";
            context.phase = "PARSING_COMPLETE";
        }

        Map<String, Object> logData = new HashMap<>();
        logData.put("requestId", requestId);
        logData.put("resultCount", resultCount);
        logData.put("success", success);
        logData.put("timestamp", LocalDateTime.now().format(formatter));
        logData.put("phase", "PARSING_COMPLETE");

        if (success) {
            logger.info("[SEARCH_PARSING_COMPLETE] {}", toJsonString(logData));
        } else {
            logger.error("[SEARCH_PARSING_FAILED] {}", toJsonString(logData));
        }
    }

    /**
     * 错误日志
     * 
     * @param requestId 请求ID
     * @param errorPhase 错误阶段
     * @param errorMessage 错误消息
     * @param exception 异常对象
     */
    public void logError(String requestId, String errorPhase, String errorMessage, Throwable exception) {
        RequestContext context = requestContexts.get(requestId);
        if (context != null) {
            context.error = errorMessage;
            context.phase = errorPhase + "_ERROR";
        }

        Map<String, Object> logData = new HashMap<>();
        logData.put("requestId", requestId);
        logData.put("errorPhase", errorPhase);
        logData.put("errorMessage", errorMessage);
        logData.put("exceptionType", exception != null ? exception.getClass().getSimpleName() : null);
        logData.put("exceptionMessage", exception != null ? exception.getMessage() : null);
        logData.put("stackTrace", exception != null ? getStackTraceString(exception) : null);
        logData.put("timestamp", LocalDateTime.now().format(formatter));
        logData.put("phase", "ERROR");
        
        // 添加请求上下文信息
        if (context != null) {
            logData.put("url", context.url);
            logData.put("params", context.params);
            logData.put("proxyUsed", context.proxyUsed);
            logData.put("retryCount", context.retryCount);
            logData.put("elapsedMs", System.currentTimeMillis() - context.startTime);
        }

        logger.error("[SEARCH_ERROR] {}", toJsonString(logData), exception);
    }

    /**
     * 获取完整的异常堆栈字符串
     * 
     * @param exception 异常对象
     * @return 堆栈字符串
     */
    public String getStackTraceString(Throwable exception) {
        if (exception == null) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(exception.getClass().getName())
          .append(": ")
          .append(exception.getMessage())
          .append("\n");
        
        StackTraceElement[] stackTrace = exception.getStackTrace();
        for (StackTraceElement element : stackTrace) {
            sb.append("\tat ")
              .append(element.getClassName())
              .append(".")
              .append(element.getMethodName())
              .append("(")
              .append(element.getFileName())
              .append(":")
              .append(element.getLineNumber())
              .append(")\n");
        }
        
        // 递归获取cause
        Throwable cause = exception.getCause();
        if (cause != null && cause != exception) {
            sb.append("Caused by: ");
            sb.append(getStackTraceString(cause));
        }
        
        return sb.toString();
    }

    /**
     * 请求失败详细日志（包含完整上下文）
     * 
     * @param requestId 请求ID
     * @param exception 异常对象
     */
    public void logRequestFailure(String requestId, Throwable exception) {
        RequestContext context = requestContexts.get(requestId);
        
        Map<String, Object> logData = new HashMap<>();
        logData.put("requestId", requestId);
        logData.put("timestamp", LocalDateTime.now().format(formatter));
        logData.put("phase", "REQUEST_FAILURE");
        logData.put("errorType", exception != null ? exception.getClass().getName() : "Unknown");
        logData.put("errorMessage", exception != null ? exception.getMessage() : "No message");
        logData.put("stackTrace", exception != null ? getStackTraceString(exception) : null);
        
        if (context != null) {
            logData.put("url", context.url);
            logData.put("method", context.method);
            logData.put("params", context.params);
            logData.put("proxyUsed", context.proxyUsed);
            logData.put("retryCount", context.retryCount);
            logData.put("statusCode", context.statusCode);
            logData.put("responseTimeMs", context.responseTimeMs);
            logData.put("elapsedMs", System.currentTimeMillis() - context.startTime);
        }
        
        logger.error("[SEARCH_REQUEST_FAILURE] {}", toJsonString(logData));
    }

    /**
     * 重试日志
     * 
     * @param requestId 请求ID
     * @param retryCount 重试次数
     * @param reason 重试原因
     */
    public void logRetry(String requestId, int retryCount, String reason) {
        RequestContext context = requestContexts.get(requestId);
        if (context != null) {
            context.retryCount = retryCount;
            context.phase = "RETRY";
        }

        Map<String, Object> logData = new HashMap<>();
        logData.put("requestId", requestId);
        logData.put("retryCount", retryCount);
        logData.put("reason", reason);
        logData.put("timestamp", LocalDateTime.now().format(formatter));
        logData.put("phase", "RETRY");

        logger.warn("[SEARCH_RETRY] {}", toJsonString(logData));
    }

    /**
     * 代理使用日志
     * 
     * @param requestId 请求ID
     * @param proxyAddress 代理地址
     * @param success 是否成功
     */
    public void logProxyUsage(String requestId, String proxyAddress, boolean success) {
        RequestContext context = requestContexts.get(requestId);
        if (context != null) {
            context.proxyUsed = proxyAddress;
        }

        Map<String, Object> logData = new HashMap<>();
        logData.put("requestId", requestId);
        logData.put("proxyAddress", proxyAddress);
        logData.put("success", success);
        logData.put("timestamp", LocalDateTime.now().format(formatter));
        logData.put("phase", "PROXY_USAGE");

        if (success) {
            logger.info("[SEARCH_PROXY_SUCCESS] {}", toJsonString(logData));
        } else {
            logger.warn("[SEARCH_PROXY_FAILED] {}", toJsonString(logData));
        }
    }

    /**
     * 请求完成总结日志
     * 
     * @param requestId 请求ID
     */
    public void logRequestSummary(String requestId) {
        RequestContext context = requestContexts.get(requestId);
        if (context == null) {
            logger.warn("[SEARCH_SUMMARY_UNKNOWN] requestId={}", requestId);
            return;
        }

        Map<String, Object> logData = new HashMap<>();
        logData.put("requestId", requestId);
        logData.put("url", context.url);
        logData.put("statusCode", context.statusCode);
        logData.put("responseTimeMs", context.responseTimeMs);
        logData.put("resultCount", context.resultCount);
        logData.put("retryCount", context.retryCount);
        logData.put("proxyUsed", context.proxyUsed);
        logData.put("error", context.error);
        logData.put("success", context.error == null);
        logData.put("timestamp", LocalDateTime.now().format(formatter));
        logData.put("phase", "REQUEST_SUMMARY");

        if (context.error == null) {
            logger.info("[SEARCH_SUMMARY_SUCCESS] {}", toJsonString(logData));
        } else {
            logger.error("[SEARCH_SUMMARY_FAILED] {}", toJsonString(logData));
        }

        // 清理上下文
        requestContexts.remove(requestId);
    }

    /**
     * 性能统计日志
     * 
     * @param operation 操作名称
     * @param durationMs 耗时（毫秒）
     */
    public void logPerformance(String operation, long durationMs) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("operation", operation);
        logData.put("durationMs", durationMs);
        logData.put("timestamp", LocalDateTime.now().format(formatter));
        logData.put("phase", "PERFORMANCE");

        if (durationMs > 5000) {
            logger.warn("[SEARCH_PERFORMANCE_SLOW] {}", toJsonString(logData));
        } else if (durationMs > 1000) {
            logger.info("[SEARCH_PERFORMANCE_NORMAL] {}", toJsonString(logData));
        } else {
            logger.debug("[SEARCH_PERFORMANCE_FAST] {}", toJsonString(logData));
        }
    }

    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 转换为JSON字符串
     */
    private String toJsonString(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append("\"").append(entry.getKey()).append("\": ");
            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append("\"").append(escapeJson(value.toString())).append("\"");
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 转义JSON字符串
     */
    private String escapeJson(String s) {
        if (s == null) {
            return null;
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 截断字符串
     */
    private String truncateString(String s, int maxLength) {
        if (s == null) {
            return null;
        }
        if (s.length() <= maxLength) {
            return s;
        }
        return s.substring(0, maxLength) + "...";
    }
}

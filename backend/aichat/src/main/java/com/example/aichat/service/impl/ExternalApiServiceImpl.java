package com.example.aichat.service.impl;

import com.example.aichat.service.ExternalApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExternalApiServiceImpl implements ExternalApiService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalApiServiceImpl.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpClient httpClient;

    @Value("${external.api.proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${external.api.proxy.host:}")
    private String proxyHost;

    @Value("${external.api.proxy.port:0}")
    private int proxyPort;

    @Value("${external.api.proxy.username:}")
    private String proxyUsername;

    @Value("${external.api.proxy.password:}")
    private String proxyPassword;

    @Value("${external.api.providers.doubao.enabled:true}")
    private boolean doubaoEnabled;

    @Value("${external.api.providers.doubao.base-url:https://api.doubao.com/v1}")
    private String doubaoBaseUrl;

    @Value("${external.api.providers.doubao.api-key-header:Authorization}")
    private String doubaoApiKeyHeader;

    @Value("${external.api.providers.deepseek.enabled:true}")
    private boolean deepseekEnabled;

    @Value("${external.api.providers.deepseek.base-url:https://api.deepseek.com/v1}")
    private String deepseekBaseUrl;

    @Value("${external.api.providers.deepseek.api-key-header:Authorization}")
    private String deepseekApiKeyHeader;

    @Value("${external.api.providers.openai.enabled:true}")
    private boolean openaiEnabled;

    @Value("${external.api.providers.openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    @Value("${external.api.providers.openai.api-key-header:Authorization}")
    private String openaiApiKeyHeader;

    @Value("${external.api.providers.anthropic.enabled:false}")
    private boolean anthropicEnabled;

    @Value("${external.api.providers.anthropic.base-url:https://api.anthropic.com/v1}")
    private String anthropicBaseUrl;

    @Value("${external.api.providers.anthropic.api-key-header:x-api-key}")
    private String anthropicApiKeyHeader;

    @Value("${external.api.providers.google.enabled:false}")
    private boolean googleEnabled;

    @Value("${external.api.providers.google.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String googleBaseUrl;

    @Value("${external.api.providers.google.api-key-header:x-goog-api-key}")
    private String googleApiKeyHeader;

    private final Map<String, ProviderConfig> providerConfigs = new ConcurrentHashMap<>();

    private static class ProviderConfig {
        String name;
        String baseUrl;
        String apiKeyHeader;
        boolean enabled;
        List<ModelInfo> models;

        ProviderConfig(String name, String baseUrl, String apiKeyHeader, boolean enabled) {
            this.name = name;
            this.baseUrl = baseUrl;
            this.apiKeyHeader = apiKeyHeader;
            this.enabled = enabled;
            this.models = new ArrayList<>();
        }
    }

    private static class ModelInfo {
        String id;
        String name;
        String description;
        boolean available;

        ModelInfo(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.available = true;
        }
    }

    private void initHttpClient() {
        if (httpClient != null) {
            return;
        }

        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL);

        if (proxyEnabled && proxyHost != null && !proxyHost.isEmpty() && proxyPort > 0) {
            builder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)));
            logger.info("外部API客户端配置代理: {}:{}", proxyHost, proxyPort);
        }

        httpClient = builder.build();
    }

    private void initProviderConfigs() {
        if (!providerConfigs.isEmpty()) {
            return;
        }

        if (doubaoEnabled) {
            ProviderConfig doubao = new ProviderConfig("豆包", doubaoBaseUrl, doubaoApiKeyHeader, true);
            doubao.models.add(new ModelInfo("doubao-3", "豆包3", "豆包最新AI模型"));
            doubao.models.add(new ModelInfo("doubao-3-128k", "豆包3-128k", "豆包大上下文模型"));
            providerConfigs.put("doubao", doubao);
        }

        if (deepseekEnabled) {
            ProviderConfig deepseek = new ProviderConfig("DeepSeek", deepseekBaseUrl, deepseekApiKeyHeader, true);
            deepseek.models.add(new ModelInfo("deepseek-chat", "DeepSeek Chat", "DeepSeek对话模型"));
            deepseek.models.add(new ModelInfo("deepseek-coder", "DeepSeek Coder", "DeepSeek代码模型"));
            providerConfigs.put("deepseek", deepseek);
        }

        if (openaiEnabled) {
            ProviderConfig openai = new ProviderConfig("OpenAI", openaiBaseUrl, openaiApiKeyHeader, true);
            openai.models.add(new ModelInfo("gpt-4o", "GPT-4o", "OpenAI最新模型"));
            openai.models.add(new ModelInfo("gpt-4", "GPT-4", "GPT-4模型"));
            openai.models.add(new ModelInfo("gpt-3.5-turbo", "GPT-3.5 Turbo", "GPT-3.5对话模型"));
            providerConfigs.put("openai", openai);
        }

        if (anthropicEnabled) {
            ProviderConfig anthropic = new ProviderConfig("Anthropic", anthropicBaseUrl, anthropicApiKeyHeader, true);
            anthropic.models.add(new ModelInfo("claude-3-opus-20240229", "Claude 3 Opus", "Anthropic最强模型"));
            anthropic.models.add(new ModelInfo("claude-3-sonnet-20240229", "Claude 3 Sonnet", "Anthropic平衡模型"));
            anthropic.models.add(new ModelInfo("claude-3-haiku-20240307", "Claude 3 Haiku", "Anthropic轻量模型"));
            providerConfigs.put("anthropic", anthropic);
        }

        if (googleEnabled) {
            ProviderConfig google = new ProviderConfig("Google", googleBaseUrl, googleApiKeyHeader, true);
            google.models.add(new ModelInfo("gemini-pro", "Gemini Pro", "Google Gemini模型"));
            google.models.add(new ModelInfo("gemini-pro-vision", "Gemini Pro Vision", "Google多模态模型"));
            providerConfigs.put("google", google);
        }

        logger.info("外部API服务商配置初始化完成，服务商数量: {}", providerConfigs.size());
    }

    @Override
    public Map<String, Object> healthCheck() {
        initHttpClient();
        initProviderConfigs();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        result.put("proxyEnabled", proxyEnabled);
        result.put("providerCount", providerConfigs.size());

        Map<String, Boolean> providerStatus = new HashMap<>();
        for (String provider : providerConfigs.keySet()) {
            providerStatus.put(provider, true);
        }
        result.put("providers", providerStatus);

        return result;
    }

    @Override
    public List<Map<String, Object>> getAvailableProviders() {
        initProviderConfigs();

        List<Map<String, Object>> providers = new ArrayList<>();

        for (Map.Entry<String, ProviderConfig> entry : providerConfigs.entrySet()) {
            Map<String, Object> providerInfo = new HashMap<>();
            providerInfo.put("id", entry.getKey());
            providerInfo.put("name", entry.getValue().name);
            providerInfo.put("baseUrl", entry.getValue().baseUrl);
            providerInfo.put("apiKeyHeader", entry.getValue().apiKeyHeader);
            providerInfo.put("enabled", entry.getValue().enabled);
            
            List<Map<String, Object>> models = new ArrayList<>();
            for (ModelInfo model : entry.getValue().models) {
                Map<String, Object> modelInfo = new HashMap<>();
                modelInfo.put("id", model.id);
                modelInfo.put("name", model.name);
                modelInfo.put("description", model.description);
                modelInfo.put("available", model.available);
                models.add(modelInfo);
            }
            providerInfo.put("models", models);

            providers.add(providerInfo);
        }

        return providers;
    }

    @Override
    public Map<String, Object> proxyRequest(String method, String url, Map<String, String> headers, Object body, int timeout) {
        // 安全加固（P0）：SSRF 防护 - 仅允许访问公网 http/https，禁止内网/回环地址
        com.example.aichat.util.UrlSecurityUtil.validateUrl(url);
        initHttpClient();

        Map<String, Object> result = new HashMap<>();

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeout));

            if (headers != null) {
                headers.forEach(requestBuilder::header);
            }

            if (body != null) {
                String bodyJson = objectMapper.writeValueAsString(body);
                requestBuilder.header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(bodyJson));
            } else {
                if ("POST".equalsIgnoreCase(method)) {
                    requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
                } else {
                    requestBuilder.GET();
                }
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            result.put("statusCode", response.statusCode());
            result.put("body", response.body());

            Map<String, String> responseHeaders = new HashMap<>();
            response.headers().map().forEach((key, values) -> {
                responseHeaders.put(key, String.join(", ", values));
            });
            result.put("headers", responseHeaders);

            logger.info("代理请求成功: url={}, status={}", url, response.statusCode());

        } catch (Exception e) {
            logger.error("代理请求失败: url={}, error={}", url, e.getMessage(), e);
            result.put("statusCode", 500);
            result.put("body", "{\"error\": \"" + e.getMessage() + "\"}");
            result.put("headers", new HashMap<>());
        }

        return result;
    }

    @Override
    public Map<String, Object> aiChat(String provider, String model, String prompt, String systemPrompt, boolean stream) {
        initHttpClient();
        initProviderConfigs();

        ProviderConfig config = providerConfigs.get(provider.toLowerCase());
        if (config == null) {
            throw new RuntimeException("服务商不存在: " + provider);
        }

        String baseUrl = config.baseUrl;
        String apiKeyHeader = config.apiKeyHeader;

        String defaultModel = config.models.isEmpty() ? "default" : config.models.get(0).id;
        String actualModel = (model == null || model.isEmpty()) ? defaultModel : model;

        String endpoint = "/chat/completions";
        if ("google".equalsIgnoreCase(provider)) {
            endpoint = "/models/" + actualModel + ":generateContent";
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();

            if ("google".equalsIgnoreCase(provider)) {
                List<Map<String, Object>> contents = new ArrayList<>();
                Map<String, Object> content = new HashMap<>();
                content.put("parts", List.of(Map.of("text", prompt)));
                contents.add(content);
                requestBody.put("contents", contents);
                requestBody.put("generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 2048
                ));
            } else {
                requestBody.put("model", actualModel);
                requestBody.put("stream", stream);

                List<Map<String, Object>> messages = new ArrayList<>();

                if (systemPrompt != null && !systemPrompt.isEmpty()) {
                    messages.add(Map.of("role", "system", "content", systemPrompt));
                }
                messages.add(Map.of("role", "user", "content", prompt));

                requestBody.put("messages", messages);
                requestBody.put("temperature", 0.7);
                requestBody.put("max_tokens", 2048);
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60));

            requestBuilder.header(apiKeyHeader, "Bearer ");

            String bodyJson = objectMapper.writeValueAsString(requestBody);
            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(bodyJson));

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            Map<String, Object> result = new HashMap<>();
            result.put("statusCode", response.statusCode());
            result.put("provider", provider);
            result.put("model", actualModel);

            if (response.statusCode() == 200) {
                try {
                    result.put("response", objectMapper.readValue(response.body(), Map.class));
                } catch (Exception e) {
                    result.put("response", response.body());
                }
            } else {
                result.put("error", response.body());
            }

            return result;

        } catch (Exception e) {
            logger.error("AI对话请求失败: provider={}, model={}, error={}", provider, actualModel, e.getMessage(), e);
            throw new RuntimeException("AI对话请求失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> aiCompletions(String provider, String model, String prompt, Integer maxTokens, Double temperature) {
        initHttpClient();
        initProviderConfigs();

        ProviderConfig config = providerConfigs.get(provider.toLowerCase());
        if (config == null) {
            throw new RuntimeException("服务商不存在: " + provider);
        }

        String baseUrl = config.baseUrl;
        String apiKeyHeader = config.apiKeyHeader;

        String defaultModel = config.models.isEmpty() ? "default" : config.models.get(0).id;
        String actualModel = (model == null || model.isEmpty()) ? defaultModel : model;

        int actualMaxTokens = maxTokens != null ? maxTokens : 1024;
        double actualTemperature = temperature != null ? temperature : 0.7;

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", actualModel);
            requestBody.put("prompt", prompt);
            requestBody.put("max_tokens", actualMaxTokens);
            requestBody.put("temperature", actualTemperature);
            requestBody.put("stream", false);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/completions"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60));

            requestBuilder.header(apiKeyHeader, "Bearer ");

            String bodyJson = objectMapper.writeValueAsString(requestBody);
            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(bodyJson));

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            Map<String, Object> result = new HashMap<>();
            result.put("statusCode", response.statusCode());
            result.put("provider", provider);
            result.put("model", actualModel);

            if (response.statusCode() == 200) {
                try {
                    result.put("response", objectMapper.readValue(response.body(), Map.class));
                } catch (Exception e) {
                    result.put("response", response.body());
                }
            } else {
                result.put("error", response.body());
            }

            return result;

        } catch (Exception e) {
            logger.error("AI补全请求失败: provider={}, model={}, error={}", provider, actualModel, e.getMessage(), e);
            throw new RuntimeException("AI补全请求失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getProviderModels(String provider) {
        initProviderConfigs();

        ProviderConfig config = providerConfigs.get(provider.toLowerCase());
        if (config == null) {
            throw new RuntimeException("服务商不存在: " + provider);
        }

        List<Map<String, Object>> models = new ArrayList<>();
        for (ModelInfo model : config.models) {
            Map<String, Object> modelInfo = new HashMap<>();
            modelInfo.put("id", model.id);
            modelInfo.put("name", model.name);
            modelInfo.put("description", model.description);
            modelInfo.put("available", model.available);
            models.add(modelInfo);
        }

        return models;
    }

    @Override
    public Map<String, Object> aiEmbeddings(String provider, String model, List<String> texts) {
        initHttpClient();
        initProviderConfigs();

        ProviderConfig config = providerConfigs.get(provider.toLowerCase());
        if (config == null) {
            throw new RuntimeException("服务商不存在: " + provider);
        }

        String baseUrl = config.baseUrl;
        String apiKeyHeader = config.apiKeyHeader;

        String defaultModel = config.models.isEmpty() ? "default" : config.models.get(0).id;
        String actualModel = (model == null || model.isEmpty()) ? defaultModel : model;

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", actualModel);
            requestBody.put("input", texts);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/embeddings"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60));

            requestBuilder.header(apiKeyHeader, "Bearer ");

            String bodyJson = objectMapper.writeValueAsString(requestBody);
            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(bodyJson));

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            Map<String, Object> result = new HashMap<>();
            result.put("statusCode", response.statusCode());
            result.put("provider", provider);
            result.put("model", actualModel);

            if (response.statusCode() == 200) {
                try {
                    result.put("response", objectMapper.readValue(response.body(), Map.class));
                } catch (Exception e) {
                    result.put("response", response.body());
                }
            } else {
                result.put("error", response.body());
            }

            return result;

        } catch (Exception e) {
            logger.error("Embeddings请求失败: provider={}, model={}, error={}", provider, actualModel, e.getMessage(), e);
            throw new RuntimeException("Embeddings请求失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> fetchUrl(String url, int timeout, boolean parse) {
        // 安全加固（P0）：SSRF 防护
        com.example.aichat.util.UrlSecurityUtil.validateUrl(url);
        initHttpClient();

        Map<String, Object> result = new HashMap<>();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .timeout(Duration.ofSeconds(timeout))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            result.put("statusCode", response.statusCode());
            result.put("url", url);

            if (response.statusCode() == 200) {
                String content = response.body();
                result.put("content", content);

                if (parse) {
                    try {
                        Document doc = Jsoup.parse(content);
                        
                        Map<String, Object> parsedData = new HashMap<>();
                        parsedData.put("title", doc.title());
                        
                        Elements paragraphs = doc.select("p");
                        List<String> texts = new ArrayList<>();
                        paragraphs.forEach(p -> texts.add(p.text()));
                        parsedData.put("paragraphs", texts);
                        
                        Elements links = doc.select("a[href]");
                        List<String> hrefs = new ArrayList<>();
                        links.forEach(a -> hrefs.add(a.attr("abs:href")));
                        parsedData.put("links", hrefs);
                        
                        result.put("parsed", parsedData);
                    } catch (Exception e) {
                        logger.warn("HTML解析失败: {}", e.getMessage());
                        result.put("parsed", null);
                    }
                }
            } else {
                result.put("error", "HTTP状态码: " + response.statusCode());
            }

            logger.info("URL获取成功: url={}, status={}", url, response.statusCode());

        } catch (Exception e) {
            logger.error("URL获取失败: url={}, error={}", url, e.getMessage(), e);
            result.put("statusCode", 500);
            result.put("error", e.getMessage());
        }

        return result;
    }

    @Override
    public byte[] downloadFile(String url) {
        // 安全加固（P0）：SSRF 防护
        com.example.aichat.util.UrlSecurityUtil.validateUrl(url);
        initHttpClient();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                logger.info("文件下载成功: url={}, size={} bytes", url, response.body().length);
                return response.body();
            } else {
                throw new RuntimeException("下载失败，HTTP状态码: " + response.statusCode());
            }

        } catch (Exception e) {
            logger.error("文件下载失败: url={}, error={}", url, e.getMessage(), e);
            throw new RuntimeException("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> webSearch(String query, String engine, Integer maxResults) {
        initHttpClient();

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        int actualMaxResults = maxResults != null ? maxResults : 10;

        String searchUrl;
        switch (engine.toLowerCase()) {
            case "bing":
                searchUrl = "https://www.bing.com/search?q=" + encodeUrl(query) + "&count=" + actualMaxResults;
                break;
            case "duckduckgo":
                searchUrl = "https://duckduckgo.com/html/?q=" + encodeUrl(query) + "&kl=zh-CN";
                break;
            case "baidu":
            default:
                searchUrl = "https://www.baidu.com/s?wd=" + encodeUrl(query) + "&rn=" + actualMaxResults;
                break;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String html = response.body();
                Document doc = Jsoup.parse(html);

                Elements items;
                if (engine.toLowerCase().contains("bing")) {
                    items = doc.select("li.b_algo");
                    for (Element item : items) {
                        Map<String, Object> searchResult = new HashMap<>();
                        Element title = item.selectFirst("h2");
                        Element link = item.selectFirst("h2 > a");
                        Element snippet = item.selectFirst("p");

                        if (title != null) searchResult.put("title", title.text());
                        if (link != null) searchResult.put("url", link.attr("href"));
                        if (snippet != null) searchResult.put("snippet", snippet.text());

                        if (searchResult.containsKey("title") && searchResult.containsKey("url")) {
                            results.add(searchResult);
                        }
                    }
                } else if (engine.toLowerCase().contains("duckduckgo")) {
                    items = doc.select("div.result");
                    for (Element item : items) {
                        Map<String, Object> searchResult = new HashMap<>();
                        Element title = item.selectFirst("a.result__a");
                        Element snippet = item.selectFirst("a.result__snippet");

                        if (title != null) {
                            searchResult.put("title", title.text());
                            searchResult.put("url", title.attr("href"));
                        }
                        if (snippet != null) searchResult.put("snippet", snippet.text());

                        if (searchResult.containsKey("title") && searchResult.containsKey("url")) {
                            results.add(searchResult);
                        }
                    }
                } else {
                    items = doc.select("div.result-op.c-container");
                    for (Element item : items) {
                        Map<String, Object> searchResult = new HashMap<>();
                        Element title = item.selectFirst("h3.t");
                        Element link = item.selectFirst("h3.t > a");
                        Element snippet = item.selectFirst("div.c-abstract");

                        if (title != null) searchResult.put("title", title.text());
                        if (link != null) searchResult.put("url", link.attr("href"));
                        if (snippet != null) searchResult.put("snippet", snippet.text());

                        if (searchResult.containsKey("title") && searchResult.containsKey("url")) {
                            results.add(searchResult);
                        }
                    }
                }
            }

            result.put("query", query);
            result.put("engine", engine);
            result.put("results", results);
            result.put("resultCount", results.size());

            logger.info("网络搜索完成: query={}, engine={}, count={}", query, engine, results.size());

        } catch (Exception e) {
            logger.error("网络搜索失败: query={}, error={}", query, e.getMessage(), e);
            result.put("error", e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> getSystemStatus() {
        initHttpClient();
        initProviderConfigs();

        Map<String, Object> status = new HashMap<>();
        status.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        status.put("proxyEnabled", proxyEnabled);
        status.put("providerCount", providerConfigs.size());

        List<String> providerIds = new ArrayList<>(providerConfigs.keySet());
        status.put("providers", providerIds);

        return status;
    }

    private String encodeUrl(String url) {
        try {
            return java.net.URLEncoder.encode(url, "UTF-8");
        } catch (Exception e) {
            return url;
        }
    }
}
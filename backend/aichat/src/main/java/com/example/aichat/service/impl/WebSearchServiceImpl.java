package com.example.aichat.service.impl;

import com.example.aichat.config.SearchLogger;
import com.example.aichat.entity.SearchResult;
import com.example.aichat.service.WebSearchService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 网络搜索服务增强实现
 * 提供AI系统互联网资料查找功能
 * 包含详细日志记录、代理支持、故障转移等功能
 */
@Service
public class WebSearchServiceImpl implements WebSearchService {

    private static final Logger logger = LoggerFactory.getLogger(WebSearchServiceImpl.class);

    @Autowired
    private SearchLogger searchLogger;

    /**
     * 代理配置
     */
    @Value("${search.proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${search.proxy.host:}")
    private String proxyHost;

    @Value("${search.proxy.port:0}")
    private int proxyPort;

    @Value("${search.proxy.username:}")
    private String proxyUsername;

    @Value("${search.proxy.password:}")
    private String proxyPassword;

    /**
     * 代理池配置
     */
    private final List<ProxyConfig> proxyPool = new ArrayList<>();
    private int currentProxyIndex = 0;

    /**
     * HTTP客户端
     */
    private HttpClient httpClient;

    /**
     * 用户代理列表，轮换使用以避免被屏蔽
     */
    private static final List<String> USER_AGENTS = Arrays.asList(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15"
    );

    /**
     * 搜索历史记录
     */
    private final ConcurrentLinkedQueue<Map<String, Object>> searchHistory = new ConcurrentLinkedQueue<>();
    private static final int MAX_HISTORY_SIZE = 100;

    /**
     * 最大重试次数（连接超时快速失败，避免不可达搜索源长时间阻塞）
     */
    private static final int MAX_RETRIES = 2;

    /**
     * 代理配置类
     */
    private static class ProxyConfig {
        String host;
        int port;
        String username;
        String password;
        boolean isHealthy = true;
        int failCount = 0;

        ProxyConfig(String host, int port, String username, String password) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
        }
    }

    /**
     * 初始化HTTP客户端
     */
    private void initHttpClient() {
        // 连接超时 4s：不可达搜索源（如境外引擎）快速失败，避免长时间阻塞接口
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .executor(Executors.newFixedThreadPool(10));

        // 配置代理
        if (proxyEnabled && proxyHost != null && !proxyHost.isEmpty() && proxyPort > 0) {
            builder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)));
            logger.info("HTTP客户端配置代理: {}:{}", proxyHost, proxyPort);
        }

        httpClient = builder.build();
    }

    /**
     * 初始化代理池
     */
    private void initProxyPool() {
        // 添加主代理
        if (proxyEnabled && proxyHost != null && !proxyHost.isEmpty()) {
            proxyPool.add(new ProxyConfig(proxyHost, proxyPort, proxyUsername, proxyPassword));
        }

        // 可以在这里添加更多备用代理
        // 示例：proxyPool.add(new ProxyConfig("backup.proxy.com", 8080, "user", "pass"));

        logger.info("代理池初始化完成，代理数量: {}", proxyPool.size());
    }

    /**
     * 获取下一个可用的代理
     */
    private ProxyConfig getNextProxy() {
        if (proxyPool.isEmpty()) {
            return null;
        }

        // 跳过不健康的代理
        int attempts = 0;
        while (attempts < proxyPool.size()) {
            ProxyConfig proxy = proxyPool.get(currentProxyIndex);
            currentProxyIndex = (currentProxyIndex + 1) % proxyPool.size();

            if (proxy.isHealthy) {
                return proxy;
            }
            attempts++;
        }

        // 如果所有代理都不健康，重置状态
        proxyPool.forEach(p -> p.isHealthy = true);
        return proxyPool.get(0);
    }

    /**
     * 标记代理失败
     */
    private void markProxyFailed(ProxyConfig proxy) {
        if (proxy != null) {
            proxy.failCount++;
            if (proxy.failCount >= 3) {
                proxy.isHealthy = false;
                logger.warn("代理标记为不健康: {}:{}", proxy.host, proxy.port);
            }
        }
    }

    /**
     * 标记代理成功
     */
    private void markProxySuccess(ProxyConfig proxy) {
        if (proxy != null) {
            proxy.failCount = 0;
            proxy.isHealthy = true;
        }
    }

    @Override
    public List<SearchResult> search(String query, int maxResults) {
        return searchWithVerification(query, maxResults, false);
    }

    @Override
    public List<SearchResult> searchWithVerification(String query, int maxResults, boolean verifyEnabled) {
        long startTime = System.currentTimeMillis();
        String mainRequestId = searchLogger.logRequestStart("search", Map.of("query", query, "maxResults", String.valueOf(maxResults)));

        logger.info("开始执行网络搜索: query={}, maxResults={}, verifyEnabled={}", query, maxResults, verifyEnabled);

        try {
            // 初始化客户端
            if (httpClient == null) {
                initHttpClient();
                initProxyPool();
            }

            // 生成搜索关键词
            List<String> keywords = generateSearchKeywords(query);
            logger.debug("生成的搜索关键词: {}", keywords);

            // 并行执行多个关键词搜索
            List<SearchResult> allResults = new ArrayList<>();
            // 安全修复：使用try-finally确保线程池一定被关闭，防止异常路径下线程泄漏
            ExecutorService executor = Executors.newFixedThreadPool(Math.min(keywords.size(), 3));
            try {
                List<Future<List<SearchResult>>> futures = new ArrayList<>();
                for (String keyword : keywords) {
                    futures.add(executor.submit(() -> performSearchWithRetry(keyword, maxResults / keywords.size() + 1)));
                }

                // 收集所有结果（单关键词最长等待 8s，整体快速兜底，避免搜索接口长时间挂起）
                for (Future<List<SearchResult>> future : futures) {
                    try {
                        List<SearchResult> results = future.get(8, TimeUnit.SECONDS);
                        allResults.addAll(results);
                    } catch (TimeoutException e) {
                        logger.warn("搜索超时: {}", e.getMessage());
                        searchLogger.logError(mainRequestId, "SEARCH_TIMEOUT", "搜索超时", e);
                    } catch (InterruptedException | ExecutionException e) {
                        logger.error("搜索执行异常", e);
                        searchLogger.logError(mainRequestId, "SEARCH_EXECUTION", "搜索执行异常", e);
                    }
                }
            } finally {
                executor.shutdown();
            }

            // 去重和排序
            List<SearchResult> deduplicatedResults = deduplicateResults(allResults);
            deduplicatedResults.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));

            // 限制结果数量
            if (deduplicatedResults.size() > maxResults) {
                deduplicatedResults = deduplicatedResults.subList(0, maxResults);
            }

            // 验证结果（如果启用）
            if (verifyEnabled) {
                deduplicatedResults = verifyResults(deduplicatedResults);
            }

            // 记录搜索历史
            recordSearchHistory(query, deduplicatedResults.size(), System.currentTimeMillis() - startTime);

            long duration = System.currentTimeMillis() - startTime;
            searchLogger.logPerformance("TOTAL_SEARCH", duration);
            searchLogger.logRequestSummary(mainRequestId);

            logger.info("搜索完成: query={}, 结果数={}, 耗时={}ms", query, deduplicatedResults.size(), duration);

            return deduplicatedResults;

        } catch (Exception e) {
            logger.error("搜索失败: query={}", query, e);
            searchLogger.logError(mainRequestId, "SEARCH_FAILED", "搜索失败: " + e.getMessage(), e);
            searchLogger.logRequestSummary(mainRequestId);
            throw new RuntimeException("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 带重试机制的搜索
     */
    private List<SearchResult> performSearchWithRetry(String keyword, int count) {
        List<SearchResult> results = new ArrayList<>();
        List<String> searchUrls = generateSearchUrls(keyword);

        for (String searchUrl : searchUrls) {
            for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
                String requestId = searchLogger.logRequestStart(searchUrl, Map.of("keyword", keyword, "attempt", String.valueOf(attempt)));

                try {
                    List<SearchResult> pageResults = fetchSearchResultsWithLogging(searchUrl, requestId);
                    results.addAll(pageResults);

                    searchLogger.logRequestSummary(requestId);

                    if (results.size() >= count) {
                        return results;
                    }
                    break; // 成功，跳出重试循环

                } catch (Exception e) {
                    searchLogger.logRetry(requestId, attempt + 1, e.getMessage());

                    if (attempt == MAX_RETRIES - 1) {
                        logger.warn("获取搜索结果失败（已达最大重试次数）: url={}", searchUrl);
                        searchLogger.logError(requestId, "MAX_RETRIES_EXCEEDED", e.getMessage(), e);
                    }

                    // 快速重试（避免不可达源长时间阻塞）
                    try {
                        Thread.sleep(300 * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        if (results.size() > count) {
            results = results.subList(0, count);
        }

        return results;
    }

    /**
     * 带详细日志的搜索结果获取
     */
    private List<SearchResult> fetchSearchResultsWithLogging(String url, String requestId) throws IOException {
        List<SearchResult> results = new ArrayList<>();

        // 记录请求发送
        searchLogger.logRequestSent(requestId, 0);

        String html = fetchUrlWithProxy(url, requestId);
        if (html == null || html.isEmpty()) {
            searchLogger.logResponseReceived(requestId, 0, 0);
            return results;
        }

        // 记录响应接收
        searchLogger.logResponseReceived(requestId, 200, html.length());

        // 开始解析
        searchLogger.logExtractionStart(requestId, "HTML_PARSER");

        Document doc = Jsoup.parse(html);

        // 解析百度搜索结果
        if (url.contains("baidu")) {
            Elements items = doc.select("div.result-op.c-container.xpath-log.new-pmd");
            for (Element item : items) {
                SearchResult result = parseBaiduResultWithLogging(item, requestId);
                if (result != null) {
                    results.add(result);
                }
            }
        }
        // 解析Bing搜索结果
        else if (url.contains("bing")) {
            Elements items = doc.select("li.b_algo");
            for (Element item : items) {
                SearchResult result = parseBingResultWithLogging(item, requestId);
                if (result != null) {
                    results.add(result);
                }
            }
        }
        // 解析DuckDuckGo搜索结果
        else if (url.contains("duckduckgo")) {
            Elements items = doc.select("div.result");
            for (Element item : items) {
                SearchResult result = parseDuckDuckGoResultWithLogging(item, requestId);
                if (result != null) {
                    results.add(result);
                }
            }
        }

        // 记录解析完成
        searchLogger.logParsingComplete(requestId, results.size(), true);

        return results;
    }

    /**
     * 使用代理获取URL内容
     */
    private String fetchUrlWithProxy(String url, String requestId) {
        ProxyConfig proxy = null;

        try {
            String userAgent = USER_AGENTS.get(new Random().nextInt(USER_AGENTS.size()));

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", userAgent)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .timeout(Duration.ofSeconds(6))
                    .GET();

            HttpRequest request = requestBuilder.build();

            // 使用代理客户端
            HttpClient client = httpClient;
            if (proxyEnabled && !proxyPool.isEmpty()) {
                proxy = getNextProxy();
                if (proxy != null) {
                    client = createProxyClient(proxy);
                    searchLogger.logProxyUsage(requestId, proxy.host + ":" + proxy.port, true);
                }
            }

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                if (proxy != null) {
                    markProxySuccess(proxy);
                }
                return response.body();
            } else {
                logger.warn("HTTP请求失败: url={}, status={}", url, response.statusCode());
                // 记录详细的HTTP错误日志
                searchLogger.logError(requestId, "HTTP_ERROR", 
                        "HTTP请求失败，状态码: " + response.statusCode(), null);
                if (proxy != null) {
                    markProxyFailed(proxy);
                }
                return null;
            }
        } catch (IOException e) {
            logger.error("网络IO异常: url={}", url, e);
            // 记录完整的异常堆栈
            searchLogger.logRequestFailure(requestId, e);
            if (proxy != null) {
                markProxyFailed(proxy);
                searchLogger.logProxyUsage(requestId, proxy.host + ":" + proxy.port, false);
            }
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("请求被中断: url={}", url, e);
            searchLogger.logRequestFailure(requestId, e);
            if (proxy != null) {
                markProxyFailed(proxy);
                searchLogger.logProxyUsage(requestId, proxy.host + ":" + proxy.port, false);
            }
            return null;
        } catch (Exception e) {
            logger.error("获取URL失败: url={}", url, e);
            // 记录完整的异常堆栈
            searchLogger.logRequestFailure(requestId, e);
            if (proxy != null) {
                markProxyFailed(proxy);
                searchLogger.logProxyUsage(requestId, proxy.host + ":" + proxy.port, false);
            }
            return null;
        }
    }

    /**
     * 创建代理客户端
     */
    private HttpClient createProxyClient(ProxyConfig proxy) {
        return HttpClient.newBuilder()
                .proxy(ProxySelector.of(new InetSocketAddress(proxy.host, proxy.port)))
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * 带日志的百度结果解析
     */
    private SearchResult parseBaiduResultWithLogging(Element item, String requestId) {
        try {
            SearchResult result = new SearchResult();
            result.setId(UUID.randomUUID().toString());

            Element titleElement = item.selectFirst("h3.t");
            if (titleElement != null) {
                result.setTitle(titleElement.text());
                searchLogger.logDataTransformation(requestId, "title", result.getTitle(), true);

                Element linkElement = titleElement.selectFirst("a");
                if (linkElement != null) {
                    result.setUrl(linkElement.attr("href"));
                    searchLogger.logDataTransformation(requestId, "url", result.getUrl(), true);
                }
            }

            Element snippetElement = item.selectFirst("div.c-abstract");
            if (snippetElement != null) {
                result.setSnippet(snippetElement.text());
                searchLogger.logDataTransformation(requestId, "snippet", result.getSnippet(), true);
            }

            Element infoElement = item.selectFirst("span.c-showurl") != null ?
                    item.selectFirst("span.c-showurl") : item.selectFirst("span.newTimeFactor_before_abs");
            if (infoElement != null) {
                result.setSource(infoElement.text());
                searchLogger.logDataTransformation(requestId, "source", result.getSource(), true);
            }

            result.setRelevanceScore(calculateRelevance(result));
            result.setContentType("text");
            result.setCreatedAt(LocalDateTime.now());
            result.setTags(extractTags(result.getSnippet()));

            return validateResult(result) ? result : null;
        } catch (Exception e) {
            searchLogger.logDataTransformation(requestId, "parseBaiduResult", null, false);
            return null;
        }
    }

    /**
     * 带日志的Bing结果解析
     */
    private SearchResult parseBingResultWithLogging(Element item, String requestId) {
        try {
            SearchResult result = new SearchResult();
            result.setId(UUID.randomUUID().toString());

            Element titleElement = item.selectFirst("h2");
            if (titleElement != null) {
                result.setTitle(titleElement.text());
                searchLogger.logDataTransformation(requestId, "title", result.getTitle(), true);

                Element linkElement = titleElement.selectFirst("a");
                if (linkElement != null) {
                    result.setUrl(linkElement.attr("href"));
                    searchLogger.logDataTransformation(requestId, "url", result.getUrl(), true);
                }
            }

            Element snippetElement = item.selectFirst("p");
            if (snippetElement != null) {
                result.setSnippet(snippetElement.text());
                searchLogger.logDataTransformation(requestId, "snippet", result.getSnippet(), true);
            }

            Element infoElement = item.selectFirst("span.b_attribution");
            if (infoElement != null) {
                String info = infoElement.text();
                String[] parts = info.split(" · ");
                if (parts.length > 0) {
                    result.setSource(parts[0]);
                }
                if (parts.length > 1) {
                    result.setPublishTime(parts[1]);
                }
            }

            result.setRelevanceScore(calculateRelevance(result));
            result.setContentType("text");
            result.setCreatedAt(LocalDateTime.now());
            result.setTags(extractTags(result.getSnippet()));

            return validateResult(result) ? result : null;
        } catch (Exception e) {
            searchLogger.logDataTransformation(requestId, "parseBingResult", null, false);
            return null;
        }
    }

    /**
     * 带日志的DuckDuckGo结果解析
     */
    private SearchResult parseDuckDuckGoResultWithLogging(Element item, String requestId) {
        try {
            SearchResult result = new SearchResult();
            result.setId(UUID.randomUUID().toString());

            Element titleElement = item.selectFirst("a.result__a");
            if (titleElement != null) {
                result.setTitle(titleElement.text());
                result.setUrl(titleElement.attr("href"));
            }

            Element snippetElement = item.selectFirst("a.result__snippet");
            if (snippetElement != null) {
                result.setSnippet(snippetElement.text());
            }

            Element infoElement = item.selectFirst("span.result__url");
            if (infoElement != null) {
                result.setSource(infoElement.text());
            }

            result.setRelevanceScore(calculateRelevance(result));
            result.setContentType("text");
            result.setCreatedAt(LocalDateTime.now());
            result.setTags(extractTags(result.getSnippet()));

            return validateResult(result) ? result : null;
        } catch (Exception e) {
            searchLogger.logDataTransformation(requestId, "parseDuckDuckGoResult", null, false);
            return null;
        }
    }

    @Override
    public List<String> generateSearchKeywords(String userQuestion) {
        List<String> keywords = new ArrayList<>();

        // 原始问题作为第一个关键词
        keywords.add(userQuestion);

        // 简单的关键词提取
        String cleaned = userQuestion.toLowerCase()
                .replaceAll("[?？]", "")
                .replaceAll("[！!。,，、]", " ")
                .trim();

        String[] words = cleaned.split("\\s+");
        List<String> importantWords = new ArrayList<>();

        for (String word : words) {
            if (!isStopWord(word)) {
                importantWords.add(word);
            }
        }

        // 生成不同长度的关键词组合
        if (importantWords.size() >= 2) {
            for (int i = 0; i < importantWords.size() - 1; i++) {
                keywords.add(importantWords.get(i) + " " + importantWords.get(i + 1));
            }

            if (importantWords.size() >= 3) {
                keywords.add(importantWords.get(0) + " " + importantWords.get(1) + " " + importantWords.get(2));
            }
        }

        return keywords.stream()
                .distinct()
                .limit(5)
                .collect(Collectors.toList());
    }

    @Override
    public List<SearchResult> verifyResults(List<SearchResult> results) {
        logger.info("开始验证搜索结果，数量: {}", results.size());

        for (SearchResult result : results) {
            boolean verified = verifySingleResult(result);
            result.setVerified(verified);
            result.setVerifiedAt(LocalDateTime.now());

            if (!verified) {
                result.setRelevanceScore(result.getRelevanceScore() * 0.5);
            }
        }

        return results;
    }

    @Override
    public Map<String, Object> extractKeyInformation(List<SearchResult> results) {
        Map<String, Object> extracted = new HashMap<>();

        extracted.put("urls", results.stream().map(SearchResult::getUrl).collect(Collectors.toList()));
        extracted.put("sources", results.stream().map(SearchResult::getSource).distinct().collect(Collectors.toList()));
        extracted.put("timestamps", results.stream()
                .filter(r -> r.getPublishTime() != null && !r.getPublishTime().isEmpty())
                .map(SearchResult::getPublishTime)
                .collect(Collectors.toList()));

        StringBuilder allSnippets = new StringBuilder();
        results.forEach(r -> {
            if (r.getSnippet() != null) {
                allSnippets.append(r.getSnippet()).append("\n\n");
            }
        });
        extracted.put("combinedSummary", allSnippets.toString().trim());

        Map<String, Integer> keywordCounts = new HashMap<>();
        for (SearchResult result : results) {
            if (result.getTags() != null) {
                for (String tag : result.getTags()) {
                    keywordCounts.merge(tag, 1, Integer::sum);
                }
            }
        }
        extracted.put("topKeywords", keywordCounts);

        return extracted;
    }

    @Override
    public List<Map<String, Object>> getSearchHistory(int limit) {
        List<Map<String, Object>> history = new ArrayList<>(searchHistory);
        Collections.reverse(history);
        if (history.size() > limit) {
            history = history.subList(0, limit);
        }
        return history;
    }

    // ========== 辅助方法 ==========

    private List<String> generateSearchUrls(String keyword) {
        List<String> urls = new ArrayList<>();
        urls.add("https://www.baidu.com/s?wd=" + encodeUrl(keyword) + "&rn=20");
        urls.add("https://www.bing.com/search?q=" + encodeUrl(keyword) + "&count=20");
        urls.add("https://duckduckgo.com/html/?q=" + encodeUrl(keyword) + "&kl=zh-CN");
        return urls;
    }

    private List<SearchResult> deduplicateResults(List<SearchResult> results) {
        Set<String> seenUrls = new HashSet<>();
        List<SearchResult> uniqueResults = new ArrayList<>();

        for (SearchResult result : results) {
            String url = result.getUrl();
            if (url != null && !seenUrls.contains(url)) {
                seenUrls.add(url);
                uniqueResults.add(result);
            }
        }

        return uniqueResults;
    }

    private boolean verifySingleResult(SearchResult result) {
        if (result.getUrl() == null || result.getUrl().isEmpty()) {
            return false;
        }

        try {
            URI uri = URI.create(result.getUrl());
            if (!uri.isAbsolute() || (!uri.getScheme().equals("http") && !uri.getScheme().equals("https"))) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        String domain = extractDomain(result.getUrl());
        if (isTrustedDomain(domain)) {
            return true;
        }

        if (result.getSnippet() != null && containsSuspiciousContent(result.getSnippet())) {
            return false;
        }

        return true;
    }

    private boolean validateResult(SearchResult result) {
        if (result.getTitle() == null || result.getTitle().isEmpty() ||
                result.getUrl() == null || result.getUrl().isEmpty()) {
            return false;
        }

        if (result.getTitle().length() < 3 || result.getTitle().length() > 200) {
            return false;
        }

        if (result.getUrl().length() > 2048) {
            return false;
        }

        try {
            URI uri = URI.create(result.getUrl());
            return uri.isAbsolute();
        } catch (Exception e) {
            return false;
        }
    }

    private double calculateRelevance(SearchResult result) {
        double score = 0.5;

        if (result.getTitle() != null && result.getTitle().length() >= 10) {
            score += 0.1;
        }

        if (result.getSnippet() != null && result.getSnippet().length() >= 20) {
            score += 0.15;
        }

        String domain = extractDomain(result.getUrl());
        if (isTrustedDomain(domain)) {
            score += 0.2;
        }

        if (result.getPublishTime() != null) {
            if (result.getPublishTime().contains("小时前") ||
                    result.getPublishTime().contains("分钟前") ||
                    result.getPublishTime().contains("今天")) {
                score += 0.1;
            }
        }

        return Math.min(1.0, Math.max(0.1, score));
    }

    private List<String> extractTags(String text) {
        List<String> tags = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tags;
        }

        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]{2,}");
        Matcher matcher = pattern.matcher(text);

        Set<String> uniqueTags = new HashSet<>();
        while (matcher.find()) {
            String tag = matcher.group();
            if (!isStopWord(tag)) {
                uniqueTags.add(tag);
            }
        }

        tags.addAll(uniqueTags);
        if (tags.size() > 5) {
            tags = tags.subList(0, 5);
        }

        return tags;
    }

    private String extractDomain(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isTrustedDomain(String domain) {
        if (domain == null) {
            return false;
        }

        Set<String> trustedDomains = new HashSet<>(Arrays.asList(
                "baidu.com", "bing.com", "google.com",
                "gov.cn", "edu.cn", "org.cn",
                "github.com", "zh.wikipedia.org", "baike.baidu.com"
        ));

        for (String trusted : trustedDomains) {
            if (domain.equals(trusted) || domain.endsWith("." + trusted)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsSuspiciousContent(String text) {
        Set<String> suspiciousPatterns = new HashSet<>(Arrays.asList(
                "免费领取", "立即下载", "点击这里", "广告",
                "赌博", "色情", "诈骗", "病毒", "木马"
        ));

        for (String pattern : suspiciousPatterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    private boolean isStopWord(String word) {
        Set<String> stopWords = new HashSet<>(Arrays.asList(
                "的", "了", "和", "是", "就", "都", "而", "及", "与",
                "着", "或", "一个", "没有", "我们", "你们", "他们",
                "什么", "怎么", "如何", "为什么", "哪个", "谁", "何时",
                "哪里", "是否", "可以", "能", "会", "应该", "必须"
        ));

        return stopWords.contains(word.toLowerCase());
    }

    private String encodeUrl(String url) {
        try {
            return java.net.URLEncoder.encode(url, "UTF-8");
        } catch (Exception e) {
            return url;
        }
    }

    private void recordSearchHistory(String query, int resultCount, long duration) {
        Map<String, Object> record = new HashMap<>();
        record.put("query", query);
        record.put("resultCount", resultCount);
        record.put("durationMs", duration);
        record.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        searchHistory.add(record);

        while (searchHistory.size() > MAX_HISTORY_SIZE) {
            searchHistory.poll();
        }
    }
}

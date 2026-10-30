package com.example.aichat;

import com.example.aichat.config.SearchLogger;
import com.example.aichat.entity.SearchResult;
import com.example.aichat.service.impl.WebSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 网络搜索服务测试类
 * 测试各种复杂查询条件和搜索场景
 *
 * 本地化方案：不再依赖真实外部搜索源（百度/Bing/DuckDuckGo）与 MySQL/Redis。
 * 直接 new WebSearchServiceImpl，注入真实 SearchLogger（纯日志组件），
 * 并用 Mockito 替换内部 HttpClient，让其按请求 URL 返回本地构造的
 * 百度/Bing/DuckDuckGo HTML 片段。这样 Jsoup 解析、相关度评分、去重排序、
 * 结果验证、信息提取、历史记录等本地逻辑全部真实执行。
 */
public class WebSearchServiceTest {

    private WebSearchServiceImpl webSearchService;

    /** 本地构造的百度结果 HTML */
    private static final String BAIDU_HTML = "<html><body>"
            + "<div class='result-op c-container xpath-log new-pmd'>"
            + "<h3 class='t'><a href='https://www.baidu.com/link?url=abc123'>人工智能最新进展与趋势分析报告</a></h3>"
            + "<div class='c-abstract'>人工智能技术在医疗、教育、金融等领域快速发展，深度学习大模型不断取得新的突破。</div>"
            + "<span class='c-showurl'>百度百科</span>"
            + "</div>"
            + "<div class='result-op c-container xpath-log new-pmd'>"
            + "<h3 class='t'><a href='https://www.baidu.com/link?url=def456'>机器学习基础与实践指南</a></h3>"
            + "<div class='c-abstract'>机器学习是人工智能的核心分支，涵盖监督学习、无监督学习与强化学习等方法。</div>"
            + "<span class='c-showurl'>百度知道</span>"
            + "</div>"
            + "</body></html>";

    /** 本地构造的 Bing 结果 HTML */
    private static final String BING_HTML = "<html><body>"
            + "<li class='b_algo'>"
            + "<h2><a href='https://www.bing.com/search?q=ai'>AI 技术应用综述</a></h2>"
            + "<p>人工智能应用涵盖自然语言处理、计算机视觉、智能推荐等多个方向，产业落地迅速。</p>"
            + "<span class='b_attribution'>Microsoft · 2024年3月15日</span>"
            + "</li>"
            + "<li class='b_algo'>"
            + "<h2><a href='https://www.bing.com/search?q=dl'>深度学习框架对比</a></h2>"
            + "<p>主流深度学习框架包括 TensorFlow、PyTorch 与 PaddlePaddle，各有生态优势。</p>"
            + "<span class='b_attribution'>Bing · 今天</span>"
            + "</li>"
            + "</body></html>";

    /** 本地构造的 DuckDuckGo 结果 HTML */
    private static final String DDG_HTML = "<html><body>"
            + "<div class='result'>"
            + "<a class='result__a' href='https://example.com/ai-intro'>深度学习入门指南</a>"
            + "<a class='result__snippet'>深度学习是机器学习的一个分支，使用多层神经网络进行特征表示学习。</a>"
            + "<span class='result__url'>example.com</span>"
            + "</div>"
            + "</body></html>";

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        webSearchService = new WebSearchServiceImpl();
        // 真实日志组件，无外部依赖
        ReflectionTestUtils.setField(webSearchService, "searchLogger", new SearchLogger());
        // 显式关闭代理（避免走代理分支）
        ReflectionTestUtils.setField(webSearchService, "proxyEnabled", false);

        // Mock 内部 HttpClient：按请求 URL 返回本地构造的搜索引擎 HTML，避免真实外网调用
        HttpClient mockClient = Mockito.mock(HttpClient.class);
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(invocation -> {
                    HttpRequest req = invocation.getArgument(0);
                    String url = req.uri().toString();
                    HttpResponse<String> resp = Mockito.mock(HttpResponse.class);
                    when(resp.statusCode()).thenReturn(200);
                    String html;
                    if (url.contains("baidu")) {
                        html = BAIDU_HTML;
                    } else if (url.contains("bing")) {
                        html = BING_HTML;
                    } else if (url.contains("duckduckgo")) {
                        html = DDG_HTML;
                    } else {
                        html = "<html><body></body></html>";
                    }
                    when(resp.body()).thenReturn(html);
                    return resp;
                });
        // 直接注入 mock client，跳过 initHttpClient() 真实建连
        ReflectionTestUtils.setField(webSearchService, "httpClient", mockClient);
    }

    /**
     * 测试1: 简单关键词搜索
     */
    @Test
    public void testSimpleKeywordSearch() {
        System.out.println("\n========== 测试1: 简单关键词搜索 ==========");

        String query = "人工智能";
        List<SearchResult> results = webSearchService.search(query, 5);

        System.out.println("查询关键词: " + query);
        System.out.println("返回结果数: " + results.size());

        assertNotNull(results, "结果不应为null");
        assertFalse(results.isEmpty(), "mock 搜索源应返回解析结果");
        results.forEach(r -> {
            System.out.println("  - 标题: " + r.getTitle());
            System.out.println("    URL: " + r.getUrl());
            System.out.println("    相关度: " + r.getRelevanceScore());
        });
    }

    /**
     * 测试2: 多关键词组合搜索
     */
    @Test
    public void testMultipleKeywordsSearch() {
        System.out.println("\n========== 测试2: 多关键词组合搜索 ==========");

        String query = "机器学习 深度学习 神经网络";
        List<SearchResult> results = webSearchService.search(query, 10);

        System.out.println("查询关键词: " + query);
        System.out.println("返回结果数: " + results.size());

        assertNotNull(results, "结果不应为null");
        assertTrue(results.size() <= 10, "结果数不应超过限制");
    }

    /**
     * 测试3: 精确匹配搜索（带引号）
     */
    @Test
    public void testExactMatchSearch() {
        System.out.println("\n========== 测试3: 精确匹配搜索 ==========");

        String query = "\"GPT-4\"";
        List<SearchResult> results = webSearchService.search(query, 5);

        System.out.println("查询关键词: " + query);
        System.out.println("返回结果数: " + results.size());

        assertNotNull(results, "结果不应为null");
    }

    /**
     * 测试4: 模糊搜索（带通配符概念）
     */
    @Test
    public void testFuzzySearch() {
        System.out.println("\n========== 测试4: 模糊搜索 ==========");

        String query = "AI 技术 应用";
        List<SearchResult> results = webSearchService.search(query, 8);

        System.out.println("查询关键词: " + query);
        System.out.println("返回结果数: " + results.size());

        assertNotNull(results, "结果不应为null");
    }

    /**
     * 测试5: 时间范围相关搜索
     */
    @Test
    public void testTimeRangeSearch() {
        System.out.println("\n========== 测试5: 时间范围相关搜索 ==========");

        String query = "2024年 科技新闻";
        List<SearchResult> results = webSearchService.search(query, 10);

        System.out.println("查询关键词: " + query);
        System.out.println("返回结果数: " + results.size());

        assertNotNull(results, "结果不应为null");

        // 检查是否包含时间信息
        long withTimeInfo = results.stream()
                .filter(r -> r.getPublishTime() != null && !r.getPublishTime().isEmpty())
                .count();
        System.out.println("包含时间信息的结果数: " + withTimeInfo);
        assertTrue(withTimeInfo > 0, "Bing 结果应带发布时间");
    }

    /**
     * 测试6: 逻辑运算符组合搜索
     */
    @Test
    public void testLogicalOperatorSearch() {
        System.out.println("\n========== 测试6: 逻辑运算符组合搜索 ==========");

        // 测试 AND 逻辑
        String queryAnd = "Python AND 数据分析";
        List<SearchResult> resultsAnd = webSearchService.search(queryAnd, 5);
        System.out.println("AND查询: " + queryAnd + " -> 结果数: " + resultsAnd.size());
        assertNotNull(resultsAnd);

        // 测试 OR 逻辑
        String queryOr = "Java OR Python";
        List<SearchResult> resultsOr = webSearchService.search(queryOr, 5);
        System.out.println("OR查询: " + queryOr + " -> 结果数: " + resultsOr.size());
        assertNotNull(resultsOr);

        // 测试 NOT 逻辑
        String queryNot = "苹果 NOT 水果";
        List<SearchResult> resultsNot = webSearchService.search(queryNot, 5);
        System.out.println("NOT查询: " + queryNot + " -> 结果数: " + resultsNot.size());
        assertNotNull(resultsNot);
    }

    /**
     * 测试7: 专业领域搜索
     */
    @Test
    public void testProfessionalDomainSearch() {
        System.out.println("\n========== 测试7: 专业领域搜索 ==========");

        String[] domains = {
            "医学 诊断 AI",
            "法律 人工智能 规范",
            "金融 科技 创新",
            "教育 在线 学习"
        };

        for (String query : domains) {
            List<SearchResult> results = webSearchService.search(query, 3);
            System.out.println("领域查询: " + query + " -> 结果数: " + results.size());
            assertNotNull(results);
        }
    }

    /**
     * 测试8: 特殊字符处理
     */
    @Test
    public void testSpecialCharactersSearch() {
        System.out.println("\n========== 测试8: 特殊字符处理 ==========");

        String[] queries = {
            "C++ 编程",
            "Node.js 教程",
            "3D 打印技术",
            "B站 热门视频"
        };

        for (String query : queries) {
            try {
                List<SearchResult> results = webSearchService.search(query, 3);
                System.out.println("特殊字符查询: " + query + " -> 结果数: " + results.size());
                assertNotNull(results, "结果不应为null");
            } catch (Exception e) {
                System.out.println("特殊字符查询: " + query + " -> 异常: " + e.getMessage());
            }
        }
    }

    /**
     * 测试9: 长查询字符串
     */
    @Test
    public void testLongQuerySearch() {
        System.out.println("\n========== 测试9: 长查询字符串 ==========");

        String query = "人工智能在医疗健康领域的最新应用发展和未来趋势分析研究报告";
        List<SearchResult> results = webSearchService.search(query, 5);

        System.out.println("长查询: " + query);
        System.out.println("查询长度: " + query.length());
        System.out.println("返回结果数: " + results.size());

        assertNotNull(results, "结果不应为null");
    }

    /**
     * 测试10: 中英文混合搜索
     */
    @Test
    public void testMixedLanguageSearch() {
        System.out.println("\n========== 测试10: 中英文混合搜索 ==========");

        String[] queries = {
            "OpenAI GPT模型",
            "Google 搜索引擎优化 SEO",
            "Apple 苹果公司 最新产品"
        };

        for (String query : queries) {
            List<SearchResult> results = webSearchService.search(query, 5);
            System.out.println("中英文查询: " + query + " -> 结果数: " + results.size());
            assertNotNull(results);
        }
    }

    /**
     * 测试11: 结果验证机制
     */
    @Test
    public void testResultVerification() {
        System.out.println("\n========== 测试11: 结果验证机制 ==========");

        String query = "人工智能最新发展";
        List<SearchResult> results = webSearchService.searchWithVerification(query, 10, true);

        System.out.println("查询关键词: " + query);
        System.out.println("返回结果数: " + results.size());

        // 统计验证通过的结果
        long verifiedCount = results.stream()
                .filter(SearchResult::getVerified)
                .count();

        System.out.println("验证通过的结果数: " + verifiedCount);
        System.out.println("验证通过率: " + (results.isEmpty() ? 0 : (double) verifiedCount / results.size() * 100) + "%");
        assertTrue(verifiedCount > 0, "可信域名(baidu/bing)结果应验证通过");

        // 检查每个结果的验证状态
        results.forEach(r -> {
            System.out.println("  - " + r.getTitle());
            System.out.println("    验证状态: " + (r.getVerified() ? "通过" : "未通过"));
            System.out.println("    相关度: " + r.getRelevanceScore());
        });
    }

    /**
     * 测试12: 关键词生成功能
     */
    @Test
    public void testKeywordGeneration() {
        System.out.println("\n========== 测试12: 关键词生成功能 ==========");

        String[] questions = {
            "什么是机器学习？",
            "如何学习Python编程？",
            "2024年有哪些重要的科技突破？",
            "人工智能在医疗领域的应用有哪些？"
        };

        for (String question : questions) {
            List<String> keywords = webSearchService.generateSearchKeywords(question);
            System.out.println("问题: " + question);
            System.out.println("生成的关键词: " + keywords);
            assertNotNull(keywords, "关键词列表不应为null");
            assertFalse(keywords.isEmpty(), "至少应包含原始问题作为关键词");
            System.out.println();
        }
    }

    /**
     * 测试13: 信息提取功能
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testInformationExtraction() {
        System.out.println("\n========== 测试13: 信息提取功能 ==========");

        String query = "人工智能发展趋势";
        List<SearchResult> results = webSearchService.search(query, 10);

        Map<String, Object> extracted = webSearchService.extractKeyInformation(results);

        System.out.println("查询关键词: " + query);
        System.out.println("提取的URL数量: " + ((List<?>) extracted.get("urls")).size());
        System.out.println("来源数量: " + ((List<?>) extracted.get("sources")).size());
        System.out.println("时间信息数量: " + ((List<?>) extracted.get("timestamps")).size());

        String summary = (String) extracted.get("combinedSummary");
        System.out.println("摘要长度: " + (summary != null ? summary.length() : 0));
        assertFalse(((List<?>) extracted.get("urls")).isEmpty(), "应提取到URL");
        assertNotNull(summary);
    }

    /**
     * 测试14: 搜索历史记录
     */
    @Test
    public void testSearchHistory() {
        System.out.println("\n========== 测试14: 搜索历史记录 ==========");

        // 执行几次搜索
        webSearchService.search("测试查询1", 3);
        webSearchService.search("测试查询2", 5);
        webSearchService.search("测试查询3", 7);

        // 获取历史记录
        List<Map<String, Object>> history = webSearchService.getSearchHistory(10);

        System.out.println("历史记录数量: " + history.size());
        assertTrue(history.size() >= 3, "应记录至少3条搜索历史");
        history.forEach(h -> {
            System.out.println("  - 查询: " + h.get("query"));
            System.out.println("    结果数: " + h.get("resultCount"));
            System.out.println("    耗时: " + h.get("durationMs") + "ms");
            System.out.println("    时间: " + h.get("timestamp"));
        });
    }

    /**
     * 测试15: 边界条件测试
     */
    @Test
    public void testBoundaryConditions() {
        System.out.println("\n========== 测试15: 边界条件测试 ==========");

        // 空查询
        List<SearchResult> emptyResults = webSearchService.search("", 5);
        System.out.println("空查询结果数: " + emptyResults.size());
        assertNotNull(emptyResults);

        // 结果数为0
        List<SearchResult> zeroResults = webSearchService.search("测试", 0);
        System.out.println("结果数限制为0: " + zeroResults.size());
        assertNotNull(zeroResults);

        // 结果数很大
        List<SearchResult> largeResults = webSearchService.search("人工智能", 100);
        System.out.println("结果数限制为100: " + largeResults.size());
        assertNotNull(largeResults);

        // 超长查询
        StringBuilder longQuery = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longQuery.append("关键词").append(i).append(" ");
        }
        List<SearchResult> longQueryResults = webSearchService.search(longQuery.toString().trim(), 5);
        System.out.println("超长查询结果数: " + longQueryResults.size());
        assertNotNull(longQueryResults);
    }

    /**
     * 测试16: 并发搜索测试
     */
    @Test
    public void testConcurrentSearch() throws InterruptedException {
        System.out.println("\n========== 测试16: 并发搜索测试 ==========");

        String[] queries = {"AI", "机器学习", "深度学习", "神经网络", "自然语言处理"};

        Thread[] threads = new Thread[queries.length];

        for (int i = 0; i < queries.length; i++) {
            final String query = queries[i];
            threads[i] = new Thread(() -> {
                List<SearchResult> results = webSearchService.search(query, 3);
                System.out.println("并发查询 [" + Thread.currentThread().getName() + "]: " + query + " -> 结果数: " + results.size());
                assertNotNull(results);
            });
        }

        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("并发测试完成");
    }
}

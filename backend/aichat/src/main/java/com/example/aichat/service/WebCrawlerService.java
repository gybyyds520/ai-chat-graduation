package com.example.aichat.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.entity.DataPool;
import com.example.aichat.entity.SearchResult;
import com.example.aichat.mapper.DataPoolMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WebCrawlerService {

    private static final Logger logger = LoggerFactory.getLogger(WebCrawlerService.class);

    private final DataPoolMapper dataPoolMapper;
    private final WebSearchService webSearchService;

    // 缓存已爬取的URL，避免重复
    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();

    // AI相关关键词列表
    private static final List<String> AI_KEYWORDS = Arrays.asList(
        "人工智能", "AI", "机器学习", "深度学习", "神经网络",
        "大语言模型", "LLM", "ChatGPT", "自然语言处理",
        "计算机视觉", "强化学习", "数据科学"
    );

    public WebCrawlerService(DataPoolMapper dataPoolMapper, WebSearchService webSearchService) {
        this.dataPoolMapper = dataPoolMapper;
        this.webSearchService = webSearchService;
    }

    /**
     * 执行AI资料收集任务
     */
    public Map<String, Object> executeCollection(String keyword, int maxPages) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> collectedData = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        try {
            logger.info("开始收集AI资料，关键词: {}, 最大页数: {}", keyword, maxPages);

            // 搜索相关内容
            for (int page = 1; page <= maxPages; page++) {
                try {
                    // 使用WebSearchService搜索
                    List<SearchResult> searchResults = webSearchService.search(keyword, 10);

                    for (SearchResult searchResult : searchResults) {
                        String url = searchResult.getUrl();
                        String title = searchResult.getTitle();
                        String snippet = searchResult.getSnippet();

                        // 避免重复爬取
                        if (visitedUrls.contains(url)) {
                            continue;
                        }
                        visitedUrls.add(url);

                        // 解析网页内容
                        Map<String, Object> parsedData = parseWebContent(url, title, snippet);
                        if (parsedData != null) {
                            // 保存到数据库
                            saveToDatabase(parsedData);
                            collectedData.add(parsedData);
                            successCount++;
                        } else {
                            failCount++;
                        }

                        // 添加延迟，避免请求过快
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    logger.error("第 {} 页搜索失败", page, e);
                    failCount++;
                }
            }

            result.put("success", true);
            result.put("totalCollected", collectedData.size());
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("data", collectedData);
            logger.info("资料收集完成，成功: {}, 失败: {}", successCount, failCount);

        } catch (Exception e) {
            logger.error("资料收集任务执行失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 解析网页内容（简化版，实际需要更复杂的HTML解析）
     */
    private Map<String, Object> parseWebContent(String url, String title, String snippet) {
        try {
            Map<String, Object> data = new HashMap<>();

            // 基本信息
            data.put("title", title);
            data.put("url", url);
            data.put("summary", snippet);

            // 提取关键词
            List<String> keywords = extractKeywords(title + " " + snippet);
            data.put("keywords", String.join(",", keywords));

            // 判断内容类型
            String contentType = classifyContent(title + " " + snippet);
            data.put("contentType", contentType);

            // 评估重要性
            int importance = evaluateImportance(title, snippet);
            data.put("importance", importance);

            return data;
        } catch (Exception e) {
            logger.error("解析网页内容失败: {}", url, e);
            return null;
        }
    }

    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();

        for (String keyword : AI_KEYWORDS) {
            if (text.contains(keyword)) {
                keywords.add(keyword);
            }
        }

        // 使用正则提取更多关键词（简化版）
        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]{2,}");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find() && keywords.size() < 5) {
            String word = matcher.group();
            if (!keywords.contains(word) && word.length() >= 2) {
                keywords.add(word);
            }
        }

        return keywords;
    }

    /**
     * 分类内容
     */
    private String classifyContent(String text) {
        if (text.contains("新闻") || text.contains("最新")) {
            return "news";
        } else if (text.contains("教程") || text.contains("入门") || text.contains("指南")) {
            return "tutorial";
        } else if (text.contains("论文") || text.contains("研究") || text.contains("学术")) {
            return "research";
        } else if (text.contains("产品") || text.contains("发布") || text.contains("发布")) {
            return "product";
        } else {
            return "general";
        }
    }

    /**
     * 评估重要性
     */
    private int evaluateImportance(String title, String snippet) {
        int score = 50; // 基础分

        // 标题包含重要关键词加分
        if (title != null && (title.contains("突破") || title.contains("重大") || title.contains("最新"))) {
            score += 20;
        }

        // 内容长度加分
        if (snippet != null && snippet.length() > 100) {
            score += 10;
        }

        // 技术术语出现频率
        if (snippet != null) {
            long techTerms = AI_KEYWORDS.stream()
                .filter(keyword -> snippet.contains(keyword))
                .count();
            score += (int) (techTerms * 5);
        }

        return Math.min(score, 100);
    }

    /**
     * 保存到数据库
     */
    private void saveToDatabase(Map<String, Object> data) {
        try {
            DataPool dataPool = new DataPool();
            dataPool.setTitle((String) data.get("title"));
            dataPool.setContent((String) data.get("summary"));
            dataPool.setDataType((String) data.get("contentType"));
            dataPool.setDescription((String) data.get("keywords"));
            dataPool.setVersion((Integer) data.get("importance"));
            dataPool.setIsPublic(0);
            dataPool.setChecksum("web_crawler");

            dataPoolMapper.insert(dataPool);
            logger.debug("数据已保存: {}", data.get("title"));
        } catch (Exception e) {
            logger.error("保存数据失败", e);
        }
    }

    /**
     * 获取收集统计信息
     */
    public Map<String, Object> getCollectionStats() {
        Map<String, Object> stats = new HashMap<>();

        // 统计总数
        long totalCount = dataPoolMapper.selectCount(null);
        stats.put("totalCount", totalCount);

        // 按类型统计
        List<Map<String, Object>> typeStats = new ArrayList<>();
        String[] types = {"news", "tutorial", "research", "product", "general"};
        for (String type : types) {
            QueryWrapper<DataPool> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("category", type);
            long count = dataPoolMapper.selectCount(queryWrapper);
            Map<String, Object> typeStat = new HashMap<>();
            typeStat.put("type", type);
            typeStat.put("count", count);
            typeStats.add(typeStat);
        }
        stats.put("typeStats", typeStats);

        // 已爬取URL数量
        stats.put("visitedUrlsCount", visitedUrls.size());

        return stats;
    }

    /**
     * 搜索已收集的资料
     */
    public Page<DataPool> searchCollectedData(String keyword, int page, int size) {
        Page<DataPool> pageInfo = new Page<>(page, size);
        QueryWrapper<DataPool> queryWrapper = new QueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                .like("title", keyword)
                .or()
                .like("content", keyword)
                .or()
                .like("tags", keyword)
            );
        }

        queryWrapper.orderByDesc("importance", "create_time");

        return dataPoolMapper.selectPage(pageInfo, queryWrapper);
    }

    /**
     * 清除已访问URL缓存
     */
    public void clearVisitedUrlsCache() {
        visitedUrls.clear();
        logger.info("已清除URL访问缓存");
    }
}

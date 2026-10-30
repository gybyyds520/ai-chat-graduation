package com.example.aichat.controller;

import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.dto.search.CitedAnswer;
import com.example.aichat.dto.search.SourceCitation;
import com.example.aichat.entity.SearchResult;
import com.example.aichat.service.SearchCitationService;
import com.example.aichat.service.WebSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 带引用标注的搜索回答控制器。
 *
 * 提供 POST /api/web-search/answer：
 * 接收 query，执行搜索，用模板拼接生成带引用序号 [index] 的摘要回答（不依赖 LLM，离线可测），
 * 返回 answerWithCitations + citations + searchTime。
 *
 * 安全：要求登录（requireUserId），错误不回显内部细节。
 */
@RestController
@RequestMapping("/api/web-search")
@PreAuthorize("isAuthenticated()")
public class WebSearchAnswerController {

    private static final Logger logger = LoggerFactory.getLogger(WebSearchAnswerController.class);

    @Autowired
    private WebSearchService webSearchService;

    @Autowired
    private SearchCitationService searchCitationService;

    /**
     * 生成带引用标注的搜索回答。
     *
     * @param requestBody 含 query，可选 maxResults
     * @return answerWithCitations / citations / searchTime
     */
    @PostMapping("/answer")
    public R<Map<String, Object>> answer(@RequestBody Map<String, Object> requestBody) {
        // 要求登录（requireUserId），防止匿名刷搜索流量
        Long userId = AuthContext.requireUserId();

        long start = System.currentTimeMillis();
        String query = requestBody.get("query") == null ? "" : String.valueOf(requestBody.get("query"));
        if (query.trim().isEmpty()) {
            return R.badRequest("搜索关键词不能为空");
        }
        int maxResults = 5;
        if (requestBody.get("maxResults") instanceof Number) {
            maxResults = ((Number) requestBody.get("maxResults")).intValue();
        }

        try {
            logger.info("带引用回答: userId={}, query={}", userId, query);
            List<SearchResult> results;
            try {
                results = webSearchService.search(query, maxResults);
            } catch (Exception e) {
                // 搜索失败（如离线）时降级为空结果，保证接口稳定
                logger.warn("搜索调用失败，降级为空结果: {}", e.getMessage());
                results = List.of();
            }
            if (results == null) {
                results = List.of();
            }

            // 模板拼接摘要（不依赖 LLM）
            String templateAnswer = buildTemplateAnswer(query, results);

            // 生成带引用标注的回答
            CitedAnswer cited = searchCitationService.buildAnswerWithSources(templateAnswer, results);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("query", query);
            data.put("answerWithCitations", cited.getAnswerWithCitations());
            data.put("citations", cited.getCitations());
            // A1：前端约定的 sources 数组 [{index,title,url,snippet}]，无搜索结果时为空列表
            data.put("sources", searchCitationService.toSourceMaps(cited.getCitations()));
            data.put("totalResults", cited.getCitations().size());
            data.put("searchTime", System.currentTimeMillis() - start);
            return R.success(data);
        } catch (Exception e) {
            logger.error("生成带引用回答失败: {}", e.getMessage(), e);
            return R.error("生成回答失败，请稍后再试");
        }
    }

    /**
     * 用模板把搜索结果拼成摘要回答（不依赖 LLM）。
     */
    private String buildTemplateAnswer(String query, List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "关于「" + query + "」，暂未检索到相关结果，请更换关键词后重试。";
        }
        StringBuilder sb = new StringBuilder("关于「").append(query).append("」，共检索到 ")
                .append(results.size()).append(" 条相关结果，要点如下。");
        int limit = Math.min(3, results.size());
        for (int i = 0; i < limit; i++) {
            SearchResult r = results.get(i);
            String title = r.getTitle() == null ? "" : r.getTitle();
            String snippet = r.getSnippet() == null ? "" : r.getSnippet();
            if (snippet.length() > 80) {
                snippet = snippet.substring(0, 80);
            }
            sb.append((i + 1)).append("、").append(title).append("。").append(snippet);
        }
        return sb.toString();
    }
}

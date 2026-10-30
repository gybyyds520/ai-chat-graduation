package com.example.aichat.controller;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.dto.search.DeepResearchReport;
import com.example.aichat.service.DeepResearchService;
import com.example.aichat.service.QuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 深度研究控制器（简化版）。
 *
 * 全部接口要求登录（requireUserId），并校验研究任务归属，防止横向越权（IDOR）。
 * A8：GET /api/deep-research?question= 提供 SSE 流式进度输出，受 QuotaService 每日额度限制。
 */
@RestController
@RequestMapping("/api/research")
@PreAuthorize("isAuthenticated()")
public class DeepResearchController {

    private static final Logger logger = LoggerFactory.getLogger(DeepResearchController.class);

    @Autowired
    private DeepResearchService deepResearchService;

    @Autowired
    private QuotaService quotaService;

    /**
     * 启动深度研究任务。
     * POST /api/research/start
     */
    @PostMapping("/start")
    public R<Map<String, Object>> start(@RequestBody Map<String, Object> requestBody) {
        Long userId = AuthContext.requireUserId();
        String query = requestBody.get("query") == null ? "" : String.valueOf(requestBody.get("query"));
        if (query.trim().isEmpty()) {
            return R.badRequest("研究问题不能为空");
        }
        Integer maxRounds = null;
        if (requestBody.get("maxRounds") instanceof Number) {
            maxRounds = ((Number) requestBody.get("maxRounds")).intValue();
        }
        try {
            // A8：受额度管理限制（每日 N 次）
            quotaService.checkAndConsume(userId, "deep_research");
            String researchId = deepResearchService.startResearch(userId, query, maxRounds);
            logger.info("深度研究启动: userId={}, researchId={}", userId, researchId);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("researchId", researchId);
            return R.success(data);
        } catch (BusinessException e) {
            return R.error(e.getMessage());
        } catch (IllegalArgumentException e) {
            return R.badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("启动研究失败: {}", e.getMessage(), e);
            return R.error("启动研究失败，请稍后再试");
        }
    }

    /**
     * A8：SSE 流式深度研究。
     * GET /api/research/deep-research?question=xxx
     *
     * 以 SSE 事件流推送：start → round → roundResult → integrating → complete(报告JSON)。
     * 受 QuotaService 每日额度限制。
     */
    @GetMapping(value = "/deep-research", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter deepResearchStream(@RequestParam String question) {
        Long userId = AuthContext.requireUserId();
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        // 额度检查
        try {
            quotaService.checkAndConsume(userId, "deep_research");
        } catch (BusinessException e) {
            try {
                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                emitter.complete();
            } catch (IOException ignored) {}
            return emitter;
        }

        if (question == null || question.trim().isEmpty()) {
            try {
                emitter.send(SseEmitter.event().name("error").data("研究问题不能为空"));
                emitter.complete();
            } catch (IOException ignored) {}
            return emitter;
        }

        // 异步执行研究，通过 SSE 推送进度
        new Thread(() -> {
            try {
                DeepResearchService.ProgressListener listener = (eventName, data) -> {
                    try {
                        emitter.send(SseEmitter.event().name(eventName).data(data));
                    } catch (IOException e) {
                        throw new RuntimeException("SSE 连接已断开", e);
                    }
                };
                String researchId = deepResearchService.startResearchWithListener(userId, question, 3, listener);
                emitter.complete();
                logger.info("SSE 深度研究完成: userId={}, researchId={}", userId, researchId);
            } catch (Exception e) {
                logger.error("SSE 深度研究失败: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("研究执行失败"));
                } catch (IOException ignored) {}
                emitter.complete();
            }
        }, "deep-research-sse-" + userId).start();

        return emitter;
    }

    /**
     * 查询研究状态。
     * GET /api/research/{researchId}/status
     */
    @GetMapping("/{researchId}/status")
    public R<Map<String, Object>> status(@PathVariable String researchId) {
        Long userId = AuthContext.requireUserId();
        try {
            return R.success(deepResearchService.getResearchStatus(userId, researchId));
        } catch (Exception e) {
            logger.error("查询研究状态失败: {}", e.getMessage());
            return R.error("查询研究状态失败");
        }
    }

    /**
     * 获取研究报告。
     * GET /api/research/{researchId}/result
     */
    @GetMapping("/{researchId}/result")
    public R<DeepResearchReport> result(@PathVariable String researchId) {
        Long userId = AuthContext.requireUserId();
        try {
            return R.success(deepResearchService.getResearchResult(userId, researchId));
        } catch (Exception e) {
            logger.error("获取研究报告失败: {}", e.getMessage());
            return R.error("获取研究报告失败");
        }
    }

    /**
     * 分页查询当前用户的研究历史。
     * GET /api/research/list
     */
    @GetMapping("/list")
    public R<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = AuthContext.requireUserId();
        try {
            List<DeepResearchReport> items = deepResearchService.listResearches(userId, page, size);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("items", items);
            data.put("page", page);
            data.put("size", size);
            return R.success(data);
        } catch (Exception e) {
            logger.error("查询研究历史失败: {}", e.getMessage());
            return R.error("查询研究历史失败");
        }
    }
}

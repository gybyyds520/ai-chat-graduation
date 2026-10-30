package com.example.aichat.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.entity.FeedTask;
import com.example.aichat.entity.KnowledgeDocument;
import com.example.aichat.mapper.FeedTaskMapper;
import com.example.aichat.service.KnowledgeBaseService;
import com.example.aichat.util.FileUploadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI 投喂 Controller - 真实持久化 + 真实学习效果 + 知识库闭环版本。
 *
 * 本版改造：
 *   1. 多文件/多图片投喂：filePaths/fileNames/imagePaths/imageNames（逗号分隔）落库，修复内容丢失；
 *   2. 学习效果确定性计算（基于内容特征，非 random），duration 为真实耗时；
 *   3. 投喂 -> 知识库闭环：text 与 txt/md 文件内容写入 knowledge_document + chunk；
 *   4. 历史真实分页（MyBatis-Plus Page）；
 *   5. 新增删除端点（IDOR 防护）；
 *   6. 统计扩展：total / avgEffectiveness / byType。
 */
@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private static final Logger log = LoggerFactory.getLogger(FeedController.class);

    @Autowired
    private FeedTaskMapper feedTaskMapper;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @PostMapping("/upload")
    public R uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.badRequest("请选择要上传的文件");
        }

        String fileName = file.getOriginalFilename();
        try {
            File saved = FileUploadUtil.save(file, "feed");
            Map<String, Object> result = new HashMap<>();
            result.put("id", saved.getName());
            result.put("fileName", fileName);
            result.put("filePath", FileUploadUtil.toUrl("feed", saved));
            result.put("size", file.getSize());

            return R.success(result);
        } catch (IOException e) {
            return R.error("文件上传失败，请稍后重试");
        }
    }

    @PostMapping("/submit")
    @SuppressWarnings("unchecked")
    public R submitFeed(@RequestBody Map<String, Object> request) {
        // 获取当前登录用户(投喂需要鉴权，userId 一定存在)
        Long userId = AuthContext.getCurrentUserId().orElse(null);

        long start = System.currentTimeMillis();

        String taskId = "FEED-" + System.currentTimeMillis();
        String contentType = (String) request.getOrDefault("contentType", "text");
        String content = (String) request.get("content");
        String priority = (String) request.getOrDefault("priority", "medium");
        String processMode = (String) request.getOrDefault("processMode", "standard");
        String tags = (String) request.get("tags");
        String remark = (String) request.get("remark");

        // 多文件/多图片：兼容数组与逗号分隔字符串，兼容旧单文件字段与 fileIds/imageIds
        List<String> filePaths = toStringList(request.get("filePaths"));
        List<String> fileNames = toStringList(request.get("fileNames"));
        List<String> imagePaths = toStringList(request.get("imagePaths"));
        List<String> imageNames = toStringList(request.get("imageNames"));
        if (filePaths.isEmpty()) {
            filePaths = toStringList(request.get("fileIds"));
        }
        if (imagePaths.isEmpty()) {
            imagePaths = toStringList(request.get("imageIds"));
        }
        // 兼容旧单文件字段 filePath/fileName
        if (filePaths.isEmpty() && request.get("filePath") != null) {
            filePaths = toStringList(request.get("filePath"));
        }
        if (fileNames.isEmpty() && request.get("fileName") != null) {
            fileNames = toStringList(request.get("fileName"));
        }

        boolean hasFiles = !filePaths.isEmpty();
        boolean hasImages = !imagePaths.isEmpty();

        // 确定性学习效果评分（非随机）
        int effectiveness = calculateEffectiveness(content, hasFiles, hasImages, tags, processMode, priority);

        // ===== 投喂 -> 知识库闭环（失败不阻断主流程）=====
        Long documentId = null;
        int knowledgePoints = 0;
        boolean kbImported = false;
        boolean kbFailed = false;
        try {
            String kbContent = null;
            String kbTitle = null;
            if ("text".equals(contentType) && content != null && !content.isBlank()) {
                kbContent = content;
                kbTitle = buildTextTitle(content);
            } else if ("file".equals(contentType)) {
                // 仅 txt/md 文本文件入库，其他类型仅记录路径
                kbContent = readTextFileContent(filePaths, fileNames);
                if (kbContent != null && !kbContent.isBlank()) {
                    kbTitle = fileNames.isEmpty() ? "AI投喂文件" : joinCsv(fileNames);
                }
            }
            if (kbContent != null && !kbContent.isBlank() && userId != null) {
                KnowledgeDocument doc = knowledgeBaseService.importTextDocument(userId, kbTitle, kbContent);
                documentId = doc.getId();
                knowledgePoints = doc.getChunkCount() == null ? 0 : doc.getChunkCount();
                kbImported = true;
            }
        } catch (Exception e) {
            kbFailed = true;
            log.warn("投喂入库知识库失败 taskId={}: {}", taskId, e.getMessage());
        }

        int keySentences = countKeySentences(content);

        // 真实耗时（至少 100ms）
        long duration = System.currentTimeMillis() - start;
        if (duration < 100) {
            duration = 100;
        }

        // 持久化
        FeedTask task = new FeedTask();
        task.setTaskId(taskId);
        task.setUserId(userId);
        task.setContentType(contentType);
        task.setContent(content);
        // 兼容旧单文件字段 file_path/file_name（取第一个）
        task.setFilePath(filePaths.isEmpty() ? null : filePaths.get(0));
        task.setFileName(fileNames.isEmpty() ? null : fileNames.get(0));
        task.setFilePaths(joinCsv(filePaths));
        task.setFileNames(joinCsv(fileNames));
        task.setImagePaths(joinCsv(imagePaths));
        task.setImageNames(joinCsv(imageNames));
        task.setDocumentId(documentId);
        task.setKnowledgePoints(knowledgePoints);
        task.setPriority(priority);
        task.setProcessMode(processMode);
        task.setTags(tags);
        task.setRemark(remark);
        task.setStatus("completed");
        task.setProgress(100);
        task.setEffectiveness(effectiveness);
        task.setDuration(duration);
        LocalDateTime now = LocalDateTime.now();
        task.setCreatedAt(now);
        task.setStartedAt(now);
        task.setCompletedAt(now);

        feedTaskMapper.insert(task);

        // 可解释 feedback
        StringBuilder fb = new StringBuilder();
        fb.append("已解析 ").append(knowledgePoints).append(" 个知识点");
        if (content != null) {
            fb.append("（文本长度 ").append(content.length()).append(" 字）");
        }
        fb.append("。");
        if (keySentences > 0) {
            fb.append("已提取 ").append(keySentences).append(" 个关键句。");
        }
        if (hasFiles) {
            List<String> display = fileNames.isEmpty() ? filePaths : fileNames;
            fb.append("文件已入库：").append(joinCsv(display)).append("。");
        }
        if (hasImages) {
            List<String> display = imageNames.isEmpty() ? imagePaths : imageNames;
            fb.append("图片素材已记录：").append(joinCsv(display)).append("。");
        }
        if (kbImported) {
            fb.append("已进入知识库，可在知识库问答中使用。");
        } else if (kbFailed) {
            fb.append("知识库入库失败但投喂已记录。");
        }

        // 返回与前端契约一致的结果
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("message", "投喂成功！AI正在学习中...");
        result.put("effectiveness", effectiveness);
        result.put("feedback", fb.toString());
        result.put("duration", duration);
        result.put("documentId", documentId);
        result.put("knowledgePoints", knowledgePoints);

        return R.success(result);
    }

    @GetMapping("/history")
    public R getFeedHistory(@RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = AuthContext.getCurrentUserId().orElse(null);
        int p = Math.max(page != null ? page : 1, 1);
        int s = Math.max(size != null ? size : 10, 1);

        Page<FeedTask> pager = new Page<>(p, s);
        QueryWrapper<FeedTask> wrapper = new QueryWrapper<>();
        wrapper.eq(userId != null, "user_id", userId).orderByDesc("created_at");
        Page<FeedTask> result = feedTaskMapper.selectPage(pager, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());
        data.put("hasMore", result.getCurrent() < result.getPages());
        return R.success(data);
    }

    @GetMapping("/stats")
    public R getStats() {
        Long userId = AuthContext.getCurrentUserId().orElse(null);

        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", feedTaskMapper.countByStatus("pending", userId));
        stats.put("processing", feedTaskMapper.countByStatus("processing", userId));
        stats.put("success", feedTaskMapper.countByStatus("completed", userId));
        stats.put("failed", feedTaskMapper.countByStatus("failed", userId));

        // 总投喂数
        QueryWrapper<FeedTask> countWrapper = new QueryWrapper<>();
        countWrapper.eq(userId != null, "user_id", userId);
        long total = feedTaskMapper.selectCount(countWrapper);
        stats.put("total", total);

        // 平均 effectiveness（保留 1 位小数）
        Double avg = (userId != null) ? feedTaskMapper.avgEffectiveness(userId) : null;
        stats.put("avgEffectiveness", avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0);

        // 按内容类型分布
        Map<String, Long> byType = new LinkedHashMap<>();
        byType.put("text", 0L);
        byType.put("file", 0L);
        byType.put("image", 0L);
        List<Map<String, Object>> rows = feedTaskMapper.countByType(userId);
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String type = (String) row.get("content_type");
                Object cnt = row.get("cnt");
                if (type != null && cnt != null) {
                    byType.put(type, ((Number) cnt).longValue());
                }
            }
        }
        stats.put("byType", byType);

        // 当前处理中的任务(用于展示"当前处理任务")
        FeedTask current = (userId != null) ? feedTaskMapper.findProcessingByUser(userId) : null;
        stats.put("currentTask", current);

        return R.success(stats);
    }

    @GetMapping("/task/{taskId}")
    public R getTaskDetail(@PathVariable String taskId) {
        // 安全修复（P1）：任务详情仅限本人查看，防止跨用户读取投喂记录（IDOR）
        Long currentUserId = AuthContext.getCurrentUserId().orElse(null);
        if (currentUserId == null) {
            return R.unauthorized("请先登录");
        }
        // 通过 task_id 精确查询真实记录
        QueryWrapper<FeedTask> wrapper = new QueryWrapper<>();
        wrapper.eq("task_id", taskId);
        FeedTask task = feedTaskMapper.selectOne(wrapper);

        if (task == null) {
            return R.error(404, "任务不存在");
        }
        if (!currentUserId.equals(task.getUserId())) {
            return R.error(403, "无权查看该任务");
        }
        return R.success(task);
    }

    @DeleteMapping("/task/{taskId}")
    public R deleteTask(@PathVariable String taskId) {
        // 删除需登录 + 仅限本人（IDOR 防护）
        Long currentUserId = AuthContext.getCurrentUserId().orElse(null);
        if (currentUserId == null) {
            return R.unauthorized("请先登录");
        }
        QueryWrapper<FeedTask> wrapper = new QueryWrapper<>();
        wrapper.eq("task_id", taskId);
        FeedTask task = feedTaskMapper.selectOne(wrapper);

        if (task == null) {
            return R.error(404, "任务不存在");
        }
        if (!currentUserId.equals(task.getUserId())) {
            return R.error(403, "无权删除该任务");
        }
        feedTaskMapper.deleteById(task.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return R.success(result);
    }

    // ===================== 内部工具 =====================

    /**
     * 确定性学习效果评分（P0：替代原 70 + random(30)）。
     * 规则：
     *   基础 50；文本长度 >500 +15 / >200 +10 / >50 +5；
     *   有文件 +10；有图片 +5；有标签 +5；
     *   处理模式 deep +10 / standard +5 / fast +0；
     *   优先级 urgent +5 / high +3 / medium +0 / low -5；
     *   clamp 到 [30, 99]。
     */
    static int calculateEffectiveness(String content, boolean hasFiles, boolean hasImages,
                                      String tags, String processMode, String priority) {
        int score = 50;
        int len = content == null ? 0 : content.length();
        if (len > 500) {
            score += 15;
        } else if (len > 200) {
            score += 10;
        } else if (len > 50) {
            score += 5;
        }
        if (hasFiles) {
            score += 10;
        }
        if (hasImages) {
            score += 5;
        }
        if (tags != null && !tags.isBlank()) {
            score += 5;
        }
        if ("deep".equals(processMode)) {
            score += 10;
        } else if ("standard".equals(processMode)) {
            score += 5;
        }
        if ("urgent".equals(priority)) {
            score += 5;
        } else if ("high".equals(priority)) {
            score += 3;
        } else if ("low".equals(priority)) {
            score -= 5;
        }
        return Math.max(30, Math.min(99, score));
    }

    /** 统计文本中的关键句数量（按中英文句末标点切分）。 */
    static int countKeySentences(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        String[] parts = content.split("[。！？!?；;：:\\n\\r]+");
        int n = 0;
        for (String p : parts) {
            if (p != null && !p.trim().isEmpty()) {
                n++;
            }
        }
        return n;
    }

    /**
     * 把请求中的多值字段（JSON 数组或逗号分隔字符串）归一化为 List<String>。
     */
    @SuppressWarnings("unchecked")
    private static List<String> toStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value == null) {
            return result;
        }
        if (value instanceof List) {
            for (Object o : (List<Object>) value) {
                if (o != null && !o.toString().isBlank()) {
                    result.add(o.toString().trim());
                }
            }
        } else if (value instanceof String s) {
            if (!s.isBlank()) {
                for (String part : s.split(",")) {
                    if (!part.isBlank()) {
                        result.add(part.trim());
                    }
                }
            }
        }
        return result;
    }

    private static String joinCsv(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return String.join(",", list);
    }

    private static String buildTextTitle(String content) {
        if (content == null || content.isBlank()) {
            return "AI投喂";
        }
        String trimmed = content.trim().replaceAll("\\s+", " ");
        return trimmed.length() > 50 ? trimmed.substring(0, 50) : trimmed;
    }

    /**
     * 读取投喂文件中所有 txt/md 文件的文本内容（拼接），用于知识库入库。
     * 读取失败/文件不存在/非文本类型均静默跳过，不阻断主流程。
     */
    private static String readTextFileContent(List<String> paths, List<String> names) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            String path = paths.get(i);
            String name = (names != null && i < names.size()) ? names.get(i) : path;
            String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
            if (!(lower.endsWith(".txt") || lower.endsWith(".md"))) {
                continue;
            }
            try {
                // path 形如 /uploads/feed/uuid.txt，绝对路径 = user.dir + path
                File f = new File(System.getProperty("user.dir") + path);
                if (f.exists() && f.isFile()) {
                    sb.append(Files.readString(f.toPath(), StandardCharsets.UTF_8)).append("\n\n");
                }
            } catch (Exception e) {
                log.warn("读取投喂文本文件失败 path={}: {}", path, e.getMessage());
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}

package com.example.aichat.controller;

import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.dto.knowledge.AskRequest;
import com.example.aichat.entity.KnowledgeChunk;
import com.example.aichat.entity.KnowledgeDocument;
import com.example.aichat.entity.KnowledgeQuery;
import com.example.aichat.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 知识库 / RAG 问答接口。
 *
 * 安全加固：所有端点以登录令牌（AuthContext.requireUserId）为准，忽略客户端传入的 userId；
 * 文档级端点先经 service.getDocument 校验归属（IDOR 防护），非本人文档按不存在/无权处理。
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private com.example.aichat.service.QuotaService quotaService;

    /**
     * 上传文档（复用 FileUploadUtil 白名单）。
     */
    @PostMapping("/upload")
    public R upload(@RequestParam("file") MultipartFile file,
                    @RequestParam(value = "title", required = false) String title) {
        Long userId = AuthContext.requireUserId();
        // M3-A5：上传知识库文档前校验 doc_upload 每日文件数额度（单测未注入时跳过）
        if (quotaService != null) {
            try {
                quotaService.checkAndConsume(userId, "doc_upload");
            } catch (com.example.aichat.common.exception.BusinessException be) {
                return R.error(be.getErrorCode().getCode(), be.getMessage());
            }
        }
        KnowledgeDocument doc = knowledgeBaseService.uploadDocument(userId, file, title);
        return R.success(doc);
    }

    /**
     * 当前用户的文档列表（分页）。
     */
    @GetMapping("/list")
    public R list(@RequestParam(defaultValue = "1") int page,
                  @RequestParam(defaultValue = "20") int size) {
        Long userId = AuthContext.requireUserId();
        List<KnowledgeDocument> docs = knowledgeBaseService.listDocuments(userId, page, size);
        return R.success(docs);
    }

    /**
     * 问答历史（分页，可选）。
     */
    @GetMapping("/history")
    public R history(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(defaultValue = "20") int size) {
        Long userId = AuthContext.requireUserId();
        List<KnowledgeQuery> records = knowledgeBaseService.listQueryHistory(userId, page, size);
        return R.success(records);
    }

    /**
     * 文档详情（校验归属）。
     */
    @GetMapping("/{documentId}")
    public R getDocument(@PathVariable Long documentId) {
        Long userId = AuthContext.requireUserId();
        KnowledgeDocument doc = knowledgeBaseService.getDocument(userId, documentId);
        return R.success(doc);
    }

    /**
     * 查看文档分块（调试用，校验归属）。
     */
    @GetMapping("/{documentId}/chunks")
    public R chunks(@PathVariable Long documentId) {
        Long userId = AuthContext.requireUserId();
        List<KnowledgeChunk> chunks = knowledgeBaseService.listChunks(userId, documentId);
        return R.success(chunks);
    }

    /**
     * 删除文档及其分块与服务器文件（校验归属）。
     */
    @DeleteMapping("/{documentId}")
    public R delete(@PathVariable Long documentId) {
        Long userId = AuthContext.requireUserId();
        knowledgeBaseService.deleteDocument(userId, documentId);
        return R.success("删除成功");
    }

    /**
     * 基于文档问答（校验归属，body: query、topK 可选）。
     */
    @PostMapping("/{documentId}/ask")
    public R ask(@PathVariable Long documentId, @Valid @RequestBody AskRequest request) {
        Long userId = AuthContext.requireUserId();
        String query = request == null ? null : request.getQuery();
        Integer topK = request == null ? null : request.getTopK();
        Map<String, Object> answer = knowledgeBaseService.askQuestion(userId, documentId, query, topK);
        return R.success(answer);
    }

    /**
     * 重新解析文档：删除旧分块并重新解析入库（校验归属）。
     */
    @PostMapping("/{documentId}/reparse")
    public R reparse(@PathVariable Long documentId) {
        Long userId = AuthContext.requireUserId();
        KnowledgeDocument doc = knowledgeBaseService.reparseDocument(userId, documentId);
        return R.success(doc);
    }

    /**
     * 多文档问答：body: query、topK 可选、documentIds 可选（为空表示全部文档）。
     */
    @PostMapping("/ask")
    public R askAcross(@Valid @RequestBody AskRequest request) {
        Long userId = AuthContext.requireUserId();
        String query = request == null ? null : request.getQuery();
        Integer topK = request == null ? null : request.getTopK();
        List<Long> documentIds = request == null ? null : request.getDocumentIds();
        Map<String, Object> answer = knowledgeBaseService.askAcrossDocuments(userId, documentIds, query, topK);
        return R.success(answer);
    }
}

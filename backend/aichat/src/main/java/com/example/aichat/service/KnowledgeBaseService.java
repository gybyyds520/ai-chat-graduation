package com.example.aichat.service;

import com.example.aichat.entity.KnowledgeChunk;
import com.example.aichat.entity.KnowledgeDocument;
import com.example.aichat.entity.KnowledgeQuery;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 知识库 / RAG 问答服务。
 *
 * 全链路：文档上传（复用 FileUploadUtil）-> 文本解析（txt）-> 滑动窗口分块 ->
 * 本地算法检索（关键词重叠评分）-> 离线抽取式问答。
 *
 * 设计约束：
 *   - 完全离线可测，不依赖 Ollama / Redis / MySQL 凭据；
 *   - 检索为纯 Java 本地算法（中文 2-gram + ASCII 词项重叠），不依赖向量库/Embedding；
 *   - 用户只能访问自己的文档（IDOR 防护，所有方法均校验 userId 归属）。
 */
public interface KnowledgeBaseService {

    /** 默认分块字符数 */
    int DEFAULT_CHUNK_SIZE = 500;

    /** 默认分块重叠字符数 */
    int DEFAULT_CHUNK_OVERLAP = 50;

    /** 默认检索 top-k */
    int DEFAULT_TOP_K = 3;

    /**
     * 上传文档：保存文件、创建文档记录、同步解析并分块入库。
     *
     * @param userId 当前登录用户 ID
     * @param file   上传文件（复用 FileUploadUtil 白名单）
     * @param title  文档标题（可空，缺省取原始文件名）
     * @return 创建好的文档记录（status=ready 或 failed）
     */
    KnowledgeDocument uploadDocument(Long userId, MultipartFile file, String title);

    /**
     * 以纯文本内容导入文档（投喂闭环专用，无 MultipartFile）。
     *
     * 直接把字符串内容作为文档正文：创建 knowledge_document（fileType=text，status=ready），
     * 按滑动窗口分块写入 knowledge_chunk。归属=当前用户。
     *
     * @param userId  当前登录用户 ID
     * @param title   文档标题（可空，缺省自动生成）
     * @param content 文档纯文本内容
     * @return 创建好的文档记录（status=ready，chunkCount=实际分块数）
     */
    KnowledgeDocument importTextDocument(Long userId, String title, String content);

    /**
     * 分页查询用户自己的文档列表。
     */
    List<KnowledgeDocument> listDocuments(Long userId, int page, int size);

    /**
     * 获取单个文档（校验归属，非本人抛出 FORBIDDEN）。
     */
    KnowledgeDocument getDocument(Long userId, Long documentId);

    /**
     * 删除文档及其全部分块与服务器文件（校验归属）。
     */
    void deleteDocument(Long userId, Long documentId);

    /**
     * 解析文档为纯文本。
     * txt：按 UTF-8 直接读取；pdf/docx：未引入解析依赖时抛出不支持异常。
     */
    String parseText(String filePath, String fileType);

    /**
     * 文本滑动窗口分块（默认 chunkSize=500，overlap=50）。
     * 仅生成分块对象并入库由调用方完成；本方法只负责切分与赋值。
     */
    List<KnowledgeChunk> chunkText(Long documentId, Long userId, String text, int chunkSize, int overlap);

    /**
     * 在指定文档的分块中检索与 query 最相关的 top-k 片段。
     *
     * @param userId     用户 ID（隔离）
     * @param documentId 文档 ID
     * @param query      用户问题
     * @param topK       返回数量
     * @return 按相似度降序排列的分块（仅返回正相关片段）
     */
    List<KnowledgeChunk> retrieveChunks(Long userId, Long documentId, String query, int topK);

    /**
     * 独立计算 query 与单块文本的相似度分数 [0,1]。
     * 相同文本为 1.0，完全无重叠为 0.0。
     */
    double computeSimilarity(String query, String chunkContent);

    /**
     * 基于文档问答：检索 top-k 片段 -> 抽取式回答 -> 落库问答记录。
     *
     * @return 含 answer、sourceChunks、documentId、query 的结果 Map
     */
    Map<String, Object> askQuestion(Long userId, Long documentId, String query, Integer topK);

    /**
     * 查看文档全部分块（调试用，校验归属）。
     */
    List<KnowledgeChunk> listChunks(Long userId, Long documentId);

    /**
     * 问答历史记录（可选）。
     */
    List<KnowledgeQuery> listQueryHistory(Long userId, int page, int size);

    /**
     * 重新解析文档：删除旧分块 -> 重新解析文件 -> 重新分块入库。
     * 纯文本导入（无服务器文件）的文档不支持重新解析。
     *
     * @return 更新后的文档记录
     */
    KnowledgeDocument reparseDocument(Long userId, Long documentId);

    /**
     * 多文档问答：在指定文档集合（或全部文档）中检索 top-k 片段并抽取式回答。
     *
     * @param userId      当前用户
     * @param documentIds 文档 ID 列表；为 null/空表示在该用户全部 ready 文档中检索
     * @param query       问题
     * @param topK        检索片段数（可选）
     * @return 含 answer、sourceChunks、documentIds、query 的结果 Map
     */
    Map<String, Object> askAcrossDocuments(Long userId, List<Long> documentIds, String query, Integer topK);
}

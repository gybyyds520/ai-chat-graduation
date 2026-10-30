package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.KnowledgeErrorCode;
import com.example.aichat.dto.knowledge.ChunkInfo;
import com.example.aichat.entity.KnowledgeChunk;
import com.example.aichat.entity.KnowledgeDocument;
import com.example.aichat.entity.KnowledgeQuery;
import com.example.aichat.mapper.KnowledgeChunkMapper;
import com.example.aichat.mapper.KnowledgeDocumentMapper;
import com.example.aichat.mapper.KnowledgeQueryMapper;
import com.example.aichat.event.UserDataCaptureEvent;
import com.example.aichat.service.KnowledgeBaseService;
import com.example.aichat.service.VectorSearchClient;
import com.example.aichat.util.FileUploadUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识库 / RAG 问答服务实现。
 *
 * 离线可测：检索与回答均为纯 Java 本地算法，不依赖 Ollama / Redis / 向量库。
 * 生产环境如需生成式回答，可在 {@link #buildExtractiveAnswer} 处预留与
 * 现有 ChatService / OllamaService 的集成点。
 */
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseServiceImpl.class);

    /** 上传文件保存子目录 */
    private static final String UPLOAD_SUBDIR = "knowledge";

    /** 抽取式回答最多引用的句子数 */
    private static final int MAX_ANSWER_SENTENCES = 3;

    /** 引用片段在响应中截断的长度 */
    private static final int CHUNK_PREVIEW_LENGTH = 300;

    /** 中文/ASCII 词项切分正则 */
    private static final Pattern ASCII_WORD = Pattern.compile("[a-z0-9]+");
    private static final Pattern CHINESE_RUN = Pattern.compile("[\\u4e00-\\u9fa5]+");

    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final KnowledgeQueryMapper queryMapper;
    private final ObjectMapper objectMapper;

    public KnowledgeBaseServiceImpl(KnowledgeDocumentMapper documentMapper,
                                    KnowledgeChunkMapper chunkMapper,
                                    KnowledgeQueryMapper queryMapper) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.queryMapper = queryMapper;
        this.objectMapper = new ObjectMapper();
    }

    /** 数据池自动采集事件发布器。 */
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 向量检索客户端（仅 aichat.vector.enabled=true 时由容器注入；否则为 null，走 2-gram 兜底）。
     * 见 docs/向量检索优化方案.md：向量召回为旁路，2-gram 始终保留为降级路径。
     */
    @Autowired(required = false)
    private VectorSearchClient vectorSearchClient;

    /** 向量检索总开关，默认关闭以保证离线测试基线不回退。 */
    @Value("${aichat.vector.enabled:false}")
    private boolean vectorEnabled;

    // ===================== 文档管理 =====================

    @Override
    public KnowledgeDocument uploadDocument(Long userId, MultipartFile file, String title) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(KnowledgeErrorCode.KB_FILE_EMPTY);
        }

        // 复用 FileUploadUtil 白名单与路径穿越防护
        File savedFile;
        try {
            savedFile = FileUploadUtil.save(file, UPLOAD_SUBDIR);
        } catch (IOException e) {
            // 空文件 / 非法扩展名等不回显内部细节，统一为业务错误
            log.warn("知识库文档上传被拒: {}", e.getMessage());
            throw new BusinessException(KnowledgeErrorCode.KB_FILE_EMPTY, "文档上传失败，请检查文件是否为空或类型是否受支持");
        }

        String originalName = file.getOriginalFilename();
        String fileType = detectFileType(originalName);

        LocalDateTime now = LocalDateTime.now();
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setUserId(userId);
        doc.setFileName(originalName);
        doc.setFileType(fileType);
        doc.setFileSize(file.getSize());
        doc.setFilePath(savedFile.getAbsolutePath());
        doc.setTitle((title == null || title.isBlank()) ? originalName : title.trim());
        doc.setChunkCount(0);
        doc.setStatus("processing");
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);
        documentMapper.insert(doc);

        try {
            String text = parseText(doc.getFilePath(), fileType);
            List<KnowledgeChunk> chunks = chunkText(doc.getId(), userId, text,
                    DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
            for (KnowledgeChunk chunk : chunks) {
                chunkMapper.insert(chunk);
            }
            doc.setChunkCount(chunks.size());
            doc.setStatus("ready");
            doc.setErrorMessage(null);
            // 旁路向量入库（best-effort，失败不阻断；开关关闭或 sidecar 未注入时为空操作）
            indexVectors(doc.getId(), userId, chunks);
        } catch (BusinessException be) {
            // 不支持的格式等：标记失败并向上抛出，不回显内部堆栈
            doc.setStatus("failed");
            doc.setErrorMessage(truncate(be.getMessage(), 500));
            documentMapper.updateById(doc);
            throw be;
        } catch (Exception e) {
            log.error("知识库文档解析失败 id={}", doc.getId(), e);
            doc.setStatus("failed");
            doc.setErrorMessage(truncate("文档解析失败", 500));
            documentMapper.updateById(doc);
            throw new BusinessException(KnowledgeErrorCode.KB_PARSE_FAILED);
        }

        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);
        return doc;
    }

    @Override
    public KnowledgeDocument importTextDocument(Long userId, String title, String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(KnowledgeErrorCode.KB_QUERY_BLANK, "导入文本内容不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        String docTitle = (title == null || title.isBlank())
                ? "AI投喂-" + now.toString().replace('T', ' ')
                : title.trim();

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setUserId(userId);
        doc.setFileName(docTitle);
        doc.setFileType("text");
        doc.setFileSize((long) content.getBytes(StandardCharsets.UTF_8).length);
        doc.setFilePath(null); // 纯文本导入无服务器文件
        doc.setTitle(docTitle);
        doc.setChunkCount(0);
        doc.setStatus("ready");
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);
        documentMapper.insert(doc);

        List<KnowledgeChunk> chunks = chunkText(doc.getId(), userId, content,
                DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
        for (KnowledgeChunk chunk : chunks) {
            chunkMapper.insert(chunk);
        }
        doc.setChunkCount(chunks.size());
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);
        return doc;
    }

    @Override
    public List<KnowledgeDocument> listDocuments(Long userId, int page, int size) {
        int p = Math.max(1, page);
        int s = Math.max(1, Math.min(100, size));
        Page<KnowledgeDocument> pager = new Page<>(p, s);
        QueryWrapper<KnowledgeDocument> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("created_at");
        return documentMapper.selectPage(pager, wrapper).getRecords();
    }

    @Override
    public KnowledgeDocument getDocument(Long userId, Long documentId) {
        KnowledgeDocument doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw new BusinessException(KnowledgeErrorCode.KB_DOC_NOT_FOUND);
        }
        if (!userId.equals(doc.getUserId())) {
            // IDOR 防护：非本人文档一律按不存在处理，避免枚举探测
            throw new BusinessException(KnowledgeErrorCode.KB_DOC_FORBIDDEN);
        }
        return doc;
    }

    @Override
    public void deleteDocument(Long userId, Long documentId) {
        KnowledgeDocument doc = getDocument(userId, documentId);
        // 删除分块（按文档 + 用户双重条件，防止跨用户误删）
        QueryWrapper<KnowledgeChunk> wrapper = new QueryWrapper<>();
        wrapper.eq("document_id", documentId).eq("user_id", userId);
        chunkMapper.delete(wrapper);
        documentMapper.deleteById(documentId);
        // 删除服务器文件（忽略不存在/删除失败，不阻断主流程）
        try {
            if (doc.getFilePath() != null) {
                Files.deleteIfExists(new File(doc.getFilePath()).toPath());
            }
        } catch (IOException | SecurityException e) {
            log.warn("删除知识库服务器文件失败 path={}: {}", doc.getFilePath(), e.getMessage());
        }
    }

    // ===================== 文本解析与分块 =====================

    @Override
    public String parseText(String filePath, String fileType) {
        String type = fileType == null ? "" : fileType.toLowerCase(Locale.ROOT);
        if ("pdf".equals(type)) {
            return parsePdfText(filePath);
        }
        if ("docx".equals(type) || "doc".equals(type)) {
            return parseDocxText(filePath);
        }
        if ("xlsx".equals(type) || "xls".equals(type)) {
            return parseXlsxText(filePath);
        }
        if ("pptx".equals(type) || "ppt".equals(type)) {
            return parsePptxText(filePath);
        }
        // txt / md 均按 UTF-8 纯文本读取
        try {
            return Files.readString(new File(filePath).toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(KnowledgeErrorCode.KB_PARSE_FAILED, "读取文档内容失败");
        }
    }

    /**
     * 使用 Apache PDFBox 抽取 PDF 纯文本。
     * 抽取失败（文件损坏 / 加密 / 非 PDF）统一包装为 KB_PARSE_FAILED，不回显内部堆栈。
     */
    private String parsePdfText(String filePath) {
        File pdfFile = new File(filePath);
        if (!pdfFile.exists() || !pdfFile.isFile()) {
            throw new BusinessException(KnowledgeErrorCode.KB_PARSE_FAILED, "PDF 文件不存在或不可读");
        }
        try (org.apache.pdfbox.pdmodel.PDDocument document =
                     org.apache.pdfbox.Loader.loadPDF(pdfFile)) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            // 按自然阅读顺序输出，抑制页眉页脚噪声
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                throw new BusinessException(KnowledgeErrorCode.KB_PARSE_FAILED,
                        "PDF 未抽取到任何文本（可能为扫描件图片，暂不支持 OCR）");
            }
            return text;
        } catch (java.io.IOException e) {
            log.warn("PDF 解析失败 path={}: {}", filePath, e.getMessage());
            throw new BusinessException(KnowledgeErrorCode.KB_PARSE_FAILED, "PDF 解析失败，请确认文件未加密且未损坏");
        }
    }

    /**
     * 使用 Apache POI XWPFDocument 抽取 .docx Word 纯文本。
     * 段落 + 表格单元格文本按阅读顺序拼接；损坏文件统一包装为 KB_PARSE_FAILED。
     */
    private String parseDocxText(String filePath) {
        File docxFile = new File(filePath);
        if (!docxFile.exists() || !docxFile.isFile()) {
            throw new BusinessException(KnowledgeErrorCode.KB_PARSE_FAILED, "Word 文件不存在或不可读");
        }
        try (org.apache.poi.xwpf.usermodel.XWPFDocument document =
                     new org.apache.poi.xwpf.usermodel.XWPFDocument(
                             new java.io.FileInputStream(docxFile))) {
            StringBuilder sb = new StringBuilder();
            // 段落
            for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : document.getParagraphs()) {
                if (p != null && p.getText() != null) {
                    sb.append(p.getText()).append("\n");
                }
            }
            // 表格
            for (org.apache.poi.xwpf.usermodel.XWPFTable table : document.getTables()) {
                for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : table.getRows()) {
                    for (org.apache.poi.xwpf.usermodel.XWPFTableCell cell : row.getTableCells()) {
                        if (cell != null && cell.getText() != null) {
                            sb.append(cell.getText()).append("\t");
                        }
                    }
                    sb.append("\n");
                }
            }
            String text = sb.toString().trim();
            if (text.isEmpty()) {
                throw new BusinessException(KnowledgeErrorCode.KB_PARSE_FAILED,
                        "Word 文档未抽取到任何文本");
            }
            return text;
        } catch (Exception e) {
            log.warn("DOCX 解析失败 path={}: {}", filePath, e.getMessage());
            throw new BusinessException(KnowledgeErrorCode.KB_PARSE_FAILED, "Word 文档解析失败，请确认文件未损坏");
        }
    }

    /**
     * 使用 Apache POI XSSFWorkbook 抽取 .xlsx Excel 纯文本。
     * 按 sheet 遍历所有单元格，以制表符/换行拼接；空单元格跳过。
     */
    private String parseXlsxText(String filePath) {
        File xlsxFile = new File(filePath);
        if (!xlsxFile.exists() || !xlsxFile.isFile()) {
            throw new BusinessException(KnowledgeErrorCode.KB_PARSE_FAILED, "Excel 文件不存在或不可读");
        }
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
                     new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                             new java.io.FileInputStream(xlsxFile))) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.getSheetAt(i);
                if (sheet == null) continue;
                sb.append("## Sheet: ").append(sheet.getSheetName()).append("\n");
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    if (row == null) continue;
                    StringBuilder rowText = new StringBuilder();
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        if (cell == null) continue;
                        String v;
                        try {
                            v = switch (cell.getCellType()) {
                                case STRING -> cell.getStringCellValue();
                                case NUMERIC -> String.valueOf(cell.getNumericCellValue());
                                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                                case FORMULA -> cell.getCellFormula();
                                default -> "";
                            };
                        } catch (Exception ex) {
                            v = "";
                        }
                        if (v != null && !v.isEmpty()) {
                            if (rowText.length() > 0) rowText.append("\t");
                            rowText.append(v);
                        }
                    }
                    if (rowText.length() > 0) {
                        sb.append(rowText).append("\n");
                    }
                }
            }
            String text = sb.toString().trim();
            if (text.isEmpty()) {
                throw new BusinessException(KnowledgeErrorCode.KB_PARSE_FAILED,
                        "Excel 文档未抽取到任何文本");
            }
            return text;
        } catch (Exception e) {
            log.warn("XLSX 解析失败 path={}: {}", filePath, e.getMessage());
            throw new BusinessException(KnowledgeErrorCode.KB_PARSE_FAILED, "Excel 文档解析失败，请确认文件未损坏");
        }
    }

    /**
     * 使用 Apache POI XMLSlideShow 抽取 .pptx PowerPoint 纯文本。
     * 遍历每张幻灯片的文本框，按页拼接。
     */
    private String parsePptxText(String filePath) {
        File pptxFile = new File(filePath);
        if (!pptxFile.exists() || !pptxFile.isFile()) {
            throw new BusinessException(KnowledgeErrorCode.KB_PARSE_FAILED, "PPT 文件不存在或不可读");
        }
        try (org.apache.poi.xslf.usermodel.XMLSlideShow ppt =
                     new org.apache.poi.xslf.usermodel.XMLSlideShow(
                             new java.io.FileInputStream(pptxFile))) {
            StringBuilder sb = new StringBuilder();
            int pageNum = 1;
            for (org.apache.poi.xslf.usermodel.XSLFSlide slide : ppt.getSlides()) {
                sb.append("## Slide ").append(pageNum++).append("\n");
                for (org.apache.poi.xslf.usermodel.XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof org.apache.poi.xslf.usermodel.XSLFTextShape textShape) {
                        for (org.apache.poi.xslf.usermodel.XSLFTextParagraph para : textShape.getTextParagraphs()) {
                            String t = para.getText();
                            if (t != null && !t.isBlank()) {
                                sb.append(t).append("\n");
                            }
                        }
                    }
                }
            }
            String text = sb.toString().trim();
            if (text.isEmpty()) {
                throw new BusinessException(KnowledgeErrorCode.KB_PARSE_FAILED,
                        "PPT 文档未抽取到任何文本");
            }
            return text;
        } catch (Exception e) {
            log.warn("PPTX 解析失败 path={}: {}", filePath, e.getMessage());
            throw new BusinessException(KnowledgeErrorCode.KB_PARSE_FAILED, "PPT 文档解析失败，请确认文件未损坏");
        }
    }

    @Override
    public List<KnowledgeChunk> chunkText(Long documentId, Long userId, String text,
                                         int chunkSize, int overlap) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        String trimmed = text;
        int size = chunkSize > 0 ? chunkSize : DEFAULT_CHUNK_SIZE;
        int step = (overlap >= 0 && overlap < size) ? size - overlap : size;

        LocalDateTime now = LocalDateTime.now();
        int index = 0;
        int start = 0;
        int len = trimmed.length();
        while (start < len) {
            int end = Math.min(start + size, len);
            String piece = trimmed.substring(start, end);
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocumentId(documentId);
            chunk.setUserId(userId);
            chunk.setChunkIndex(index++);
            chunk.setContent(piece);
            chunk.setTokenCount(piece.length());
            chunk.setCreatedAt(now);
            chunks.add(chunk);
            if (end >= len) {
                break;
            }
            start += step;
        }
        return chunks;
    }

    // ===================== 检索 =====================

    @Override
    public double computeSimilarity(String query, String chunkContent) {
        Set<String> qTokens = new HashSet<>(tokenize(query));
        if (qTokens.isEmpty()) {
            return 0.0;
        }
        Set<String> cTokens = new HashSet<>(tokenize(chunkContent));
        int overlap = 0;
        for (String t : qTokens) {
            if (cTokens.contains(t)) {
                overlap++;
            }
        }
        return (double) overlap / qTokens.size();
    }

    @Override
    public List<KnowledgeChunk> retrieveChunks(Long userId, Long documentId, String query, int topK) {
        getDocument(userId, documentId); // 归属校验
        int k = topK > 0 ? topK : DEFAULT_TOP_K;
        List<KnowledgeChunk> all = chunkMapper.selectByDocumentId(documentId);
        if (all == null || all.isEmpty()) {
            return new ArrayList<>();
        }

        // 向量召回路径（aichat.vector.enabled=true 且 sidecar 可用时）：
        // 用语义 embedding 召回 top-k，失败/空结果自动降级到下方 2-gram 关键词检索。
        if (vectorEnabled && vectorSearchClient != null) {
            List<KnowledgeChunk> vectorResult = retrieveByVector(userId, documentId, query, k, all);
            if (!vectorResult.isEmpty()) {
                return vectorResult;
            }
            log.debug("向量召回为空，降级到 2-gram 关键词检索 docId={}", documentId);
        }

        // 按相似度降序，仅保留正相关片段
        List<ScoredChunk> scored = new ArrayList<>();
        for (KnowledgeChunk c : all) {
            // 二次隔离：仅检索本人分块
            if (!userId.equals(c.getUserId())) {
                continue;
            }
            double score = computeSimilarity(query, c.getContent());
            if (score > 0) {
                scored.add(new ScoredChunk(c, score));
            }
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        List<KnowledgeChunk> result = new ArrayList<>();
        for (int i = 0; i < Math.min(k, scored.size()); i++) {
            result.add(scored.get(i).chunk);
        }
        return result;
    }

    /**
     * 向量召回：调用 sidecar 取 embedding top-k，按命中顺序重建 KnowledgeChunk。
     * sidecar 侧已强制 user_id + document_id 过滤，此处不做跨用户兜底；
     * 若 sidecar 返回空或异常，返回空列表由调用方降级到 2-gram。
     */
    private List<KnowledgeChunk> retrieveByVector(Long userId, Long documentId, String query,
                                                  int k, List<KnowledgeChunk> all) {
        try {
            List<VectorSearchClient.Hit> hits = vectorSearchClient.query(userId, documentId, query, k);
            if (hits == null || hits.isEmpty()) {
                return new ArrayList<>();
            }
            // 建立 chunkId -> 本地 chunk 映射，回填 id / userId / createdAt 等元数据
            Map<Long, KnowledgeChunk> byId = new HashMap<>();
            for (KnowledgeChunk c : all) {
                if (c.getId() != null) {
                    byId.put(c.getId(), c);
                }
            }
            List<KnowledgeChunk> result = new ArrayList<>();
            for (VectorSearchClient.Hit h : hits) {
                KnowledgeChunk base = byId.get(Long.valueOf(h.chunkId));
                KnowledgeChunk ch = base != null ? base : new KnowledgeChunk();
                // sidecar 返回的 content 为权威文本，确保与向量库一致
                ch.setContent(h.content);
                ch.setChunkIndex(h.chunkIndex);
                ch.setDocumentId(documentId);
                ch.setUserId(userId);
                result.add(ch);
            }
            return result;
        } catch (Exception e) {
            log.warn("向量召回异常(降级到2-gram) docId={}: {}", documentId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * best-effort 向量入库：开关关闭 / sidecar 未注入 / 调用失败时静默跳过，
     * 不影响 MySQL 主流程（2-gram 兜底始终可用）。
     */
    private void indexVectors(Long documentId, Long userId, List<KnowledgeChunk> chunks) {
        if (!vectorEnabled || vectorSearchClient == null || chunks == null || chunks.isEmpty()) {
            return;
        }
        List<long[]> idIndex = new ArrayList<>();
        List<String> contents = new ArrayList<>();
        for (KnowledgeChunk c : chunks) {
            if (c.getId() == null || c.getContent() == null || c.getContent().isBlank()) {
                continue;
            }
            long idx = c.getChunkIndex() == null ? 0L : c.getChunkIndex();
            idIndex.add(new long[]{c.getId(), idx});
            contents.add(c.getContent());
        }
        if (!idIndex.isEmpty()) {
            vectorSearchClient.ingest(userId, documentId, idIndex, contents);
        }
    }
    @Override
    public Map<String, Object> askQuestion(Long userId, Long documentId, String query, Integer topK) {
        getDocument(userId, documentId);
        if (query == null || query.isBlank()) {
            throw new BusinessException(KnowledgeErrorCode.KB_QUERY_BLANK);
        }
        int k = (topK == null || topK <= 0) ? DEFAULT_TOP_K : topK;

        List<KnowledgeChunk> topChunks = retrieveChunks(userId, documentId, query, k);

        // 构建注入上下文（生产环境可将片段拼成 prompt 交给 ChatService/OllamaService）
        StringBuilder contextBuilder = new StringBuilder();
        for (KnowledgeChunk c : topChunks) {
            contextBuilder.append(c.getContent()).append("\n");
        }

        String answer = buildExtractiveAnswer(query, topChunks);

        List<ChunkInfo> sourceChunks = new ArrayList<>();
        List<Long> sourceIds = new ArrayList<>();
        for (KnowledgeChunk c : topChunks) {
            sourceChunks.add(new ChunkInfo(c.getChunkIndex(), truncate(c.getContent(), CHUNK_PREVIEW_LENGTH),
                    computeSimilarity(query, c.getContent())));
            if (c.getId() != null) {
                sourceIds.add(c.getId());
            }
        }

        // 落库问答记录（sourceChunks 存 JSON）
        try {
            KnowledgeQuery record = new KnowledgeQuery();
            record.setUserId(userId);
            record.setDocumentId(documentId);
            record.setQuery(query);
            record.setAnswer(answer);
            record.setSourceChunks(objectMapper.writeValueAsString(sourceIds));
            record.setCreatedAt(LocalDateTime.now());
            queryMapper.insert(record);
        } catch (IOException e) {
            log.warn("保存知识库问答记录失败: {}", e.getMessage());
        }

        // 数据池自动采集：知识库查询问题（不记录完整回答）
        if (eventPublisher != null) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("documentId", documentId);
            eventPublisher.publishEvent(new UserDataCaptureEvent(
                    userId, "knowledge_query", query, meta,
                    "kbq:" + documentId + ":" + query.hashCode(),
                    LocalDateTime.now()));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("answer", answer);
        result.put("sourceChunks", sourceChunks);
        result.put("documentId", documentId);
        result.put("query", query);
        return result;
    }

    @Override
    public List<KnowledgeChunk> listChunks(Long userId, Long documentId) {
        getDocument(userId, documentId);
        List<KnowledgeChunk> all = chunkMapper.selectByDocumentId(documentId);
        List<KnowledgeChunk> filtered = new ArrayList<>();
        if (all != null) {
            for (KnowledgeChunk c : all) {
                if (userId.equals(c.getUserId())) {
                    filtered.add(c);
                }
            }
        }
        return filtered;
    }

    @Override
    public List<KnowledgeQuery> listQueryHistory(Long userId, int page, int size) {
        int p = Math.max(1, page);
        int s = Math.max(1, Math.min(100, size));
        Page<KnowledgeQuery> pager = new Page<>(p, s);
        QueryWrapper<KnowledgeQuery> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("created_at");
        return queryMapper.selectPage(pager, wrapper).getRecords();
    }

    // ===================== 重新解析 / 多文档问答 =====================

    @Override
    public KnowledgeDocument reparseDocument(Long userId, Long documentId) {
        KnowledgeDocument doc = getDocument(userId, documentId); // 归属校验
        if (doc.getFilePath() == null || doc.getFilePath().isBlank()) {
            throw new BusinessException(KnowledgeErrorCode.KB_PARSE_FAILED,
                    "纯文本导入的文档无服务器文件，不支持重新解析");
        }
        // 标记处理中
        doc.setStatus("processing");
        doc.setErrorMessage(null);
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);

        // 删除旧分块（文档 + 用户双重条件）
        QueryWrapper<KnowledgeChunk> del = new QueryWrapper<>();
        del.eq("document_id", documentId).eq("user_id", userId);
        chunkMapper.delete(del);

        try {
            String text = parseText(doc.getFilePath(), doc.getFileType());
            List<KnowledgeChunk> chunks = chunkText(documentId, userId, text,
                    DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
            for (KnowledgeChunk chunk : chunks) {
                chunkMapper.insert(chunk);
            }
            doc.setChunkCount(chunks.size());
            doc.setStatus("ready");
            doc.setErrorMessage(null);
            indexVectors(documentId, userId, chunks);
        } catch (BusinessException be) {
            doc.setStatus("failed");
            doc.setErrorMessage(truncate(be.getMessage(), 500));
            documentMapper.updateById(doc);
            throw be;
        } catch (Exception e) {
            log.error("知识库文档重新解析失败 id={}", documentId, e);
            doc.setStatus("failed");
            doc.setErrorMessage(truncate("文档解析失败", 500));
            documentMapper.updateById(doc);
            throw new BusinessException(KnowledgeErrorCode.KB_PARSE_FAILED);
        }

        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);
        return doc;
    }

    @Override
    public Map<String, Object> askAcrossDocuments(Long userId, List<Long> documentIds, String query, Integer topK) {
        if (query == null || query.isBlank()) {
            throw new BusinessException(KnowledgeErrorCode.KB_QUERY_BLANK);
        }
        int k = (topK == null || topK <= 0) ? DEFAULT_TOP_K : topK;

        // 解析目标文档集合：空 -> 该用户全部 ready 文档；非空 -> 逐个归属校验
        List<KnowledgeDocument> targets = new ArrayList<>();
        if (documentIds == null || documentIds.isEmpty()) {
            List<KnowledgeDocument> all = listDocuments(userId, 1, 100);
            for (KnowledgeDocument d : all) {
                if ("ready".equalsIgnoreCase(d.getStatus())) {
                    targets.add(d);
                }
            }
        } else {
            for (Long id : documentIds) {
                targets.add(getDocument(userId, id)); // 非本人/不存在抛错
            }
        }

        // 跨文档收集本人分块并全局打分
        List<ScoredChunk> scored = new ArrayList<>();
        for (KnowledgeDocument d : targets) {
            List<KnowledgeChunk> all = chunkMapper.selectByDocumentId(d.getId());
            if (all == null) continue;
            for (KnowledgeChunk c : all) {
                if (!userId.equals(c.getUserId())) continue;
                double score = computeSimilarity(query, c.getContent());
                if (score > 0) {
                    scored.add(new ScoredChunk(c, score));
                }
            }
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        List<KnowledgeChunk> topChunks = new ArrayList<>();
        for (int i = 0; i < Math.min(k, scored.size()); i++) {
            topChunks.add(scored.get(i).chunk);
        }

        String answer = buildExtractiveAnswer(query, topChunks);

        List<ChunkInfo> sourceChunks = new ArrayList<>();
        List<Long> sourceIds = new ArrayList<>();
        for (KnowledgeChunk c : topChunks) {
            sourceChunks.add(new ChunkInfo(c.getChunkIndex(), truncate(c.getContent(), CHUNK_PREVIEW_LENGTH),
                    computeSimilarity(query, c.getContent())));
            if (c.getId() != null) sourceIds.add(c.getId());
        }

        // 落库问答记录（document_id 记为 null 表示跨文档问答）
        try {
            KnowledgeQuery record = new KnowledgeQuery();
            record.setUserId(userId);
            record.setDocumentId(null);
            record.setQuery(query);
            record.setAnswer(answer);
            record.setSourceChunks(objectMapper.writeValueAsString(sourceIds));
            record.setCreatedAt(LocalDateTime.now());
            queryMapper.insert(record);
        } catch (IOException e) {
            log.warn("保存跨文档问答记录失败: {}", e.getMessage());
        }

        List<Long> docIdList = new ArrayList<>();
        for (KnowledgeDocument d : targets) {
            docIdList.add(d.getId());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("answer", answer);
        result.put("sourceChunks", sourceChunks);
        result.put("documentIds", docIdList);
        result.put("query", query);
        return result;
    }

    // ===================== 内部工具 =====================

    /**
     * 离线抽取式回答：从 top-k 片段中切句，挑选与问题词项重叠最多的句子作为回答。
     * 不依赖任何 LLM，保证测试完全离线。
     */
    private String buildExtractiveAnswer(String query, List<KnowledgeChunk> topChunks) {
        if (topChunks == null || topChunks.isEmpty()) {
            return "未在文档中检索到与问题相关的内容，请更换问法或确认文档已就绪。";
        }
        Set<String> qTokens = new HashSet<>(tokenize(query));

        List<Map.Entry<String, Integer>> scoredSentences = new ArrayList<>();
        for (KnowledgeChunk c : topChunks) {
            String[] parts = c.getContent().split("[。！？!?；;：:\\n\\r]+");
            for (String s : parts) {
                String sentence = s == null ? "" : s.trim();
                if (sentence.isEmpty()) {
                    continue;
                }
                int overlap = 0;
                for (String t : tokenize(sentence)) {
                    if (qTokens.contains(t)) {
                        overlap++;
                    }
                }
                scoredSentences.add(new HashMap.SimpleEntry<>(sentence, overlap));
            }
        }
        // 取重叠最多的若干句子
        scoredSentences.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        List<String> picked = new ArrayList<>();
        for (Map.Entry<String, Integer> e : scoredSentences) {
            if (e.getValue() > 0 && picked.size() < MAX_ANSWER_SENTENCES) {
                picked.add(e.getKey());
            }
        }
        if (picked.isEmpty()) {
            // 无显式词项命中时，兜底返回首段前若干字符
            String fallback = topChunks.get(0).getContent().trim();
            return fallback.length() > CHUNK_PREVIEW_LENGTH
                    ? fallback.substring(0, CHUNK_PREVIEW_LENGTH) + "……"
                    : fallback;
        }
        return String.join(" ", picked);
    }

    /**
     * 简单离线分词：ASCII 连续字母数字作为词项；中文按 2-gram（滑窗二元组）切分。
     * 无第三方分词库依赖，足够支撑关键词重叠检索。
     */
    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) {
            return tokens;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        Matcher am = ASCII_WORD.matcher(lower);
        while (am.find()) {
            tokens.add(am.group());
        }
        Matcher cm = CHINESE_RUN.matcher(lower);
        while (cm.find()) {
            String run = cm.group();
            if (run.length() == 1) {
                tokens.add(run);
            } else {
                for (int i = 0; i < run.length() - 1; i++) {
                    tokens.add(run.substring(i, i + 2));
                }
            }
        }
        return tokens;
    }

    /** 从原始文件名推断文件类型，仅识别知识库关心的几种。 */
    private String detectFileType(String fileName) {
        if (fileName == null) {
            return "txt";
        }
        String name = fileName.toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot + 1) : "";
        switch (ext) {
            case "txt":
            case "md":
                return "txt";
            case "pdf":
                return "pdf";
            case "doc":
            case "docx":
                return "docx";
            case "xls":
            case "xlsx":
                return "xlsx";
            case "ppt":
            case "pptx":
                return "pptx";
            default:
                return ext;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** 内部记录：分块及其相似度，用于排序。 */
    private static final class ScoredChunk {
        final KnowledgeChunk chunk;
        final double score;

        ScoredChunk(KnowledgeChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }

    // 便于其它模块复用句子切分（预留）
    static List<String> splitSentences(String text) {
        if (text == null) {
            return new ArrayList<>();
        }
        String[] parts = text.split("[。！？!?；;：:\\n\\r]+");
        List<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(parts));
        return list;
    }
}

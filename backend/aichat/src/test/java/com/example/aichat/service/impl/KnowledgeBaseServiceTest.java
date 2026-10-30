package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.KnowledgeErrorCode;
import com.example.aichat.entity.KnowledgeChunk;
import com.example.aichat.entity.KnowledgeDocument;
import com.example.aichat.entity.KnowledgeQuery;
import com.example.aichat.mapper.KnowledgeChunkMapper;
import com.example.aichat.mapper.KnowledgeDocumentMapper;
import com.example.aichat.mapper.KnowledgeQueryMapper;
import com.example.aichat.service.KnowledgeBaseService;
import com.example.aichat.service.VectorSearchClient;
import com.example.aichat.util.FileUploadUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * KnowledgeBaseService 单元测试（Mockito 隔离，完全离线）。
 *
 * 覆盖：上传成功/空文件/非法扩展名、txt 解析、分块（正常/超短/空/overlap）、
 * 检索（相关命中/top-k 数量/无关低分）、相似度计算、抽取式问答、引用片段、
 * 删除联动、归属校验（IDOR）。
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceTest {

    private static final Long USER = 100L;
    private static final Long OTHER_USER = 999L;
    private static final Long DOC_ID = 1L;

    @TempDir
    Path tempDir;

    @Mock
    private KnowledgeDocumentMapper documentMapper;
    @Mock
    private KnowledgeChunkMapper chunkMapper;
    @Mock
    private KnowledgeQueryMapper queryMapper;

    private KnowledgeBaseService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeBaseServiceImpl(documentMapper, chunkMapper, queryMapper);
    }

    // ---------- 工具 ----------

    private KnowledgeDocument ownedDoc(Long id, Long userId) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(id);
        doc.setUserId(userId);
        doc.setStatus("ready");
        return doc;
    }

    private KnowledgeChunk chunk(Long id, Long docId, Long userId, int index, String content) {
        KnowledgeChunk c = new KnowledgeChunk();
        c.setId(id);
        c.setDocumentId(docId);
        c.setUserId(userId);
        c.setChunkIndex(index);
        c.setContent(content);
        return c;
    }

    /** 写一个 UTF-8 临时 txt 文件并返回 File。 */
    private File writeTxtFile(String fileName, String content) throws IOException {
        Path p = tempDir.resolve(fileName);
        Files.writeString(p, content, StandardCharsets.UTF_8);
        return p.toFile();
    }

    // ===================== 上传 =====================

    @Test
    void uploadDocument_txt上传成功_状态ready且分块入库() throws IOException {
        String content = "人工智能是计算机科学的一个分支。它研究如何让机器模拟人类智能行为。";
        File txtFile = writeTxtFile("ai.txt", content);
        MockMultipartFile upload = new MockMultipartFile("file", "ai.txt", "text/plain",
                content.getBytes(StandardCharsets.UTF_8));

        try (MockedStatic<FileUploadUtil> mocked = Mockito.mockStatic(FileUploadUtil.class)) {
            mocked.when(() -> FileUploadUtil.save(any(MultipartFile.class), anyString())).thenReturn(txtFile);

            KnowledgeDocument result = service.uploadDocument(USER, upload, "AI文档");

            assertNotNull(result);
            assertEquals("AI文档", result.getTitle());
            assertEquals("txt", result.getFileType());
            assertEquals(USER, result.getUserId());
            // 短文本只产生 1 个分块
            verify(documentMapper).insert(any(KnowledgeDocument.class));
            verify(chunkMapper, times(1)).insert(any(KnowledgeChunk.class));
            // 捕获最终更新的文档，断言 status=ready
            ArgumentCaptor<KnowledgeDocument> captor = ArgumentCaptor.forClass(KnowledgeDocument.class);
            verify(documentMapper, atLeast(1)).updateById(captor.capture());
            KnowledgeDocument updated = captor.getValue();
            assertEquals("ready", updated.getStatus());
            assertEquals(1, updated.getChunkCount());
        }
    }

    @Test
    void uploadDocument_空文件_拒绝且不落库() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        try (MockedStatic<FileUploadUtil> mocked = Mockito.mockStatic(FileUploadUtil.class)) {
            mocked.when(() -> FileUploadUtil.save(any(MultipartFile.class), anyString()))
                    .thenThrow(new IOException("上传文件为空"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.uploadDocument(USER, empty, "t"));
            assertNotNull(ex);
            verify(documentMapper, never()).insert(any(KnowledgeDocument.class));
            verify(chunkMapper, never()).insert(any(KnowledgeChunk.class));
        }
    }

    @Test
    void uploadDocument_非法扩展名_拒绝() {
        MockMultipartFile exe = new MockMultipartFile("file", "evil.exe", "application/octet-stream", "x".getBytes());

        try (MockedStatic<FileUploadUtil> mocked = Mockito.mockStatic(FileUploadUtil.class)) {
            mocked.when(() -> FileUploadUtil.save(any(MultipartFile.class), anyString()))
                    .thenThrow(new IOException("不支持的文件类型: .exe"));

            assertThrows(BusinessException.class, () -> service.uploadDocument(USER, exe, "t"));
            verify(documentMapper, never()).insert(any(KnowledgeDocument.class));
        }
    }

    // ===================== 文本解析 =====================

    @Test
    void parseText_txt内容正确解析() throws IOException {
        String content = "知识库测试文本。RAG 检索。";
        File f = writeTxtFile("note.txt", content);

        String parsed = service.parseText(f.getAbsolutePath(), "txt");

        assertEquals(content, parsed);
    }

    @Test
    void parseText_pdf真实PDF_正确抽取文本() throws IOException {
        // 使用 PDFBox 3.x 构造一个最小可解析 PDF（ASCII 文本，Helvetica 内置字体可渲染）
        Path pdfPath = tempDir.resolve("kb.pdf");
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
            doc.addPage(page);
            org.apache.pdfbox.pdmodel.font.PDType1Font helvetica =
                    new org.apache.pdfbox.pdmodel.font.PDType1Font(
                            org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
            try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                         new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(helvetica, 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("Knowledge Base PDF test RAG retrieval");
                cs.endText();
            }
            doc.save(pdfPath.toFile());
        }

        String parsed = service.parseText(pdfPath.toAbsolutePath().toString(), "pdf");

        assertNotNull(parsed);
        assertTrue(parsed.contains("Knowledge Base PDF test RAG retrieval"),
                "PDF 抽取文本应包含正文，实际=" + parsed);
    }

    @Test
    void parseText_pdf文件不存在_抛出解析失败异常() {
        // PDF 已接入 PDFBox：不存在/损坏的文件不再报"不支持类型"，而是报解析失败
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.parseText(tempDir.resolve("not-exist.pdf").toString(), "pdf"));
        assertEquals(KnowledgeErrorCode.KB_PARSE_FAILED, ex.getErrorCode());
    }

    @Test
    void parseText_docx文件不存在_抛出解析失败异常() {
        // DOCX 已接入 Apache POI：不存在/损坏的文件报解析失败（不再是"不支持类型"）
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.parseText(tempDir.resolve("not-exist.docx").toString(), "docx"));
        assertEquals(KnowledgeErrorCode.KB_PARSE_FAILED, ex.getErrorCode());
    }

    // ===================== 分块 =====================

    @Test
    void chunkText_正常文本_分块数正确且index连续overlap生效() {
        // 1200 字符，chunkSize=500, overlap=50 -> step=450 -> 3 块
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1200; i++) {
            sb.append("字");
        }
        String text = sb.toString();

        List<KnowledgeChunk> chunks = service.chunkText(DOC_ID, USER, text, 500, 50);

        assertEquals(3, chunks.size(), "1200 字符按 500/50 应切成 3 块");
        assertEquals(0, chunks.get(0).getChunkIndex());
        assertEquals(1, chunks.get(1).getChunkIndex());
        assertEquals(2, chunks.get(2).getChunkIndex());
        // 第一块 [0,500)，第二块 [450,950)，重叠区为 [450,500)
        assertEquals(500, chunks.get(0).getContent().length());
        assertEquals(500, chunks.get(1).getContent().length());
        assertEquals(USER, chunks.get(0).getUserId());
        assertEquals(DOC_ID, chunks.get(0).getDocumentId());
    }

    @Test
    void chunkText_超短文本_单块() {
        List<KnowledgeChunk> chunks = service.chunkText(DOC_ID, USER, "很短", 500, 50);
        assertEquals(1, chunks.size());
        assertEquals(0, chunks.get(0).getChunkIndex());
        assertEquals("很短", chunks.get(0).getContent());
    }

    @Test
    void chunkText_空文本_返回空列表() {
        assertTrue(service.chunkText(DOC_ID, USER, "", 500, 50).isEmpty());
        assertTrue(service.chunkText(DOC_ID, USER, "   ", 500, 50).isEmpty());
        assertTrue(service.chunkText(DOC_ID, USER, null, 500, 50).isEmpty());
    }

    // ===================== 检索 =====================

    @Test
    void retrieveChunks_相关片段被命中且数量正确() {
        when(documentMapper.selectById(DOC_ID)).thenReturn(ownedDoc(DOC_ID, USER));
        KnowledgeChunk rel = chunk(1L, DOC_ID, USER, 0, "人工智能与机器学习是计算机科学的分支。");
        KnowledgeChunk rel2 = chunk(2L, DOC_ID, USER, 1, "人工智能也用于医疗图像识别与辅助诊断。");
        KnowledgeChunk irrel = chunk(3L, DOC_ID, USER, 2, "今天天气晴朗适合去公园散步野餐。");
        when(chunkMapper.selectByDocumentId(DOC_ID)).thenReturn(List.of(rel, rel2, irrel));

        List<KnowledgeChunk> result = service.retrieveChunks(USER, DOC_ID, "人工智能", 2);

        assertEquals(2, result.size(), "topK=2 应返回 2 个正相关片段");
        // 最相关（含“人工智能”）应排第一
        assertTrue(result.get(0).getContent().contains("人工智能"));
    }

    @Test
    void retrieveChunks_无关query_返回空() {
        when(documentMapper.selectById(DOC_ID)).thenReturn(ownedDoc(DOC_ID, USER));
        KnowledgeChunk irrel = chunk(3L, DOC_ID, USER, 0, "今天天气晴朗适合去公园散步野餐。");
        when(chunkMapper.selectByDocumentId(DOC_ID)).thenReturn(List.of(irrel));

        List<KnowledgeChunk> result = service.retrieveChunks(USER, DOC_ID, "量子计算", 3);

        assertTrue(result.isEmpty(), "完全无关的 query 应返回空（相似度为 0 被过滤）");
    }


    @Test
    void retrieveChunks_向量开启且sidecar命中_按向量序返回() {
        VectorSearchClient mockClient = Mockito.mock(VectorSearchClient.class);
        when(documentMapper.selectById(DOC_ID)).thenReturn(ownedDoc(DOC_ID, USER));
        KnowledgeChunk rel = chunk(1L, DOC_ID, USER, 0, "人工智能与机器学习是计算机科学的分支。");
        KnowledgeChunk weather = chunk(3L, DOC_ID, USER, 2, "今天天气晴朗适合去公园散步野餐。");
        when(chunkMapper.selectByDocumentId(DOC_ID)).thenReturn(List.of(rel, weather));
        // 向量侧把天气片段判为最相关（语义召回，可能与字面不同）
        when(mockClient.query(USER, DOC_ID, "怎么培养语感", 3))
                .thenReturn(List.of(new VectorSearchClient.Hit("3", 2, 0.78, "今天天气晴朗适合去公园散步野餐。"),
                        new VectorSearchClient.Hit("1", 0, 0.55, "人工智能与机器学习是计算机科学的分支。")));
        ReflectionTestUtils.setField(service, "vectorEnabled", true);
        ReflectionTestUtils.setField(service, "vectorSearchClient", mockClient);

        List<KnowledgeChunk> result = service.retrieveChunks(USER, DOC_ID, "怎么培养语感", 3);

        assertEquals(2, result.size(), "应返回向量召回的 2 个片段");
        assertEquals("今天天气晴朗适合去公园散步野餐。", result.get(0).getContent(),
                "向量序第一应为 sidecar 返回的第一命中");
    }

    @Test
    void retrieveChunks_向量开启但sidecar空_降级2gram() {
        VectorSearchClient mockClient = Mockito.mock(VectorSearchClient.class);
        when(documentMapper.selectById(DOC_ID)).thenReturn(ownedDoc(DOC_ID, USER));
        KnowledgeChunk rel = chunk(1L, DOC_ID, USER, 0, "人工智能与机器学习是计算机科学的分支。");
        when(chunkMapper.selectByDocumentId(DOC_ID)).thenReturn(List.of(rel));
        when(mockClient.query(USER, DOC_ID, "人工智能", 3)).thenReturn(List.of());
        ReflectionTestUtils.setField(service, "vectorEnabled", true);
        ReflectionTestUtils.setField(service, "vectorSearchClient", mockClient);

        List<KnowledgeChunk> result = service.retrieveChunks(USER, DOC_ID, "人工智能", 3);

        assertEquals(1, result.size(), "sidecar 空时应降级到 2-gram 并命中文本片段");
        assertTrue(result.get(0).getContent().contains("人工智能"));
    }
    @Test
    void computeSimilarity_相同文本分数最高() {
        double same = service.computeSimilarity("人工智能 机器学习", "人工智能 机器学习");
        assertEquals(1.0, same, 1e-9, "完全相同文本相似度应为 1.0");
    }

    @Test
    void computeSimilarity_完全不同文本分数为0() {
        double diff = service.computeSimilarity("人工智能 机器学习", "量子计算 区块链技术");
        assertEquals(0.0, diff, 1e-9, "无任何词项重叠应得 0 分");
    }

    @Test
    void computeSimilarity_部分重叠分数介于0和1() {
        double partial = service.computeSimilarity("人工智能 深度学习",
                "深度学习是机器学习的一个子领域");
        assertTrue(partial > 0.0 && partial < 1.0,
                "部分重叠应介于 (0,1)，实际=" + partial);
    }

    // ===================== 问答 =====================

    @Test
    void askQuestion_抽取式回答包含相关句子且返回sourceChunks() {
        when(documentMapper.selectById(DOC_ID)).thenReturn(ownedDoc(DOC_ID, USER));
        KnowledgeChunk rel = chunk(1L, DOC_ID, USER, 0,
                "人工智能是计算机科学的一个重要分支。它研究如何让机器模拟人类的智能。");
        when(chunkMapper.selectByDocumentId(DOC_ID)).thenReturn(List.of(rel));

        Map<String, Object> resp = service.askQuestion(USER, DOC_ID, "什么是人工智能", null);

        assertNotNull(resp);
        String answer = (String) resp.get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("人工智能"), "抽取式回答应包含问题关键词所在句子: " + answer);
        @SuppressWarnings("unchecked")
        List<com.example.aichat.dto.knowledge.ChunkInfo> sources =
                (List<com.example.aichat.dto.knowledge.ChunkInfo>) resp.get("sourceChunks");
        assertNotNull(sources);
        assertEquals(1, sources.size());
        assertEquals(0, sources.get(0).getChunkIndex());
        assertTrue(sources.get(0).getSimilarity() > 0.0);
        assertEquals(DOC_ID, resp.get("documentId"));
        // 问答记录应落库
        verify(queryMapper).insert(any(KnowledgeQuery.class));
    }

    @Test
    void askQuestion_空问题_拒绝() {
        when(documentMapper.selectById(DOC_ID)).thenReturn(ownedDoc(DOC_ID, USER));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.askQuestion(USER, DOC_ID, "  ", null));
        assertEquals(KnowledgeErrorCode.KB_QUERY_BLANK, ex.getErrorCode());
        verify(queryMapper, never()).insert(any(KnowledgeQuery.class));
    }

    // ===================== 删除与归属 =====================

    @Test
    void deleteDocument_删除分块与文档() {
        when(documentMapper.selectById(DOC_ID)).thenReturn(ownedDoc(DOC_ID, USER));

        service.deleteDocument(USER, DOC_ID);

        verify(chunkMapper).delete(any());
        verify(documentMapper).deleteById(DOC_ID);
    }

    @Test
    void getDocument_非所有者_抛FORBIDDEN() {
        when(documentMapper.selectById(DOC_ID)).thenReturn(ownedDoc(DOC_ID, OTHER_USER));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getDocument(USER, DOC_ID));
        assertEquals(KnowledgeErrorCode.KB_DOC_FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void getDocument_不存在_抛NOT_FOUND() {
        when(documentMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getDocument(USER, 999L));
        assertEquals(KnowledgeErrorCode.KB_DOC_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void retrieveChunks_非所有者文档_拒绝访问() {
        when(documentMapper.selectById(DOC_ID)).thenReturn(ownedDoc(DOC_ID, OTHER_USER));

        assertThrows(BusinessException.class,
                () -> service.retrieveChunks(USER, DOC_ID, "人工智能", 3));
        verify(chunkMapper, never()).selectByDocumentId(anyLong());
    }

    // ===================== 多文档问答 / 重新解析 =====================

    @Test
    void askAcrossDocuments_指定两文档_全局排序取最相关() {
        KnowledgeDocument d1 = ownedDoc(1L, USER);
        KnowledgeDocument d2 = ownedDoc(2L, USER);
        when(documentMapper.selectById(1L)).thenReturn(d1);
        when(documentMapper.selectById(2L)).thenReturn(d2);
        KnowledgeChunk aiChunk = chunk(11L, 1L, USER, 0, "人工智能是计算机科学的一个重要分支。");
        KnowledgeChunk weatherChunk = chunk(22L, 2L, USER, 0, "今天天气晴朗适合去公园散步野餐。");
        when(chunkMapper.selectByDocumentId(1L)).thenReturn(List.of(aiChunk));
        when(chunkMapper.selectByDocumentId(2L)).thenReturn(List.of(weatherChunk));

        Map<String, Object> resp = service.askAcrossDocuments(USER, List.of(1L, 2L), "什么是人工智能", 3);

        assertNotNull(resp);
        String answer = (String) resp.get("answer");
        assertTrue(answer.contains("人工智能"), "应跨文档命中文本: " + answer);
        @SuppressWarnings("unchecked")
        List<Long> docIds = (List<Long>) resp.get("documentIds");
        assertEquals(2, docIds.size());
        verify(queryMapper).insert(any(KnowledgeQuery.class));
    }

    @Test
    void askAcrossDocuments_非本人文档_拒绝() {
        when(documentMapper.selectById(1L)).thenReturn(ownedDoc(1L, OTHER_USER));
        assertThrows(BusinessException.class,
                () -> service.askAcrossDocuments(USER, List.of(1L), "人工智能", 3));
    }

    @Test
    void askAcrossDocuments_空问题_拒绝() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.askAcrossDocuments(USER, List.of(1L), "  ", 3));
        assertEquals(KnowledgeErrorCode.KB_QUERY_BLANK, ex.getErrorCode());
    }

    @Test
    void reparseDocument_重新解析_删除旧块并重插() throws IOException {
        String content = "深度学习是机器学习的一个子领域。它使用神经网络。";
        File txtFile = writeTxtFile("reparse.txt", content);
        KnowledgeDocument doc = ownedDoc(DOC_ID, USER);
        doc.setFilePath(txtFile.getAbsolutePath());
        doc.setFileType("txt");
        when(documentMapper.selectById(DOC_ID)).thenReturn(doc);

        KnowledgeDocument result = service.reparseDocument(USER, DOC_ID);

        assertEquals("ready", result.getStatus());
        verify(chunkMapper).delete(any());
        verify(chunkMapper, times(1)).insert(any(KnowledgeChunk.class));
    }

    @Test
    void reparseDocument_纯文本导入无文件_拒绝() {
        KnowledgeDocument doc = ownedDoc(DOC_ID, USER);
        doc.setFilePath(null);
        when(documentMapper.selectById(DOC_ID)).thenReturn(doc);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.reparseDocument(USER, DOC_ID));
        assertEquals(KnowledgeErrorCode.KB_PARSE_FAILED, ex.getErrorCode());
        verify(chunkMapper, never()).delete(any());
    }

    @Test
    void reparseDocument_非所有者_拒绝() {
        when(documentMapper.selectById(DOC_ID)).thenReturn(ownedDoc(DOC_ID, OTHER_USER));
        assertThrows(BusinessException.class,
                () -> service.reparseDocument(USER, DOC_ID));
    }
}

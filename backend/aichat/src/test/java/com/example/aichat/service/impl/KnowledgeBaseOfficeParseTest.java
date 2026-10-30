package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.mapper.KnowledgeChunkMapper;
import com.example.aichat.mapper.KnowledgeDocumentMapper;
import com.example.aichat.mapper.KnowledgeQueryMapper;
import com.example.aichat.service.KnowledgeBaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * 知识库 Office 文档解析单元测试（A6）。
 *
 * 使用 Apache POI 在临时目录生成真实的 .docx/.xlsx/.pptx 文件，
 * 再调用 KnowledgeBaseServiceImpl.parseText 验证文本提取。完全离线，不触网。
 */
class KnowledgeBaseOfficeParseTest {

    @TempDir
    Path tempDir;

    private KnowledgeBaseService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeBaseServiceImpl(
                mock(KnowledgeDocumentMapper.class),
                mock(KnowledgeChunkMapper.class),
                mock(KnowledgeQueryMapper.class));
    }

    @Test
    void docx应提取段落文本() throws Exception {
        File docx = new File(tempDir.toFile(), "test.docx");
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument()) {
            doc.createParagraph().createRun().setText("人工智能是计算机科学的分支");
            doc.createParagraph().createRun().setText("它研究让机器模拟人类智能");
            try (FileOutputStream out = new FileOutputStream(docx)) {
                doc.write(out);
            }
        }

        String text = service.parseText(docx.getAbsolutePath(), "docx");
        assertNotNull(text);
        assertTrue(text.contains("人工智能"));
        assertTrue(text.contains("模拟人类智能"));
    }

    @Test
    void xlsx应提取单元格文本() throws Exception {
        File xlsx = new File(tempDir.toFile(), "test.xlsx");
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.xssf.usermodel.XSSFSheet sheet = wb.createSheet("Sheet1");
            sheet.createRow(0).createCell(0).setCellValue("姓名");
            sheet.getRow(0).createCell(1).setCellValue("分数");
            sheet.createRow(1).createCell(0).setCellValue("小明");
            sheet.getRow(1).createCell(1).setCellValue(95);
            try (FileOutputStream out = new FileOutputStream(xlsx)) {
                wb.write(out);
            }
        }

        String text = service.parseText(xlsx.getAbsolutePath(), "xlsx");
        assertNotNull(text);
        assertTrue(text.contains("姓名"));
        assertTrue(text.contains("小明"));
    }

    @Test
    void pptx应提取幻灯片文本() throws Exception {
        File pptx = new File(tempDir.toFile(), "test.pptx");
        try (org.apache.poi.xslf.usermodel.XMLSlideShow ppt = new org.apache.poi.xslf.usermodel.XMLSlideShow()) {
            org.apache.poi.xslf.usermodel.XSLFSlide slide = ppt.createSlide();
            org.apache.poi.xslf.usermodel.XSLFTextBox box = slide.createTextBox();
            box.setText("这是第一页幻灯片标题");
            try (FileOutputStream out = new FileOutputStream(pptx)) {
                ppt.write(out);
            }
        }

        String text = service.parseText(pptx.getAbsolutePath(), "pptx");
        assertNotNull(text);
        assertTrue(text.contains("幻灯片标题"));
    }

    @Test
    void 损坏的docx应抛出KB_PARSE_FAILED而非500堆栈() {
        File bad = new File(tempDir.toFile(), "bad.docx");
        try {
            java.nio.file.Files.writeString(bad.toPath(), "this is not a real docx");
        } catch (Exception e) {
            fail("写临时文件失败");
        }
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.parseText(bad.getAbsolutePath(), "docx"));
        // 应是友好错误，不是裸 RuntimeException
        assertNotNull(ex.getMessage());
    }

    @Test
    void 不存在的文件应抛出友好错误() {
        assertThrows(BusinessException.class,
                () -> service.parseText(tempDir.resolve("nope.docx").toString(), "docx"));
    }
}

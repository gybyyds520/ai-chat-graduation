package com.example.aichat.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.common.security.UserPrincipal;
import com.example.aichat.dto.R;
import com.example.aichat.entity.FeedTask;
import com.example.aichat.entity.KnowledgeDocument;
import com.example.aichat.mapper.FeedTaskMapper;
import com.example.aichat.service.KnowledgeBaseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FeedController 端点测试（Mockito 隔离 + 模拟登录态）。
 *
 * 覆盖：
 *   1. text 投喂：content 落库、effectiveness 确定性计算、feedback 可解释；
 *   2. text 投喂知识库闭环：documentId 非空、knowledgePoints > 0；
 *   3. file 投喂：filePaths/fileNames 逗号分隔落库；
 *   4. image 投喂：imagePaths/imageNames 逗号分隔落库；
 *   5. history 真实分页：list/total/hasMore；
 *   6. delete 本人成功 / 7. 非本人 403；
 *   8. stats 扩展：total/avgEffectiveness/byType。
 */
@ExtendWith(MockitoExtension.class)
class FeedControllerTest {

    private static final Long CURRENT_USER = 100L;
    private static final Long OTHER_USER = 999L;

    @Mock
    private FeedTaskMapper feedTaskMapper;
    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    private FeedController controller;

    @BeforeEach
    void setUp() {
        controller = new FeedController();
        ReflectionTestUtils.setField(controller, "feedTaskMapper", feedTaskMapper);
        ReflectionTestUtils.setField(controller, "knowledgeBaseService", knowledgeBaseService);
        authenticateAs(CURRENT_USER);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId) {
        UserPrincipal principal = new UserPrincipal(userId, "u", List.of("ROLE_USER"));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Map<String, Object> textRequest(String content) {
        Map<String, Object> req = new HashMap<>();
        req.put("contentType", "text");
        req.put("content", content);
        req.put("priority", "high");
        req.put("processMode", "deep");
        req.put("tags", "java,backend");
        return req;
    }

    // ---------- 1. text 投喂：content 落库 + 确定性 effectiveness + feedback 可解释 ----------
    @Test
    @SuppressWarnings("unchecked")
    void submit_text_落库并确定性计算effectiveness() {
        // 长文本(>500) + high + deep + tags => 50 +15 +0 +0 +5 +10 +3 = 83
        String content = "长".repeat(600);
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(100L);
        doc.setChunkCount(3);
        when(knowledgeBaseService.importTextDocument(eq(CURRENT_USER), anyString(), anyString())).thenReturn(doc);

        R resp = controller.submitFeed(textRequest(content));

        assertEquals(200, resp.getCode());
        Map<String, Object> data = (Map<String, Object>) resp.getData();
        assertEquals(83, data.get("effectiveness"), "确定性评分应为 83");
        String feedback = (String) data.get("feedback");
        assertTrue(feedback.contains("知识点"), "feedback 应说明知识点数量");
        assertTrue(feedback.contains("文本长度"), "feedback 应说明文本长度");
        assertNotNull(data.get("duration"));
        assertTrue((Long) data.get("duration") >= 100, "duration 至少 100ms");

        // 落库字段校验
        ArgumentCaptor<FeedTask> captor = ArgumentCaptor.forClass(FeedTask.class);
        verify(feedTaskMapper).insert(captor.capture());
        FeedTask saved = captor.getValue();
        assertEquals("text", saved.getContentType());
        assertEquals(content, saved.getContent());
        assertEquals(83, saved.getEffectiveness());
        assertEquals("completed", saved.getStatus());
    }

    // ---------- 1b. 确定性：相同输入两次提交 effectiveness 一致 ----------
    @Test
    @SuppressWarnings("unchecked")
    void submit_text_相同输入两次effectiveness一致() {
        String content = "测试内容".repeat(100);
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(1L);
        doc.setChunkCount(2);
        when(knowledgeBaseService.importTextDocument(any(), anyString(), anyString())).thenReturn(doc);

        controller.submitFeed(textRequest(content));
        R resp2 = controller.submitFeed(textRequest(content));

        ArgumentCaptor<FeedTask> captor = ArgumentCaptor.forClass(FeedTask.class);
        verify(feedTaskMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        List<FeedTask> all = captor.getAllValues();
        assertEquals(all.get(0).getEffectiveness(), all.get(1).getEffectiveness(),
                "相同输入两次投喂 effectiveness 必须一致（非随机）");
        assertEquals(all.get(0).getEffectiveness(), ((Number) ((Map<String, Object>) resp2.getData()).get("effectiveness")).intValue());
    }

    // ---------- 2. text 投喂知识库闭环：documentId 非空、knowledgePoints > 0 ----------
    @Test
    @SuppressWarnings("unchecked")
    void submit_text_知识库入库documentId非空() {
        String content = "这是一段用于知识库入库测试的内容，长度足够触发分块。";
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(777L);
        doc.setChunkCount(4);
        when(knowledgeBaseService.importTextDocument(eq(CURRENT_USER), anyString(), eq(content))).thenReturn(doc);

        R resp = controller.submitFeed(textRequest(content));

        Map<String, Object> data = (Map<String, Object>) resp.getData();
        assertEquals(777L, ((Number) data.get("documentId")).longValue());
        assertEquals(4, data.get("knowledgePoints"));

        ArgumentCaptor<FeedTask> captor = ArgumentCaptor.forClass(FeedTask.class);
        verify(feedTaskMapper).insert(captor.capture());
        FeedTask saved = captor.getValue();
        assertEquals(777L, saved.getDocumentId());
        assertEquals(4, saved.getKnowledgePoints());
        String feedback = (String) data.get("feedback");
        assertTrue(feedback.contains("知识库"), "入库成功 feedback 应提示已进入知识库");
    }

    // ---------- 3. file 投喂：filePaths/fileNames 逗号分隔落库 ----------
    @Test
    @SuppressWarnings("unchecked")
    void submit_file_多文件路径逗号分隔落库() {
        Map<String, Object> req = new HashMap<>();
        req.put("contentType", "file");
        req.put("filePaths", List.of("/uploads/feed/a.txt", "/uploads/feed/b.md"));
        req.put("fileNames", List.of("a.txt", "b.md"));
        req.put("priority", "medium");
        req.put("processMode", "fast");

        R resp = controller.submitFeed(req);
        assertEquals(200, resp.getCode());

        ArgumentCaptor<FeedTask> captor = ArgumentCaptor.forClass(FeedTask.class);
        verify(feedTaskMapper).insert(captor.capture());
        FeedTask saved = captor.getValue();
        assertEquals("/uploads/feed/a.txt,/uploads/feed/b.md", saved.getFilePaths());
        assertEquals("a.txt,b.md", saved.getFileNames());
        // 旧单文件字段取第一个
        assertEquals("/uploads/feed/a.txt", saved.getFilePath());
        assertEquals("a.txt", saved.getFileName());
        // 非文本/文件不存在时不入库
        assertNull(saved.getDocumentId());
    }

    // ---------- 4. image 投喂：imagePaths/imageNames 逗号分隔落库 ----------
    @Test
    @SuppressWarnings("unchecked")
    void submit_image_多图片路径逗号分隔落库() {
        Map<String, Object> req = new HashMap<>();
        req.put("contentType", "image");
        req.put("imagePaths", List.of("/uploads/feed/1.png", "/uploads/feed/2.jpg"));
        req.put("imageNames", List.of("1.png", "2.jpg"));
        req.put("priority", "low");
        req.put("processMode", "fast");

        R resp = controller.submitFeed(req);
        assertEquals(200, resp.getCode());

        ArgumentCaptor<FeedTask> captor = ArgumentCaptor.forClass(FeedTask.class);
        verify(feedTaskMapper).insert(captor.capture());
        FeedTask saved = captor.getValue();
        assertEquals("/uploads/feed/1.png,/uploads/feed/2.jpg", saved.getImagePaths());
        assertEquals("1.png,2.jpg", saved.getImageNames());
        // 图片不入库知识库
        assertNull(saved.getDocumentId());
        String feedback = (String) ((Map<String, Object>) resp.getData()).get("feedback");
        assertTrue(feedback.contains("图片素材"));
    }

    // ---------- 5. history 真实分页 ----------
    @Test
    @SuppressWarnings("unchecked")
    void history_真实分页返回list_total_page_size_hasMore() {
        FeedTask t1 = new FeedTask();
        t1.setId(1L);
        FeedTask t2 = new FeedTask();
        t2.setId(2L);
        Page<FeedTask> pager = new Page<>(1, 5);
        pager.setRecords(List.of(t1, t2));
        pager.setTotal(12);
        when(feedTaskMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(pager);

        R resp = controller.getFeedHistory(1, 5);
        assertEquals(200, resp.getCode());
        Map<String, Object> data = (Map<String, Object>) resp.getData();
        assertEquals(2, ((List<?>) data.get("list")).size());
        assertEquals(12L, ((Number) data.get("total")).longValue());
        assertEquals(1L, ((Number) data.get("page")).longValue());
        assertEquals(5L, ((Number) data.get("size")).longValue());
        // total=12,size=5 => pages=3, current=1 => hasMore=true
        assertEquals(Boolean.TRUE, data.get("hasMore"));
    }

    // ---------- 6. delete 本人成功 ----------
    @Test
    void delete_本人_成功删除() {
        FeedTask owned = new FeedTask();
        owned.setId(5L);
        owned.setUserId(CURRENT_USER);
        owned.setTaskId("FEED-1");
        when(feedTaskMapper.selectOne(any(Wrapper.class))).thenReturn(owned);

        R resp = controller.deleteTask("FEED-1");
        assertEquals(200, resp.getCode());
        verify(feedTaskMapper).deleteById(5L);
        assertEquals(Boolean.TRUE, ((Map<String, Object>) resp.getData()).get("success"));
    }

    // ---------- 7. delete 非本人 403 ----------
    @Test
    void delete_非本人_返回403且不删除() {
        FeedTask foreign = new FeedTask();
        foreign.setId(6L);
        foreign.setUserId(OTHER_USER);
        foreign.setTaskId("FEED-2");
        when(feedTaskMapper.selectOne(any(Wrapper.class))).thenReturn(foreign);

        R resp = controller.deleteTask("FEED-2");
        assertEquals(403, resp.getCode());
        verify(feedTaskMapper, never()).deleteById(anyLong());
    }

    // ---------- 8. stats 扩展 ----------
    @Test
    @SuppressWarnings("unchecked")
    void stats_返回total_avgEffectiveness_byType() {
        when(feedTaskMapper.countByStatus("pending", CURRENT_USER)).thenReturn(1);
        when(feedTaskMapper.countByStatus("processing", CURRENT_USER)).thenReturn(0);
        when(feedTaskMapper.countByStatus("completed", CURRENT_USER)).thenReturn(10);
        when(feedTaskMapper.countByStatus("failed", CURRENT_USER)).thenReturn(1);
        when(feedTaskMapper.selectCount(any(Wrapper.class))).thenReturn(12L);
        when(feedTaskMapper.avgEffectiveness(CURRENT_USER)).thenReturn(75.67);
        List<Map<String, Object>> rows = List.of(
                Map.of("content_type", "text", "cnt", 7L),
                Map.of("content_type", "file", "cnt", 4L),
                Map.of("content_type", "image", "cnt", 1L));
        when(feedTaskMapper.countByType(CURRENT_USER)).thenReturn(rows);
        when(feedTaskMapper.findProcessingByUser(CURRENT_USER)).thenReturn(null);

        R resp = controller.getStats();
        assertEquals(200, resp.getCode());
        Map<String, Object> stats = (Map<String, Object>) resp.getData();
        assertEquals(12L, ((Number) stats.get("total")).longValue());
        assertEquals(75.7, ((Number) stats.get("avgEffectiveness")).doubleValue(), 0.001);
        Map<String, Object> byType = (Map<String, Object>) stats.get("byType");
        assertEquals(7L, ((Number) byType.get("text")).longValue());
        assertEquals(4L, ((Number) byType.get("file")).longValue());
        assertEquals(1L, ((Number) byType.get("image")).longValue());
    }
}

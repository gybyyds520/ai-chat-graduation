package com.example.aichat.controller;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.KnowledgeErrorCode;
import com.example.aichat.common.exception.UserErrorCode;
import com.example.aichat.common.security.UserPrincipal;
import com.example.aichat.dto.R;
import com.example.aichat.dto.knowledge.AskRequest;
import com.example.aichat.entity.KnowledgeDocument;
import com.example.aichat.service.KnowledgeBaseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * KnowledgeBaseController 端点测试（Mockito 隔离 + 模拟登录态）。
 *
 * 覆盖：
 *   1. upload 端点走 requireUserId 并把当前用户透传给 service；
 *   2. ask 端点返回正确结构；
 *   3. 非所有者删除被拒（service 层归属校验抛 FORBIDDEN，下游删除不执行）；
 *   4. 未登录访问抛 TOKEN_INVALID。
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBaseControllerTest {

    private static final Long CURRENT_USER = 100L;
    private static final Long DOC_ID = 1L;

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    private KnowledgeBaseController controller;

    @BeforeEach
    void setUp() {
        controller = new KnowledgeBaseController();
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

    @Test
    void upload_携带登录态_透传当前用户() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "内容".getBytes());
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(DOC_ID);
        doc.setUserId(CURRENT_USER);
        doc.setStatus("ready");
        when(knowledgeBaseService.uploadDocument(eq(CURRENT_USER), eq(file), eq("标题"))).thenReturn(doc);

        R resp = controller.upload(file, "标题");

        assertEquals(200, resp.getCode());
        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(knowledgeBaseService).uploadDocument(userIdCaptor.capture(), eq(file), eq("标题"));
        assertEquals(CURRENT_USER, userIdCaptor.getValue(), "必须以登录用户 ID 调用 service");
    }

    @Test
    void ask_返回正确结构() {
        AskRequest req = new AskRequest();
        req.setQuery("什么是人工智能");
        req.setTopK(3);
        Map<String, Object> answer = Map.of(
                "answer", "人工智能是计算机科学分支。",
                "documentId", DOC_ID,
                "query", "什么是人工智能",
                "sourceChunks", List.of(Map.of("chunkIndex", 0, "similarity", 0.8)));
        when(knowledgeBaseService.askQuestion(CURRENT_USER, DOC_ID, "什么是人工智能", 3)).thenReturn(answer);

        R resp = controller.ask(DOC_ID, req);

        assertEquals(200, resp.getCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.getData();
        assertEquals("人工智能是计算机科学分支。", data.get("answer"));
        assertEquals(DOC_ID, data.get("documentId"));
    }

    @Test
    void delete_非所有者_抛FORBIDDEN且不执行删除() {
        // service 层归属校验：deleteDocument 内部先 getDocument，非本人文档抛 FORBIDDEN
        org.mockito.Mockito.doThrow(new BusinessException(KnowledgeErrorCode.KB_DOC_FORBIDDEN))
                .when(knowledgeBaseService).deleteDocument(CURRENT_USER, DOC_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.delete(DOC_ID));
        assertEquals(KnowledgeErrorCode.KB_DOC_FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void getDocument_非所有者_抛FORBIDDEN() {
        when(knowledgeBaseService.getDocument(CURRENT_USER, DOC_ID))
                .thenThrow(new BusinessException(KnowledgeErrorCode.KB_DOC_FORBIDDEN));

        assertThrows(BusinessException.class, () -> controller.getDocument(DOC_ID));
    }

    @Test
    void 未登录访问_抛TOKEN_INVALID() {
        SecurityContextHolder.clearContext();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.list(1, 20));
        assertEquals(UserErrorCode.TOKEN_INVALID, ex.getErrorCode());
        verify(knowledgeBaseService, never()).listDocuments(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }
}

package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.AiCharacter;
import com.example.aichat.entity.ChatMessage;
import com.example.aichat.service.AiCharacterService;
import com.example.aichat.service.AiMemoryService;
import com.example.aichat.service.CharacterMemoryService;
import com.example.aichat.service.ChatMessageService;
import com.example.aichat.service.ConversationContextService;
import com.example.aichat.common.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * M1 ①③④：ChatController 新增端点（重新生成/编辑/撤回/标记已读）控制器单测。
 */
@ExtendWith(MockitoExtension.class)
class ChatControllerM1Test {

    private static final Long CURRENT_USER = 100L;

    @Mock
    private ChatMessageService chatMessageService;
    @Mock
    private AiCharacterService aiCharacterService;
    @Mock
    private ConversationContextService conversationContextService;
    @Mock
    private CharacterMemoryService characterMemoryService;
    @Mock
    private AiMemoryService aiMemoryService;
    @Mock
    private com.example.aichat.service.ai.ModelRouter modelRouter;

    private ChatController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatController();
        ReflectionTestUtils.setField(controller, "chatMessageService", chatMessageService);
        ReflectionTestUtils.setField(controller, "aiCharacterService", aiCharacterService);
        ReflectionTestUtils.setField(controller, "conversationContextService", conversationContextService);
        ReflectionTestUtils.setField(controller, "characterMemoryService", characterMemoryService);
        ReflectionTestUtils.setField(controller, "aiMemoryService", aiMemoryService);
        ReflectionTestUtils.setField(controller, "modelRouter", modelRouter);

        UserPrincipal principal = new UserPrincipal(CURRENT_USER, "u", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private ChatMessage msg(Long id, Long userId, String senderType, LocalDateTime time) {
        ChatMessage m = new ChatMessage();
        m.setId(id);
        m.setUserId(userId);
        m.setSenderType(senderType);
        m.setContent("hello");
        m.setSessionId("sess1");
        m.setCharacterId(1L);
        m.setCreateTime(time);
        return m;
    }

    // ---- ④ recall ----
    @Test
    void recall_本人2分钟内_成功() {
        when(chatMessageService.findOwnedMessage(1L, CURRENT_USER))
                .thenReturn(msg(1L, CURRENT_USER, "user", LocalDateTime.now().minusSeconds(30)));
        when(chatMessageService.recallMessage(1L)).thenReturn(true);

        R<?> r = controller.recallMessage(1L);
        assertEquals(200, r.getCode());
    }

    @Test
    void recall_非本人_401() {
        when(chatMessageService.findOwnedMessage(1L, CURRENT_USER)).thenReturn(null);
        R<?> r = controller.recallMessage(1L);
        assertEquals(401, r.getCode());
    }

    @Test
    void recall_超过2分钟_400() {
        when(chatMessageService.findOwnedMessage(1L, CURRENT_USER))
                .thenReturn(msg(1L, CURRENT_USER, "user", LocalDateTime.now().minusMinutes(5)));
        R<?> r = controller.recallMessage(1L);
        assertEquals(400, r.getCode());
    }

    // ---- ① edit ----
    @Test
    void edit_本人user消息_成功() {
        when(chatMessageService.findOwnedMessage(1L, CURRENT_USER))
                .thenReturn(msg(1L, CURRENT_USER, "user", LocalDateTime.now().minusMinutes(1)));
        when(chatMessageService.updateContent(eq(1L), anyString())).thenReturn(true);
        when(chatMessageService.deleteAfterMessage(anyString(), eq(1L))).thenReturn(1);

        R<?> r = controller.editMessage(1L, Map.of("content", "edited"));
        assertEquals(200, r.getCode());
    }

    @Test
    void edit_空内容_400() {
        R<?> r = controller.editMessage(1L, Map.of("content", "  "));
        assertEquals(400, r.getCode());
    }

    @Test
    void edit_非本人_401() {
        when(chatMessageService.findOwnedMessage(any(), eq(CURRENT_USER))).thenReturn(null);
        R<?> r = controller.editMessage(1L, Map.of("content", "x"));
        assertEquals(401, r.getCode());
    }

    // ---- ③ batch read ----
    @Test
    void markSessionRead_会话属本人_成功() {
        when(chatMessageService.getRecentMessages("sess1", 1))
                .thenReturn(List.of(msg(1L, CURRENT_USER, "user", LocalDateTime.now())));
        R<?> r = controller.markSessionRead("sess1");
        assertEquals(200, r.getCode());
    }

    // ---- ① regenerate ----
    @Test
    void regenerate_非本人AI消息_401() {
        when(chatMessageService.findById(1L)).thenReturn(msg(1L, 999L, "assistant", LocalDateTime.now()));
        R<?> r = controller.regenerate(1L);
        assertEquals(401, r.getCode());
    }

    @Test
    void regenerate_非AI消息_400() {
        when(chatMessageService.findById(1L)).thenReturn(msg(1L, CURRENT_USER, "user", LocalDateTime.now()));
        R<?> r = controller.regenerate(1L);
        assertEquals(400, r.getCode());
    }

    @Test
    void regenerate_正常_调用LLM并落库() {
        ChatMessage ai = msg(1L, CURRENT_USER, "assistant", LocalDateTime.now());
        when(chatMessageService.findById(1L)).thenReturn(ai);
        when(chatMessageService.getRecentMessages(anyString(), anyInt()))
                .thenReturn(List.of(msg(2L, CURRENT_USER, "user", LocalDateTime.now())));
        when(aiCharacterService.getCharacterById(1L)).thenReturn(new AiCharacter());
        when(aiMemoryService.getActiveMemoriesForContext(any(), anyInt())).thenReturn(List.of());
        when(modelRouter.chatStream(any(), any(), any())).thenReturn(Flux.just("新回复"));

        R<?> r = controller.regenerate(1L);
        assertEquals(200, r.getCode());
        verify(chatMessageService).deleteById(1L);
        verify(chatMessageService).saveMessage(eq("sess1"), eq(CURRENT_USER), eq(1L), eq("assistant"), eq("新回复"));
    }
}

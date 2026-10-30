package com.example.aichat.controller;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.UserErrorCode;
import com.example.aichat.common.security.AuthContext;
import com.example.aichat.common.security.UserPrincipal;
import com.example.aichat.service.CharacterPlazaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 角色广场 /api/character/square 控制器单元测试。
 *
 * 覆盖：无关键词走列表、有关键词走搜索、“使用”需登录（requireUserId）。
 */
@ExtendWith(MockitoExtension.class)
class CharacterSquareControllerTest {

    private static final Long CURRENT_USER = 7L;

    @Mock
    private CharacterPlazaService characterPlazaService;

    private CharacterSquareController controller;

    @BeforeEach
    void setUp() {
        controller = new CharacterSquareController();
        ReflectionTestUtils.setField(controller, "characterPlazaService", characterPlazaService);
        UserPrincipal principal = new UserPrincipal(CURRENT_USER, "u", List.of("ROLE_USER"));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void square_无关键词_应走公开列表() {
        Map<String, Object> page = new HashMap<>();
        page.put("list", List.of());
        when(characterPlazaService.listPublicCharacters(1, 12, "古风", "hot")).thenReturn(page);

        var res = controller.square(1, 12, "古风", "hot", null);
        assertNotNull(res);
        verify(characterPlazaService).listPublicCharacters(1, 12, "古风", "hot");
        verify(characterPlazaService, never()).searchPublicCharacters(any(), anyInt(), anyInt());
    }

    @Test
    void square_有关键词_应走搜索() {
        Map<String, Object> page = new HashMap<>();
        page.put("list", List.of());
        when(characterPlazaService.searchPublicCharacters("学霸", 1, 12)).thenReturn(page);

        controller.square(1, 12, null, "hot", "学霸");
        verify(characterPlazaService).searchPublicCharacters("学霸", 1, 12);
        verify(characterPlazaService, never()).listPublicCharacters(anyInt(), anyInt(), any(), any());
    }

    @Test
    void use_未登录_抛TOKEN_INVALID() {
        SecurityContextHolder.clearContext();
        BusinessException ex = assertThrows(BusinessException.class, () -> controller.use(1L));
        assertEquals(UserErrorCode.TOKEN_INVALID, ex.getErrorCode());
        verify(characterPlazaService, never()).incrementUseCount(any());
    }

    @Test
    void use_已登录_记录使用次数() {
        when(characterPlazaService.incrementUseCount(5L)).thenReturn(true);
        assertDoesNotThrow(() -> controller.use(5L));
        verify(characterPlazaService).incrementUseCount(5L);
    }

    @Test
    void use_角色不存在_返回错误() {
        when(characterPlazaService.incrementUseCount(404L)).thenReturn(false);
        assertDoesNotThrow(() -> controller.use(404L));
    }
}

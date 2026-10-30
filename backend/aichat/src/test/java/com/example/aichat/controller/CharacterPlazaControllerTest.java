package com.example.aichat.controller;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.dto.R;
import com.example.aichat.service.CharacterPlazaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 角色广场控制器测试。
 *
 * 覆盖：公开列表匿名可访问（不要求登录）、详情匿名返回、使用端点强制 requireUserId。
 */
@ExtendWith(MockitoExtension.class)
class CharacterPlazaControllerTest {

    @Mock
    private CharacterPlazaService characterPlazaService;

    @InjectMocks
    private CharacterPlazaController controller;

    @Test
    void list_公开列表匿名可访问_不依赖登录态() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("list", List.of());
        when(characterPlazaService.listPublicCharacters(1, 12, null, "hot")).thenReturn(payload);

        R result = controller.list(1, 12, null, "hot");

        assertEquals(200, result.getCode());
        assertSame(payload, result.getData());
    }

    @Test
    void detail_角色存在_匿名访问返回详情() {
        Map<String, Object> detail = new HashMap<>();
        detail.put("found", true);
        when(characterPlazaService.getCharacterDetail(1L, null)).thenReturn(detail);

        R result = controller.detail(1L, null);

        assertEquals(200, result.getCode());
        verify(characterPlazaService).getCharacterDetail(1L, null);
    }

    @Test
    void use_未登录应抛业务异常_证明requireUserId生效() {
        // 无 SecurityContext，requireUserId 必须抛 BusinessException（401）
        assertThrows(BusinessException.class, () -> controller.use(1L));
        verify(characterPlazaService, never()).incrementUseCount(anyLong());
    }

    @Test
    void share_未登录应抛业务异常_证明requireUserId生效() {
        assertThrows(BusinessException.class, () -> controller.share(1L));
        verify(characterPlazaService, never()).shareToPlaza(anyLong(), anyLong());
    }

    @Test
    void unshare_未登录应抛业务异常_证明requireUserId生效() {
        assertThrows(BusinessException.class, () -> controller.unshare(1L));
        verify(characterPlazaService, never()).unshareFromPlaza(anyLong(), anyLong());
    }
}

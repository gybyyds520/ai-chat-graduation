package com.example.aichat.common.security;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.entity.AiElf;
import com.example.aichat.service.AiElfService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * B2: ElfOwnershipChecker 单元测试。
 * 验证注解切面背后的归属校验逻辑生效。
 */
@ExtendWith(MockitoExtension.class)
class ElfOwnershipCheckerTest {

    @Mock
    private AiElfService aiElfService;

    @InjectMocks
    private ElfOwnershipChecker checker;

    @Test
    void supports_返回elf标识() {
        assertEquals("elf", checker.supports());
    }

    @Test
    void 归属一致_不抛异常() {
        AiElf elf = new AiElf();
        elf.setId(10L);
        elf.setUserId(42L);
        when(aiElfService.getElfById(10L)).thenReturn(elf);

        assertDoesNotThrow(() -> checker.checkOwnership(10L, 42L));
    }

    @Test
    void 归属他人_抛BusinessException() {
        AiElf elf = new AiElf();
        elf.setId(10L);
        elf.setUserId(999L); // 属于用户999
        when(aiElfService.getElfById(10L)).thenReturn(elf);

        // 用户42试图访问用户999的精灵
        BusinessException ex = assertThrows(BusinessException.class,
                () -> checker.checkOwnership(10L, 42L));
        assertNotNull(ex.getMessage());
    }

    @Test
    void 精灵不存在_抛异常() {
        when(aiElfService.getElfById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> checker.checkOwnership(999L, 42L));
        assertNotNull(ex.getMessage());
    }

    @Test
    void resourceId为Number_自动转Long() {
        AiElf elf = new AiElf();
        elf.setId(5L);
        elf.setUserId(42L);
        when(aiElfService.getElfById(5L)).thenReturn(elf);

        // 传入 Integer 而非 Long，应自动转换
        assertDoesNotThrow(() -> checker.checkOwnership(5, 42L));
    }
}

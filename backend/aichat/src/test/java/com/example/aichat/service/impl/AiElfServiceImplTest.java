package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.entity.AiElf;
import com.example.aichat.entity.AiElfSleep;
import com.example.aichat.mapper.*;
import com.example.aichat.service.SoulEngineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AI 模块 - 精灵服务单元测试（Mock Mapper）。
 *
 * 覆盖点：
 *   1. 创建精灵：已有则复用、没有则插入且默认名称"小精灵"；
 *   2. 睡眠记录边界：quality 超出 0-10 抛业务异常；
 *   3. 睡眠记录边界：睡眠时间为空抛业务异常；
 *   4. 正常睡眠记录计算时长（跨夜自动 +24h）。
 */
@ExtendWith(MockitoExtension.class)
class AiElfServiceImplTest {

    @Mock private AiElfMapper aiElfMapper;
    @Mock private AiElfAlarmMapper aiElfAlarmMapper;
    @Mock private AiElfScheduleMapper aiElfScheduleMapper;
    @Mock private AiElfSleepMapper aiElfSleepMapper;
    @Mock private AiElfFocusMapper aiElfFocusMapper;
    @Mock private AiElfHealthReminderMapper aiElfHealthReminderMapper;
    @Mock private SleepAnalysisReportMapper sleepAnalysisReportMapper;
    @Mock private ElfVoiceCommandMapper elfVoiceCommandMapper;
    @Mock private HealthAnalysisRecordMapper healthAnalysisRecordMapper;
    @Mock private AiElfInteractionLogMapper aiElfInteractionLogMapper;
    @Mock private SoulEngineService soulEngineService;

    @InjectMocks
    private AiElfServiceImpl service;

    @Test
    void 创建精灵_已存在_直接复用不插入() {
        AiElf existing = new AiElf();
        existing.setId(5L);
        existing.setUserId(1L);
        when(aiElfMapper.findByUserId(1L)).thenReturn(existing);

        AiElf result = service.createElf(1L, "newName", "dark");
        assertSame(existing, result);
        verify(aiElfMapper, never()).insert(any(AiElf.class));
    }

    @Test
    void 创建精灵_不存在_插入且默认名称() {
        when(aiElfMapper.findByUserId(1L)).thenReturn(null);
        AiElf result = service.createElf(1L, null, null);
        assertEquals("小精灵", result.getName());
        assertEquals("default", result.getTheme());
        assertEquals(1, result.getLevel());
        verify(aiElfMapper).insert(any(AiElf.class));
    }

    @Test
    void 睡眠记录_质量越界_抛业务异常() {
        assertThrows(BusinessException.class,
                () -> service.recordSleep(1L, 1L, "23:00", "07:00", 11));
        assertThrows(BusinessException.class,
                () -> service.recordSleep(1L, 1L, "23:00", "07:00", -1));
        verify(aiElfSleepMapper, never()).insert(any(AiElfSleep.class));
    }

    @Test
    void 睡眠记录_时间为空_抛业务异常() {
        assertThrows(BusinessException.class,
                () -> service.recordSleep(1L, 1L, "", "07:00", 5));
    }

    @Test
    void 睡眠记录_正常_计算时长跨夜() {
        AiElfSleep sleep = service.recordSleep(1L, 1L, "23:00", "07:00", 8);
        assertNotNull(sleep);
        // 8 小时（跨夜）
        assertEquals(8.0, sleep.getDurationHours(), 1e-6);
        verify(aiElfSleepMapper).insert(sleep);
    }

    @Test
    void 互动_触发精灵灵魂演化() {
        AiElf elf = new AiElf();
        elf.setId(9L);
        elf.setUserId(1L);
        elf.setAffection(0);
        elf.setIntimacy(0);
        elf.setMood(80);
        elf.setFullness(70);
        elf.setExperience(0);
        elf.setRelationshipLevel(1);
        when(aiElfMapper.selectById(9L)).thenReturn(elf);

        AiElf result = service.addAffection(9L, 1L, 0, "feed");

        assertNotNull(result);
        // 灵魂引擎被触发（feed 经历）
        verify(soulEngineService).evolveElfSoul(eq(1L), eq(9L), contains("喂食"));
    }

    @Test
    void 互动_灵魂演化异常_不影响养成主链路() {
        AiElf elf = new AiElf();
        elf.setId(10L);
        elf.setUserId(1L);
        elf.setAffection(0);
        elf.setIntimacy(0);
        elf.setMood(80);
        elf.setFullness(70);
        elf.setExperience(0);
        elf.setRelationshipLevel(1);
        when(aiElfMapper.selectById(10L)).thenReturn(elf);
        doThrow(new RuntimeException("soul down"))
                .when(soulEngineService).evolveElfSoul(any(Long.class), any(Long.class), any(String.class));

        AiElf result = service.addAffection(10L, 1L, 0, "pat");

        assertNotNull(result);
        assertTrue(result.getAffection() >= 3); // 好感度照常增加
        verify(soulEngineService).evolveElfSoul(any(Long.class), any(Long.class), any(String.class));
    }
}

package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.entity.AiElf;
import com.example.aichat.entity.AiElfInteractionLog;
import com.example.aichat.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 桌面养成四维状态（心情 mood / 饱腹 fullness / 亲密度 intimacy / 经验 experience）服务单元测试。
 *
 * 覆盖：喂食效果、对话心情影响、睡眠恢复效果、自然衰减粒度、状态对动作表现的建议、上限钳制、精灵不存在拒绝。
 */
@ExtendWith(MockitoExtension.class)
class AiElfGrowthServiceTest {

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

    @InjectMocks
    private AiElfServiceImpl service;

    private AiElf elf(long id, long userId, Integer mood, Integer fullness, int intimacy) {
        AiElf e = new AiElf();
        e.setId(id);
        e.setUserId(userId);
        e.setAffection(10);
        e.setIntimacy(intimacy);
        e.setRelationshipLevel(2);
        e.setExperience(0);
        e.setMood(mood);
        e.setFullness(fullness);
        return e;
    }

    @Test
    void feed_饱腹加20心情加5并写日志() {
        AiElf e = elf(1L, 1L, 80, 70, 100);
        when(aiElfMapper.selectById(1L)).thenReturn(e);
        when(aiElfSleepMapper.getAverageSleepQuality(1L)).thenReturn(null);

        AiElf result = service.feed(1L, 1L);

        assertEquals(90, result.getFullness()); // 70+20
        assertEquals(85, result.getMood());      // 80+5
        verify(aiElfInteractionLogMapper).insert((AiElfInteractionLog) any());
    }

    @Test
    void chat互动_心情加2() {
        AiElf e = elf(1L, 1L, 80, 70, 100);
        when(aiElfMapper.selectById(1L)).thenReturn(e);
        when(aiElfSleepMapper.getAverageSleepQuality(1L)).thenReturn(null);

        AiElf result = service.addAffection(1L, 1L, 2, "chat");

        assertEquals(82, result.getMood());
        assertEquals(70, result.getFullness()); // 聊天不影响饱腹
    }

    @Test
    void sleep互动_心情加30饱腹减5() {
        AiElf e = elf(1L, 1L, 60, 50, 100);
        when(aiElfMapper.selectById(1L)).thenReturn(e);
        when(aiElfSleepMapper.getAverageSleepQuality(1L)).thenReturn(null);

        AiElf result = service.addAffection(1L, 1L, 3, "sleep");

        assertEquals(90, result.getMood());   // 60+30
        assertEquals(45, result.getFullness()); // 50-5
    }

    @Test
    void applyNaturalDecay_满10分钟_心情饱腹各减1() {
        AiElf e = elf(1L, 1L, 50, 40, 100);
        when(aiElfMapper.selectById(1L)).thenReturn(e);

        AiElf result = service.applyNaturalDecay(1L, 1L, 20); // 20分钟 = 2个粒度

        assertEquals(48, result.getMood());     // 50-2
        assertEquals(38, result.getFullness()); // 40-2
        verify(aiElfMapper).updateById(e);
    }

    @Test
    void applyNaturalDecay_不足10分钟_不更新() {
        AiElf e = elf(1L, 1L, 50, 40, 100);
        when(aiElfMapper.selectById(1L)).thenReturn(e);

        AiElf result = service.applyNaturalDecay(1L, 1L, 9);

        assertEquals(50, result.getMood());
        verify(aiElfMapper, never()).updateById(any(AiElf.class));
    }

    @Test
    void getGrowthState_低心情低饱腹高亲密_返回动作建议() {
        // mood=20<30 lethargic; fullness=10<20 hungry; intimacy=80>=80 affectionate
        AiElf e = elf(1L, 1L, 20, 10, 800);
        when(aiElfMapper.selectById(1L)).thenReturn(e);

        Map<String, Object> state = service.getGrowthState(1L, 1L);

        assertEquals(20, state.get("mood"));
        assertEquals(10, state.get("fullness"));
        assertEquals(true, state.get("lethargic"));
        assertEquals(true, state.get("hungry"));
        assertEquals(true, state.get("affectionate"));
    }

    @Test
    void feed_心情饱腹上限100钳制() {
        AiElf e = elf(1L, 1L, 99, 95, 100);
        when(aiElfMapper.selectById(1L)).thenReturn(e);
        when(aiElfSleepMapper.getAverageSleepQuality(1L)).thenReturn(null);

        AiElf result = service.feed(1L, 1L);

        assertTrue(result.getFullness() <= 100);
        assertTrue(result.getMood() <= 100);
    }

    @Test
    void growth_精灵不存在_抛业务异常() {
        when(aiElfMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.getGrowthState(999L, 1L));
        assertThrows(BusinessException.class, () -> service.feed(999L, 1L));
    }
}

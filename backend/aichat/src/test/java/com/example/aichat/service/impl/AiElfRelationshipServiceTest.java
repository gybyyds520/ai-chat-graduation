package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.entity.AiElf;
import com.example.aichat.entity.AiElfInteractionLog;
import com.example.aichat.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AI 精灵养成深化（好感度/关系等级）服务单元测试（Mock Mapper）。
 *
 * 覆盖：addAffection（增加/上限/自动升级）、getRelationshipInfo（等级/称号/下一阈值）、
 *       recordInteraction、dailyReward（幂等）、getInteractionHistory、精灵不存在拒绝。
 */
@ExtendWith(MockitoExtension.class)
class AiElfRelationshipServiceTest {

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

    private AiElf elf(long id, long userId, int affection, int intimacy, int level) {
        AiElf e = new AiElf();
        e.setId(id);
        e.setUserId(userId);
        e.setAffection(affection);
        e.setIntimacy(intimacy);
        e.setRelationshipLevel(level);
        e.setExperience(0);
        return e;
    }

    @Test
    void addAffection_正常_增加好感与亲密度并写日志() {
        AiElf e = elf(1L, 1L, 0, 0, 1);
        when(aiElfMapper.selectById(1L)).thenReturn(e);
        when(aiElfSleepMapper.getAverageSleepQuality(1L)).thenReturn(null); // 无睡眠加成

        AiElf result = service.addAffection(1L, 1L, 5, "chat");

        assertEquals(5, result.getAffection());
        assertEquals(50, result.getIntimacy());
        assertEquals(1, result.getRelationshipLevel());
        verify(aiElfInteractionLogMapper).insert(any(AiElfInteractionLog.class));
        verify(aiElfMapper, atLeastOnce()).updateById(e);
    }

    @Test
    void addAffection_好感上限100_钳制() {
        AiElf e = elf(1L, 1L, 98, 900, 6);
        when(aiElfMapper.selectById(1L)).thenReturn(e);
        when(aiElfSleepMapper.getAverageSleepQuality(1L)).thenReturn(null);

        AiElf result = service.addAffection(1L, 1L, 20, "play");
        assertEquals(100, result.getAffection());
        // 亲密度同样不超过 1000
        assertTrue(result.getIntimacy() <= 1000);
    }

    @Test
    void addAffection_达到阈值_自动升级关系等级() {
        // 好感 5，+20 => 25，跨过 25 阈值升至 3 级（朋友）
        AiElf e = elf(1L, 1L, 5, 50, 1);
        when(aiElfMapper.selectById(1L)).thenReturn(e);
        when(aiElfSleepMapper.getAverageSleepQuality(1L)).thenReturn(null);

        AiElf result = service.addAffection(1L, 1L, 20, "feed");
        assertEquals(25, result.getAffection());
        assertEquals(3, result.getRelationshipLevel());
    }

    @Test
    void getRelationshipInfo_返回等级称号与下一阈值() {
        // 好感 30：3 级（朋友），下一阈值 45，还需 15
        AiElf e = elf(1L, 1L, 30, 300, 3);
        when(aiElfMapper.selectById(1L)).thenReturn(e);
        when(aiElfInteractionLogMapper.countSince(eq(1L), any())).thenReturn(2L);

        Map<String, Object> info = service.getRelationshipInfo(1L, 1L);
        assertEquals(30, info.get("affection"));
        assertEquals(3, info.get("relationshipLevel"));
        assertEquals("朋友", info.get("relationshipTitle"));
        assertEquals(15, info.get("nextLevelAffection"));
        assertEquals(2L, info.get("todayInteractions"));
    }

    @Test
    void recordInteraction_写入互动日志() {
        AiElfInteractionLog log = service.recordInteraction(1L, 1L, "chat", 2);
        assertEquals("chat", log.getInteractionType());
        assertEquals(2, log.getPointsGained());
        assertNotNull(log.getCreatedAt());
        verify(aiElfInteractionLogMapper).insert(log);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getInteractionHistory_分页查询() {
        Page<AiElfInteractionLog> page = new Page<>(1, 10);
        when(aiElfInteractionLogMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(page);
        assertSame(page, service.getInteractionHistory(1L, 1L, 1, 10));
    }

    @Test
    void dailyReward_首次_发放并加好感() {
        AiElf e = elf(1L, 1L, 10, 100, 2);
        when(aiElfMapper.selectById(1L)).thenReturn(e);
        when(aiElfInteractionLogMapper.countByTypeSince(eq(1L), eq("daily_reward"), any())).thenReturn(0L);
        when(aiElfInteractionLogMapper.countSince(eq(1L), any())).thenReturn(1L);

        Map<String, Object> result = service.dailyReward(1L, 1L);
        assertEquals(true, result.get("rewarded"));
        verify(aiElfMapper).updateById(e);
        verify(aiElfInteractionLogMapper).insert(any(AiElfInteractionLog.class));
    }

    @Test
    void dailyReward_重复_幂等不重复发放() {
        AiElf e = elf(1L, 1L, 10, 100, 2);
        when(aiElfMapper.selectById(1L)).thenReturn(e);
        when(aiElfInteractionLogMapper.countByTypeSince(eq(1L), eq("daily_reward"), any())).thenReturn(1L);
        when(aiElfInteractionLogMapper.countSince(eq(1L), any())).thenReturn(1L);

        Map<String, Object> result = service.dailyReward(1L, 1L);
        assertEquals(false, result.get("rewarded"));
        // 已领取：不应再更新精灵
        verify(aiElfMapper, never()).updateById(any(AiElf.class));
    }

    @Test
    void addAffection_精灵不存在_抛业务异常() {
        when(aiElfMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.addAffection(999L, 1L, 5, "chat"));
    }
}

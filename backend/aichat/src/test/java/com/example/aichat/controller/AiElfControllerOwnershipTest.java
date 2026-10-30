package com.example.aichat.controller;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.UserErrorCode;
import com.example.aichat.common.security.AuthContext;
import com.example.aichat.common.security.UserPrincipal;
import com.example.aichat.entity.AiElf;
import com.example.aichat.entity.AiElfAlarm;
import com.example.aichat.entity.AiElfSchedule;
import com.example.aichat.service.AiElfService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 精灵接口 P0 越权（IDOR）归属校验单元测试。
 *
 * 直接实例化 Controller，Mock {@link AiElfService}，通过 SecurityContextHolder
 * 模拟当前登录用户，验证各端点在进入业务逻辑前是否完成 elfId 归属校验：
 *   1. 归属匹配（elf.userId == currentUserId）→ 正常执行下游；
 *   2. 归属拒绝（elf.userId != currentUserId）→ 抛 BusinessException，下游不被调用；
 *   3. 精灵不存在 → 抛 BusinessException；
 *   4. alarmId / scheduleId 维度端点：由记录反查 elfId 后校验归属；
 *   5. 未登录 → 抛 TOKEN_INVALID。
 *
 * 覆盖端点：updateElf、addExperience、getAlarms、getSchedules、getSleepRecords、
 *           getSleepSummary、getVoiceCommands、getHealthReports、getHealthSummary、
 *           updateAlarm、deleteAlarm、updateSchedule、deleteSchedule。
 */
@ExtendWith(MockitoExtension.class)
class AiElfControllerOwnershipTest {

    private static final Long CURRENT_USER = 1L;
    private static final Long OTHER_USER = 999L;
    private static final Long ELF_ID = 10L;

    @Mock
    private AiElfService aiElfService;

    private AiElfController controller;

    @BeforeEach
    void setUp() {
        controller = new AiElfController();
        ReflectionTestUtils.setField(controller, "aiElfService", aiElfService);
        authenticateAs(CURRENT_USER, "current");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId, String username) {
        UserPrincipal principal = new UserPrincipal(userId, username, List.of("ROLE_USER"));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private AiElf elf(Long id, Long userId) {
        AiElf elf = new AiElf();
        elf.setId(id);
        elf.setUserId(userId);
        return elf;
    }

    private AiElfAlarm alarm(Long id, Long elfId, Long userId) {
        AiElfAlarm alarm = new AiElfAlarm();
        alarm.setId(id);
        alarm.setElfId(elfId);
        alarm.setUserId(userId);
        return alarm;
    }

    private AiElfSchedule schedule(Long id, Long elfId, Long userId) {
        AiElfSchedule schedule = new AiElfSchedule();
        schedule.setId(id);
        schedule.setElfId(elfId);
        schedule.setUserId(userId);
        return schedule;
    }

    // ---------- elfId 维度端点（B2: 已迁移到 @OwnershipCheck AOP 注解） ----------
    // 归属拒绝/不存在的测试移至 ElfOwnershipCheckerTest（AOP 切面层测试）。
    // 此处仅验证 Controller 方法本身正常调用下游 Service。

    @Test
    void updateElf_归属匹配_正常执行下游() {
        when(aiElfService.updateElf(eq(ELF_ID), any())).thenReturn(elf(ELF_ID, CURRENT_USER));

        assertDoesNotThrow(() -> controller.updateElf(ELF_ID, Map.of("name", "newName")));
        verify(aiElfService).updateElf(eq(ELF_ID), any());
    }

    @Test
    void addExperience_正常增加经验() {
        assertDoesNotThrow(() -> controller.addExperience(ELF_ID, Map.of("experience", 50)));
        verify(aiElfService).addExperience(eq(ELF_ID), eq(50));
    }

    @Test
    void getAlarms_正常返回() {
        when(aiElfService.getAlarms(ELF_ID)).thenReturn(List.of());

        assertDoesNotThrow(() -> controller.getAlarms(ELF_ID));
        verify(aiElfService).getAlarms(ELF_ID);
    }

    @Test
    void getSleepRecords_正常返回() {
        when(aiElfService.getSleepRecords(ELF_ID)).thenReturn(List.of());

        assertDoesNotThrow(() -> controller.getSleepRecords(ELF_ID));
        verify(aiElfService).getSleepRecords(ELF_ID);
    }

    @Test
    void getHealthReports_正常返回() {
        when(aiElfService.getHealthReports(eq(ELF_ID), anyInt())).thenReturn(List.of());

        assertDoesNotThrow(() -> controller.getHealthReports(ELF_ID, 7));
        verify(aiElfService).getHealthReports(eq(ELF_ID), eq(7));
    }

    @Test
    void getSchedules_归属拒绝_抛异常() {
        when(aiElfService.getElfById(ELF_ID)).thenReturn(elf(ELF_ID, OTHER_USER));

        assertThrows(BusinessException.class, () -> controller.getSchedules(ELF_ID));
        verify(aiElfService, never()).getSchedules(any());
    }

    @Test
    void getSleepSummary_归属匹配_正常返回() {
        when(aiElfService.getElfById(ELF_ID)).thenReturn(elf(ELF_ID, CURRENT_USER));
        when(aiElfService.getSleepSummary(ELF_ID)).thenReturn(Map.of("avgQuality", 8));

        assertDoesNotThrow(() -> controller.getSleepSummary(ELF_ID));
        verify(aiElfService).getSleepSummary(ELF_ID);
    }

    @Test
    void getVoiceCommands_归属拒绝_抛异常() {
        when(aiElfService.getElfById(ELF_ID)).thenReturn(elf(ELF_ID, OTHER_USER));

        assertThrows(BusinessException.class, () -> controller.getVoiceCommands(ELF_ID));
        verify(aiElfService, never()).getVoiceCommands(any());
    }

    @Test
    void getHealthSummary_归属拒绝_抛异常() {
        when(aiElfService.getElfById(ELF_ID)).thenReturn(elf(ELF_ID, OTHER_USER));

        assertThrows(BusinessException.class, () -> controller.getHealthSummary(ELF_ID));
        verify(aiElfService, never()).getHealthSummary(any());
    }

    // ---------- alarmId / scheduleId 维度端点（反查 elfId 后校验） ----------

    @Test
    void updateAlarm_闹钟反查到他人精灵_抛异常且不更新() {
        Long alarmId = 55L;
        when(aiElfService.getAlarmById(alarmId)).thenReturn(alarm(alarmId, ELF_ID, OTHER_USER));
        when(aiElfService.getElfById(ELF_ID)).thenReturn(elf(ELF_ID, OTHER_USER));

        assertThrows(BusinessException.class,
                () -> controller.updateAlarm(alarmId, Map.of("enabled", true)));
        verify(aiElfService, never()).updateAlarm(any(), any());
    }

    @Test
    void updateAlarm_闹钟不存在_抛异常() {
        Long alarmId = 55L;
        when(aiElfService.getAlarmById(alarmId)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.updateAlarm(alarmId, Map.of()));
        assertEquals("闹钟不存在", ex.getMessage());
        verify(aiElfService, never()).updateAlarm(any(), any());
    }

    @Test
    void deleteAlarm_闹钟反查到他人精灵_抛异常且不删除() {
        Long alarmId = 56L;
        when(aiElfService.getAlarmById(alarmId)).thenReturn(alarm(alarmId, ELF_ID, OTHER_USER));
        when(aiElfService.getElfById(ELF_ID)).thenReturn(elf(ELF_ID, OTHER_USER));

        assertThrows(BusinessException.class, () -> controller.deleteAlarm(alarmId));
        verify(aiElfService, never()).deleteAlarm(any());
    }

    @Test
    void updateSchedule_日程反查到他人精灵_抛异常且不更新() {
        Long scheduleId = 77L;
        when(aiElfService.getScheduleById(scheduleId)).thenReturn(schedule(scheduleId, ELF_ID, OTHER_USER));
        when(aiElfService.getElfById(ELF_ID)).thenReturn(elf(ELF_ID, OTHER_USER));

        assertThrows(BusinessException.class,
                () -> controller.updateSchedule(scheduleId, Map.of("completed", true)));
        verify(aiElfService, never()).updateSchedule(any(), any());
    }

    @Test
    void updateSchedule_日程不存在_抛异常() {
        Long scheduleId = 77L;
        when(aiElfService.getScheduleById(scheduleId)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.updateSchedule(scheduleId, Map.of()));
        assertEquals("日程不存在", ex.getMessage());
        verify(aiElfService, never()).updateSchedule(any(), any());
    }

    @Test
    void deleteSchedule_日程反查到他人精灵_抛异常且不删除() {
        Long scheduleId = 78L;
        when(aiElfService.getScheduleById(scheduleId)).thenReturn(schedule(scheduleId, ELF_ID, OTHER_USER));
        when(aiElfService.getElfById(ELF_ID)).thenReturn(elf(ELF_ID, OTHER_USER));

        assertThrows(BusinessException.class, () -> controller.deleteSchedule(scheduleId));
        verify(aiElfService, never()).deleteSchedule(any());
    }

    // ---------- body 携带 elfId 的写端点（P0 补全覆盖：必须校验 elfId 归属） ----------

    @Test
    void createAlarm_elfId属他人_抛异常且不创建() {
        when(aiElfService.getElfById(ELF_ID)).thenReturn(elf(ELF_ID, OTHER_USER));

        assertThrows(BusinessException.class, () -> controller.createAlarm(Map.of(
                "elfId", ELF_ID, "userId", CURRENT_USER, "title", "t", "time", "07:00")));
        verify(aiElfService, never()).createAlarm(any(), any(), any(), any(), any());
    }

    @Test
    void createAlarm_elfId属本人_正常创建() {
        when(aiElfService.getElfById(ELF_ID)).thenReturn(elf(ELF_ID, CURRENT_USER));

        assertDoesNotThrow(() -> controller.createAlarm(Map.of(
                "elfId", ELF_ID, "userId", CURRENT_USER, "title", "t", "time", "07:00")));
        verify(aiElfService).createAlarm(eq(ELF_ID), eq(CURRENT_USER), eq("t"), eq("07:00"), any());
    }

    @Test
    void createSchedule_elfId属他人_抛异常且不创建() {
        when(aiElfService.getElfById(ELF_ID)).thenReturn(elf(ELF_ID, OTHER_USER));

        assertThrows(BusinessException.class, () -> controller.createSchedule(Map.of(
                "elfId", ELF_ID, "userId", CURRENT_USER, "title", "t", "startTime", "2026-01-01T08:00:00")));
        verify(aiElfService, never()).createSchedule(any(), any(), any(), any(), any());
    }

    @Test
    void recordSleep_elfId属他人_抛异常且不记录() {
        when(aiElfService.getElfById(ELF_ID)).thenReturn(elf(ELF_ID, OTHER_USER));

        assertThrows(BusinessException.class, () -> controller.recordSleep(Map.of(
                "elfId", ELF_ID, "userId", CURRENT_USER,
                "sleepTime", "23:00", "wakeTime", "07:00", "quality", 8)));
        verify(aiElfService, never()).recordSleep(any(), any(), any(), any(), anyInt());
    }

    @Test
    void createSmartAlarm_elfId属他人_抛异常且不创建() {
        when(aiElfService.getElfById(ELF_ID)).thenReturn(elf(ELF_ID, OTHER_USER));

        assertThrows(BusinessException.class, () -> controller.createSmartAlarm(Map.of(
                "elfId", ELF_ID, "userId", CURRENT_USER, "title", "t", "baseTime", "07:00")));
        verify(aiElfService, never()).createSmartAlarm(any(), any(), any(), any());
    }

    @Test
    void adjustAlarmBySleepQuality_闹钟属他人_抛异常且不调整() {
        Long alarmId = 55L;
        when(aiElfService.getAlarmById(alarmId)).thenReturn(alarm(alarmId, ELF_ID, OTHER_USER));
        when(aiElfService.getElfById(ELF_ID)).thenReturn(elf(ELF_ID, OTHER_USER));

        assertThrows(BusinessException.class, () -> controller.adjustAlarmBySleepQuality(Map.of(
                "elfId", ELF_ID, "userId", CURRENT_USER, "alarmId", alarmId)));
        verify(aiElfService, never()).adjustAlarmBySleepQuality(any(), any(), any());
    }

    @Test
    void processVoiceCommand_elfId属他人_抛异常且不执行() {
        when(aiElfService.getElfById(ELF_ID)).thenReturn(elf(ELF_ID, OTHER_USER));

        assertThrows(BusinessException.class, () -> controller.processVoiceCommand(Map.of(
                "elfId", ELF_ID, "userId", CURRENT_USER, "commandText", "设定闹钟")));
        verify(aiElfService, never()).processVoiceCommand(any(), any(), any());
    }

    @Test
    void generateHealthReport_elfId属他人_抛异常且不生成() {
        when(aiElfService.getElfById(ELF_ID)).thenReturn(elf(ELF_ID, OTHER_USER));

        assertThrows(BusinessException.class, () -> controller.generateHealthReport(Map.of(
                "elfId", ELF_ID, "userId", CURRENT_USER)));
        verify(aiElfService, never()).generateHealthReport(any(), any());
    }

    // ---------- 登录态 ----------

    @Test
    void 未登录调用_抛TOKEN_INVALID() {
        SecurityContextHolder.clearContext();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.getHealthSummary(ELF_ID));
        assertEquals(UserErrorCode.TOKEN_INVALID, ex.getErrorCode());
        verify(aiElfService, never()).getElfById(any());
    }

    @Test
    void AuthContext_当前用户_可用() {
        //  sanity：确认 setUp 注入的登录态确实是 CURRENT_USER，避免用例误判
        assertEquals(CURRENT_USER, AuthContext.requireUserId());
    }
}

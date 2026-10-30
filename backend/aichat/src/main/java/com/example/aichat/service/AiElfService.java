package com.example.aichat.service;

import com.example.aichat.entity.AiElf;
import com.example.aichat.entity.AiElfAlarm;
import com.example.aichat.entity.AiElfInteractionLog;
import com.example.aichat.entity.AiElfSchedule;
import com.example.aichat.entity.AiElfSleep;
import com.example.aichat.entity.AiElfFocus;
import com.example.aichat.entity.AiElfHealthReminder;
import com.example.aichat.entity.SleepAnalysisReport;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Map;

public interface AiElfService {
    
    AiElf getElfByUserId(Long userId);

    /**
     * 按主键查询精灵（用于归属校验）。不存在返回 null。
     */
    AiElf getElfById(Long elfId);

    /**
     * 按主键查询闹钟（用于由 alarmId 反查 elfId 后做归属校验）。不存在返回 null。
     */
    AiElfAlarm getAlarmById(Long alarmId);

    /**
     * 按主键查询日程（用于由 scheduleId 反查 elfId 后做归属校验）。不存在返回 null。
     */
    AiElfSchedule getScheduleById(Long scheduleId);

    AiElf createElf(Long userId, String name, String theme);
    
    AiElf updateElf(Long elfId, Map<String, Object> updates);
    
    void addExperience(Long elfId, int experience);
    
    AiElfAlarm createAlarm(Long elfId, Long userId, String title, String time, String repeatDays);
    
    List<AiElfAlarm> getAlarms(Long elfId);
    
    AiElfAlarm updateAlarm(Long alarmId, Map<String, Object> updates);
    
    void deleteAlarm(Long alarmId);
    
    AiElfSchedule createSchedule(Long elfId, Long userId, String title, String startTime, String endTime);
    
    List<AiElfSchedule> getSchedules(Long elfId);
    
    AiElfSchedule updateSchedule(Long scheduleId, Map<String, Object> updates);
    
    void deleteSchedule(Long scheduleId);
    
    AiElfSleep recordSleep(Long elfId, Long userId, String sleepTime, String wakeTime, int quality);
    
    List<AiElfSleep> getSleepRecords(Long elfId);
    
    Map<String, Object> getSleepSummary(Long elfId);
    
    SleepAnalysisReport generateSleepReport(Long elfId, Long userId);
    
    List<SleepAnalysisReport> getSleepReports(Long elfId, int limit);
    
    AiElfFocus startFocus(Long elfId, Long userId, String goal);
    
    AiElfFocus endFocus(Long elfId, Long userId);
    
    AiElfFocus getCurrentFocus(Long elfId);
    
    List<AiElfFocus> getFocusRecords(Long elfId);
    
    Map<String, Object> getFocusSummary(Long elfId);
    
    AiElfHealthReminder createHealthReminder(Long elfId, Long userId, String reminderType, int intervalMinutes);
    
    List<AiElfHealthReminder> getHealthReminders(Long elfId);
    
    AiElfHealthReminder updateHealthReminder(Long reminderId, Map<String, Object> updates);
    
    void deleteHealthReminder(Long reminderId);
    
    void checkHealthReminders(Long elfId);
    
    AiElfAlarm createSmartAlarm(Long elfId, Long userId, String title, String baseTime);
    
    AiElfAlarm adjustAlarmBySleepQuality(Long elfId, Long userId, Long alarmId);
    
    String processVoiceCommand(Long elfId, Long userId, String commandText);
    
    List<com.example.aichat.entity.ElfVoiceCommand> getVoiceCommands(Long elfId);
    
    com.example.aichat.entity.HealthAnalysisRecord generateHealthReport(Long elfId, Long userId);
    
    List<com.example.aichat.entity.HealthAnalysisRecord> getHealthReports(Long elfId, int limit);
    
    Map<String, Object> getHealthSummary(Long elfId);

    // ==================== AI 精灵养成深化（好感度/关系等级） ====================

    /**
     * 增加好感度（上限 100），同时增加亲密度，达到阈值自动升级关系等级；
     * 同步写入互动日志并增加经验值；睡眠评分高时给予加成。
     *
     * @param points 本次基础好感度（<=0 时按互动类型默认值）
     * @return 更新后的精灵
     */
    AiElf addAffection(Long elfId, Long userId, int points, String interactionType);

    /**
     * 返回关系信息：好感度、关系等级、亲密度、称号、下一等级所需好感度、今日互动次数。
     */
    Map<String, Object> getRelationshipInfo(Long elfId, Long userId);

    /** 记录一条互动日志（纯记录，不改精灵属性）。 */
    AiElfInteractionLog recordInteraction(Long elfId, Long userId, String interactionType, int points);

    /** 分页查询互动历史（按时间倒序）。 */
    IPage<AiElfInteractionLog> getInteractionHistory(Long elfId, Long userId, int page, int size);

    /**
     * 每日登录奖励（幂等）：当天首次互动额外加好感度，重复调用不重复发放。
     * 返回是否本次新发放。
     */
    Map<String, Object> dailyReward(Long elfId, Long userId);

    // ==================== 桌面养成四维状态（心情/饱腹/亲密度/经验） ====================

    /**
     * 喂食：饱腹度 +20、心情 +5（均钳制 0-100），写互动日志并加经验。
     * @return 更新后的精灵
     */
    AiElf feed(Long elfId, Long userId);

    /**
     * 摸头：亲密度 +10、心情 +8（均钳制 0-100），写互动日志并加经验。
     * @return 更新后的精灵
     */
    AiElf pat(Long elfId, Long userId);

    /**
     * 状态自然衰减：距上次互动经过 minutes 分钟，每满 10 分钟心情 -1、饱腹 -1（不低于 0）。
     * 惰性更新：仅当跨过 10 分钟粒度时才落库；无衰减则原样返回。
     * @param minutes 距上次互动的分钟数（调用方计算）
     * @return 更新后的精灵（无变化时返回当前态）
     */
    AiElf applyNaturalDecay(Long elfId, Long userId, long minutes);

    /**
     * 返回养成状态：心情/饱腹/亲密度/经验值，以及基于阈值的动作表现建议
     * （mood<30 动作变慢低头、fullness<20 无精神、intimacy>80 主动挥手跟随）。
     */
    Map<String, Object> getGrowthState(Long elfId, Long userId);
}
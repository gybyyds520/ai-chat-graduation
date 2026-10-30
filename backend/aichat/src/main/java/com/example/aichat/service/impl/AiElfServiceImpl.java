package com.example.aichat.service.impl;

import com.example.aichat.entity.AiElf;
import com.example.aichat.entity.AiElfAlarm;
import com.example.aichat.entity.AiElfFocus;
import com.example.aichat.entity.AiElfHealthReminder;
import com.example.aichat.entity.AiElfInteractionLog;
import com.example.aichat.entity.AiElfSchedule;
import com.example.aichat.entity.AiElfSleep;
import com.example.aichat.entity.ElfVoiceCommand;
import com.example.aichat.entity.HealthAnalysisRecord;
import com.example.aichat.entity.SleepAnalysisReport;
import com.example.aichat.mapper.AiElfAlarmMapper;
import com.example.aichat.mapper.AiElfFocusMapper;
import com.example.aichat.mapper.AiElfHealthReminderMapper;
import com.example.aichat.mapper.AiElfInteractionLogMapper;
import com.example.aichat.mapper.AiElfMapper;
import com.example.aichat.mapper.AiElfScheduleMapper;
import com.example.aichat.mapper.AiElfSleepMapper;
import com.example.aichat.mapper.ElfVoiceCommandMapper;
import com.example.aichat.mapper.HealthAnalysisRecordMapper;
import com.example.aichat.mapper.SleepAnalysisReportMapper;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.AiErrorCode;
import com.example.aichat.event.UserDataCaptureEvent;
import com.example.aichat.service.AiElfService;
import com.example.aichat.service.SoulEngineService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiElfServiceImpl implements AiElfService {

    private static final Logger logger = LoggerFactory.getLogger(AiElfServiceImpl.class);

    // ========== 关系等级体系（按好感度阈值） ==========
    // 阈值：1级0、2级10、3级25、4级45、5级70、6级100（满级）
    private static final int[] LEVEL_THRESHOLDS = {0, 10, 25, 45, 70, 100};
    private static final String[] LEVEL_TITLES = {"陌生", "熟悉", "朋友", "好友", "亲密", "挚友"};
    private static final int MAX_AFFECTION = 100;
    private static final int MAX_INTIMACY = 1000;
    // ===== 桌面养成四维状态（V40） =====
    private static final int MAX_MOOD = 100;
    private static final int DEFAULT_MOOD = 80;
    private static final int DEFAULT_FULLNESS = 70;
    private static final int MAX_FULLNESS = 100;
    private static final int DECAY_INTERVAL_MINUTES = 10;
    private static final int DECAY_STEP = 1;
    /** mood < 30 → 动作变慢/低头/叹气；fullness < 20 → 无精神；intimacy>80 主动挥手 */
    private static final int MOOD_LOW_THRESHOLD = 30;
    private static final int FULLNESS_LOW_THRESHOLD = 20;
    private static final int INTIMACY_HIGH_THRESHOLD = 80;

    private final AiElfMapper aiElfMapper;
    private final AiElfAlarmMapper aiElfAlarmMapper;
    private final AiElfScheduleMapper aiElfScheduleMapper;
    private final AiElfSleepMapper aiElfSleepMapper;
    private final AiElfFocusMapper aiElfFocusMapper;
    private final AiElfHealthReminderMapper aiElfHealthReminderMapper;
    private final SleepAnalysisReportMapper sleepAnalysisReportMapper;
    private final ElfVoiceCommandMapper elfVoiceCommandMapper;
    private final HealthAnalysisRecordMapper healthAnalysisRecordMapper;
    private final AiElfInteractionLogMapper aiElfInteractionLogMapper;
    private final SoulEngineService soulEngineService;

    public AiElfServiceImpl(AiElfMapper aiElfMapper,
                            AiElfAlarmMapper aiElfAlarmMapper,
                            AiElfScheduleMapper aiElfScheduleMapper,
                            AiElfSleepMapper aiElfSleepMapper,
                            AiElfFocusMapper aiElfFocusMapper,
                            AiElfHealthReminderMapper aiElfHealthReminderMapper,
                            SleepAnalysisReportMapper sleepAnalysisReportMapper,
                            ElfVoiceCommandMapper elfVoiceCommandMapper,
                            HealthAnalysisRecordMapper healthAnalysisRecordMapper,
                            AiElfInteractionLogMapper aiElfInteractionLogMapper,
                            SoulEngineService soulEngineService) {
        this.aiElfMapper = aiElfMapper;
        this.aiElfAlarmMapper = aiElfAlarmMapper;
        this.aiElfScheduleMapper = aiElfScheduleMapper;
        this.aiElfSleepMapper = aiElfSleepMapper;
        this.aiElfFocusMapper = aiElfFocusMapper;
        this.aiElfHealthReminderMapper = aiElfHealthReminderMapper;
        this.sleepAnalysisReportMapper = sleepAnalysisReportMapper;
        this.elfVoiceCommandMapper = elfVoiceCommandMapper;
        this.healthAnalysisRecordMapper = healthAnalysisRecordMapper;
        this.aiElfInteractionLogMapper = aiElfInteractionLogMapper;
        this.soulEngineService = soulEngineService;
    }

    /** 数据池自动采集事件发布器。 */
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public AiElf getElfByUserId(Long userId) {
        return aiElfMapper.findByUserId(userId);
    }

    @Override
    public AiElf getElfById(Long elfId) {
        return aiElfMapper.selectById(elfId);
    }

    @Override
    public AiElfAlarm getAlarmById(Long alarmId) {
        return aiElfAlarmMapper.selectById(alarmId);
    }

    @Override
    public AiElfSchedule getScheduleById(Long scheduleId) {
        return aiElfScheduleMapper.selectById(scheduleId);
    }

    @Override
    public AiElf createElf(Long userId, String name, String theme) {
        AiElf existing = aiElfMapper.findByUserId(userId);
        if (existing != null) {
            return existing;
        }
        
        AiElf elf = new AiElf();
        elf.setUserId(userId);
        elf.setName(name != null ? name : "小精灵");
        elf.setTheme(theme != null ? theme : "default");
        elf.setPersonality("friendly");
        elf.setLevel(1);
        elf.setExperience(0);
        elf.setAffection(0);
        elf.setRelationshipLevel(1);
        elf.setIntimacy(0);
        elf.setCreatedAt(LocalDateTime.now());
        elf.setUpdatedAt(LocalDateTime.now());
        
        aiElfMapper.insert(elf);
        logger.info("[AI小精灵] 用户 {} 创建小精灵: {}", userId, name);
        return elf;
    }

    @Override
    @Transactional
    public AiElf updateElf(Long elfId, Map<String, Object> updates) {
        AiElf elf = aiElfMapper.selectById(elfId);
        if (elf == null) {
            throw new BusinessException(AiErrorCode.CHARACTER_NOT_FOUND);
        }
        // 安全修复：所有者校验 - 通过 elf.userId 与当前请求用户比对，
        // 但本服务层无 HTTP 上下文，由 Controller 在调用前传入 ownerUserId 做权限校验。
        // 此处仅做安全转换保护，避免 ClassCastException。
        updates.forEach((key, value) -> {
            if (value == null) return;
            switch (key) {
                case "name": elf.setName(value.toString()); break;
                case "theme": elf.setTheme(value.toString()); break;
                case "personality": elf.setPersonality(value.toString()); break;
                case "avatar": elf.setAvatar(value.toString()); break;
            }
        });
        elf.setUpdatedAt(LocalDateTime.now());
        aiElfMapper.updateById(elf);
        return elf;
    }

    @Override
    public void addExperience(Long elfId, int experience) {
        AiElf elf = aiElfMapper.selectById(elfId);
        if (elf != null) {
            elf.setExperience(elf.getExperience() + experience);
            int level = 1 + elf.getExperience() / 100;
            elf.setLevel(level);
            elf.setUpdatedAt(LocalDateTime.now());
            aiElfMapper.updateById(elf);
        }
    }

    @Override
    public AiElfAlarm createAlarm(Long elfId, Long userId, String title, String time, String repeatDays) {
        AiElfAlarm alarm = new AiElfAlarm();
        alarm.setElfId(elfId);
        alarm.setUserId(userId);
        alarm.setTitle(title);
        alarm.setTime(time);
        alarm.setRepeatDays(repeatDays != null ? repeatDays : "daily");
        alarm.setEnabled(true);
        alarm.setRingtone("default");
        alarm.setCreatedAt(LocalDateTime.now());
        alarm.setUpdatedAt(LocalDateTime.now());
        
        aiElfAlarmMapper.insert(alarm);
        logger.info("[小精灵闹钟] 创建闹钟: {}", title);
        return alarm;
    }

    @Override
    public List<AiElfAlarm> getAlarms(Long elfId) {
        return aiElfAlarmMapper.getAlarmsByElfId(elfId);
    }

    @Override
    public AiElfAlarm updateAlarm(Long alarmId, Map<String, Object> updates) {
        AiElfAlarm alarm = aiElfAlarmMapper.selectById(alarmId);
        if (alarm != null) {
            updates.forEach((key, value) -> {
                switch (key) {
                    case "title": alarm.setTitle((String) value); break;
                    case "time": alarm.setTime((String) value); break;
                    case "repeatDays": alarm.setRepeatDays((String) value); break;
                    case "enabled": alarm.setEnabled((Boolean) value); break;
                    case "ringtone": alarm.setRingtone((String) value); break;
                    case "message": alarm.setMessage((String) value); break;
                }
            });
            alarm.setUpdatedAt(LocalDateTime.now());
            aiElfAlarmMapper.updateById(alarm);
        }
        return alarm;
    }

    @Override
    public void deleteAlarm(Long alarmId) {
        aiElfAlarmMapper.deleteById(alarmId);
    }

    @Override
    public AiElfSchedule createSchedule(Long elfId, Long userId, String title, String startTime, String endTime) {
        AiElfSchedule schedule = new AiElfSchedule();
        schedule.setElfId(elfId);
        schedule.setUserId(userId);
        schedule.setTitle(title);
        schedule.setStartTime(LocalDateTime.parse(startTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        if (endTime != null) {
            schedule.setEndTime(LocalDateTime.parse(endTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        schedule.setReminderMinutes(15);
        schedule.setCompleted(false);
        schedule.setCreatedAt(LocalDateTime.now());
        schedule.setUpdatedAt(LocalDateTime.now());
        
        aiElfScheduleMapper.insert(schedule);
        logger.info("[小精灵日程] 创建日程: {}", title);
        return schedule;
    }

    @Override
    public List<AiElfSchedule> getSchedules(Long elfId) {
        return aiElfScheduleMapper.getSchedulesByElfId(elfId);
    }

    @Override
    public AiElfSchedule updateSchedule(Long scheduleId, Map<String, Object> updates) {
        AiElfSchedule schedule = aiElfScheduleMapper.selectById(scheduleId);
        if (schedule != null) {
            updates.forEach((key, value) -> {
                switch (key) {
                    case "title": schedule.setTitle((String) value); break;
                    case "startTime": schedule.setStartTime(LocalDateTime.parse((String) value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)); break;
                    case "endTime": schedule.setEndTime(LocalDateTime.parse((String) value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)); break;
                    case "location": schedule.setLocation((String) value); break;
                    case "description": schedule.setDescription((String) value); break;
                    case "completed": schedule.setCompleted((Boolean) value); break;
                }
            });
            schedule.setUpdatedAt(LocalDateTime.now());
            aiElfScheduleMapper.updateById(schedule);
        }
        return schedule;
    }

    @Override
    public void deleteSchedule(Long scheduleId) {
        aiElfScheduleMapper.deleteById(scheduleId);
    }

    @Override
    public AiElfSleep recordSleep(Long elfId, Long userId, String sleepTime, String wakeTime, int quality) {
        // 参数校验：quality 必须在 0-10 之间，否则抛业务异常
        if (quality < 0 || quality > 10) {
            throw new BusinessException(AiErrorCode.CHAT_MESSAGE_BLANK, "睡眠质量评分必须在 0-10 之间");
        }
        if (sleepTime == null || wakeTime == null || sleepTime.isBlank() || wakeTime.isBlank()) {
            throw new BusinessException(AiErrorCode.CHAT_MESSAGE_BLANK, "睡眠时间和起床时间不能为空");
        }
        AiElfSleep sleep = new AiElfSleep();
        sleep.setElfId(elfId);
        sleep.setUserId(userId);
        sleep.setSleepTime(sleepTime);
        sleep.setWakeTime(wakeTime);
        sleep.setQuality(quality);

        // 安全修复：try-catch DateTimeParseException，避免格式错误导致 500
        LocalTime sleepLocal;
        LocalTime wakeLocal;
        try {
            sleepLocal = LocalTime.parse(sleepTime);
            wakeLocal = LocalTime.parse(wakeTime);
        } catch (java.time.format.DateTimeParseException e) {
            throw new BusinessException(AiErrorCode.CHAT_MESSAGE_BLANK, "时间格式错误，应为 HH:mm");
        }
        double duration = wakeLocal.toSecondOfDay() - sleepLocal.toSecondOfDay();
        if (duration < 0) {
            duration += 24 * 3600;
        }
        sleep.setDurationHours(duration / 3600.0);
        sleep.setRecordDate(LocalDate.now());
        sleep.setCreatedAt(LocalDateTime.now());

        aiElfSleepMapper.insert(sleep);
        logger.info("[小精灵睡眠] 记录睡眠 - 时长: {}小时", sleep.getDurationHours());
        return sleep;
    }

    @Override
    public List<AiElfSleep> getSleepRecords(Long elfId) {
        return aiElfSleepMapper.getSleepRecordsByElfId(elfId);
    }

    @Override
    public Map<String, Object> getSleepSummary(Long elfId) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("avgDuration", aiElfSleepMapper.getAverageSleepDuration(elfId));
        summary.put("avgQuality", aiElfSleepMapper.getAverageSleepQuality(elfId));
        return summary;
    }

    @Override
    public SleepAnalysisReport generateSleepReport(Long elfId, Long userId) {
        List<AiElfSleep> sleepRecords = aiElfSleepMapper.getSleepRecordsByElfId(elfId);
        
        if (sleepRecords.isEmpty()) {
            return null;
        }
        
        double avgDuration = aiElfSleepMapper.getAverageSleepDuration(elfId);
        double avgQuality = aiElfSleepMapper.getAverageSleepQuality(elfId);
        
        int sleepScore = calculateSleepScore(avgDuration, avgQuality);
        String suggestions = generateSleepSuggestions(avgDuration, avgQuality);
        
        SleepAnalysisReport report = new SleepAnalysisReport();
        report.setElfId(elfId);
        report.setUserId(userId);
        report.setReportDate(LocalDate.now());
        report.setAvgDuration(avgDuration);
        report.setAvgQuality((int) avgQuality);
        report.setSleepScore(sleepScore);
        report.setSuggestions(suggestions);
        report.setDeepSleepRatio(0.25);
        report.setCreatedAt(LocalDateTime.now());
        
        sleepAnalysisReportMapper.insert(report);
        logger.info("[睡眠报告] 生成报告 - 评分: {}", sleepScore);
        return report;
    }

    private int calculateSleepScore(double avgDuration, double avgQuality) {
        // Bug 修复（P0）：原实现时长+质量各 40 分，满分仅 80 而非 100。
        // 修正为：时长 50 分 + 质量 50 分 = 100 分。
        int score = 0;

        if (avgDuration >= 7 && avgDuration <= 9) {
            score += 50;
        } else if (avgDuration >= 6 && avgDuration < 7) {
            score += 40;
        } else if (avgDuration >= 5 && avgDuration < 6) {
            score += 25;
        } else if (avgDuration < 5) {
            score += 10;
        } else {
            // avgDuration > 9，睡眠过长也扣分
            score += 35;
        }

        if (avgQuality >= 8) {
            score += 50;
        } else if (avgQuality >= 6) {
            score += 35;
        } else if (avgQuality >= 4) {
            score += 20;
        } else {
            score += 5;
        }

        return Math.min(score, 100);
    }

    private String generateSleepSuggestions(double avgDuration, double avgQuality) {
        StringBuilder suggestions = new StringBuilder();
        
        if (avgDuration < 7) {
            suggestions.append("建议增加睡眠时间，成人建议每天7-9小时。");
        } else if (avgDuration > 9) {
            suggestions.append("睡眠时长充足，保持良好的作息规律。");
        }
        
        if (avgQuality < 6) {
            suggestions.append("建议改善睡眠环境，保持卧室安静、黑暗、凉爽。");
        }
        
        suggestions.append("睡前1小时避免使用电子设备，可以尝试阅读或冥想。");
        return suggestions.toString();
    }

    @Override
    public List<SleepAnalysisReport> getSleepReports(Long elfId, int limit) {
        return sleepAnalysisReportMapper.getReportsByElfId(elfId, limit);
    }

    @Override
    public AiElfFocus startFocus(Long elfId, Long userId, String goal) {
        AiElfFocus current = aiElfFocusMapper.getCurrentFocus(elfId);
        if (current != null && !current.getCompleted()) {
            endFocus(elfId, userId);
        }
        
        AiElfFocus focus = new AiElfFocus();
        focus.setElfId(elfId);
        focus.setUserId(userId);
        focus.setStartTime(LocalDateTime.now());
        focus.setGoal(goal);
        focus.setCompleted(false);
        focus.setCreatedAt(LocalDateTime.now());
        
        aiElfFocusMapper.insert(focus);
        logger.info("[专注计时] 开始专注 - 目标: {}", goal);
        return focus;
    }

    @Override
    @Transactional
    // 安全修复：endFocus 涉及多步写操作（更新 focus + 更新 elf + 添加经验），必须原子化
    public AiElfFocus endFocus(Long elfId, Long userId) {
        AiElfFocus focus = aiElfFocusMapper.getCurrentFocus(elfId);
        if (focus != null && !focus.getCompleted()) {
            // 安全修复：所有者校验，防止用户结束他人的专注会话
            if (!focus.getUserId().equals(userId)) {
                throw new BusinessException(AiErrorCode.CHARACTER_NOT_FOUND, "无权操作他人专注会话");
            }
            focus.setEndTime(LocalDateTime.now());
            int duration = (int) java.time.Duration.between(focus.getStartTime(), focus.getEndTime()).toMinutes();
            focus.setDurationMinutes(duration);
            focus.setCompleted(true);
            aiElfFocusMapper.updateById(focus);

            AiElf elf = aiElfMapper.selectById(elfId);
            if (elf != null) {
                elf.setFocusCount(elf.getFocusCount() + 1);
                elf.setTotalFocusTime(elf.getTotalFocusTime() + duration);
                aiElfMapper.updateById(elf);
                addExperience(elfId, duration / 10);
            }

            logger.info("[专注计时] 结束专注 - 时长: {}分钟", duration);
        }
        return focus;
    }

    @Override
    public AiElfFocus getCurrentFocus(Long elfId) {
        return aiElfFocusMapper.getCurrentFocus(elfId);
    }

    @Override
    public List<AiElfFocus> getFocusRecords(Long elfId) {
        return aiElfFocusMapper.getFocusRecordsByElfId(elfId);
    }

    @Override
    public Map<String, Object> getFocusSummary(Long elfId) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalMinutes", aiElfFocusMapper.getTotalFocusMinutes(elfId));
        summary.put("focusCount", aiElfFocusMapper.getFocusCount(elfId));
        return summary;
    }

    @Override
    public AiElfHealthReminder createHealthReminder(Long elfId, Long userId, String reminderType, int intervalMinutes) {
        AiElfHealthReminder existing = aiElfHealthReminderMapper.findByType(elfId, reminderType);
        if (existing != null) {
            return existing;
        }
        
        AiElfHealthReminder reminder = new AiElfHealthReminder();
        reminder.setElfId(elfId);
        reminder.setUserId(userId);
        reminder.setReminderType(reminderType);
        reminder.setEnabled(true);
        reminder.setIntervalMinutes(intervalMinutes);
        reminder.setCreatedAt(LocalDateTime.now());
        
        aiElfHealthReminderMapper.insert(reminder);
        return reminder;
    }

    @Override
    public List<AiElfHealthReminder> getHealthReminders(Long elfId) {
        return aiElfHealthReminderMapper.getRemindersByElfId(elfId);
    }

    @Override
    public AiElfHealthReminder updateHealthReminder(Long reminderId, Map<String, Object> updates) {
        AiElfHealthReminder reminder = aiElfHealthReminderMapper.selectById(reminderId);
        if (reminder != null) {
            updates.forEach((key, value) -> {
                switch (key) {
                    case "enabled": reminder.setEnabled((Boolean) value); break;
                    case "intervalMinutes": reminder.setIntervalMinutes((Integer) value); break;
                }
            });
            aiElfHealthReminderMapper.updateById(reminder);
        }
        return reminder;
    }

    @Override
    public void deleteHealthReminder(Long reminderId) {
        aiElfHealthReminderMapper.deleteById(reminderId);
    }

    @Override
    public void checkHealthReminders(Long elfId) {
        List<AiElfHealthReminder> reminders = aiElfHealthReminderMapper.getRemindersByElfId(elfId);
        LocalDateTime now = LocalDateTime.now();
        
        for (AiElfHealthReminder reminder : reminders) {
            if (reminder.getEnabled()) {
                LocalDateTime lastReminded = reminder.getLastReminded();
                if (lastReminded == null || java.time.Duration.between(lastReminded, now).toMinutes() >= reminder.getIntervalMinutes()) {
                    reminder.setLastReminded(now);
                    aiElfHealthReminderMapper.updateById(reminder);
                    logger.info("[健康提醒] 触发提醒: {}", reminder.getReminderType());
                }
            }
        }
    }

    @Override
    public AiElfAlarm createSmartAlarm(Long elfId, Long userId, String title, String baseTime) {
        AiElfAlarm alarm = new AiElfAlarm();
        alarm.setElfId(elfId);
        alarm.setUserId(userId);
        alarm.setTitle(title);
        alarm.setTime(baseTime);
        alarm.setSmartEnabled(true);
        alarm.setCreatedAt(LocalDateTime.now());
        
        aiElfAlarmMapper.insert(alarm);
        logger.info("[智能闹钟] 创建智能闹钟 - 时间: {}", baseTime);
        return alarm;
    }

    @Override
    public AiElfAlarm adjustAlarmBySleepQuality(Long elfId, Long userId, Long alarmId) {
        AiElfAlarm alarm = aiElfAlarmMapper.selectById(alarmId);
        if (alarm == null || !alarm.getSmartEnabled()) {
            return alarm;
        }
        
        List<AiElfSleep> recentSleep = aiElfSleepMapper.getSleepRecordsByElfId(elfId);
        if (recentSleep.isEmpty()) {
            return alarm;
        }
        
        double avgQuality = aiElfSleepMapper.getAverageSleepQuality(elfId);
        int adjustmentMinutes = 0;
        
        if (avgQuality < 5) {
            adjustmentMinutes = 30;
        } else if (avgQuality < 7) {
            adjustmentMinutes = 15;
        }
        
        if (adjustmentMinutes > 0) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime alarmTime = LocalTime.parse(alarm.getTime(), formatter);
            LocalTime adjustedTime = alarmTime.plusMinutes(adjustmentMinutes);
            String originalTime = alarm.getTime();
            alarm.setTime(adjustedTime.format(formatter));
            alarm.setAdjustedMinutes(adjustmentMinutes);
            aiElfAlarmMapper.updateById(alarm);

            // Bug 修复（P1）：原日志输出 alarm.getTime() 已被修改为新时间，
            // 期望输出"原时间 → 调整后"，应保留 originalTime 在 setTime 之前。
            logger.info("[智能闹钟] 根据睡眠质量调整闹钟 - 原时间: {}, 调整后: {}, 调整分钟数: {}",
                originalTime, alarm.getTime(), adjustmentMinutes);
        }
        
        return alarm;
    }

    @Override
    public String processVoiceCommand(Long elfId, Long userId, String commandText) {
        ElfVoiceCommand voiceCommand = new ElfVoiceCommand();
        voiceCommand.setElfId(elfId);
        voiceCommand.setUserId(userId);
        voiceCommand.setCommandText(commandText);
        voiceCommand.setExecuted(false);
        voiceCommand.setCreatedAt(LocalDateTime.now());
        
        String commandType = classifyVoiceCommand(commandText);
        voiceCommand.setCommandType(commandType);
        
        String response = executeVoiceCommand(elfId, userId, commandType, commandText);
        voiceCommand.setExecuted(true);
        
        elfVoiceCommandMapper.insert(voiceCommand);
        logger.info("[语音命令] 处理命令: {} -> 类型: {}", commandText, commandType);
        
        return response;
    }

    private String classifyVoiceCommand(String command) {
        String lower = command.toLowerCase();
        if (lower.contains("闹钟") || lower.contains("提醒") || lower.contains("起床")) {
            return "alarm";
        } else if (lower.contains("专注") || lower.contains("学习") || lower.contains("计时")) {
            return "focus";
        } else if (lower.contains("睡眠") || lower.contains("睡觉") || lower.contains("晚安")) {
            return "sleep";
        } else if (lower.contains("日程") || lower.contains("安排") || lower.contains("计划")) {
            return "schedule";
        } else if (lower.contains("健康") || lower.contains("报告") || lower.contains("分析")) {
            return "health";
        }
        return "general";
    }

    private String executeVoiceCommand(Long elfId, Long userId, String commandType, String command) {
        switch (commandType) {
            case "alarm":
                return "好的，我来帮你设置闹钟。";
            case "focus":
                startFocus(elfId, userId, "语音启动专注");
                return "好的，开始专注模式！";
            case "sleep":
                return "晚安！祝你有个好梦。";
            case "schedule":
                return "我来帮你查看今天的日程安排。";
            case "health":
                generateHealthReport(elfId, userId);
                return "已生成你的健康报告，请查看。";
            default:
                return "我收到了你的指令，正在处理中...";
        }
    }

    @Override
    public List<ElfVoiceCommand> getVoiceCommands(Long elfId) {
        return elfVoiceCommandMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ElfVoiceCommand>()
                .eq("elf_id", elfId)
                .orderByDesc("created_at")
        );
    }

    @Override
    public HealthAnalysisRecord generateHealthReport(Long elfId, Long userId) {
        LocalDate today = LocalDate.now();
        HealthAnalysisRecord existing = healthAnalysisRecordMapper.findByElfIdAndDate(elfId, today);
        if (existing != null) {
            return existing;
        }
        
        int focusDuration = aiElfFocusMapper.getTotalFocusMinutes(elfId) != null 
            ? aiElfFocusMapper.getTotalFocusMinutes(elfId).intValue() : 0;
        
        double avgSleepQuality = aiElfSleepMapper.getAverageSleepQuality(elfId) != null 
            ? aiElfSleepMapper.getAverageSleepQuality(elfId) : 0;
        
        int healthScore = calculateHealthScore(focusDuration, (int) avgSleepQuality);
        String suggestions = generateHealthSuggestions(focusDuration, (int) avgSleepQuality);
        
        HealthAnalysisRecord record = new HealthAnalysisRecord();
        record.setElfId(elfId);
        record.setUserId(userId);
        record.setAnalysisDate(today);
        record.setFocusDuration(focusDuration);
        record.setSleepQuality((int) avgSleepQuality);
        record.setHealthScore(healthScore);
        record.setSuggestions(suggestions);
        record.setCreatedAt(LocalDateTime.now());
        
        healthAnalysisRecordMapper.insert(record);
        logger.info("[健康报告] 生成报告 - 健康评分: {}", healthScore);
        return record;
    }

    private int calculateHealthScore(int focusDuration, int sleepQuality) {
        int score = 0;
        
        if (focusDuration >= 60) {
            score += 30;
        } else if (focusDuration >= 30) {
            score += 20;
        } else if (focusDuration >= 10) {
            score += 10;
        }
        
        if (sleepQuality >= 8) {
            score += 40;
        } else if (sleepQuality >= 6) {
            score += 30;
        } else if (sleepQuality >= 4) {
            score += 20;
        }
        
        return Math.min(score, 100);
    }

    private String generateHealthSuggestions(int focusDuration, int sleepQuality) {
        StringBuilder suggestions = new StringBuilder();
        
        if (focusDuration < 30) {
            suggestions.append("今天的专注时间较少，建议适当安排学习或工作时间。");
        }
        
        if (sleepQuality < 6) {
            suggestions.append("建议改善睡眠质量，保持规律作息。");
        }
        
        if (suggestions.length() == 0) {
            suggestions.append("今天的健康状态良好，继续保持！");
        }
        
        return suggestions.toString();
    }

    @Override
    public List<HealthAnalysisRecord> getHealthReports(Long elfId, int limit) {
        return healthAnalysisRecordMapper.findByElfIdRecent(elfId, limit);
    }

    @Override
    public Map<String, Object> getHealthSummary(Long elfId) {
        Map<String, Object> summary = new HashMap<>();
        List<HealthAnalysisRecord> recent = healthAnalysisRecordMapper.findByElfIdRecent(elfId, 7);
        
        if (!recent.isEmpty()) {
            int avgHealthScore = (int) recent.stream()
                .mapToInt(HealthAnalysisRecord::getHealthScore)
                .average()
                .orElse(0);
            int totalFocus = recent.stream()
                .mapToInt(HealthAnalysisRecord::getFocusDuration)
                .sum();
            
            summary.put("avgHealthScore", avgHealthScore);
            summary.put("totalFocusMinutes", totalFocus);
            summary.put("reportCount", recent.size());
        }
        
        return summary;
    }

    // ==================== AI 精灵养成深化 ====================

    /**
     * 根据好感度计算关系等级与称号。
     * @return 长度 2 的数组：[level(1-6), title]
     */
    private int[] resolveLevel(int affection) {
        int level = 1;
        for (int i = 0; i < LEVEL_THRESHOLDS.length; i++) {
            if (affection >= LEVEL_THRESHOLDS[i]) {
                level = i + 1;
            }
        }
        return new int[]{level, level - 1};
    }

    private String titleOf(int levelIndex) {
        return LEVEL_TITLES[Math.max(0, Math.min(LEVEL_TITLES.length - 1, levelIndex))];
    }

    /** 依据近期睡眠评分计算互动好感度加成系数。 */
    private double sleepBonusMultiplier(Long elfId) {
        Double avgQuality = aiElfSleepMapper.getAverageSleepQuality(elfId);
        if (avgQuality == null) {
            return 1.0;
        }
        if (avgQuality >= 8) {
            return 1.5;
        }
        if (avgQuality >= 6) {
            return 1.25;
        }
        return 1.0;
    }

    @Override
    @Transactional
    public AiElf addAffection(Long elfId, Long userId, int points, String interactionType) {
        AiElf elf = aiElfMapper.selectById(elfId);
        if (elf == null) {
            throw new BusinessException(AiErrorCode.ELF_NOT_FOUND);
        }
        // 默认值：points<=0 时按互动类型给基础值
        int base = points > 0 ? points : defaultPoints(interactionType);
        double multiplier = sleepBonusMultiplier(elfId);
        int gained = (int) Math.round(base * multiplier);

        int affection = (elf.getAffection() == null ? 0 : elf.getAffection()) + gained;
        if (affection > MAX_AFFECTION) affection = MAX_AFFECTION;
        if (affection < 0) affection = 0;
        elf.setAffection(affection);

        int intimacy = (elf.getIntimacy() == null ? 0 : elf.getIntimacy()) + gained * 10;
        if (intimacy > MAX_INTIMACY) intimacy = MAX_INTIMACY;
        elf.setIntimacy(intimacy);

        // 桌面养成四维：按互动类型调整心情/饱腹（chat 心情+2、feed 饱腹+20心情+5、sleep 心情+30饱腹-5、play 心情+3）
        applyGrowthEffects(elf, interactionType);

        int[] lv = resolveLevel(affection);
        elf.setRelationshipLevel(lv[0]);
        elf.setLastInteractAt(LocalDateTime.now());
        elf.setUpdatedAt(LocalDateTime.now());
        aiElfMapper.updateById(elf);

        // 记录互动日志
        recordInteraction(elfId, userId, interactionType, gained);
        // 与既有经验值体系衔接：互动同时加经验
        addExperience(elfId, gained);

        // 灵魂引擎联动：精灵互动同时演化精灵的灵魂（经历=互动类型+好感变化）
        // 灵魂算法：这一刻的自己 + 发生的经历 + 如何感受和理解 + 过去留下的记忆 = 下一刻的自己
        try {
            String soulExperience = describeElfInteraction(interactionType, gained);
            soulEngineService.evolveElfSoul(userId, elfId, soulExperience);
        } catch (Exception soulEx) {
            logger.warn("精灵灵魂演化失败，忽略: {}", soulEx.getMessage());
        }

        logger.info("[精灵养成] 用户 {} 与精灵 {} 互动({}) +好感{} 等级{} 称号{}",
                userId, elfId, interactionType, gained, lv[0], titleOf(lv[1]));
        return elf;
    }

    /**
     * 将精灵互动描述为灵魂经历文本（供灵魂引擎演化 I(t)=感受与理解）。
     */
    private String describeElfInteraction(String interactionType, int gained) {
        if (interactionType == null) {
            return "用户与精灵进行了一次互动，好感度 +" + gained;
        }
        return switch (interactionType) {
            case "feed" -> "用户给精灵喂食，精灵饱腹感提升，好感度 +" + gained;
            case "pat" -> "用户轻轻抚摸精灵，精灵心里暖暖的，好感度 +" + gained;
            case "play" -> "用户陪精灵玩耍，精灵玩得很开心，好感度 +" + gained;
            case "sleep" -> "精灵在用户的陪伴下入睡，安心又温暖，好感度 +" + gained;
            case "chat" -> "用户与精灵聊天，精灵感受到被倾听，好感度 +" + gained;
            default -> "用户与精灵进行了一次" + interactionType + "互动，好感度 +" + gained;
        };
    }

    private int defaultPoints(String interactionType) {
        if (interactionType == null) {
            return 2;
        }
        switch (interactionType) {
            case "chat": return 2;
            case "feed": return 5;
            case "pat": return 3;
            case "play": return 5;
            case "sleep": return 3;
            default: return 2;
        }
    }

    @Override
    public Map<String, Object> getRelationshipInfo(Long elfId, Long userId) {
        AiElf elf = aiElfMapper.selectById(elfId);
        if (elf == null) {
            throw new BusinessException(AiErrorCode.ELF_NOT_FOUND);
        }
        int affection = elf.getAffection() == null ? 0 : elf.getAffection();
        int[] lv = resolveLevel(affection);
        int level = lv[0];
        int levelIndex = lv[1];

        // 下一等级所需好感度（满级为 0）
        int nextLevelAffection = 0;
        if (levelIndex + 1 < LEVEL_THRESHOLDS.length) {
            nextLevelAffection = LEVEL_THRESHOLDS[levelIndex + 1] - affection;
            if (nextLevelAffection < 0) nextLevelAffection = 0;
        }

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long todayCount = aiElfInteractionLogMapper.countSince(elfId, startOfToday);

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("affection", affection);
        info.put("affectionMax", MAX_AFFECTION);
        info.put("relationshipLevel", level);
        info.put("relationshipTitle", titleOf(levelIndex));
        info.put("intimacy", elf.getIntimacy() == null ? 0 : elf.getIntimacy());
        info.put("nextLevelAffection", nextLevelAffection);
        info.put("isMaxLevel", level >= LEVEL_THRESHOLDS.length);
        info.put("todayInteractions", todayCount);
        info.put("lastInteractAt", elf.getLastInteractAt());
        return info;
    }

    @Override
    public AiElfInteractionLog recordInteraction(Long elfId, Long userId, String interactionType, int points) {
        AiElfInteractionLog log = new AiElfInteractionLog();
        log.setElfId(elfId);
        log.setUserId(userId);
        log.setInteractionType(interactionType != null ? interactionType : "chat");
        log.setPointsGained(points);
        log.setCreatedAt(LocalDateTime.now());
        aiElfInteractionLogMapper.insert(log);

        // 数据池自动采集：精灵互动类型
        if (eventPublisher != null) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("elfId", elfId);
            meta.put("interactionType", interactionType);
            eventPublisher.publishEvent(new UserDataCaptureEvent(
                    userId, "ai_elf_interaction",
                    "elf_interaction:" + (interactionType != null ? interactionType : "chat"),
                    meta, "elflog:" + log.getId(), log.getCreatedAt()));
        }
        return log;
    }

    @Override
    public IPage<AiElfInteractionLog> getInteractionHistory(Long elfId, Long userId, int page, int size) {
        int p = page < 1 ? 1 : page;
        int s = size < 1 ? 10 : Math.min(size, 50);
        Page<AiElfInteractionLog> pageParam = new Page<>(p, s);
        QueryWrapper<AiElfInteractionLog> wrapper = new QueryWrapper<>();
        wrapper.eq("elf_id", elfId).orderByDesc("created_at");
        return aiElfInteractionLogMapper.selectPage(pageParam, wrapper);
    }

    @Override
    @Transactional
    public Map<String, Object> dailyReward(Long elfId, Long userId) {
        AiElf elf = aiElfMapper.selectById(elfId);
        if (elf == null) {
            throw new BusinessException(AiErrorCode.ELF_NOT_FOUND);
        }
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long already = aiElfInteractionLogMapper.countByTypeSince(elfId, "daily_reward", startOfToday);
        Map<String, Object> result = new HashMap<>();
        if (already > 0) {
            // 幂等：今日已领取
            result.put("rewarded", false);
            result.put("message", "今日奖励已领取");
            result.put("relationship", getRelationshipInfo(elfId, userId));
            return result;
        }
        // 发放 +5 好感度
        int affection = (elf.getAffection() == null ? 0 : elf.getAffection()) + 5;
        if (affection > MAX_AFFECTION) affection = MAX_AFFECTION;
        elf.setAffection(affection);
        elf.setIntimacy((elf.getIntimacy() == null ? 0 : elf.getIntimacy()) + 50);
        int[] lv = resolveLevel(affection);
        elf.setRelationshipLevel(lv[0]);
        elf.setLastInteractAt(LocalDateTime.now());
        elf.setUpdatedAt(LocalDateTime.now());
        aiElfMapper.updateById(elf);
        recordInteraction(elfId, userId, "daily_reward", 5);

        result.put("rewarded", true);
        result.put("message", "已领取每日奖励 +5 好感度");
        result.put("relationship", getRelationshipInfo(elfId, userId));
        return result;
    }

    // ==================== 桌面养成四维状态（心情/饱腹/亲密度/经验） ====================

    private int moodOf(AiElf elf) {
        return elf.getMood() == null ? DEFAULT_MOOD : elf.getMood();
    }

    private int fullnessOf(AiElf elf) {
        return elf.getFullness() == null ? DEFAULT_FULLNESS : elf.getFullness();
    }

    private int clampGrowth(int v, int max) {
        if (v < 0) return 0;
        if (v > max) return max;
        return v;
    }

    /**
     * 按互动类型调整心情/饱腹（纯内存修改，不落库；由调用方统一 updateById）。
     * chat: 心情+2；feed: 饱腹+20、心情+5；sleep: 心情+30、饱腹-5；play: 心情+3。
     */
    private void applyGrowthEffects(AiElf elf, String interactionType) {
        int mood = moodOf(elf);
        int fullness = fullnessOf(elf);
        if (interactionType == null) {
            elf.setMood(mood);
            elf.setFullness(fullness);
            return;
        }
        switch (interactionType) {
            case "chat":
                mood += 2;
                break;
            case "feed":
                fullness += 20;
                mood += 5;
                break;
            case "pat":
                mood += 8;
                break;
            case "sleep":
                mood += 30;
                fullness -= 5;
                break;
            case "play":
                mood += 3;
                break;
            default:
                break;
        }
        elf.setMood(clampGrowth(mood, MAX_MOOD));
        elf.setFullness(clampGrowth(fullness, MAX_FULLNESS));
    }

    @Override
    @Transactional
    public AiElf feed(Long elfId, Long userId) {
        // 喂食复用互动管线：feed 类型 → 饱腹+20、心情+5、亲密度+、经验+，并写日志
        return addAffection(elfId, userId, 5, "feed");
    }

    @Override
    @Transactional
    public AiElf pat(Long elfId, Long userId) {
        // 摸头复用互动管线：pat 类型 → 心情+8、亲密度+、经验+，并写日志
        return addAffection(elfId, userId, 3, "pat");
    }

    @Override
    public AiElf applyNaturalDecay(Long elfId, Long userId, long minutes) {
        AiElf elf = aiElfMapper.selectById(elfId);
        if (elf == null) {
            throw new BusinessException(AiErrorCode.ELF_NOT_FOUND);
        }
        if (minutes < DECAY_INTERVAL_MINUTES) {
            return elf; // 未满一个衰减粒度，无变化
        }
        long steps = minutes / DECAY_INTERVAL_MINUTES;
        int mood = moodOf(elf) - (int) Math.min(steps * DECAY_STEP, moodOf(elf));
        int fullness = fullnessOf(elf) - (int) Math.min(steps * DECAY_STEP, fullnessOf(elf));
        elf.setMood(clampGrowth(mood, MAX_MOOD));
        elf.setFullness(clampGrowth(fullness, MAX_FULLNESS));
        elf.setUpdatedAt(LocalDateTime.now());
        aiElfMapper.updateById(elf);
        logger.info("[精灵养成] 自然衰减 elf={} 经过{}分钟 mood={} fullness={}", elfId, minutes, elf.getMood(), elf.getFullness());
        return elf;
    }

    @Override
    public Map<String, Object> getGrowthState(Long elfId, Long userId) {
        AiElf elf = aiElfMapper.selectById(elfId);
        if (elf == null) {
            throw new BusinessException(AiErrorCode.ELF_NOT_FOUND);
        }
        int mood = moodOf(elf);
        int fullness = fullnessOf(elf);
        int intimacy = elf.getIntimacy() == null ? 0 : elf.getIntimacy();
        int experience = elf.getExperience() == null ? 0 : elf.getExperience();

        // 状态对动作表现的影响建议（前端据此调整动画播放频率/姿态）
        boolean lowMood = mood < MOOD_LOW_THRESHOLD;
        boolean lowFullness = fullness < FULLNESS_LOW_THRESHOLD;
        boolean highIntimacy = intimacy >= INTIMACY_HIGH_THRESHOLD;

        Map<String, Object> state = new LinkedHashMap<>();
        state.put("mood", mood);
        state.put("moodMax", MAX_MOOD);
        state.put("fullness", fullness);
        state.put("fullnessMax", MAX_FULLNESS);
        state.put("intimacy", intimacy);
        state.put("experience", experience);
        state.put("level", elf.getLevel());
        // 动作表现建议
        state.put("lethargic", lowMood);            // mood<30 → 动作变慢/低头/叹气
        state.put("hungry", lowFullness);            // fullness<20 → 无精神/坐地
        state.put("affectionate", highIntimacy);     // intimacy>=80 → 主动挥手/跟随
        // 推荐下一步动作（前端可做引导气泡）
        String suggestion;
        if (lowFullness) {
            suggestion = "饿饿，想吃饭～";
        } else if (lowMood) {
            suggestion = "有点提不起劲，陪我聊聊天吧";
        } else if (highIntimacy) {
            suggestion = "最喜欢你啦！";
        } else {
            suggestion = "今天也元气满满！";
        }
        state.put("suggestion", suggestion);
        return state;
    }
}
package com.example.aichat.controller;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.AiErrorCode;
import com.example.aichat.common.security.AuthContext;
import com.example.aichat.common.security.OwnershipCheck;
import com.example.aichat.dto.R;
import com.example.aichat.entity.AiElf;
import com.example.aichat.entity.AiElfAlarm;
import com.example.aichat.entity.AiElfInteractionLog;
import com.example.aichat.entity.AiElfSchedule;
import com.example.aichat.entity.AiElfSleep;
import com.example.aichat.service.AiElfService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 精灵接口。
 *
 * 安全加固（P0）：所有以 userId 标识“归属用户”的接口一律以登录令牌为准
 * （AuthContext.requireSelf），忽略客户端伪造的 userId，防止任意用户
 * 读取/操作他人的精灵、闹钟、日程、睡眠与健康数据。
 */
@RestController
@RequestMapping("/api/elf")
public class AiElfController {

    @Autowired
    private AiElfService aiElfService;

    @Autowired
    private com.example.aichat.service.ElfReminderService elfReminderService;

    @Autowired
    private com.example.aichat.service.SoulEngineService soulEngineService;

    /**
     * P0 越权修复（IDOR）：校验指定 elfId 属于当前登录用户。
     * 未登录由 AuthContext.requireUserId() 抛出 401；精灵不存在抛 404；
     * 精灵归属他人抛 403，杜绝任意登录用户枚举 elfId 读取/修改他人精灵数据。
     */
    private void verifyElfOwnership(Long elfId) {
        Long currentUserId = AuthContext.requireUserId();
        AiElf elf = aiElfService.getElfById(elfId);
        if (elf == null) {
            throw new BusinessException(AiErrorCode.ELF_NOT_FOUND, "精灵不存在");
        }
        // currentUserId 非空（requireUserId 已保证），用其 equals 避免 elf.getUserId() 为 null 时 NPE
        if (!currentUserId.equals(elf.getUserId())) {
            throw new BusinessException(AiErrorCode.ELF_FORBIDDEN, "无权操作该精灵");
        }
    }

    /**
     * 以 alarmId 为路径参数的端点：先反查闹钟得到 elfId，再校验精灵归属。
     */
    private void verifyAlarmOwnership(Long alarmId) {
        AiElfAlarm alarm = aiElfService.getAlarmById(alarmId);
        if (alarm == null) {
            throw new BusinessException(AiErrorCode.ELF_NOT_FOUND, "闹钟不存在");
        }
        verifyElfOwnership(alarm.getElfId());
    }

    /**
     * 以 scheduleId 为路径参数的端点：先反查日程得到 elfId，再校验精灵归属。
     */
    private void verifyScheduleOwnership(Long scheduleId) {
        AiElfSchedule schedule = aiElfService.getScheduleById(scheduleId);
        if (schedule == null) {
            throw new BusinessException(AiErrorCode.ELF_NOT_FOUND, "日程不存在");
        }
        verifyElfOwnership(schedule.getElfId());
    }

    @GetMapping("/get")
    public R getElf(@RequestParam Long userId) {
        // 安全修复（P0）：仅本人可读取自己的精灵
        Long current = AuthContext.requireSelf(userId);
        AiElf elf = aiElfService.getElfByUserId(current);
        if (elf != null) {
            return R.success(elf);
        }
        return R.success(null);
    }

    @PostMapping("/create")
    public R createElf(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        if (userIdObj == null) {
            throw new BusinessException(AiErrorCode.CHAT_MESSAGE_BLANK, "userId 不能为空");
        }
        if (!(userIdObj instanceof Number)) {
            throw new BusinessException(AiErrorCode.CHAT_MESSAGE_BLANK, "userId 必须为数字");
        }
        // 安全修复（P0）：创建者强制取当前登录用户
        Long userId = AuthContext.requireSelf(((Number) userIdObj).longValue());
        String name = request.get("name") != null ? request.get("name").toString() : null;
        String theme = request.get("theme") != null ? request.get("theme").toString() : null;
        AiElf elf = aiElfService.createElf(userId, name, theme);
        return R.success(elf);
    }

    @OwnershipCheck(resource = "elf", idParam = "elfId")
    @PutMapping("/{elfId}")
    public R updateElf(@PathVariable Long elfId, @RequestBody Map<String, Object> updates) {
        AiElf elf = aiElfService.updateElf(elfId, updates);
        return R.success(elf);
    }

    @OwnershipCheck(resource = "elf", idParam = "elfId")
    @PostMapping("/{elfId}/experience")
    public R addExperience(@PathVariable Long elfId, @RequestBody Map<String, Integer> request) {
        Integer experience = request.get("experience");
        if (experience == null) {
            return R.error("参数错误");
        }
        aiElfService.addExperience(elfId, experience);
        return R.success("经验已增加");
    }

    @PostMapping("/alarm")
    public R createAlarm(@RequestBody Map<String, Object> request) {
        Object elfIdObj = request.get("elfId");
        Object userIdObj = request.get("userId");
        if (elfIdObj == null || userIdObj == null) {
            throw new BusinessException(AiErrorCode.CHAT_MESSAGE_BLANK, "elfId 与 userId 不能为空");
        }
        if (!(elfIdObj instanceof Number) || !(userIdObj instanceof Number)) {
            throw new BusinessException(AiErrorCode.CHAT_MESSAGE_BLANK, "elfId 与 userId 必须为数字");
        }
        Long elfId = ((Number) elfIdObj).longValue();
        Long userId = AuthContext.requireSelf(((Number) userIdObj).longValue());
        // P0 越权修复：body 携带的 elfId 也必须属于当前登录用户，否则可向他人精灵注入闹钟
        verifyElfOwnership(elfId);
        String title = request.get("title") != null ? request.get("title").toString() : null;
        String time = request.get("time") != null ? request.get("time").toString() : null;
        if (title == null || title.isBlank() || time == null || time.isBlank()) {
            throw new BusinessException(AiErrorCode.CHAT_MESSAGE_BLANK, "title 与 time 不能为空");
        }
        String repeatDays = request.get("repeatDays") != null ? request.get("repeatDays").toString() : null;

        AiElfAlarm alarm = aiElfService.createAlarm(elfId, userId, title, time, repeatDays);
        return R.success(alarm);
    }

    @OwnershipCheck(resource = "elf", idParam = "elfId")
    @GetMapping("/alarms")
    public R getAlarms(@RequestParam Long elfId) {
        List<AiElfAlarm> alarms = aiElfService.getAlarms(elfId);
        return R.success(alarms);
    }

    @PutMapping("/alarm/{alarmId}")
    public R updateAlarm(@PathVariable Long alarmId, @RequestBody Map<String, Object> updates) {
        // P0 越权修复：由 alarmId 反查精灵并校验归属
        verifyAlarmOwnership(alarmId);
        AiElfAlarm alarm = aiElfService.updateAlarm(alarmId, updates);
        return R.success(alarm);
    }

    @DeleteMapping("/alarm/{alarmId}")
    public R deleteAlarm(@PathVariable Long alarmId) {
        // P0 越权修复：由 alarmId 反查精灵并校验归属
        verifyAlarmOwnership(alarmId);
        aiElfService.deleteAlarm(alarmId);
        return R.success("删除成功");
    }

    @PostMapping("/schedule")
    public R createSchedule(@RequestBody Map<String, Object> request) {
        Object elfIdObj = request.get("elfId");
        Object userIdObj = request.get("userId");
        String title = (String) request.get("title");
        String startTime = (String) request.get("startTime");
        
        if (elfIdObj == null || userIdObj == null || title == null || startTime == null) {
            return R.error("参数错误");
        }
        
        Long elfId = ((Number) elfIdObj).longValue();
        Long userId = AuthContext.requireSelf(((Number) userIdObj).longValue());
        // P0 越权修复：elfId 必须属于当前登录用户
        verifyElfOwnership(elfId);
        String endTime = (String) request.get("endTime");
        
        AiElfSchedule schedule = aiElfService.createSchedule(elfId, userId, title, startTime, endTime);
        return R.success(schedule);
    }

    @GetMapping("/schedules")
    public R getSchedules(@RequestParam Long elfId) {
        // P0 越权修复：校验精灵归属当前登录用户
        verifyElfOwnership(elfId);
        List<AiElfSchedule> schedules = aiElfService.getSchedules(elfId);
        return R.success(schedules);
    }

    @PutMapping("/schedule/{scheduleId}")
    public R updateSchedule(@PathVariable Long scheduleId, @RequestBody Map<String, Object> updates) {
        // P0 越权修复：由 scheduleId 反查精灵并校验归属
        verifyScheduleOwnership(scheduleId);
        AiElfSchedule schedule = aiElfService.updateSchedule(scheduleId, updates);
        return R.success(schedule);
    }

    @DeleteMapping("/schedule/{scheduleId}")
    public R deleteSchedule(@PathVariable Long scheduleId) {
        // P0 越权修复：由 scheduleId 反查精灵并校验归属
        verifyScheduleOwnership(scheduleId);
        aiElfService.deleteSchedule(scheduleId);
        return R.success("删除成功");
    }

    @PostMapping("/sleep")
    public R recordSleep(@RequestBody Map<String, Object> request) {
        Object elfIdObj = request.get("elfId");
        Object userIdObj = request.get("userId");
        String sleepTime = (String) request.get("sleepTime");
        String wakeTime = (String) request.get("wakeTime");
        Object qualityObj = request.get("quality");
        
        if (elfIdObj == null || userIdObj == null || sleepTime == null || wakeTime == null || qualityObj == null) {
            return R.error("参数错误");
        }
        
        Long elfId = ((Number) elfIdObj).longValue();
        Long userId = AuthContext.requireSelf(((Number) userIdObj).longValue());
        // P0 越权修复：elfId 必须属于当前登录用户
        verifyElfOwnership(elfId);
        int quality = ((Number) qualityObj).intValue();
        
        AiElfSleep sleep = aiElfService.recordSleep(elfId, userId, sleepTime, wakeTime, quality);
        return R.success(sleep);
    }

    @OwnershipCheck(resource = "elf", idParam = "elfId")
    @GetMapping("/sleep/records")
    public R getSleepRecords(@RequestParam Long elfId) {
        List<AiElfSleep> records = aiElfService.getSleepRecords(elfId);
        return R.success(records);
    }

    @GetMapping("/sleep/summary")
    public R getSleepSummary(@RequestParam Long elfId) {
        // P0 越权修复：校验精灵归属当前登录用户
        verifyElfOwnership(elfId);
        Map<String, Object> summary = aiElfService.getSleepSummary(elfId);
        return R.success(summary);
    }

    @PostMapping("/smart-alarm")
    public R createSmartAlarm(@RequestBody Map<String, Object> request) {
        Object elfIdObj = request.get("elfId");
        Object userIdObj = request.get("userId");
        String title = (String) request.get("title");
        String baseTime = (String) request.get("baseTime");
        
        if (elfIdObj == null || userIdObj == null || title == null || baseTime == null) {
            return R.error("参数错误");
        }
        
        Long elfId = ((Number) elfIdObj).longValue();
        Long userId = AuthContext.requireSelf(((Number) userIdObj).longValue());
        // P0 越权修复：elfId 必须属于当前登录用户
        verifyElfOwnership(elfId);
        
        AiElfAlarm alarm = aiElfService.createSmartAlarm(elfId, userId, title, baseTime);
        return R.success(alarm);
    }

    @PostMapping("/smart-alarm/adjust")
    public R adjustAlarmBySleepQuality(@RequestBody Map<String, Object> request) {
        Object elfIdObj = request.get("elfId");
        Object userIdObj = request.get("userId");
        Object alarmIdObj = request.get("alarmId");
        
        if (elfIdObj == null || userIdObj == null || alarmIdObj == null) {
            return R.error("参数错误");
        }

        Long alarmId = ((Number) alarmIdObj).longValue();
        // P0 越权修复：先反查闹钟得到 elfId，再校验精灵归属当前登录用户
        verifyAlarmOwnership(alarmId);
        // 安全修复（P0）：userId 以当前登录用户为准
        AiElfAlarm alarm = aiElfService.adjustAlarmBySleepQuality(
            ((Number) elfIdObj).longValue(),
            AuthContext.requireSelf(((Number) userIdObj).longValue()),
            alarmId
        );
        return R.success(alarm);
    }

    @PostMapping("/voice/command")
    public R processVoiceCommand(@RequestBody Map<String, Object> request) {
        Object elfIdObj = request.get("elfId");
        Object userIdObj = request.get("userId");
        String commandText = (String) request.get("commandText");
        
        if (elfIdObj == null || userIdObj == null || commandText == null) {
            return R.error("参数错误");
        }
        
        Long elfId = ((Number) elfIdObj).longValue();
        // P0 越权修复：elfId 必须属于当前登录用户
        verifyElfOwnership(elfId);
        String response = aiElfService.processVoiceCommand(
            elfId,
            AuthContext.requireSelf(((Number) userIdObj).longValue()),
            commandText
        );
        return R.success(Map.of("response", response));
    }

    @GetMapping("/voice/commands")
    public R getVoiceCommands(@RequestParam Long elfId) {
        // P0 越权修复：校验精灵归属当前登录用户
        verifyElfOwnership(elfId);
        List<?> commands = aiElfService.getVoiceCommands(elfId);
        return R.success(commands);
    }

    @PostMapping("/health/report")
    public R generateHealthReport(@RequestBody Map<String, Object> request) {
        Object elfIdObj = request.get("elfId");
        Object userIdObj = request.get("userId");
        
        if (elfIdObj == null || userIdObj == null) {
            return R.error("参数错误");
        }
        
        Long elfId = ((Number) elfIdObj).longValue();
        // P0 越权修复：elfId 必须属于当前登录用户
        verifyElfOwnership(elfId);
        Object report = aiElfService.generateHealthReport(
            elfId,
            AuthContext.requireSelf(((Number) userIdObj).longValue())
        );
        return R.success(report);
    }

    @OwnershipCheck(resource = "elf", idParam = "elfId")
    @GetMapping("/health/reports")
    public R getHealthReports(@RequestParam Long elfId, @RequestParam(defaultValue = "7") int limit) {
        List<?> reports = aiElfService.getHealthReports(elfId, limit);
        return R.success(reports);
    }

    @GetMapping("/health/summary")
    public R getHealthSummary(@RequestParam Long elfId) {
        // P0 越权修复：校验精灵归属当前登录用户
        verifyElfOwnership(elfId);
        Map<String, Object> summary = aiElfService.getHealthSummary(elfId);
        return R.success(summary);
    }

    // ==================== AI 精灵养成深化 ====================

    /**
     * 记录互动并增加好感度。
     * body: { interactionType: chat/feed/play/sleep, points?: number }
     */
    @PostMapping("/{elfId}/interact")
    public R interact(@PathVariable Long elfId, @RequestBody Map<String, Object> request) {
        // P0 越权修复：校验精灵归属当前登录用户
        Long userId = verifyElfOwnershipAndGetUser(elfId);
        Object typeObj = request.get("interactionType");
        if (typeObj == null || typeObj.toString().isBlank()) {
            throw new BusinessException(AiErrorCode.CHAT_MESSAGE_BLANK, "interactionType 不能为空");
        }
        String interactionType = typeObj.toString();
        int points = 0;
        if (request.get("points") instanceof Number) {
            points = ((Number) request.get("points")).intValue();
        }
        AiElf elf = aiElfService.addAffection(elfId, userId, points, interactionType);
        return R.success(elf);
    }

    /** 关系信息（好感度/等级/亲密度/称号） */
    @GetMapping("/{elfId}/relationship")
    public R relationship(@PathVariable Long elfId) {
        Long userId = verifyElfOwnershipAndGetUser(elfId);
        return R.success(aiElfService.getRelationshipInfo(elfId, userId));
    }

    /** 互动历史（分页） */
    @GetMapping("/{elfId}/interactions")
    public R interactions(@PathVariable Long elfId,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size) {
        Long userId = verifyElfOwnershipAndGetUser(elfId);
        IPage<AiElfInteractionLog> history = aiElfService.getInteractionHistory(elfId, userId, page, size);
        return R.success(history);
    }

    /** 每日奖励（幂等） */
    @PostMapping("/{elfId}/daily-reward")
    public R dailyReward(@PathVariable Long elfId) {
        Long userId = verifyElfOwnershipAndGetUser(elfId);
        return R.success(aiElfService.dailyReward(elfId, userId));
    }

    // ==================== 桌面养成四维状态 ====================

    /** 喂食：饱腹 +20、心情 +5 */
    @PostMapping("/{elfId}/feed")
    public R feed(@PathVariable Long elfId) {
        Long userId = verifyElfOwnershipAndGetUser(elfId);
        return R.success(aiElfService.feed(elfId, userId));
    }

    /** 摸头：亲密度 +10、心情 +8（桌面精灵摸摸头互动） */
    @PostMapping("/{elfId}/pat")
    public R pat(@PathVariable Long elfId) {
        Long userId = verifyElfOwnershipAndGetUser(elfId);
        return R.success(aiElfService.pat(elfId, userId));
    }

    /** 养成状态：心情/饱腹/亲密度/经验 + 动作表现建议 */
    @GetMapping("/{elfId}/growth")
    public R growth(@PathVariable Long elfId) {
        Long userId = verifyElfOwnershipAndGetUser(elfId);
        return R.success(aiElfService.getGrowthState(elfId, userId));
    }

    /** 精灵灵魂状态（灵魂引擎 elf 维度）：当前情绪/心情/信念/演化次数 */
    @GetMapping("/{elfId}/soul")
    public R elfSoul(@PathVariable Long elfId) {
        Long userId = verifyElfOwnershipAndGetUser(elfId);
        return R.success(soulEngineService.getElfSoulOverview(userId, elfId));
    }

    /**
     * 校验精灵归属并返回当前登录用户 ID（供养成端点复用）。
     */
    private Long verifyElfOwnershipAndGetUser(Long elfId) {
        Long currentUserId = AuthContext.requireUserId();
        AiElf elf = aiElfService.getElfById(elfId);
        if (elf == null) {
            throw new BusinessException(AiErrorCode.ELF_NOT_FOUND, "精灵不存在");
        }
        if (!currentUserId.equals(elf.getUserId())) {
            throw new BusinessException(AiErrorCode.ELF_FORBIDDEN, "无权操作该精灵");
        }
        return currentUserId;
    }

    // ==================== M1 ② 定时提醒（星野式留存） ====================

    /**
     * 设置一条定时提醒（时间 + 内容）。到点后由定时任务推送 WebSocket 给本人。
     * body: { title, content, remindTime(yyyy-MM-ddTHH:mm:ss) }
     */
    @PostMapping("/reminder")
    public R createReminder(@RequestBody Map<String, Object> request) {
        Long currentUserId = AuthContext.requireUserId();
        String title = request.get("title") != null ? request.get("title").toString() : null;
        String content = request.get("content") != null ? request.get("content").toString() : null;
        Object timeObj = request.get("remindTime") != null ? request.get("remindTime") : request.get("time");
        if (title == null || title.isBlank() || timeObj == null) {
            throw new BusinessException(AiErrorCode.CHAT_MESSAGE_BLANK, "title 与 remindTime 不能为空");
        }
        java.time.LocalDateTime remindTime;
        try {
            remindTime = java.time.LocalDateTime.parse(timeObj.toString());
        } catch (Exception e) {
            return R.badRequest("remindTime 格式应为 yyyy-MM-ddTHH:mm:ss");
        }
        if (remindTime.isBefore(java.time.LocalDateTime.now())) {
            return R.badRequest("提醒时间不能早于当前时间");
        }
        com.example.aichat.entity.ElfReminder reminder =
                elfReminderService.createReminder(currentUserId, title, content, remindTime);
        return R.success(reminder);
    }

    /**
     * 查询我的提醒列表。
     */
    @GetMapping("/reminders")
    public R listReminders() {
        Long currentUserId = AuthContext.requireUserId();
        return R.success(elfReminderService.listReminders(currentUserId));
    }
}

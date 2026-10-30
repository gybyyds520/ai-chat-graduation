package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.aichat.entity.ElfReminder;
import com.example.aichat.mapper.ElfReminderMapper;
import com.example.aichat.service.ElfReminderService;
import com.example.aichat.websocket.ChatWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * M1 ②：定时提醒到点推送 + AI 主动消息（星野式留存）。
 */
@Service
public class ElfReminderServiceImpl implements ElfReminderService {

    private static final Logger logger = LoggerFactory.getLogger(ElfReminderServiceImpl.class);

    /** 主动消息最小间隔（小时），避免对同一用户频繁打扰。 */
    private static final long PROACTIVE_COOLDOWN_HOURS = 4;

    private final ElfReminderMapper reminderMapper;
    private final ChatWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 用户上一次主动消息推送时间，用于节流。 */
    private final Map<Long, LocalDateTime> lastProactivePush = new ConcurrentHashMap<>();

    @Autowired
    public ElfReminderServiceImpl(ElfReminderMapper reminderMapper,
                                  ChatWebSocketHandler webSocketHandler) {
        this.reminderMapper = reminderMapper;
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public ElfReminder createReminder(Long userId, String title, String content, LocalDateTime remindTime) {
        ElfReminder reminder = new ElfReminder();
        reminder.setUserId(userId);
        reminder.setTitle(title);
        reminder.setContent(content);
        reminder.setRemindTime(remindTime);
        reminder.setTriggered(false);
        reminder.setCreateTime(LocalDateTime.now());
        reminderMapper.insert(reminder);
        logger.info("创建定时提醒 - userId: {}, title: {}, remindTime: {}", userId, title, remindTime);
        return reminder;
    }

    @Override
    public List<ElfReminder> listReminders(Long userId) {
        QueryWrapper<ElfReminder> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("remind_time");
        return reminderMapper.selectList(wrapper);
    }

    @Override
    public int triggerDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        QueryWrapper<ElfReminder> wrapper = new QueryWrapper<>();
        wrapper.eq("triggered", false).le("remind_time", now);
        List<ElfReminder> due = reminderMapper.selectList(wrapper);

        int pushed = 0;
        for (ElfReminder r : due) {
            try {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("type", "elf_reminder");
                payload.put("reminderId", r.getId());
                payload.put("title", r.getTitle());
                payload.put("content", r.getContent());
                payload.put("timestamp", now.toString());
                webSocketHandler.sendMessageToUser(r.getUserId(), objectMapper.writeValueAsString(payload));

                r.setTriggered(true);
                reminderMapper.updateById(r);
                pushed++;
            } catch (Exception e) {
                logger.warn("推送提醒失败 - reminderId: {}, err: {}", r.getId(), e.getMessage());
            }
        }
        if (pushed > 0) {
            logger.info("定时提醒推送完成 - 条数: {}", pushed);
        }
        return pushed;
    }

    @Override
    public int sendProactiveMessages() {
        Set<Long> onlineUsers = webSocketHandler.getOnlineUserIds();
        if (onlineUsers == null || onlineUsers.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        int pushed = 0;
        for (Long userId : onlineUsers) {
            LocalDateTime last = lastProactivePush.get(userId);
            if (last != null && last.plusHours(PROACTIVE_COOLDOWN_HOURS).isAfter(now)) {
                continue; // 节流
            }
            try {
                String greeting = pickGreeting(now);
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("type", "proactive_message");
                payload.put("sender", "elf");
                payload.put("content", greeting);
                payload.put("timestamp", now.toString());
                webSocketHandler.sendMessageToUser(userId, objectMapper.writeValueAsString(payload));
                lastProactivePush.put(userId, now);
                pushed++;
            } catch (Exception e) {
                logger.warn("推送主动消息失败 - userId: {}, err: {}", userId, e.getMessage());
            }
        }
        if (pushed > 0) {
            logger.info("AI 主动消息触达 - 人数: {}", pushed);
        }
        return pushed;
    }

    private String pickGreeting(LocalDateTime now) {
        int hour = now.getHour();
        if (hour < 6) return "夜深了，早点休息呀，我会一直陪着你～";
        if (hour < 11) return "早上好！新的一天，想我了吗？";
        if (hour < 14) return "中午啦，记得吃午饭，别太累着自己哦～";
        if (hour < 18) return "下午好，忙完了吗？来找我聊会儿天吧。";
        return "晚上好～今天过得怎么样？我在等你和我说说话呢。";
    }
}

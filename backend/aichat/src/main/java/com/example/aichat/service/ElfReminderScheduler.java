package com.example.aichat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * M1 ②：定时任务 —— 到点推送用户设置的提醒，并周期性由 AI 精灵主动发消息留存。
 */
@Component
public class ElfReminderScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ElfReminderScheduler.class);

    @Autowired
    private ElfReminderService elfReminderService;

    /** 每分钟扫描一次到期提醒并推送给在线用户。 */
    @Scheduled(fixedDelayString = "60000", initialDelayString = "30000")
    public void pushDueReminders() {
        try {
            elfReminderService.triggerDueReminders();
        } catch (Exception e) {
            logger.warn("定时提醒扫描异常: {}", e.getMessage());
        }
    }

    /** 每 30 分钟由 AI 精灵对在线用户主动发一条问候（内部有节流）。 */
    @Scheduled(fixedDelayString = "1800000", initialDelayString = "120000")
    public void pushProactive() {
        try {
            elfReminderService.sendProactiveMessages();
        } catch (Exception e) {
            logger.warn("AI 主动消息扫描异常: {}", e.getMessage());
        }
    }
}

package com.example.aichat.service;

import com.example.aichat.entity.ElfReminder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * M1 ②：定时提醒与 AI 主动消息（星野式留存）。
 */
public interface ElfReminderService {

    /**
     * 为当前用户创建一条定时提醒。
     */
    ElfReminder createReminder(Long userId, String title, String content, LocalDateTime remindTime);

    /**
     * 查询当前用户的提醒列表（按时间倒序）。
     */
    List<ElfReminder> listReminders(Long userId);

    /**
     * 扫描所有到期但未触发的提醒，通过 WebSocket 推送给在线用户。
     * 由定时任务调用，也可手动触发用于测试。
     *
     * @return 本次推送的条数
     */
    int triggerDueReminders();

    /**
     * AI 主动消息：对长时间未对话的用户，由精灵角色主动发起一条问候/提醒。
     * 由定时任务周期调用。
     *
     * @return 本次主动触达的用户数
     */
    int sendProactiveMessages();
}

package com.example.aichat.service.impl;

import com.example.aichat.entity.ElfReminder;
import com.example.aichat.mapper.ElfReminderMapper;
import com.example.aichat.websocket.ChatWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * M1 ②：定时提醒到点推送 + AI 主动消息节流 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class ElfReminderServiceImplTest {

    @Mock
    private ElfReminderMapper reminderMapper;

    @Mock
    private ChatWebSocketHandler webSocketHandler;

    @InjectMocks
    private ElfReminderServiceImpl service;

    private ElfReminder reminder(Long id, Long userId, boolean triggered, LocalDateTime time) {
        ElfReminder r = new ElfReminder();
        r.setId(id);
        r.setUserId(userId);
        r.setTitle("喝水");
        r.setContent("该喝水了");
        r.setTriggered(triggered);
        r.setRemindTime(time);
        return r;
    }

    @Test
    void triggerDueReminders_推送到期并标记() throws Exception {
        List<ElfReminder> due = List.of(
                reminder(1L, 100L, false, LocalDateTime.now().minusMinutes(1)));
        when(reminderMapper.selectList(any())).thenReturn(due);
        when(reminderMapper.updateById(any(ElfReminder.class))).thenReturn(1);

        int pushed = service.triggerDueReminders();
        assertEquals(1, pushed);
        verify(webSocketHandler).sendMessageToUser(eq(100L), contains("elf_reminder"));
        assertTrue(due.get(0).getTriggered());
    }

    @Test
    void triggerDueReminders_无到期_返回0() throws Exception {
        when(reminderMapper.selectList(any())).thenReturn(List.of());
        assertEquals(0, service.triggerDueReminders());
        verify(webSocketHandler, never()).sendMessageToUser(anyLong(), any());
    }

    @Test
    void sendProactiveMessages_在线用户_推送并节流() throws Exception {
        when(webSocketHandler.getOnlineUserIds()).thenReturn(Set.of(100L, 200L));

        int first = service.sendProactiveMessages();
        assertEquals(2, first);
        verify(webSocketHandler, times(2)).sendMessageToUser(anyLong(), contains("proactive_message"));

        // 第二次在节流窗口内，不应再推送
        int second = service.sendProactiveMessages();
        assertEquals(0, second);
    }

    @Test
    void sendProactiveMessages_无人在线_返回0() {
        when(webSocketHandler.getOnlineUserIds()).thenReturn(Set.of());
        assertEquals(0, service.sendProactiveMessages());
    }

    @Test
    void createReminder_字段正确落库() {
        LocalDateTime t = LocalDateTime.now().plusHours(1);
        ElfReminder r = service.createReminder(100L, "运动", "去跑步", t);
        assertEquals(100L, r.getUserId());
        assertEquals("运动", r.getTitle());
        assertFalse(r.getTriggered());
        verify(reminderMapper).insert(r);
    }
}

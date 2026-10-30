package com.example.aichat.controller;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.SocialErrorCode;
import com.example.aichat.common.security.UserPrincipal;
import com.example.aichat.entity.GroupBot;
import com.example.aichat.entity.GroupChat;
import com.example.aichat.service.GroupAiChatService;
import com.example.aichat.service.GroupChatService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 群机器人管理接口 P0 越权（IDOR）群主校验单元测试。
 *
 * 覆盖：createGroupBot / updateGroupBot / deleteGroupBot 在进入业务前必须校验
 * 当前登录用户为该群群主，否则抛 NOT_GROUP_OWNER，下游写操作不得执行。
 */
@ExtendWith(MockitoExtension.class)
class GroupChatBotOwnerTest {

    private static final Long CURRENT_USER = 1L;
    private static final Long OTHER_USER = 999L;
    private static final Long GROUP_ID = 10L;
    private static final Long BOT_ID = 20L;

    @Mock
    private GroupChatService groupChatService;

    @Mock
    private GroupAiChatService groupAiChatService;

    private GroupChatController controller;

    @BeforeEach
    void setUp() {
        controller = new GroupChatController();
        ReflectionTestUtils.setField(controller, "groupChatService", groupChatService);
        ReflectionTestUtils.setField(controller, "groupAiChatService", groupAiChatService);
        UserPrincipal principal = new UserPrincipal(CURRENT_USER, "current", List.of("ROLE_USER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private GroupChat ownedGroup() {
        GroupChat g = new GroupChat();
        g.setId(GROUP_ID);
        g.setOwnerId(CURRENT_USER);
        return g;
    }

    private GroupBot botInGroup() {
        GroupBot bot = new GroupBot();
        bot.setId(BOT_ID);
        bot.setGroupId(GROUP_ID);
        bot.setBotName("bot");
        return bot;
    }

    @Test
    void createGroupBot_非群主_抛NOT_GROUP_OWNER且不创建() {
        GroupChat foreign = ownedGroup();
        foreign.setOwnerId(OTHER_USER);
        when(groupChatService.getGroupById(GROUP_ID)).thenReturn(foreign);

        BusinessException ex = assertThrows(BusinessException.class, () -> controller.createGroupBot(
                Map.of("groupId", GROUP_ID, "botName", "bot")));
        assertEquals(SocialErrorCode.NOT_GROUP_OWNER, ex.getErrorCode());
        verify(groupChatService, never()).createGroupBot(anyLong(), any(), any());
    }

    @Test
    void createGroupBot_群主_正常创建() {
        when(groupChatService.getGroupById(GROUP_ID)).thenReturn(ownedGroup());

        assertDoesNotThrow(() -> controller.createGroupBot(
                Map.of("groupId", GROUP_ID, "botName", "bot")));
        verify(groupChatService).createGroupBot(eq(GROUP_ID), eq("bot"), any());
    }

    @Test
    void updateGroupBot_机器人所属群非群主_抛异常且不更新() {
        GroupChat foreign = ownedGroup();
        foreign.setOwnerId(OTHER_USER);
        when(groupChatService.getGroupBotById(BOT_ID)).thenReturn(botInGroup());
        when(groupChatService.getGroupById(GROUP_ID)).thenReturn(foreign);

        BusinessException ex = assertThrows(BusinessException.class, () -> controller.updateGroupBot(
                BOT_ID, Map.of("botName", "x")));
        assertEquals(SocialErrorCode.NOT_GROUP_OWNER, ex.getErrorCode());
        verify(groupChatService, never()).updateGroupBot(anyLong(), any());
    }

    @Test
    void updateGroupBot_群主_正常更新() {
        when(groupChatService.getGroupBotById(BOT_ID)).thenReturn(botInGroup());
        when(groupChatService.getGroupById(GROUP_ID)).thenReturn(ownedGroup());

        assertDoesNotThrow(() -> controller.updateGroupBot(BOT_ID, Map.of("botName", "x")));
        verify(groupChatService).updateGroupBot(eq(BOT_ID), any());
    }

    @Test
    void deleteGroupBot_机器人所属群非群主_抛异常且不删除() {
        GroupChat foreign = ownedGroup();
        foreign.setOwnerId(OTHER_USER);
        when(groupChatService.getGroupBotById(BOT_ID)).thenReturn(botInGroup());
        when(groupChatService.getGroupById(GROUP_ID)).thenReturn(foreign);

        BusinessException ex = assertThrows(BusinessException.class, () -> controller.deleteGroupBot(BOT_ID));
        assertEquals(SocialErrorCode.NOT_GROUP_OWNER, ex.getErrorCode());
        verify(groupChatService, never()).deleteGroupBot(anyLong());
    }

    @Test
    void deleteGroupBot_机器人不存在_返回错误不删除() {
        when(groupChatService.getGroupBotById(BOT_ID)).thenReturn(null);

        assertDoesNotThrow(() -> controller.deleteGroupBot(BOT_ID));
        verify(groupChatService, never()).deleteGroupBot(anyLong());
    }
}

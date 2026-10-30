package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.SocialErrorCode;
import com.example.aichat.entity.GroupMember;
import com.example.aichat.entity.GroupMessage;
import com.example.aichat.mapper.AiCharacterMapper;
import com.example.aichat.mapper.GroupAiMemberMapper;
import com.example.aichat.mapper.GroupBotMapper;
import com.example.aichat.mapper.GroupChatMapper;
import com.example.aichat.mapper.GroupMemberMapper;
import com.example.aichat.mapper.GroupMessageMapper;
import com.example.aichat.mapper.UserInterestMapper;
import com.example.aichat.service.ContentAuditService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * P1-4 回归：读取群历史消息必须校验调用者为群成员，防止遍历 groupId 读他人群聊记录。
 */
class GroupChatMessagesAuthTest {

    private GroupChatServiceImpl newService(GroupMemberMapper memberMapper) {
        return new GroupChatServiceImpl(
                mock(GroupChatMapper.class), memberMapper, mock(GroupMessageMapper.class),
                mock(UserInterestMapper.class), mock(GroupBotMapper.class),
                mock(ContentAuditService.class), mock(GroupAiMemberMapper.class),
                mock(AiCharacterMapper.class));
    }

    @Test
    void 非成员读取群消息_拒绝() {
        GroupMemberMapper memberMapper = mock(GroupMemberMapper.class);
        when(memberMapper.findMember(10L, 99L)).thenReturn(null);
        GroupChatServiceImpl service = newService(memberMapper);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getGroupMessages(10L, 99L, 20));
        assertEquals(SocialErrorCode.NOT_GROUP_MEMBER, ex.getErrorCode());
    }

    @Test
    void 成员读取群消息_放行() {
        GroupMemberMapper memberMapper = mock(GroupMemberMapper.class);
        GroupMessageMapper messageMapper = mock(GroupMessageMapper.class);
        when(memberMapper.findMember(10L, 1L)).thenReturn(new GroupMember());
        when(messageMapper.getMessagesByGroupId(eq(10L), anyInt())).thenReturn(List.of(new GroupMessage()));
        GroupChatServiceImpl service = new GroupChatServiceImpl(
                mock(GroupChatMapper.class), memberMapper, messageMapper,
                mock(UserInterestMapper.class), mock(GroupBotMapper.class),
                mock(ContentAuditService.class), mock(GroupAiMemberMapper.class),
                mock(AiCharacterMapper.class));

        List<GroupMessage> result = service.getGroupMessages(10L, 1L, 20);
        assertEquals(1, result.size());
    }
}

package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.SocialErrorCode;
import com.example.aichat.entity.AiCharacter;
import com.example.aichat.entity.GroupAiMember;
import com.example.aichat.entity.GroupChat;
import com.example.aichat.entity.GroupMember;
import com.example.aichat.entity.GroupMessage;
import com.example.aichat.mapper.AiCharacterMapper;
import com.example.aichat.mapper.GroupAiMemberMapper;
import com.example.aichat.mapper.GroupChatMapper;
import com.example.aichat.mapper.GroupMemberMapper;
import com.example.aichat.mapper.GroupMessageMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 多 AI 角色同场群聊编排测试（Mock Mapper）。
 *
 * 覆盖：触发一轮发言（多角色轮番落库/非成员拒绝/空成员）、轮流取下一个、mock 回复生成。
 */
@ExtendWith(MockitoExtension.class)
class GroupAiChatServiceImplTest {

    @Mock
    private GroupAiMemberMapper groupAiMemberMapper;
    @Mock
    private AiCharacterMapper aiCharacterMapper;
    @Mock
    private GroupMessageMapper groupMessageMapper;
    @Mock
    private GroupChatMapper groupChatMapper;
    @Mock
    private GroupMemberMapper groupMemberMapper;

    @InjectMocks
    private GroupAiChatServiceImpl service;

    private GroupChat sampleGroup() {
        GroupChat g = new GroupChat();
        g.setId(10L);
        g.setOwnerId(1L);
        return g;
    }

    private GroupMember sampleMember() {
        GroupMember m = new GroupMember();
        m.setGroupId(10L);
        m.setUserId(1L);
        return m;
    }

    private GroupAiMember aiMember(Long characterId) {
        GroupAiMember m = new GroupAiMember();
        m.setGroupId(10L);
        m.setCharacterId(characterId);
        m.setIsActive(true);
        return m;
    }

    private AiCharacter character(Long id, String name, String personality) {
        AiCharacter c = new AiCharacter();
        c.setId(id);
        c.setName(name);
        c.setPersonality(personality);
        return c;
    }

    @Test
    void triggerAiRound_多角色轮番回复_每个AI落库并返回列表() {
        when(groupChatMapper.selectById(10L)).thenReturn(sampleGroup());
        when(groupMemberMapper.findMember(10L, 1L)).thenReturn(sampleMember());
        when(groupAiMemberMapper.findActiveByGroupId(10L))
                .thenReturn(Arrays.asList(aiMember(1L), aiMember(2L)));
        when(aiCharacterMapper.selectById(1L)).thenReturn(character(1L, "Alice", "温柔"));
        when(aiCharacterMapper.selectById(2L)).thenReturn(character(2L, "Bob", "幽默"));

        List<Map<String, Object>> replies = service.triggerAiRound(10L, 1L, "今天聊什么？");

        assertEquals(2, replies.size());
        assertEquals(1L, replies.get(0).get("characterId"));
        assertEquals(2L, replies.get(1).get("characterId"));
        assertTrue(((String) replies.get(0).get("content")).contains("Alice"));
        verify(groupMessageMapper, times(2)).insert(any(GroupMessage.class));
    }

    @Test
    void triggerAiRound_群不存在应抛GROUP_NOT_FOUND() {
        when(groupChatMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.triggerAiRound(99L, 1L, "话题"));
        assertEquals(SocialErrorCode.GROUP_NOT_FOUND, ex.getErrorCode());
        verify(groupMessageMapper, never()).insert(any(GroupMessage.class));
    }

    @Test
    void triggerAiRound_非成员应抛NOT_GROUP_MEMBER() {
        when(groupChatMapper.selectById(10L)).thenReturn(sampleGroup());
        when(groupMemberMapper.findMember(10L, 99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.triggerAiRound(10L, 99L, "话题"));
        assertEquals(SocialErrorCode.NOT_GROUP_MEMBER, ex.getErrorCode());
        verify(groupMessageMapper, never()).insert(any(GroupMessage.class));
    }

    @Test
    void triggerAiRound_无激活AI应返回空列表() {
        when(groupChatMapper.selectById(10L)).thenReturn(sampleGroup());
        when(groupMemberMapper.findMember(10L, 1L)).thenReturn(sampleMember());
        when(groupAiMemberMapper.findActiveByGroupId(10L)).thenReturn(List.of());

        List<Map<String, Object>> replies = service.triggerAiRound(10L, 1L, "话题");

        assertTrue(replies.isEmpty());
        verify(groupMessageMapper, never()).insert(any(GroupMessage.class));
    }

    @Test
    void getNextAiCharacter_有成员应返回第一个() {
        when(groupAiMemberMapper.findActiveByGroupId(10L))
                .thenReturn(Arrays.asList(aiMember(1L), aiMember(2L), aiMember(3L)));

        GroupAiMember next = service.getNextAiCharacter(10L);
        assertNotNull(next);
        assertEquals(1L, next.getCharacterId());
    }

    @Test
    void getNextAiCharacter_无成员应返回null() {
        when(groupAiMemberMapper.findActiveByGroupId(10L)).thenReturn(List.of());
        assertNull(service.getNextAiCharacter(10L));
    }

    @Test
    void triggerAiRound后_getNextAiCharacter应轮转至下一个() {
        when(groupChatMapper.selectById(10L)).thenReturn(sampleGroup());
        when(groupMemberMapper.findMember(10L, 1L)).thenReturn(sampleMember());
        when(groupAiMemberMapper.findActiveByGroupId(10L))
                .thenReturn(Arrays.asList(aiMember(1L), aiMember(2L), aiMember(3L)));
        when(aiCharacterMapper.selectById(anyLong())).thenAnswer(inv ->
                character(inv.getArgument(0), "AI" + inv.getArgument(0), "普通"));

        service.triggerAiRound(10L, 1L, "话题");

        // 游标已后移一位，下一个应为 characterId=2
        GroupAiMember next = service.getNextAiCharacter(10L);
        assertEquals(2L, next.getCharacterId());
    }

    @Test
    void generateAiReply_mock回复应包含角色名与话题() {
        AiCharacter c = character(1L, "小助手", "严谨");
        String reply = service.generateAiReply(c, "如何学习英语", List.of());

        assertTrue(reply.contains("小助手"));
        assertTrue(reply.contains("如何学习英语"));
    }

    @Test
    void generateAiReply_不同角色应产生不同口吻() {
        AiCharacter a = character(1L, "Alice", "温柔体贴");
        AiCharacter b = character(2L, "Bob", "理性冷静");
        String ra = service.generateAiReply(a, "话题", List.of());
        String rb = service.generateAiReply(b, "话题", List.of());
        // 即使话题相同，角色名不同也应导致不同回复文本
        assertNotEquals(ra, rb);
    }
}

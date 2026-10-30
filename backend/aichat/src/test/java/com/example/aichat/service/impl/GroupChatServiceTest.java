package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.SocialErrorCode;
import com.example.aichat.dto.audit.AuditResult;
import com.example.aichat.entity.AiCharacter;
import com.example.aichat.entity.GroupAiMember;
import com.example.aichat.entity.GroupChat;
import com.example.aichat.entity.GroupMember;
import com.example.aichat.mapper.AiCharacterMapper;
import com.example.aichat.mapper.GroupAiMemberMapper;
import com.example.aichat.mapper.GroupBotMapper;
import com.example.aichat.mapper.GroupChatMapper;
import com.example.aichat.mapper.GroupMemberMapper;
import com.example.aichat.mapper.GroupMessageMapper;
import com.example.aichat.mapper.UserInterestMapper;
import com.example.aichat.service.ContentAuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * GroupChatService 单元测试（Mockito 隔离）。
 *
 * 覆盖点：
 *   1. createGroup：正常创建群+群主成员；
 *   2. createGroup：空群名抛 GROUP_NAME_BLANK；
 *   3. createGroup：群名过长抛 GROUP_NAME_TOO_LONG；
 *   4. joinGroup：群不存在抛 GROUP_NOT_FOUND；
 *   5. joinGroup：重复加入幂等，不重复插入；
 *   6. joinGroup：正常加入并累加成员数；
 *   7. leaveGroup：正常退出并递减成员数；
 *   8. sendMessage：非成员抛 NOT_GROUP_MEMBER（P0 权限）；
 *   9. sendMessage：空内容抛 GROUP_MESSAGE_BLANK；
 *  10. sendMessage：成员正常发言；
 *  11. updateAnnouncement：正常更新公告。
 */
@ExtendWith(MockitoExtension.class)
class GroupChatServiceTest {

    @Mock
    private GroupChatMapper groupChatMapper;
    @Mock
    private GroupMemberMapper groupMemberMapper;
    @Mock
    private GroupMessageMapper groupMessageMapper;
    @Mock
    private UserInterestMapper userInterestMapper;
    @Mock
    private GroupBotMapper groupBotMapper;
    @Mock
    private ContentAuditService contentAuditService;
    @Mock
    private GroupAiMemberMapper groupAiMemberMapper;
    @Mock
    private AiCharacterMapper aiCharacterMapper;

    @InjectMocks
    private GroupChatServiceImpl groupChatService;

    private GroupChat sampleGroup(Long id, int memberCount) {
        GroupChat g = new GroupChat();
        g.setId(id);
        g.setName("测试群");
        g.setOwnerId(1L);
        g.setMemberCount(memberCount);
        g.setIsPublic(true);
        return g;
    }

    private GroupMember sampleMember(Long groupId, Long userId) {
        GroupMember m = new GroupMember();
        m.setId(groupId * 100 + userId);
        m.setGroupId(groupId);
        m.setUserId(userId);
        return m;
    }

    // ============ createGroup ============

    @Test
    void createGroup_正常应创建群并加入群主成员() {
        GroupChat result = groupChatService.createGroup(1L, "新群", "描述");

        assertNotNull(result);
        verify(groupChatMapper).insert(any(GroupChat.class));
        verify(groupMemberMapper).insert(any(GroupMember.class));
    }

    @Test
    void createGroup_空群名应抛GROUP_NAME_BLANK() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> groupChatService.createGroup(1L, "  ", "描述"));
        assertEquals(SocialErrorCode.GROUP_NAME_BLANK, ex.getErrorCode());
        verifyNoInteractions(groupChatMapper);
        verifyNoInteractions(groupMemberMapper);
    }

    @Test
    void createGroup_群名过长应抛GROUP_NAME_TOO_LONG() {
        String longName = "n".repeat(51);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> groupChatService.createGroup(1L, longName, "描述"));
        assertEquals(SocialErrorCode.GROUP_NAME_TOO_LONG, ex.getErrorCode());
        verifyNoInteractions(groupChatMapper);
    }

    // ============ joinGroup ============

    @Test
    void joinGroup_群不存在应抛GROUP_NOT_FOUND() {
        when(groupChatMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> groupChatService.joinGroup(99L, 1L));
        assertEquals(SocialErrorCode.GROUP_NOT_FOUND, ex.getErrorCode());
        verify(groupMemberMapper, never()).insert(any(GroupMember.class));
    }

    @Test
    void joinGroup_重复加入应幂等不重复插入() {
        when(groupChatMapper.selectById(10L)).thenReturn(sampleGroup(10L, 5));
        when(groupMemberMapper.findMember(10L, 1L)).thenReturn(sampleMember(10L, 1L));

        groupChatService.joinGroup(10L, 1L);

        verify(groupMemberMapper, never()).insert(any(GroupMember.class));
        verify(groupChatMapper, never()).updateById(any(GroupChat.class));
    }

    @Test
    void joinGroup_正常加入应累加成员数() {
        when(groupChatMapper.selectById(10L)).thenReturn(sampleGroup(10L, 5));
        when(groupMemberMapper.findMember(10L, 1L)).thenReturn(null);

        groupChatService.joinGroup(10L, 1L);

        verify(groupMemberMapper).insert(any(GroupMember.class));
        verify(groupChatMapper).updateById(argThat((GroupChat g) -> g.getMemberCount() == 6));
    }

    // ============ leaveGroup ============

    @Test
    void leaveGroup_正常退出应递减成员数() {
        when(groupMemberMapper.findMember(10L, 1L)).thenReturn(sampleMember(10L, 1L));
        when(groupChatMapper.selectById(10L)).thenReturn(sampleGroup(10L, 5));

        groupChatService.leaveGroup(10L, 1L);

        verify(groupMemberMapper).deleteById(anyLong());
        verify(groupChatMapper).updateById(argThat((GroupChat g) -> g.getMemberCount() == 4));
    }

    // ============ sendMessage ============

    @Test
    void sendMessage_非成员应抛NOT_GROUP_MEMBER() {
        when(groupMemberMapper.findMember(10L, 99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> groupChatService.sendMessage(10L, 99L, "Stranger", "消息内容"));
        assertEquals(SocialErrorCode.NOT_GROUP_MEMBER, ex.getErrorCode());
        verify(groupMessageMapper, never()).insert(any(com.example.aichat.entity.GroupMessage.class));
    }

    @Test
    void sendMessage_空内容应抛GROUP_MESSAGE_BLANK() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> groupChatService.sendMessage(10L, 1L, "Alice", "  "));
        assertEquals(SocialErrorCode.GROUP_MESSAGE_BLANK, ex.getErrorCode());
        verify(groupMessageMapper, never()).insert(any(com.example.aichat.entity.GroupMessage.class));
    }

    @Test
    void sendMessage_成员正常发言应落库() {
        when(groupMemberMapper.findMember(10L, 1L)).thenReturn(sampleMember(10L, 1L));
        when(contentAuditService.audit(anyString(), anyString(), anyLong())).thenReturn(AuditResult.pass());

        groupChatService.sendMessage(10L, 1L, "Alice", "大家好");

        verify(groupMessageMapper).insert(any(com.example.aichat.entity.GroupMessage.class));
    }

    // ============ updateAnnouncement ============

    @Test
    void updateAnnouncement_群主正常更新() {
        when(groupChatMapper.selectById(10L)).thenReturn(sampleGroup(10L, 5));

        GroupChat result = groupChatService.updateAnnouncement(10L, 1L, "新公告");

        assertNotNull(result);
        assertEquals("新公告", result.getAnnouncement());
        verify(groupChatMapper).updateById(any(GroupChat.class));
    }

    @Test
    void updateAnnouncement_非群主应抛NOT_GROUP_OWNER() {
        when(groupChatMapper.selectById(10L)).thenReturn(sampleGroup(10L, 5));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> groupChatService.updateAnnouncement(10L, 99L, "新公告"));
        assertEquals(SocialErrorCode.NOT_GROUP_OWNER, ex.getErrorCode());
        verify(groupChatMapper, never()).updateById(any(GroupChat.class));
    }

    @Test
    void updateAnnouncement_群不存在应抛GROUP_NOT_FOUND() {
        when(groupChatMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> groupChatService.updateAnnouncement(99L, 1L, "新公告"));
        assertEquals(SocialErrorCode.GROUP_NOT_FOUND, ex.getErrorCode());
        verify(groupChatMapper, never()).updateById(any(GroupChat.class));
    }

    // ============ updateSettings ============

    @Test
    void updateSettings_群主正常更新() {
        when(groupChatMapper.selectById(10L)).thenReturn(sampleGroup(10L, 5));

        GroupChat result = groupChatService.updateSettings(10L, 1L, "{\"mute\":true}");

        assertNotNull(result);
        assertEquals("{\"mute\":true}", result.getSettings());
        verify(groupChatMapper).updateById(any(GroupChat.class));
    }

    @Test
    void updateSettings_非群主应抛NOT_GROUP_OWNER() {
        when(groupChatMapper.selectById(10L)).thenReturn(sampleGroup(10L, 5));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> groupChatService.updateSettings(10L, 99L, "{}"));
        assertEquals(SocialErrorCode.NOT_GROUP_OWNER, ex.getErrorCode());
        verify(groupChatMapper, never()).updateById(any(GroupChat.class));
    }

    // ============ deleteGroup ============

    @Test
    void deleteGroup_群主正常删除() {
        when(groupChatMapper.selectById(10L)).thenReturn(sampleGroup(10L, 5));

        groupChatService.deleteGroup(10L, 1L);

        verify(groupMessageMapper).delete(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(groupMemberMapper).delete(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(groupChatMapper).deleteById(10L);
    }

    @Test
    void deleteGroup_非群主应抛NOT_GROUP_OWNER() {
        when(groupChatMapper.selectById(10L)).thenReturn(sampleGroup(10L, 5));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> groupChatService.deleteGroup(10L, 99L));
        assertEquals(SocialErrorCode.NOT_GROUP_OWNER, ex.getErrorCode());
        verify(groupChatMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteGroup_群不存在应抛GROUP_NOT_FOUND() {
        when(groupChatMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> groupChatService.deleteGroup(99L, 1L));
        assertEquals(SocialErrorCode.GROUP_NOT_FOUND, ex.getErrorCode());
        verify(groupChatMapper, never()).deleteById(anyLong());
        verify(groupMessageMapper, never()).delete(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    // ============ 多 AI 群聊：群成员管理 ============

    private AiCharacter sampleCharacter(Long id) {
        AiCharacter c = new AiCharacter();
        c.setId(id);
        c.setName("AI-" + id);
        c.setIsShared(true);
        return c;
    }

    @Test
    void addAiCharacterToGroup_群成员正常添加_应插入AI成员() {
        when(groupChatMapper.selectById(10L)).thenReturn(sampleGroup(10L, 3));
        when(groupMemberMapper.findMember(10L, 1L)).thenReturn(sampleMember(10L, 1L));
        when(aiCharacterMapper.selectById(5L)).thenReturn(sampleCharacter(5L));
        when(groupAiMemberMapper.findByGroupAndCharacter(10L, 5L)).thenReturn(null);

        groupChatService.addAiCharacterToGroup(10L, 1L, 5L);

        verify(groupAiMemberMapper).insert(any(GroupAiMember.class));
    }

    @Test
    void addAiCharacterToGroup_非群成员应抛NOT_GROUP_MEMBER() {
        when(groupChatMapper.selectById(10L)).thenReturn(sampleGroup(10L, 3));
        when(groupMemberMapper.findMember(10L, 99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> groupChatService.addAiCharacterToGroup(10L, 99L, 5L));
        assertEquals(SocialErrorCode.NOT_GROUP_MEMBER, ex.getErrorCode());
        verify(groupAiMemberMapper, never()).insert(any(GroupAiMember.class));
    }

    @Test
    void addAiCharacterToGroup_已激活重复添加应幂等() {
        GroupAiMember active = new GroupAiMember();
        active.setGroupId(10L);
        active.setCharacterId(5L);
        active.setIsActive(true);
        when(groupChatMapper.selectById(10L)).thenReturn(sampleGroup(10L, 3));
        when(groupMemberMapper.findMember(10L, 1L)).thenReturn(sampleMember(10L, 1L));
        when(aiCharacterMapper.selectById(5L)).thenReturn(sampleCharacter(5L));
        when(groupAiMemberMapper.findByGroupAndCharacter(10L, 5L)).thenReturn(active);

        groupChatService.addAiCharacterToGroup(10L, 1L, 5L);

        verify(groupAiMemberMapper, never()).insert(any(GroupAiMember.class));
        verify(groupAiMemberMapper, never()).updateById(any(GroupAiMember.class));
    }

    @Test
    void removeAiCharacterFromGroup_群主正常移除_应置为不激活() {
        GroupAiMember active = new GroupAiMember();
        active.setGroupId(10L);
        active.setCharacterId(5L);
        active.setIsActive(true);
        when(groupChatMapper.selectById(10L)).thenReturn(sampleGroup(10L, 3));
        when(groupAiMemberMapper.findByGroupAndCharacter(10L, 5L)).thenReturn(active);

        groupChatService.removeAiCharacterFromGroup(10L, 1L, 5L);

        assertFalse(active.getIsActive());
        verify(groupAiMemberMapper).updateById(any(GroupAiMember.class));
    }

    @Test
    void removeAiCharacterFromGroup_非群主应抛NOT_GROUP_OWNER() {
        when(groupChatMapper.selectById(10L)).thenReturn(sampleGroup(10L, 3));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> groupChatService.removeAiCharacterFromGroup(10L, 99L, 5L));
        assertEquals(SocialErrorCode.NOT_GROUP_OWNER, ex.getErrorCode());
        verify(groupAiMemberMapper, never()).updateById(any(GroupAiMember.class));
    }

    @Test
    void listAiCharactersInGroup_应返回激活成员列表() {
        groupChatService.listAiCharactersInGroup(10L);
        verify(groupAiMemberMapper).findActiveByGroupId(10L);
    }
}

package com.example.aichat.service.impl;

import com.example.aichat.entity.AiCharacter;
import com.example.aichat.dto.audit.AuditResult;
import com.example.aichat.mapper.AiCharacterMapper;
import com.example.aichat.mapper.CharacterShareMapper;
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
 * AI 模块 - 角色服务单元测试（Mock Mapper）。
 *
 * 覆盖点：
 *   1. 更新非本人角色 -> 返回 null（越权拒绝）；
 *   2. 更新系统角色 -> 返回 null；
 *   3. 删除非本人角色 -> false；
 *   4. 删除系统角色 -> false；
 *   5. isOwner / canAccess 判定（本人 / 陌生人 / 系统角色）。
 */
@ExtendWith(MockitoExtension.class)
class AiCharacterServiceImplTest {

    @Mock
    private AiCharacterMapper aiCharacterMapper;

    @Mock
    private CharacterShareMapper characterShareMapper;
    @Mock
    private ContentAuditService contentAuditService;

    @InjectMocks
    private AiCharacterServiceImpl service;

    private AiCharacter character(Long id, Long ownerId, boolean system) {
        AiCharacter c = new AiCharacter();
        c.setId(id);
        c.setUserId(ownerId);
        c.setIsSystem(system);
        return c;
    }

    @Test
    void 更新非本人角色_应拒绝返回null() {
        when(aiCharacterMapper.selectById(1L)).thenReturn(character(1L, 999L, false));
        AiCharacter update = character(1L, null, false);
        update.setName("hacked");
        assertNull(service.updateCharacter(update, 1L));
        verify(aiCharacterMapper, never()).updateById(any(AiCharacter.class));
    }

    @Test
    void 更新系统角色_应拒绝() {
        when(aiCharacterMapper.selectById(1L)).thenReturn(character(1L, null, true));
        assertNull(service.updateCharacter(character(1L, null, false), 1L));
    }

    @Test
    void 更新本人角色_应成功() {
        AiCharacter owner = character(1L, 1L, false);
        when(aiCharacterMapper.selectById(1L)).thenReturn(owner);
        when(contentAuditService.audit(anyString(), anyString(), anyLong())).thenReturn(AuditResult.pass());
        AiCharacter patch = character(1L, 1L, false);
        patch.setName("newName");
        assertNotNull(service.updateCharacter(patch, 1L));
        verify(aiCharacterMapper).updateById(any(AiCharacter.class));
    }

    @Test
    void 删除非本人角色_应拒绝() {
        when(aiCharacterMapper.selectById(1L)).thenReturn(character(1L, 999L, false));
        assertFalse(service.deleteCharacter(1L, 1L));
        verify(aiCharacterMapper, never()).deleteById(anyLong());
    }

    @Test
    void 删除系统角色_应拒绝() {
        when(aiCharacterMapper.selectById(1L)).thenReturn(character(1L, null, true));
        assertFalse(service.deleteCharacter(1L, 1L));
    }

    @Test
    void isOwner_本人true陌生人false() {
        when(aiCharacterMapper.selectById(1L)).thenReturn(character(1L, 1L, false));
        assertTrue(service.isOwner(1L, 1L));
        assertFalse(service.isOwner(2L, 1L));
    }

    @Test
    void canAccess_系统角色人人可访问_陌生人无分享不可() {
        when(aiCharacterMapper.selectById(1L)).thenReturn(character(1L, 1L, true));
        assertTrue(service.canAccess(2L, 1L));

        when(aiCharacterMapper.selectById(2L)).thenReturn(character(2L, 1L, false));
        when(characterShareMapper.existsByCharacterIdAndUserId(2L, 2L)).thenReturn(false);
        assertFalse(service.canAccess(2L, 2L));
        assertTrue(service.canAccess(1L, 2L));
    }

    // ============ 角色创建字段丰富化（B组） ============

    @Test
    void createCharacter_应保留新字段greetingTagsVoiceIdCategory() {
        when(contentAuditService.audit(anyString(), anyString(), anyLong())).thenReturn(AuditResult.pass());

        AiCharacter c = new AiCharacter();
        c.setUserId(1L);
        c.setName("导师");
        c.setPersonality("严谨");
        c.setGreeting("你好，我是导师");
        c.setTags("教育,学习");
        c.setVoiceId("voice-male-1");
        c.setCategory("知识");
        c.setDescription("一位耐心的学习导师");
        c.setAvatarStyle("bottts");

        AiCharacter created = service.createCharacter(c);

        assertNotNull(created);
        assertEquals("你好，我是导师", created.getGreeting());
        assertEquals("教育,学习", created.getTags());
        assertEquals("voice-male-1", created.getVoiceId());
        assertEquals("知识", created.getCategory());
        assertEquals("一位耐心的学习导师", created.getDescription());
        assertEquals("bottts", created.getAvatarStyle());
        verify(aiCharacterMapper).insert(any(AiCharacter.class));
    }

    @Test
    void createCharacter_空greeting不应影响创建() {
        when(contentAuditService.audit(anyString(), anyString(), anyLong())).thenReturn(AuditResult.pass());

        AiCharacter c = new AiCharacter();
        c.setUserId(1L);
        c.setName("无开场白角色");
        c.setGreeting(null);
        c.setExampleDialogue("");

        AiCharacter created = service.createCharacter(c);

        assertNotNull(created);
        assertNull(created.getGreeting());
        verify(aiCharacterMapper).insert(any(AiCharacter.class));
    }

    @Test
    void updateCharacter_应透传新字段到updateById() {
        when(aiCharacterMapper.selectById(1L)).thenReturn(character(1L, 1L, false));
        when(contentAuditService.audit(anyString(), anyString(), anyLong())).thenReturn(AuditResult.pass());

        AiCharacter patch = character(1L, 1L, false);
        patch.setPersonality("幽默");
        patch.setTags("搞笑,日常");
        patch.setCategory("娱乐");
        patch.setGreeting("嗨~");

        AiCharacter updated = service.updateCharacter(patch, 1L);

        assertNotNull(updated);
        verify(aiCharacterMapper).updateById(argThat((AiCharacter p) ->
                "幽默".equals(p.getPersonality()) && "搞笑,日常".equals(p.getTags())
                        && "娱乐".equals(p.getCategory()) && "嗨~".equals(p.getGreeting())));
    }

    @Test
    void getCharacterByIdWithAccess_应能读取新字段详情() {
        AiCharacter stored = character(1L, 1L, false);
        stored.setGreeting("欢迎回来");
        stored.setTags("治愈,陪伴");
        stored.setVoiceId("voice-soft-1");
        when(aiCharacterMapper.selectById(1L)).thenReturn(stored);

        AiCharacter detail = service.getCharacterByIdWithAccess(1L, 1L);

        assertNotNull(detail);
        assertEquals("欢迎回来", detail.getGreeting());
        assertEquals("治愈,陪伴", detail.getTags());
        assertEquals("voice-soft-1", detail.getVoiceId());
    }
}

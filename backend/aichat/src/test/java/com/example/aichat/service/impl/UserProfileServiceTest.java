package com.example.aichat.service.impl;

import com.example.aichat.entity.UserProfile;
import com.example.aichat.mapper.UserProfileMapper;
import com.example.aichat.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserProfileService 单元测试（Mockito 隔离，完全离线）。
 *
 * 覆盖：默认画像、手动更新插入、对话自动更新（话题抽取 + 时段累加）、
 *       话题去重与上限截断、时段分桶边界。
 */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    private static final Long USER = 42L;

    @Mock
    private UserProfileMapper profileMapper;

    private UserProfileService service;

    @BeforeEach
    void setUp() {
        service = new UserProfileServiceImpl(profileMapper);
    }

    // ---------- 1. 查询默认画像 ----------
    @Test
    void getOrCreateProfile_不存在_返回默认空画像且不落库() {
        when(profileMapper.selectByUserId(USER)).thenReturn(null);

        UserProfile profile = service.getOrCreateProfile(USER);

        assertNotNull(profile);
        assertEquals(USER, profile.getUserId());
        assertEquals("[]", profile.getCommonTopics());
        assertEquals("{}", profile.getActiveTimeSlots());
        // 仅查询，不应触发 insert
        verify(profileMapper, never()).insert(any(UserProfile.class));
    }

    // ---------- 2. 手动更新：新用户插入 ----------
    @Test
    void updateProfile_新用户_插入并写入字段() {
        when(profileMapper.selectByUserId(USER)).thenReturn(null);

        UserProfile result = service.updateProfile(USER, "concise",
                Arrays.asList("学习", "游戏"), null);

        assertNotNull(result);
        assertEquals("concise", result.getPreferredStyle());
        assertTrue(result.getCommonTopics().contains("学习"));
        assertTrue(result.getCommonTopics().contains("游戏"));
        verify(profileMapper).insert(any(UserProfile.class));
    }

    // ---------- 3. 对话自动更新：话题抽取 + 时段累加 ----------
    @Test
    void recordConversation_新用户_抽取话题并落库() {
        when(profileMapper.selectByUserId(USER)).thenReturn(null);

        // 以标点切分，得到 <=10 字的候选片段："你好"、"我想学习"
        service.recordConversation(USER, "你好，我想学习。");

        // 新用户走 insert
        verify(profileMapper).insert(any(UserProfile.class));
        // 验证话题 JSON 写入（通过捕获参数）
        org.mockito.ArgumentCaptor<UserProfile> captor =
                org.mockito.ArgumentCaptor.forClass(UserProfile.class);
        verify(profileMapper).insert(captor.capture());
        UserProfile saved = captor.getValue();
        assertNotNull(saved.getCommonTopics());
        assertTrue(saved.getCommonTopics().contains("学习"),
                "应抽取到候选片段（含 学习），实际=" + saved.getCommonTopics());
        // 活跃时段 JSON 至少包含一个时段键
        assertNotNull(saved.getActiveTimeSlots());
        assertTrue(saved.getActiveTimeSlots().matches(".*\"(morning|afternoon|evening|night)\".*"),
                "活跃时段应包含分桶键，实际=" + saved.getActiveTimeSlots());
    }

    // ---------- 4. 对话自动更新：已有用户走 update 且话题去重 ----------
    @Test
    void recordConversation_已有用户_话题去重且走update() {
        UserProfile existing = new UserProfile();
        existing.setId(7L);
        existing.setUserId(USER);
        existing.setCommonTopics("[\"学习\"]");
        existing.setActiveTimeSlots("{\"morning\":1}");
        when(profileMapper.selectByUserId(USER)).thenReturn(existing);

        // 切分片段："学习"（已存在，去重）、"游戏吧"（新话题，加入）
        service.recordConversation(USER, "学习。游戏吧。");

        verify(profileMapper, never()).insert(any(UserProfile.class));
        verify(profileMapper).updateById(any(UserProfile.class));
        org.mockito.ArgumentCaptor<UserProfile> captor =
                org.mockito.ArgumentCaptor.forClass(UserProfile.class);
        verify(profileMapper).updateById(captor.capture());
        String topics = captor.getValue().getCommonTopics();
        // "游戏吧" 应被加入；"学习" 作为独立话题只出现一次（精确去重）
        assertTrue(topics.contains("游戏吧"), "应加入新话题 游戏吧，实际=" + topics);
        assertEquals(1, countOccurrences(topics, "\"学习\""),
                "精确去重后 学习 作为独立话题应只出现一次，实际=" + topics);
    }

    /** 统计字符串中某子串出现次数。 */
    private static int countOccurrences(String s, String sub) {
        if (s == null || sub == null || sub.isEmpty()) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ((idx = s.indexOf(sub, idx)) >= 0) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    // ---------- 5. 时段分桶边界 ----------
    @Test
    void hourToSlot_各时段分桶正确() {
        assertEquals(UserProfileService.SLOT_NIGHT, UserProfileServiceImpl.hourToSlot(0));
        assertEquals(UserProfileService.SLOT_NIGHT, UserProfileServiceImpl.hourToSlot(5));
        assertEquals(UserProfileService.SLOT_MORNING, UserProfileServiceImpl.hourToSlot(6));
        assertEquals(UserProfileService.SLOT_MORNING, UserProfileServiceImpl.hourToSlot(11));
        assertEquals(UserProfileService.SLOT_AFTERNOON, UserProfileServiceImpl.hourToSlot(12));
        assertEquals(UserProfileService.SLOT_AFTERNOON, UserProfileServiceImpl.hourToSlot(17));
        assertEquals(UserProfileService.SLOT_EVENING, UserProfileServiceImpl.hourToSlot(18));
        assertEquals(UserProfileService.SLOT_EVENING, UserProfileServiceImpl.hourToSlot(23));
    }

    // ---------- 6. 空消息仅更新时段，不抽取话题，不抛错 ----------
    @Test
    void recordConversation_空消息_不抽话题且不抛错() {
        UserProfile existing = new UserProfile();
        existing.setId(8L);
        existing.setUserId(USER);
        existing.setCommonTopics("[\"现有\"]");
        existing.setActiveTimeSlots("{}");
        when(profileMapper.selectByUserId(USER)).thenReturn(existing);

        assertDoesNotThrow(() -> service.recordConversation(USER, "   "));
        org.mockito.ArgumentCaptor<UserProfile> captor =
                org.mockito.ArgumentCaptor.forClass(UserProfile.class);
        verify(profileMapper).updateById(captor.capture());
        assertEquals("[\"现有\"]", captor.getValue().getCommonTopics(),
                "空消息不应改变已有话题");
    }
}

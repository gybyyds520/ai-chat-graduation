package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.UserErrorCode;
import com.example.aichat.entity.User;
import com.example.aichat.entity.UserStatistics;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.mapper.UserStatisticsMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProfileServiceImpl 单元测试（Mockito 隔离）。
 *
 * 覆盖点：
 *   1. 公开主页查询：存在/不存在用户；
 *   2. 资料更新：用户不存在返回 null、字符串字段正常更新、
 *      非字符串值安全跳过（修复 ClassCastException）；
 *   3. 头像上传：不存在返回 null、存在则写回 avatarUrl；
 *   4. 统计：无记录时初始化、有记录直接返回、增量统计调用对应 mapper。
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private UserStatisticsMapper userStatisticsMapper;

    @InjectMocks
    private ProfileServiceImpl profileService;

    // ============ 公开主页 ============

    @Test
    void getProfile_存在用户返回用户() {
        User u = new User();
        u.setId(1L);
        u.setUsername("alice");
        when(userMapper.selectById(1L)).thenReturn(u);

        assertSame(u, profileService.getProfile(1L));
    }

    @Test
    void getProfile_不存在返回null() {
        when(userMapper.selectById(404L)).thenReturn(null);
        assertNull(profileService.getProfile(404L));
    }

    // ============ 资料更新 ============

    @Test
    void updateProfile_用户不存在返回null() {
        when(userMapper.selectById(404L)).thenReturn(null);
        assertNull(profileService.updateProfile(404L, Map.of("nickname", "x")));
    }

    @Test
    void updateProfile_字符串字段正常更新() {
        User u = new User();
        u.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(u);

        Map<String, Object> updates = new HashMap<>();
        updates.put("nickname", "新昵称");
        updates.put("bio", "个人简介");
        updates.put("interests", "编程");

        User result = profileService.updateProfile(1L, updates);

        assertEquals("新昵称", result.getNickname());
        assertEquals("个人简介", result.getBio());
        assertEquals("编程", result.getInterests());
        verify(userMapper).updateById(u);
    }

    @Test
    void updateProfile_非字符串值应安全跳过不抛异常() {
        // 修复前：gender 传入 Boolean/Number 会 (String) 强转抛 ClassCastException
        User u = new User();
        u.setId(1L);
        u.setGender("未知");
        when(userMapper.selectById(1L)).thenReturn(u);

        Map<String, Object> updates = new HashMap<>();
        updates.put("gender", Boolean.TRUE); // 非字符串
        updates.put("nickname", "正常昵称");

        User result = profileService.updateProfile(1L, updates);

        assertNotNull(result);
        assertEquals("正常昵称", result.getNickname());
        assertEquals("未知", result.getGender(), "非字符串值不应覆盖原字段");
        verify(userMapper).updateById(u);
    }

    @Test
    void updateProfile_昵称超长应拒绝不落库() {
        User u = new User();
        u.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(u);
        String longNickname = "n".repeat(51);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> profileService.updateProfile(1L, Map.of("nickname", longNickname)));
        assertEquals(UserErrorCode.NICKNAME_TOO_LONG, ex.getErrorCode());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void updateProfile_简介超长应拒绝不落库() {
        User u = new User();
        u.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(u);
        String longBio = "b".repeat(501);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> profileService.updateProfile(1L, Map.of("bio", longBio)));
        assertEquals(UserErrorCode.BIO_TOO_LONG, ex.getErrorCode());
        verify(userMapper, never()).updateById(any(User.class));
    }

    // ============ 头像 ============

    @Test
    void uploadAvatar_用户不存在返回null() {
        when(userMapper.selectById(404L)).thenReturn(null);
        assertNull(profileService.uploadAvatar(404L, "/avatar/x.png"));
    }

    @Test
    void uploadAvatar_存在则写回avatarUrl() {
        User u = new User();
        u.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(u);

        String url = profileService.uploadAvatar(1L, "/avatars/a.png");

        assertEquals("/avatars/a.png", url);
        assertEquals("/avatars/a.png", u.getAvatarUrl());
        verify(userMapper).updateById(u);
    }

    // ============ 统计 ============

    @Test
    void getStatistics_无记录应初始化() {
        when(userStatisticsMapper.getByUserId(1L)).thenReturn(null);

        UserStatistics stats = profileService.getStatistics(1L);

        assertNotNull(stats);
        assertEquals(1L, stats.getUserId());
        verify(userStatisticsMapper).insert(stats);
    }

    @Test
    void getStatistics_有记录直接返回() {
        UserStatistics existing = new UserStatistics();
        existing.setUserId(1L);
        when(userStatisticsMapper.getByUserId(1L)).thenReturn(existing);

        assertSame(existing, profileService.getStatistics(1L));
        verify(userStatisticsMapper, never()).insert(any(UserStatistics.class));
    }

    @Test
    void incrementStatistic_post类型调用对应mapper() {
        UserStatistics existing = new UserStatistics();
        existing.setUserId(1L);
        when(userStatisticsMapper.getByUserId(1L)).thenReturn(existing);

        profileService.incrementStatistic(1L, "post");

        verify(userStatisticsMapper).incrementPostCount(1L);
    }

    @Test
    void incrementStatistic_无记录应初始化不累加() {
        when(userStatisticsMapper.getByUserId(1L)).thenReturn(null);

        UserStatistics stats = profileService.incrementStatistic(1L, "post");

        assertNotNull(stats);
        verify(userStatisticsMapper, never()).incrementPostCount(any());
    }
}

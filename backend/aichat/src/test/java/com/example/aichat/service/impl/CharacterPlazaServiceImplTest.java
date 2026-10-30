package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.entity.AiCharacter;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.AiCharacterMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.CharacterPlazaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 角色广场服务单元测试（Mock Mapper）。
 *
 * 覆盖：列表分页/分类/排序、搜索、详情（公开/所有者）、热门排行、分类去重、使用次数递增。
 */
@ExtendWith(MockitoExtension.class)
class CharacterPlazaServiceImplTest {

    @Mock
    private AiCharacterMapper aiCharacterMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private CharacterPlazaServiceImpl service;

    private AiCharacter character(Long id, String name, Integer useCount, String category) {
        AiCharacter c = new AiCharacter();
        c.setId(id);
        c.setName(name);
        c.setIsShared(true);
        c.setUseCount(useCount);
        c.setCategory(category);
        c.setUserId(100L);
        return c;
    }

    // ============ listPublicCharacters ============

    @Test
    void listPublicCharacters_应返回分页结构_并查询总数与列表() {
        when(aiCharacterMapper.selectCount(any())).thenReturn(25L);
        when(aiCharacterMapper.selectList(any())).thenReturn(
                Arrays.asList(character(1L, "A", 10, "治愈")));

        Map<String, Object> result = service.listPublicCharacters(2, 10, null, "hot");

        assertEquals(25L, result.get("total"));
        assertEquals(1, ((List<?>) result.get("list")).size());
        assertEquals(2, result.get("page"));
        assertEquals(10, result.get("size"));
        verify(aiCharacterMapper).selectCount(any());
        verify(aiCharacterMapper).selectList(any());
    }

    @Test
    void listPublicCharacters_分类与排序参数不应抛错() {
        when(aiCharacterMapper.selectCount(any())).thenReturn(0L);
        when(aiCharacterMapper.selectList(any())).thenReturn(List.of());

        assertNotNull(service.listPublicCharacters(1, 12, "治愈", "rating"));
        assertNotNull(service.listPublicCharacters(1, 12, null, "created"));
        assertNotNull(service.listPublicCharacters(1, 12, null, "hot"));
    }

    @Test
    void listPublicCharacters_页码过小应收敛为第1页() {
        when(aiCharacterMapper.selectCount(any())).thenReturn(0L);
        when(aiCharacterMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> result = service.listPublicCharacters(0, 12, null, "hot");
        assertEquals(1, result.get("page"));
    }

    // ============ searchPublicCharacters ============

    @Test
    void searchPublicCharacters_应返回结果与关键词回显() {
        when(aiCharacterMapper.selectCount(any())).thenReturn(1L);
        when(aiCharacterMapper.selectList(any())).thenReturn(
                Arrays.asList(character(2L, "小助手", 3, null)));

        Map<String, Object> result = service.searchPublicCharacters("助手", 1, 12);

        assertEquals(1L, result.get("total"));
        assertEquals("助手", result.get("keyword"));
        assertEquals(1, ((List<?>) result.get("list")).size());
    }

    @Test
    void searchPublicCharacters_空关键词应回退空串不抛错() {
        when(aiCharacterMapper.selectCount(any())).thenReturn(0L);
        when(aiCharacterMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> result = service.searchPublicCharacters("  ", 1, 12);
        assertEquals("", result.get("keyword"));
    }

    // ============ getCharacterDetail ============

    @Test
    void getCharacterDetail_非所有者应isOwnerFalse() {
        when(aiCharacterMapper.selectById(1L)).thenReturn(character(1L, "A", 5, "治愈"));

        Map<String, Object> detail = service.getCharacterDetail(1L, 999L);

        assertEquals(Boolean.TRUE, detail.get("found"));
        assertEquals(5, detail.get("useCount"));
        assertEquals(Boolean.FALSE, detail.get("isOwner"));
    }

    @Test
    void getCharacterDetail_所有者应isOwnerTrue_匿名user为null不抛错() {
        when(aiCharacterMapper.selectById(1L)).thenReturn(character(1L, "A", 5, "治愈"));

        Map<String, Object> detail = service.getCharacterDetail(1L, 100L);
        assertEquals(Boolean.TRUE, detail.get("isOwner"));

        Map<String, Object> anon = service.getCharacterDetail(1L, null);
        assertEquals(Boolean.FALSE, anon.get("isOwner"));
    }

    @Test
    void getCharacterDetail_不存在应foundFalse() {
        when(aiCharacterMapper.selectById(404L)).thenReturn(null);
        Map<String, Object> detail = service.getCharacterDetail(404L, null);
        assertEquals(Boolean.FALSE, detail.get("found"));
    }

    // ============ getHotCharacters ============

    @Test
    void getHotCharacters_应返回热门列表并调用mapper() {
        when(aiCharacterMapper.selectList(any())).thenReturn(
                Arrays.asList(character(1L, "热1", 100, null), character(2L, "热2", 50, null)));

        List<AiCharacter> hot = service.getHotCharacters(5);

        assertEquals(2, hot.size());
        verify(aiCharacterMapper).selectList(any());
    }

    // ============ getCategories ============

    @Test
    void getCategories_应去重并保持顺序() {
        AiCharacter c1 = new AiCharacter(); c1.setCategory("治愈");
        AiCharacter c2 = new AiCharacter(); c2.setCategory("知识");
        AiCharacter c3 = new AiCharacter(); c3.setCategory("治愈");
        when(aiCharacterMapper.selectList(any())).thenReturn(Arrays.asList(c1, c2, c3));

        List<String> categories = service.getCategories();

        assertEquals(2, categories.size());
        assertTrue(categories.contains("治愈"));
        assertTrue(categories.contains("知识"));
    }

    // ============ incrementUseCount ============

    @Test
    void incrementUseCount_应在原值基础上加一() {
        AiCharacter existing = character(1L, "A", 7, null);
        when(aiCharacterMapper.selectById(1L)).thenReturn(existing);
        when(aiCharacterMapper.updateById(any(AiCharacter.class))).thenReturn(1);

        boolean ok = service.incrementUseCount(1L);

        assertTrue(ok);
        verify(aiCharacterMapper).updateById(argThat((AiCharacter p) -> p.getUseCount() == 8));
    }

    @Test
    void incrementUseCount_角色不存在应返回false() {
        when(aiCharacterMapper.selectById(404L)).thenReturn(null);
        assertFalse(service.incrementUseCount(404L));
        verify(aiCharacterMapper, never()).updateById(any(AiCharacter.class));
    }

    // ============ 广场分享 / 取消分享（归属校验）============

    @Test
    void shareToPlaza_创建者本人_应成功并置isSharedTrue() {
        AiCharacter mine = character(1L, "我的角色", 3, "治愈");
        mine.setUserId(100L);
        when(aiCharacterMapper.selectById(1L)).thenReturn(mine);
        when(aiCharacterMapper.updateById(any(AiCharacter.class))).thenReturn(1);

        assertTrue(service.shareToPlaza(1L, 100L));
        verify(aiCharacterMapper).updateById(argThat((AiCharacter p) ->
                p.getId().equals(1L) && Boolean.TRUE.equals(p.getIsShared())));
    }

    @Test
    void shareToPlaza_他人角色_应抛业务异常403() {
        AiCharacter ownedByOther = character(2L, "别人的角色", 3, null);
        ownedByOther.setUserId(999L);
        when(aiCharacterMapper.selectById(2L)).thenReturn(ownedByOther);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.shareToPlaza(2L, 100L));
        assertEquals(403, ex.getErrorCode().getHttpStatus());
        verify(aiCharacterMapper, never()).updateById(any(AiCharacter.class));
    }

    @Test
    void unshareFromPlaza_创建者本人_应成功并置isSharedFalse() {
        AiCharacter mine = character(3L, "我的角色", 5, null);
        mine.setUserId(100L);
        when(aiCharacterMapper.selectById(3L)).thenReturn(mine);
        when(aiCharacterMapper.updateById(any(AiCharacter.class))).thenReturn(1);

        assertTrue(service.unshareFromPlaza(3L, 100L));
        verify(aiCharacterMapper).updateById(argThat((AiCharacter p) ->
                p.getId().equals(3L) && Boolean.FALSE.equals(p.getIsShared())));
    }

    @Test
    void shareToPlaza_角色不存在_应抛业务异常404() {
        when(aiCharacterMapper.selectById(404L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.shareToPlaza(404L, 100L));
        assertEquals(404, ex.getErrorCode().getHttpStatus());
    }

    // ============ 排序 ============

    @Test
    void list_sortBy_hot_应按useCount降序() {
        when(aiCharacterMapper.selectCount(any())).thenReturn(0L);
        when(aiCharacterMapper.selectList(any())).thenReturn(List.of());

        service.listPublicCharacters(1, 12, null, "hot");

        ArgumentCaptor<Wrapper<AiCharacter>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(aiCharacterMapper).selectCount(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("use_count"));
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    void list_sortBy_created_应按id降序() {
        when(aiCharacterMapper.selectCount(any())).thenReturn(0L);
        when(aiCharacterMapper.selectList(any())).thenReturn(List.of());

        service.listPublicCharacters(1, 12, null, "created");

        ArgumentCaptor<Wrapper<AiCharacter>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(aiCharacterMapper).selectCount(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("ORDER BY"));
        assertTrue(sql.contains("id"));
    }

    @Test
    void list_sortBy_rating_应按rating降序() {
        when(aiCharacterMapper.selectCount(any())).thenReturn(0L);
        when(aiCharacterMapper.selectList(any())).thenReturn(List.of());

        service.listPublicCharacters(1, 12, null, "rating");

        ArgumentCaptor<Wrapper<AiCharacter>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(aiCharacterMapper).selectCount(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("rating"));
        assertTrue(sql.contains("ORDER BY"));
    }

    // ============ 分页 hasMore ============

    @Test
    void list_总数大于当前页_应hasMoreTrue() {
        when(aiCharacterMapper.selectCount(any())).thenReturn(25L);
        when(aiCharacterMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> result = service.listPublicCharacters(1, 12, null, "hot");
        assertEquals(Boolean.TRUE, result.get("hasMore"));
    }

    @Test
    void list_总数已覆盖_应hasMoreFalse() {
        when(aiCharacterMapper.selectCount(any())).thenReturn(12L);
        when(aiCharacterMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> result = service.listPublicCharacters(1, 12, null, "hot");
        assertEquals(Boolean.FALSE, result.get("hasMore"));
    }

    // ============ 详情增强（创建者昵称）============

    @Test
    void detail_应返回创建者昵称_优先nickname() {
        AiCharacter c = character(1L, "A", 5, "治愈");
        c.setUserId(100L);
        User creator = new User();
        creator.setId(100L);
        creator.setUsername("zhangsan");
        creator.setNickname("张三");
        when(aiCharacterMapper.selectById(1L)).thenReturn(c);
        when(userMapper.selectById(100L)).thenReturn(creator);

        Map<String, Object> detail = service.getCharacterDetail(1L, null);
        assertEquals("张三", detail.get("creatorNickname"));
    }

    @Test
    void detail_创建者无昵称_应回退username() {
        AiCharacter c = character(2L, "B", 5, null);
        c.setUserId(200L);
        User creator = new User();
        creator.setId(200L);
        creator.setUsername("lisi");
        when(aiCharacterMapper.selectById(2L)).thenReturn(c);
        when(userMapper.selectById(200L)).thenReturn(creator);

        Map<String, Object> detail = service.getCharacterDetail(2L, null);
        assertEquals("lisi", detail.get("creatorNickname"));
    }

    @Test
    void detail_创建者不存在_应返回匿名用户() {
        AiCharacter c = character(3L, "C", 5, null);
        c.setUserId(300L);
        when(aiCharacterMapper.selectById(3L)).thenReturn(c);
        when(userMapper.selectById(300L)).thenReturn(null);

        Map<String, Object> detail = service.getCharacterDetail(3L, null);
        assertEquals("匿名用户", detail.get("creatorNickname"));
    }
}

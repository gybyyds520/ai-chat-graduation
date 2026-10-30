package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.entity.Announcement;
import com.example.aichat.mapper.AnnouncementMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AnnouncementService 单元测试（Mockito 隔离测试）。
 *
 * 覆盖点：
 *   1. createAnnouncement - 正常创建公告
 *   2. createAnnouncement - 可选字段设置
 *   3. updateAnnouncement - 正常更新
 *   4. updateAnnouncement - 公告不存在返回 null
 *   5. deleteAnnouncement - 正常删除
 *   6. deleteAnnouncement - 删除不存在返回 false
 *   7. getAnnouncementById - 查询存在/不存在
 *   8. getAnnouncementPage - 分页查询
 *   9. getAllAnnouncements - 查询全部
 *  10. getActiveAnnouncements - 查询已发布
 *  11. publishAnnouncement - 发布公告
 *  12. unpublishAnnouncement - 取消发布
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementMapper announcementMapper;

    @InjectMocks
    private AnnouncementServiceImpl announcementService;

    private Announcement sampleAnnouncement;

    @BeforeEach
    void setUp() {
        sampleAnnouncement = new Announcement();
        sampleAnnouncement.setId(1L);
        sampleAnnouncement.setTitle("系统维护通知");
        sampleAnnouncement.setContent("本周六凌晨2-4点系统维护");
        sampleAnnouncement.setStatus("DRAFT");
        sampleAnnouncement.setPriority(1);
    }

    @Test
    void createAnnouncement_正常创建应返回公告对象() {
        Map<String, Object> request = new HashMap<>();
        request.put("title", "系统维护通知");
        request.put("content", "本周六凌晨2-4点系统维护");
        request.put("priority", 5);

        Announcement result = announcementService.createAnnouncement(request);

        assertNotNull(result);
        assertEquals("系统维护通知", result.getTitle());
        assertEquals("本周六凌晨2-4点系统维护", result.getContent());
        assertEquals("DRAFT", result.getStatus());
        assertEquals(5, result.getPriority());
        assertEquals(0, result.getViewCount());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        verify(announcementMapper).insert(any(Announcement.class));
    }

    @Test
    void createAnnouncement_可选字段应正确设置() {
        Map<String, Object> request = new HashMap<>();
        request.put("title", "测试公告");
        request.put("content", "测试内容");
        request.put("publishTime", "2026-09-14T10:00:00");
        request.put("displayScope", "ALL");
        request.put("author", "admin");
        request.put("createdBy", 1L);

        Announcement result = announcementService.createAnnouncement(request);

        assertNotNull(result.getPublishTime());
        assertEquals("ALL", result.getDisplayScope());
        assertEquals("admin", result.getAuthor());
        assertEquals(1L, result.getCreatedBy());
    }

    @Test
    void updateAnnouncement_正常更新应返回更新后对象() {
        when(announcementMapper.selectById(1L)).thenReturn(sampleAnnouncement);

        Map<String, Object> request = new HashMap<>();
        request.put("title", "更新后的标题");
        request.put("content", "更新后的内容");

        Announcement result = announcementService.updateAnnouncement(1L, request);

        assertNotNull(result);
        assertEquals("更新后的标题", result.getTitle());
        assertEquals("更新后的内容", result.getContent());
        verify(announcementMapper).updateById(any(Announcement.class));
    }

    @Test
    void updateAnnouncement_公告不存在应返回null() {
        when(announcementMapper.selectById(999L)).thenReturn(null);

        Map<String, Object> request = new HashMap<>();
        request.put("title", "新标题");

        Announcement result = announcementService.updateAnnouncement(999L, request);

        assertNull(result);
        verify(announcementMapper, never()).updateById(any(Announcement.class));
    }

    @Test
    void deleteAnnouncement_存在的公告应返回true() {
        when(announcementMapper.deleteById(1L)).thenReturn(1);

        boolean result = announcementService.deleteAnnouncement(1L);

        assertTrue(result);
    }

    @Test
    void deleteAnnouncement_不存在的公告应返回false() {
        when(announcementMapper.deleteById(999L)).thenReturn(0);

        boolean result = announcementService.deleteAnnouncement(999L);

        assertFalse(result);
    }

    @Test
    void getAnnouncementById_存在应返回对象() {
        when(announcementMapper.selectById(1L)).thenReturn(sampleAnnouncement);

        Announcement result = announcementService.getAnnouncementById(1L);

        assertNotNull(result);
        assertEquals("系统维护通知", result.getTitle());
    }

    @Test
    void getAnnouncementById_不存在应返回null() {
        when(announcementMapper.selectById(999L)).thenReturn(null);

        Announcement result = announcementService.getAnnouncementById(999L);

        assertNull(result);
    }

    @Test
    void getAnnouncementPage_应返回分页结果() {
        Page<Announcement> page = new Page<>(1, 10);
        page.setRecords(List.of(sampleAnnouncement));
        page.setTotal(1);

        when(announcementMapper.selectPage(any(Page.class), any())).thenReturn(page);

        IPage<Announcement> result = announcementService.getAnnouncementPage(1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void getAllAnnouncements_应返回列表() {
        List<Announcement> announcements = List.of(sampleAnnouncement);
        when(announcementMapper.selectList(any())).thenReturn(announcements);

        List<Announcement> result = announcementService.getAllAnnouncements();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getActiveAnnouncements_应返回已发布公告() {
        List<Announcement> activeList = List.of(sampleAnnouncement);
        when(announcementMapper.selectList(any())).thenReturn(activeList);

        List<Announcement> result = announcementService.getActiveAnnouncements();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void publishAnnouncement_正常发布应返回发布后对象() {
        sampleAnnouncement.setStatus("DRAFT");
        when(announcementMapper.selectById(1L)).thenReturn(sampleAnnouncement);

        Announcement result = announcementService.publishAnnouncement(1L);

        assertNotNull(result);
        assertEquals("PUBLISHED", result.getStatus());
        assertNotNull(result.getPublishTime());
        assertNotNull(result.getStartTime());
        verify(announcementMapper).updateById(any(Announcement.class));
    }

    @Test
    void publishAnnouncement_公告不存在应返回null() {
        when(announcementMapper.selectById(999L)).thenReturn(null);

        Announcement result = announcementService.publishAnnouncement(999L);

        assertNull(result);
        verify(announcementMapper, never()).updateById(any(Announcement.class));
    }

    @Test
    void unpublishAnnouncement_正常取消发布应返回草稿对象() {
        sampleAnnouncement.setStatus("PUBLISHED");
        when(announcementMapper.selectById(1L)).thenReturn(sampleAnnouncement);

        Announcement result = announcementService.unpublishAnnouncement(1L);

        assertNotNull(result);
        assertEquals("DRAFT", result.getStatus());
        verify(announcementMapper).updateById(any(Announcement.class));
    }
}

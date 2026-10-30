package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.entity.Notification;
import com.example.aichat.mapper.NotificationMapper;
import com.example.aichat.websocket.ChatWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * NotificationService 单元测试（Mockito 隔离）。
 *
 * 覆盖点：
 *   1. createNotification：落库并标记初始状态；
 *   2. listNotifications：分页查询、类型筛选、isDeleted 过滤、排序、hasMore；
 *   3. countUnread：委托 mapper 统计未读数；
 *   4. markAllAsRead：委托 mapper 全部已读；
 *   5. markAsRead：通知不存在时不操作、不抛异常；
 *   6. deleteNotification：通知不存在时不操作；
 *   7. clearAllNotifications：批量软删除，返回删除条数。
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private ChatWebSocketHandler chatWebSocketHandler;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification sampleNotification(Long id, Long userId, boolean read) {
        Notification n = new Notification();
        n.setId(id);
        n.setUserId(userId);
        n.setType("comment");
        n.setMessage("新评论");
        n.setIsRead(read);
        n.setIsDeleted(false);
        return n;
    }

    @Test
    void createNotification_应落库并标记初始状态() {
        Notification result = notificationService.createNotification(
                1L, "comment", "post", 100L, 2L, "Bob", "新评论");

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertFalse(result.getIsRead());
        assertFalse(result.getIsDeleted());
        assertNotNull(result.getCreatedAt());
        verify(notificationMapper).insert(any(Notification.class));
    }

    // ---------- listNotifications 分页 ----------

    @Test
    @SuppressWarnings("unchecked")
    void listNotifications_分页_返回正确records和total() {
        Page<Notification> page = new Page<>(1, 10);
        page.setRecords(List.of(sampleNotification(1L, 1L, false)));
        page.setTotal(25);
        when(notificationMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

        IPage<Notification> result = notificationService.listNotifications(1L, 1, 10, null);

        assertEquals(1, result.getRecords().size());
        assertEquals(25, result.getTotal());
        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());
        assertEquals(3, result.getPages()); // 25/10 = 3 页
    }

    @Test
    @SuppressWarnings("unchecked")
    void listNotifications_type筛选_wrapper含type条件() {
        when(notificationMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(new Page<>());

        notificationService.listNotifications(1L, 1, 20, "like");

        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(notificationMapper).selectPage(any(Page.class), captor.capture());
        String sql = captor.getValue().getExpression().getNormal().getSqlSegment();
        assertTrue(sql.contains("user_id"));
        assertTrue(sql.contains("is_deleted"));
        assertTrue(sql.contains("type"), "type 筛选应出现在 WHERE 条件中: " + sql);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listNotifications_type为all或不传_不加type条件() {
        when(notificationMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(new Page<>());

        notificationService.listNotifications(1L, 1, 20, "all");

        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(notificationMapper).selectPage(any(Page.class), captor.capture());
        String sql = captor.getValue().getExpression().getNormal().getSqlSegment();
        assertFalse(sql.contains("type"), "type=all 不应加 type 过滤: " + sql);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listNotifications_isDeleted过滤_wrapper含isDeleted条件() {
        when(notificationMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(new Page<>());

        notificationService.listNotifications(1L, 1, 20, null);

        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(notificationMapper).selectPage(any(Page.class), captor.capture());
        String sql = captor.getValue().getExpression().getNormal().getSqlSegment();
        assertTrue(sql.contains("is_deleted"), "应过滤已删除通知: " + sql);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listNotifications_按createdAt降序排列() {
        when(notificationMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(new Page<>());

        notificationService.listNotifications(1L, 1, 20, null);

        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(notificationMapper).selectPage(any(Page.class), captor.capture());
        String orderBySql = captor.getValue().getExpression().getOrderBy().getSqlSegment();
        assertTrue(orderBySql.contains("created_at"), "应按 created_at 排序: " + orderBySql);
        assertTrue(orderBySql.contains("DESC"), "应降序: " + orderBySql);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listNotifications_hasMore_第一页有更多_最后一页无更多() {
        // 第一页：total=25, size=10, pages=3, current=1 → hasMore=true
        Page<Notification> page1 = new Page<>(1, 10);
        page1.setRecords(List.of(sampleNotification(1L, 1L, false)));
        page1.setTotal(25);
        when(notificationMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page1);

        IPage<Notification> result1 = notificationService.listNotifications(1L, 1, 10, null);
        assertTrue(result1.getCurrent() < result1.getPages(), "第1页应还有更多");

        // 最后一页：current=3, pages=3 → hasMore=false
        Page<Notification> page3 = new Page<>(3, 10);
        page3.setRecords(List.of(sampleNotification(25L, 1L, false)));
        page3.setTotal(25);
        when(notificationMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page3);

        IPage<Notification> result3 = notificationService.listNotifications(1L, 3, 10, null);
        assertFalse(result3.getCurrent() < result3.getPages(), "最后一页应无更多");
    }

    // ---------- countUnread ----------

    @Test
    void countUnread_应委托mapper返回未读数() {
        when(notificationMapper.countUnread(1L)).thenReturn(3);

        int count = notificationService.countUnread(1L);

        assertEquals(3, count);
    }

    // ---------- markAllAsRead ----------

    @Test
    void markAllAsRead_应委托mapper() {
        notificationService.markAllAsRead(1L);

        verify(notificationMapper).markAllAsRead(1L);
    }

    // ---------- markAsRead ----------

    @Test
    void markAsRead_通知不存在应静默不操作() {
        when(notificationMapper.selectById(999L)).thenReturn(null);

        assertDoesNotThrow(() -> notificationService.markAsRead(999L));
        verify(notificationMapper, never()).markAsRead(anyLong());
    }

    // ---------- deleteNotification ----------

    @Test
    void deleteNotification_通知不存在应静默不操作() {
        when(notificationMapper.selectById(999L)).thenReturn(null);

        assertDoesNotThrow(() -> notificationService.deleteNotification(999L));
        verify(notificationMapper, never()).updateById(any(Notification.class));
    }

    // ---------- clearAllNotifications ----------

    @Test
    void clearAllNotifications_应返回删除条数() {
        when(notificationMapper.clearAllByUserId(1L)).thenReturn(7);

        int deleted = notificationService.clearAllNotifications(1L);

        assertEquals(7, deleted);
        verify(notificationMapper).clearAllByUserId(1L);
    }

    @Test
    void clearAllNotifications_空用户应返回0() {
        when(notificationMapper.clearAllByUserId(999L)).thenReturn(0);

        int deleted = notificationService.clearAllNotifications(999L);

        assertEquals(0, deleted);
    }
}

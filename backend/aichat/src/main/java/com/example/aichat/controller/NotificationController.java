package com.example.aichat.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.aichat.dto.R;
import com.example.aichat.entity.Notification;
import com.example.aichat.entity.NotificationSetting;
import com.example.aichat.service.NotificationService;
import com.example.aichat.service.NotificationSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationSettingService notificationSettingService;

    @GetMapping("/list")
    public R getNotifications(@RequestParam Long userId,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "20") int size,
                              @RequestParam(required = false) String type) {
        // 安全修复（P0）：通知仅本人可读
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        IPage<Notification> result = notificationService.listNotifications(current, page, size, type);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());
        long currentPage = result.getCurrent();
        long totalPages = result.getPages();
        data.put("hasMore", currentPage < totalPages);
        return R.success(data);
    }

    @GetMapping("/unread/count")
    public R countUnread(@RequestParam Long userId) {
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        int count = notificationService.countUnread(current);
        return R.success(count);
    }

    @PostMapping("/read/{notificationId}")
    public R markAsRead(@PathVariable Long notificationId) {
        // 所有权校验在 NotificationServiceImpl 内完成
        notificationService.markAsRead(notificationId);
        return R.success("已标记为已读");
    }

    @PostMapping("/read/all")
    public R markAllAsRead(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        notificationService.markAllAsRead(current);
        return R.success("全部已标记为已读");
    }

    @DeleteMapping("/{notificationId}")
    public R deleteNotification(@PathVariable Long notificationId) {
        // 所有权校验在 NotificationServiceImpl 内完成
        notificationService.deleteNotification(notificationId);
        return R.success("删除成功");
    }

    @PostMapping("/clear-all")
    public R clearAll(@RequestParam Long userId) {
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        int deletedCount = notificationService.clearAllNotifications(current);
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("deletedCount", deletedCount);
        return R.success(data);
    }

    @GetMapping("/settings")
    public R getSettings(@RequestParam Long userId) {
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        NotificationSetting settings = notificationSettingService.getSettings(current);
        Map<String, Object> data = new HashMap<>();
        data.put("likeEnabled", settings.getLikeEnabled());
        data.put("commentEnabled", settings.getCommentEnabled());
        data.put("shareEnabled", settings.getShareEnabled());
        data.put("systemEnabled", settings.getSystemEnabled());
        return R.success(data);
    }

    @PutMapping("/settings")
    public R updateSettings(@RequestParam Long userId, @RequestBody Map<String, Object> request) {
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        Boolean likeEnabled = toBool(request.get("likeEnabled"));
        Boolean commentEnabled = toBool(request.get("commentEnabled"));
        Boolean shareEnabled = toBool(request.get("shareEnabled"));
        Boolean systemEnabled = toBool(request.get("systemEnabled"));
        NotificationSetting updated = notificationSettingService.updateSettings(
                current, likeEnabled, commentEnabled, shareEnabled, systemEnabled);
        Map<String, Object> data = new HashMap<>();
        data.put("likeEnabled", updated.getLikeEnabled());
        data.put("commentEnabled", updated.getCommentEnabled());
        data.put("shareEnabled", updated.getShareEnabled());
        data.put("systemEnabled", updated.getSystemEnabled());
        return R.success(data);
    }

    private Boolean toBool(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }
}

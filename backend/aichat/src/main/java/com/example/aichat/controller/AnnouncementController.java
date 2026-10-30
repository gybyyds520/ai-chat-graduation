package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.Announcement;
import com.example.aichat.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @GetMapping("/public/active")
    @org.springframework.security.access.prepost.PreAuthorize("permitAll")
    public R getPublicActiveAnnouncements() {
        List<Announcement> announcements = announcementService.getActiveAnnouncements();
        return R.success(announcements);
    }

    @GetMapping("/public/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("permitAll")
    public R getPublicAnnouncementById(@PathVariable Long id) {
        Announcement announcement = announcementService.getAnnouncementById(id);
        if (announcement == null) {
            return R.error("公告不存在");
        }
        return R.success(announcement);
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public R createAnnouncement(@RequestBody Map<String, Object> request) {
        String title = (String) request.get("title");
        String content = (String) request.get("content");
        
        if (title == null || title.trim().isEmpty()) {
            return R.badRequest("公告标题不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            return R.badRequest("公告内容不能为空");
        }
        
        Announcement announcement = announcementService.createAnnouncement(request);
        return R.success(announcement);
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public R updateAnnouncement(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Announcement announcement = announcementService.updateAnnouncement(id, request);
        if (announcement == null) {
            return R.error("公告不存在");
        }
        return R.success(announcement);
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public R deleteAnnouncement(@PathVariable Long id) {
        boolean success = announcementService.deleteAnnouncement(id);
        if (success) {
            return R.success("删除成功");
        }
        return R.error("删除失败");
    }

    @GetMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public R getAnnouncementById(@PathVariable Long id) {
        Announcement announcement = announcementService.getAnnouncementById(id);
        if (announcement == null) {
            return R.error("公告不存在");
        }
        return R.success(announcement);
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public R getAnnouncements(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "20") Integer size) {
        return R.success(announcementService.getAnnouncementPage(page, size));
    }

    @GetMapping("/list")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public R getAllAnnouncements() {
        List<Announcement> announcements = announcementService.getAllAnnouncements();
        return R.success(announcements);
    }

    @GetMapping("/active")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public R getActiveAnnouncements() {
        List<Announcement> announcements = announcementService.getActiveAnnouncements();
        return R.success(announcements);
    }

    @PostMapping("/{id}/publish")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public R publishAnnouncement(@PathVariable Long id) {
        Announcement announcement = announcementService.publishAnnouncement(id);
        if (announcement == null) {
            return R.error("公告不存在");
        }
        return R.success(announcement);
    }

    @PostMapping("/{id}/unpublish")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public R unpublishAnnouncement(@PathVariable Long id) {
        Announcement announcement = announcementService.unpublishAnnouncement(id);
        if (announcement == null) {
            return R.error("公告不存在");
        }
        return R.success(announcement);
    }
}
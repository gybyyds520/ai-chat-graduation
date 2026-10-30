package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.entity.Announcement;
import com.example.aichat.mapper.AnnouncementMapper;
import com.example.aichat.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Override
    public Announcement createAnnouncement(Map<String, Object> request) {
        Announcement announcement = new Announcement();
        
        announcement.setTitle((String) request.get("title"));
        announcement.setContent((String) request.get("content"));
        announcement.setStatus("DRAFT");
        announcement.setPriority(request.get("priority") != null ? ((Number) request.get("priority")).intValue() : 0);
        
        if (request.get("publishTime") != null) {
            announcement.setPublishTime(LocalDateTime.parse((String) request.get("publishTime")));
        }
        if (request.get("startTime") != null) {
            announcement.setStartTime(LocalDateTime.parse((String) request.get("startTime")));
        }
        if (request.get("endTime") != null) {
            announcement.setEndTime(LocalDateTime.parse((String) request.get("endTime")));
        }
        
        announcement.setDisplayScope((String) request.get("displayScope"));
        announcement.setAuthor((String) request.get("author"));
        announcement.setCreatedBy(request.get("createdBy") != null ? ((Number) request.get("createdBy")).longValue() : null);
        announcement.setViewCount(0);
        announcement.setCreatedAt(LocalDateTime.now());
        announcement.setUpdatedAt(LocalDateTime.now());
        
        announcementMapper.insert(announcement);
        return announcement;
    }

    @Override
    public Announcement updateAnnouncement(Long id, Map<String, Object> request) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            return null;
        }
        
        if (request.containsKey("title")) {
            announcement.setTitle((String) request.get("title"));
        }
        if (request.containsKey("content")) {
            announcement.setContent((String) request.get("content"));
        }
        if (request.containsKey("priority")) {
            announcement.setPriority(((Number) request.get("priority")).intValue());
        }
        if (request.containsKey("publishTime")) {
            announcement.setPublishTime(LocalDateTime.parse((String) request.get("publishTime")));
        }
        if (request.containsKey("startTime")) {
            announcement.setStartTime(LocalDateTime.parse((String) request.get("startTime")));
        }
        if (request.containsKey("endTime")) {
            announcement.setEndTime(LocalDateTime.parse((String) request.get("endTime")));
        }
        if (request.containsKey("displayScope")) {
            announcement.setDisplayScope((String) request.get("displayScope"));
        }
        if (request.containsKey("author")) {
            announcement.setAuthor((String) request.get("author"));
        }
        
        announcement.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(announcement);
        return announcement;
    }

    @Override
    public boolean deleteAnnouncement(Long id) {
        return announcementMapper.deleteById(id) > 0;
    }

    @Override
    public Announcement getAnnouncementById(Long id) {
        return announcementMapper.selectById(id);
    }

    @Override
    public IPage<Announcement> getAnnouncementPage(Integer page, Integer size) {
        Page<Announcement> pageParam = new Page<>(page, size);
        QueryWrapper<Announcement> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("created_at");
        return announcementMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public List<Announcement> getAllAnnouncements() {
        QueryWrapper<Announcement> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("created_at");
        return announcementMapper.selectList(wrapper);
    }

    @Override
    public List<Announcement> getActiveAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        QueryWrapper<Announcement> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "PUBLISHED");
        // 正确的逻辑：(start_time <= now OR start_time IS NULL) AND (end_time >= now OR end_time IS NULL)
        wrapper.and(w -> w.le("start_time", now).or().isNull("start_time"));
        wrapper.and(w -> w.ge("end_time", now).or().isNull("end_time"));
        wrapper.orderByDesc("priority").orderByDesc("publish_time");
        return announcementMapper.selectList(wrapper);
    }

    @Override
    public Announcement publishAnnouncement(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            return null;
        }
        announcement.setStatus("PUBLISHED");
        if (announcement.getPublishTime() == null) {
            announcement.setPublishTime(LocalDateTime.now());
        }
        if (announcement.getStartTime() == null) {
            announcement.setStartTime(LocalDateTime.now());
        }
        announcement.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(announcement);
        return announcement;
    }

    @Override
    public Announcement unpublishAnnouncement(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            return null;
        }
        announcement.setStatus("DRAFT");
        announcement.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(announcement);
        return announcement;
    }
}
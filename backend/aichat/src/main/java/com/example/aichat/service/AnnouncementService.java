package com.example.aichat.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.aichat.entity.Announcement;

import java.util.List;
import java.util.Map;

public interface AnnouncementService {

    Announcement createAnnouncement(Map<String, Object> request);

    Announcement updateAnnouncement(Long id, Map<String, Object> request);

    boolean deleteAnnouncement(Long id);

    Announcement getAnnouncementById(Long id);

    IPage<Announcement> getAnnouncementPage(Integer page, Integer size);

    List<Announcement> getAllAnnouncements();

    List<Announcement> getActiveAnnouncements();

    Announcement publishAnnouncement(Long id);

    Announcement unpublishAnnouncement(Long id);
}
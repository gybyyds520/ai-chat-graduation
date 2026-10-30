package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户画像（简单版）。
 *
 * 阶段三 P2 引入：基于用户对话行为自动沉淀轻量画像，供对话风格个性化、
 * 话题推荐、活跃时段统计使用。一个用户一条记录（user_id 唯一）。
 *
 * 字段说明：
 *   preferredStyle   对话风格偏好（concise / detailed / funny / serious 等，可空）
 *   commonTopics     常见话题（JSON 数组字符串，如 ["学习","游戏"]）
 *   activeTimeSlots  活跃时段（JSON，如 {"morning":12,"afternoon":5}）
 */
@TableName("user_profile")
public class UserProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID（唯一） */
    private Long userId;

    /** 对话风格偏好 */
    private String preferredStyle;

    /** 常见话题，JSON 数组字符串 */
    private String commonTopics;

    /** 活跃时段统计，JSON 对象字符串 */
    private String activeTimeSlots;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPreferredStyle() {
        return preferredStyle;
    }

    public void setPreferredStyle(String preferredStyle) {
        this.preferredStyle = preferredStyle;
    }

    public String getCommonTopics() {
        return commonTopics;
    }

    public void setCommonTopics(String commonTopics) {
        this.commonTopics = commonTopics;
    }

    public String getActiveTimeSlots() {
        return activeTimeSlots;
    }

    public void setActiveTimeSlots(String activeTimeSlots) {
        this.activeTimeSlots = activeTimeSlots;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

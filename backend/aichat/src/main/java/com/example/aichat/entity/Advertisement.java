package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 广告实体类
 */
@Data
@TableName("advertisement")
public class Advertisement {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 广告标题
     */
    private String title;

    /**
     * 广告内容/描述
     */
    private String content;

    /**
     * 广告链接
     */
    private String linkUrl;

    /**
     * 广告图片URL
     */
    private String imageUrl;

    /**
     * 广告位置（如：首页banner、登录页、注册页等）
     */
    private String position;

    /**
     * 广告状态：ENABLED-启用, DISABLED-禁用
     */
    private String status;

    /**
     * 排序优先级（数字越小优先级越高）
     */
    private Integer priority;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 点击次数
     */
    private Integer clickCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 创建人
     */
    private Long createdBy;
}
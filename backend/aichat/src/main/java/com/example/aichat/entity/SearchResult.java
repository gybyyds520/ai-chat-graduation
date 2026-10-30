package com.example.aichat.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 网络搜索结果实体
 */
@Data
public class SearchResult {
    
    /**
     * 结果ID
     */
    private String id;
    
    /**
     * 搜索关键词
     */
    private String query;
    
    /**
     * 搜索标题
     */
    private String title;
    
    /**
     * 链接URL
     */
    private String url;
    
    /**
     * 摘要内容
     */
    private String snippet;
    
    /**
     * 来源网站
     */
    private String source;
    
    /**
     * 发布时间
     */
    private String publishTime;
    
    /**
     * 相关度评分 (0-1)
     */
    private Double relevanceScore;
    
    /**
     * 内容类型
     */
    private String contentType;
    
    /**
     * 标签列表
     */
    private List<String> tags;
    
    /**
     * 是否验证通过
     */
    private Boolean verified;
    
    /**
     * 验证时间
     */
    private LocalDateTime verifiedAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

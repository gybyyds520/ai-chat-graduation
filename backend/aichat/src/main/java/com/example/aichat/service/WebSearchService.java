package com.example.aichat.service;

import com.example.aichat.entity.SearchResult;

import java.util.List;
import java.util.Map;

/**
 * 网络搜索服务接口
 * 提供AI系统互联网资料查找功能
 */
public interface WebSearchService {
    
    /**
     * 执行网络搜索
     * 
     * @param query 用户查询问题
     * @param maxResults 最大结果数
     * @return 搜索结果列表
     */
    List<SearchResult> search(String query, int maxResults);
    
    /**
     * 执行搜索并验证结果
     * 
     * @param query 用户查询问题
     * @param maxResults 最大结果数
     * @param verifyEnabled 是否启用验证
     * @return 搜索结果列表
     */
    List<SearchResult> searchWithVerification(String query, int maxResults, boolean verifyEnabled);
    
    /**
     * 生成搜索关键词
     * 
     * @param userQuestion 用户问题
     * @return 搜索关键词列表
     */
    List<String> generateSearchKeywords(String userQuestion);
    
    /**
     * 验证搜索结果
     * 
     * @param results 待验证的结果列表
     * @return 验证后的结果列表
     */
    List<SearchResult> verifyResults(List<SearchResult> results);
    
    /**
     * 提取关键信息
     * 
     * @param results 搜索结果列表
     * @return 提取的关键信息
     */
    Map<String, Object> extractKeyInformation(List<SearchResult> results);
    
    /**
     * 获取搜索历史
     * 
     * @param limit 返回数量限制
     * @return 搜索历史记录
     */
    List<Map<String, Object>> getSearchHistory(int limit);
}

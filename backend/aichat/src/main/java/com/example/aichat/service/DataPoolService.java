package com.example.aichat.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.entity.User;
import com.example.aichat.entity.UserDataRecord;
import com.example.aichat.mapper.UserDataRecordMapper;
import com.example.aichat.mapper.UserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DataPoolService {

    private static final Logger logger = LoggerFactory.getLogger(DataPoolService.class);

    private final UserDataRecordMapper userDataRecordMapper;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final DataPoolConfigService configService;

    public DataPoolService(UserDataRecordMapper userDataRecordMapper, ObjectMapper objectMapper,
                           UserMapper userMapper, DataPoolConfigService configService) {
        this.userDataRecordMapper = userDataRecordMapper;
        this.objectMapper = objectMapper;
        this.userMapper = userMapper;
        this.configService = configService;
    }

    @Transactional
    @Async
    public void processAndSaveUserData(Long userId, List<Map<String, Object>> dataList) {
        logger.info("开始处理用户数据, userId: {}, 数据条数: {}", userId, dataList.size());

        // autoSave 开关改为从 DB 配置读取（默认开启），不再使用内存 static 变量
        if (!configService.isAutoSaveEnabled()) {
            logger.info("自动保存功能已关闭，跳过数据存储");
            return;
        }

        // 决策：移除 isFreeUser 限制，所有用户数据都采集（含付费用户）。
        // 数据池目标是全量用户行为分析，付费用户数据价值更高，详见规则文档第 10 章。

        Map<String, List<Map<String, Object>>> groupedData = groupDataByContent(dataList);

        for (Map.Entry<String, List<Map<String, Object>>> entry : groupedData.entrySet()) {
            String groupId = entry.getKey();
            List<Map<String, Object>> groupItems = entry.getValue();

            for (Map<String, Object> dataItem : groupItems) {
                try {
                    UserDataRecord record = createRecordFromData(userId, dataItem, groupId);
                    
                    if (isDuplicate(record)) {
                        logger.debug("发现重复数据，跳过保存, userId: {}, dataType: {}", userId, record.getDataType());
                        continue;
                    }
                    
                    if (validateRecord(record)) {
                        record.setStatus("VALIDATED");
                        record.setIntegrityScore(calculateIntegrityScore(record));
                    } else {
                        record.setStatus("PENDING_VALIDATION");
                        record.setIntegrityScore(0.5);
                    }

                    record.setTags(extractTags(record));
                    
                    userDataRecordMapper.insert(record);
                } catch (Exception e) {
                    logger.error("保存数据记录失败", e);
                }
            }
        }

        logger.info("用户数据处理完成, userId: {}, 数据条数: {}", userId, dataList.size());
    }

    private UserDataRecord createRecordFromData(Long userId, Map<String, Object> dataItem, String groupId) {
        UserDataRecord record = new UserDataRecord();
        
        record.setUserId(userId);
        
        String dataType = (String) dataItem.get("dataType");
        record.setDataType(dataType != null ? dataType : "unknown");
        
        Object content = dataItem.get("content");
        try {
            record.setContent(content instanceof String ? (String) content : objectMapper.writeValueAsString(content));
        } catch (JsonProcessingException e) {
            record.setContent(content != null ? content.toString() : "");
        }
        
        Object metadata = dataItem.get("metadata");
        try {
            record.setMetadata(metadata != null ? objectMapper.writeValueAsString(metadata) : "{}");
        } catch (JsonProcessingException e) {
            record.setMetadata("{}");
        }
        
        String captureTimeStr = (String) dataItem.get("captureTime");
        if (captureTimeStr != null) {
            try {
                record.setCaptureTime(LocalDateTime.parse(captureTimeStr, DateTimeFormatter.ISO_DATE_TIME));
            } catch (Exception e) {
                record.setCaptureTime(LocalDateTime.now());
            }
        } else {
            record.setCaptureTime(LocalDateTime.now());
        }
        
        record.setGroupId(groupId);
        record.setBackupStatus("PENDING_BACKUP");
        
        return record;
    }

    private Map<String, List<Map<String, Object>>> groupDataByContent(List<Map<String, Object>> dataList) {
        Map<String, List<Map<String, Object>>> groups = new HashMap<>();
        
        for (Map<String, Object> dataItem : dataList) {
            String groupKey = generateGroupKey(dataItem);
            
            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(dataItem);
        }
        
        return groups;
    }

    private String generateGroupKey(Map<String, Object> dataItem) {
        String dataType = (String) dataItem.get("dataType");
        String captureTime = (String) dataItem.get("captureTime");
        
        if (captureTime != null && captureTime.length() >= 10) {
            return dataType + "_" + captureTime.substring(0, 10);
        }
        
        return dataType + "_" + LocalDateTime.now().toLocalDate();
    }

    private boolean validateRecord(UserDataRecord record) {
        if (record.getUserId() == null) return false;
        if (record.getDataType() == null || record.getDataType().isEmpty()) return false;
        if (record.getContent() == null || record.getContent().isEmpty()) return false;
        if (record.getCaptureTime() == null) return false;
        
        return true;
    }

    private Double calculateIntegrityScore(UserDataRecord record) {
        double score = 0.5;
        
        if (record.getContent() != null && record.getContent().length() > 10) {
            score += 0.2;
        }
        
        if (record.getMetadata() != null && !record.getMetadata().equals("{}")) {
            score += 0.15;
        }
        
        if (record.getTags() != null && !record.getTags().isEmpty()) {
            score += 0.15;
        }
        
        return Math.min(1.0, score);
    }

    private String extractTags(UserDataRecord record) {
        List<String> tags = new ArrayList<>();
        
        tags.add(record.getDataType());
        
        try {
            if (record.getMetadata() != null && !record.getMetadata().isEmpty()) {
                Map<String, Object> metadata = objectMapper.readValue(record.getMetadata(), Map.class);
                
                String actionType = (String) metadata.get("actionType");
                if (actionType != null) {
                    tags.add(actionType);
                }
                
                String formId = (String) metadata.get("formId");
                if (formId != null) {
                    tags.add("form:" + formId);
                }
                
                String editorId = (String) metadata.get("editorId");
                if (editorId != null) {
                    tags.add("editor:" + editorId);
                }
            }
        } catch (Exception e) {
            logger.warn("解析metadata失败", e);
        }
        
        String content = record.getContent();
        if (content != null && content.length() > 0) {
            Set<String> keywordTags = extractKeywordTags(content);
            tags.addAll(keywordTags);
        }
        
        return String.join(",", tags);
    }

    private Set<String> extractKeywordTags(String content) {
        Set<String> tags = new HashSet<>();
        
        Map<String, String> keywordMap = Map.of(
            "聊天", "chat",
            "对话", "conversation",
            "问题", "question",
            "回答", "answer",
            "搜索", "search",
            "登录", "login",
            "注册", "register",
            "设置", "settings",
            "角色", "character",
            "好友", "friend"
        );
        
        for (Map.Entry<String, String> entry : keywordMap.entrySet()) {
            if (content.contains(entry.getKey())) {
                tags.add(entry.getValue());
            }
        }
        
        Pattern urlPattern = Pattern.compile("https?://[^\\s]+");
        Matcher urlMatcher = urlPattern.matcher(content);
        if (urlMatcher.find()) {
            tags.add("url");
        }
        
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Matcher emailMatcher = emailPattern.matcher(content);
        if (emailMatcher.find()) {
            tags.add("email");
        }
        
        Pattern phonePattern = Pattern.compile("1[3-9]\\d{9}");
        Matcher phoneMatcher = phonePattern.matcher(content);
        if (phoneMatcher.find()) {
            tags.add("phone");
        }
        
        return tags;
    }

    /**
     * 分页查询用户数据记录。
     *
     * P2 修复：keyword 原在 Controller 层接收但未透传到 Service，搜索失效。
     * 现统一用 MyBatis-Plus QueryWrapper 组合 userId / dataType / keyword（content 或 tags 模糊匹配），
     * 避免新增 Mapper XML。keyword 为 null/空白时不过滤，等价于原行为。
     */
    public Map<String, Object> getUserDataRecords(Integer page, Integer size, Long userId, String dataType, String keyword) {
        Page<UserDataRecord> pageRequest = new Page<>(page, size);

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserDataRecord> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        if (dataType != null && !dataType.isBlank()) {
            wrapper.eq("data_type", dataType);
        }
        if (keyword != null && !keyword.isBlank()) {
            // 同时匹配内容与标签，提升管理端检索命中率
            wrapper.and(w -> w.like("content", keyword).or().like("tags", keyword));
        }
        wrapper.orderByDesc("capture_time");

        IPage<UserDataRecord> pageResult = userDataRecordMapper.selectPage(pageRequest, wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("current", pageResult.getCurrent());
        result.put("size", pageResult.getSize());

        return result;
    }

    public Map<String, Object> getDataStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalRecords", userDataRecordMapper.selectTotalCount());
        stats.put("backedUpRecords", userDataRecordMapper.selectBackedUpCount());
        stats.put("validatedRecords", userDataRecordMapper.selectValidatedCount());
        
        List<Map<String, Object>> typeStats = userDataRecordMapper.selectStatsByType();
        stats.put("byType", typeStats);
        
        List<Map<String, Object>> userStats = userDataRecordMapper.selectStatsByUser();
        stats.put("byUser", userStats);
        // 用户数据管理"用户数"统计 = 产生过数据记录的不同用户数
        stats.put("userCount", userStats != null ? userStats.size() : 0);
        
        return stats;
    }

    public List<String> getAllDataTypes() {
        return userDataRecordMapper.selectAllDataTypes();
    }

    public void deleteRecord(Long id) {
        userDataRecordMapper.deleteById(id);
    }

    public UserDataRecord getRecordById(Long id) {
        return userDataRecordMapper.selectById(id);
    }

    public UserDataRecord addRecord(UserDataRecord record) {
        if (record.getStatus() == null) record.setStatus("VALIDATED");
        if (record.getIntegrityScore() == null) record.setIntegrityScore(0.5);
        if (record.getCaptureTime() == null) record.setCaptureTime(LocalDateTime.now());
        if (record.getBackupStatus() == null) record.setBackupStatus("PENDING_BACKUP");
        if (record.getGroupId() == null) record.setGroupId("manual_" + System.currentTimeMillis());
        userDataRecordMapper.insert(record);
        return record;
    }

    public void updateRecord(UserDataRecord record) {
        userDataRecordMapper.updateById(record);
    }

    public List<UserDataRecord> exportAllRecords() {
        return userDataRecordMapper.selectList(null);
    }

    public void deleteBatchRecords(List<Long> ids) {
        userDataRecordMapper.deleteBatchIds(ids);
    }

    public Map<String, Object> analyzeUserData(Long userId, String dataType) {
        Map<String, Object> analysis = new HashMap<>();
        
        List<UserDataRecord> records;
        if (userId != null && dataType != null) {
            records = userDataRecordMapper.selectByUserIdAndType(userId, dataType);
        } else if (userId != null) {
            records = userDataRecordMapper.selectByUserId(userId);
        } else if (dataType != null) {
            records = userDataRecordMapper.selectByDataType(dataType);
        } else {
            records = userDataRecordMapper.selectList(null);
        }
        
        Map<String, Long> typeCount = new HashMap<>();
        Map<String, Long> tagCount = new HashMap<>();
        int totalContentLength = 0;
        double avgIntegrityScore = 0;
        
        for (UserDataRecord record : records) {
            typeCount.merge(record.getDataType(), 1L, Long::sum);
            
            if (record.getTags() != null) {
                for (String tag : record.getTags().split(",")) {
                    tagCount.merge(tag.trim(), 1L, Long::sum);
                }
            }
            
            if (record.getContent() != null) {
                totalContentLength += record.getContent().length();
            }
            
            if (record.getIntegrityScore() != null) {
                avgIntegrityScore += record.getIntegrityScore();
            }
        }
        
        analysis.put("totalRecords", records.size());
        analysis.put("byType", typeCount);
        analysis.put("byTag", tagCount);
        analysis.put("totalContentLength", totalContentLength);
        analysis.put("avgContentLength", records.size() > 0 ? totalContentLength / records.size() : 0);
        analysis.put("avgIntegrityScore", records.size() > 0 ? avgIntegrityScore / records.size() : 0);
        
        return analysis;
    }

    private boolean isFreeUser(Long userId) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                return false;
            }
            String versionType = user.getVersionType();
            return versionType == null || versionType.isEmpty() || "FREE".equals(versionType);
        } catch (Exception e) {
            logger.error("判断用户版本类型失败, userId: {}", userId, e);
            return false;
        }
    }

    public boolean isAutoSaveEnabled() {
        return configService.isAutoSaveEnabled();
    }

    public void setAutoSaveEnabled(boolean enabled) {
        configService.setAutoSaveEnabled(enabled);
        logger.info("自动保存状态已更新为: {}", enabled);
    }

    /** 管理端聚合读取数据池配置。 */
    public Map<String, Object> getDataPoolConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("autoSaveEnabled", configService.isAutoSaveEnabled());
        config.put("retentionDays", configService.getRetentionDays());
        config.put("maxRecordsPerUser", configService.getMaxRecordsPerUser());
        return config;
    }

    /** 管理端更新数据池配置（仅更新传入的非空字段）。 */
    public void updateDataPoolConfig(Boolean autoSaveEnabled, Integer retentionDays, Integer maxRecordsPerUser) {
        if (autoSaveEnabled != null) {
            configService.setAutoSaveEnabled(autoSaveEnabled);
        }
        if (retentionDays != null && retentionDays > 0) {
            configService.setRetentionDays(retentionDays);
        }
        if (maxRecordsPerUser != null && maxRecordsPerUser > 0) {
            configService.setMaxRecordsPerUser(maxRecordsPerUser);
        }
    }

    /**
     * 清理过期记录：删除 capture_time 早于（当前时间 - 保留天数）的数据。
     * 保留天数由 DB 配置 retentionDays 决定，默认 180 天。
     * 可由定时任务或管理端 stats/维护端点触发。
     *
     * @return 删除的记录数
     */
    public int cleanupExpiredRecords() {
        int retentionDays = configService.getRetentionDays();
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserDataRecord> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.lt("capture_time", threshold);
        int deleted = userDataRecordMapper.delete(wrapper);
        logger.info("清理过期数据完成, 保留天数={}, 阈值={}, 删除条数={}", retentionDays, threshold, deleted);
        return deleted;
    }

    public Map<String, Object> checkDuplicates() {
        List<Map<String, Object>> duplicates = userDataRecordMapper.selectDuplicateRecords();
        long totalDuplicates = duplicates.stream().mapToLong(m -> ((Number) m.get("count")).longValue() - 1).sum();
        
        Map<String, Object> result = new HashMap<>();
        result.put("duplicateGroups", duplicates.size());
        result.put("totalDuplicateRecords", totalDuplicates);
        result.put("duplicates", duplicates);
        
        return result;
    }

    @Transactional
    public Map<String, Object> removeDuplicates() {
        List<Map<String, Object>> duplicates = userDataRecordMapper.selectDuplicateRecords();
        int deletedCount = 0;
        
        for (Map<String, Object> duplicate : duplicates) {
            Long userId = ((Number) duplicate.get("user_id")).longValue();
            String dataType = (String) duplicate.get("data_type");
            String content = (String) duplicate.get("content");
            
            List<Long> idsToDelete = userDataRecordMapper.selectDuplicateIdsToDelete(userId, dataType, content);
            if (!idsToDelete.isEmpty()) {
                userDataRecordMapper.deleteBatchIds(idsToDelete);
                deletedCount += idsToDelete.size();
                logger.info("删除重复数据, userId: {}, dataType: {}, 删除条数: {}", userId, dataType, idsToDelete.size());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", deletedCount);
        result.put("processedGroups", duplicates.size());
        
        return result;
    }

    private boolean isDuplicate(UserDataRecord record) {
        UserDataRecord existing = userDataRecordMapper.selectByUniqueKey(
            record.getUserId(), 
            record.getDataType(), 
            record.getContent()
        );
        return existing != null;
    }

    public void autoRemoveDuplicates() {
        logger.info("开始自动去重");
        Map<String, Object> result = removeDuplicates();
        logger.info("自动去重完成, 删除重复记录数: {}, 处理组数: {}", 
            result.get("deletedCount"), result.get("processedGroups"));
    }
}
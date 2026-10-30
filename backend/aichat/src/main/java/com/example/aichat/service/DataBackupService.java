package com.example.aichat.service;

import com.example.aichat.entity.UserDataRecord;
import com.example.aichat.mapper.UserDataRecordMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DataBackupService {

    private static final Logger logger = LoggerFactory.getLogger(DataBackupService.class);

    private final UserDataRecordMapper userDataRecordMapper;
    private final ObjectMapper objectMapper;

    @Value("${app.backup.path:./backups}")
    private String backupPath;

    public DataBackupService(UserDataRecordMapper userDataRecordMapper) {
        this.userDataRecordMapper = userDataRecordMapper;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @PostConstruct
    public void init() {
        File backupDir = new File(backupPath);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
            logger.info("创建备份目录: {}", backupDir.getAbsolutePath());
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void performFullBackup() {
        logger.info("开始执行全量备份...");
        try {
            fullBackup();
            logger.info("全量备份完成");
        } catch (Exception e) {
            logger.error("全量备份失败", e);
        }
    }

    @Scheduled(cron = "0 */30 * * * ?")
    public void performIncrementalBackup() {
        logger.info("开始执行增量备份...");
        try {
            incrementalBackup();
            logger.info("增量备份完成");
        } catch (Exception e) {
            logger.error("增量备份失败", e);
        }
    }

    @Transactional
    public void fullBackup() throws IOException {
        List<UserDataRecord> records = userDataRecordMapper.selectList(null);
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("full_backup_%s.json", timestamp);
        Path filePath = Paths.get(backupPath, filename);

        Map<String, Object> backupData = new HashMap<>();
        backupData.put("backupType", "FULL");
        backupData.put("timestamp", LocalDateTime.now().toString());
        backupData.put("recordCount", records.size());
        backupData.put("records", records);

        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            objectMapper.writeValue(writer, backupData);
        }

        updateBackupStatus(records, "BACKED_UP");
        
        cleanupOldBackups(7);
    }

    @Transactional
    public void incrementalBackup() throws IOException {
        List<UserDataRecord> records = userDataRecordMapper.selectList(null);
        records.removeIf(r -> "BACKED_UP".equals(r.getBackupStatus()));

        if (records.isEmpty()) {
            logger.info("没有需要增量备份的数据");
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("incremental_backup_%s.json", timestamp);
        Path filePath = Paths.get(backupPath, filename);

        Map<String, Object> backupData = new HashMap<>();
        backupData.put("backupType", "INCREMENTAL");
        backupData.put("timestamp", LocalDateTime.now().toString());
        backupData.put("recordCount", records.size());
        backupData.put("records", records);

        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            objectMapper.writeValue(writer, backupData);
        }

        updateBackupStatus(records, "BACKED_UP");
    }

    private void updateBackupStatus(List<UserDataRecord> records, String status) {
        for (UserDataRecord record : records) {
            record.setBackupStatus(status);
            userDataRecordMapper.updateById(record);
        }
    }

    private void cleanupOldBackups(int retentionDays) {
        try {
            Path backupDir = Paths.get(backupPath);
            if (!Files.exists(backupDir)) return;

            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);

            Files.list(backupDir)
                .filter(path -> {
                    String filename = path.getFileName().toString();
                    if (!filename.endsWith(".json")) return false;
                    
                    try {
                        String timestampStr = filename.replaceAll("^(full|incremental)_backup_(\\d{8}_\\d{6})\\.json$", "$2");
                        if (!timestampStr.equals(filename)) {
                            LocalDateTime fileTime = LocalDateTime.parse(timestampStr, 
                                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                            return fileTime.isBefore(cutoffTime);
                        }
                    } catch (Exception e) {
                        logger.warn("无法解析备份文件时间戳: {}", filename);
                    }
                    return false;
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        logger.info("删除过期备份文件: {}", path);
                    } catch (IOException e) {
                        logger.error("删除备份文件失败: {}", path, e);
                    }
                });
        } catch (IOException e) {
            logger.error("清理旧备份失败", e);
        }
    }

    public List<String> listBackups() {
        List<String> backups = new ArrayList<>();
        try {
            Path backupDir = Paths.get(backupPath);
            if (!Files.exists(backupDir)) return backups;

            Files.list(backupDir)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    backups.add(path.getFileName().toString());
                });
        } catch (IOException e) {
            logger.error("获取备份列表失败", e);
        }
        return backups;
    }

    @Transactional
    public String restoreFromBackup(String filename) throws IOException {
        // 安全加固：防止路径穿越，仅允许纯文件名（不含路径分隔符）
        if (filename == null || filename.isBlank() || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return "无效的备份文件名";
        }

        Path filePath = Paths.get(backupPath, filename);

        // 安全加固：确认解析后的路径仍在备份目录内
        Path backupDirPath = Paths.get(backupPath).toAbsolutePath().normalize();
        Path resolvedPath = filePath.toAbsolutePath().normalize();
        if (!resolvedPath.startsWith(backupDirPath)) {
            return "无效的备份文件路径";
        }

        if (!Files.exists(filePath)) {
            return "备份文件不存在";
        }

        String content = Files.readString(filePath);
        Map<String, Object> backupData = objectMapper.readValue(content, Map.class);
        
        List<Map<String, Object>> records = (List<Map<String, Object>>) backupData.get("records");
        
        int restoredCount = 0;
        int skippedCount = 0;
        
        for (Map<String, Object> recordData : records) {
            try {
                Long originalId = recordData.containsKey("id") ? ((Number) recordData.get("id")).longValue() : null;
                
                UserDataRecord existing = originalId != null ? userDataRecordMapper.selectById(originalId) : null;
                
                if (existing != null) {
                    skippedCount++;
                    continue;
                }
                
                UserDataRecord newRecord = new UserDataRecord();
                newRecord.setUserId(((Number) recordData.get("userId")).longValue());
                newRecord.setDataType((String) recordData.get("dataType"));
                newRecord.setContent((String) recordData.get("content"));
                newRecord.setMetadata((String) recordData.get("metadata"));
                newRecord.setTags((String) recordData.get("tags"));
                newRecord.setIntegrityScore(recordData.get("integrityScore") != null ? 
                    ((Number) recordData.get("integrityScore")).doubleValue() : 0.5);
                newRecord.setStatus((String) recordData.get("status"));
                newRecord.setGroupId((String) recordData.get("groupId"));
                newRecord.setBackupStatus("BACKED_UP");
                
                if (recordData.get("captureTime") != null) {
                    try {
                        newRecord.setCaptureTime(LocalDateTime.parse((String) recordData.get("captureTime")));
                    } catch (Exception e) {
                        newRecord.setCaptureTime(LocalDateTime.now());
                    }
                } else {
                    newRecord.setCaptureTime(LocalDateTime.now());
                }
                
                userDataRecordMapper.insert(newRecord);
                restoredCount++;
            } catch (Exception e) {
                logger.error("恢复记录失败", e);
                skippedCount++;
            }
        }
        
        return String.format("恢复完成: 成功 %d 条, 跳过 %d 条", restoredCount, skippedCount);
    }

    public Map<String, Object> getBackupStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            Path backupDir = Paths.get(backupPath);
            if (!Files.exists(backupDir)) {
                stats.put("totalBackups", 0);
                stats.put("totalSize", 0);
                return stats;
            }

            long totalSize = 0;
            int totalBackups = 0;
            int fullBackups = 0;
            int incrementalBackups = 0;

            List<Path> backupFiles = Files.list(backupDir)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .toList();

            for (Path path : backupFiles) {
                totalSize += Files.size(path);
                totalBackups++;
                
                String filename = path.getFileName().toString();
                if (filename.startsWith("full_backup")) {
                    fullBackups++;
                } else if (filename.startsWith("incremental_backup")) {
                    incrementalBackups++;
                }
            }

            stats.put("totalBackups", totalBackups);
            stats.put("fullBackups", fullBackups);
            stats.put("incrementalBackups", incrementalBackups);
            stats.put("totalSize", formatFileSize(totalSize));
            
        } catch (IOException e) {
            logger.error("获取备份统计失败", e);
            stats.put("totalBackups", 0);
            stats.put("totalSize", 0);
        }

        return stats;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
package com.example.aichat.config;

import java.util.Arrays;
import java.util.List;

/**
 * 数据分类常量配置
 * 定义数据池系统中所有分类标准和存储配置
 */
public class DataClassificationConfig {

    // ==================== 数据类型 ====================
    
    /**
     * 数据类型枚举
     */
    public enum DataType {
        TEXT("text", "文本数据"),
        STRUCTURED("structured", "结构化数据"),
        IMAGE("image", "图像数据"),
        AUDIO("audio", "音频数据"),
        VIDEO("video", "视频数据"),
        DOCUMENT("document", "文档数据");

        private final String value;
        private final String description;

        DataType(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static DataType fromValue(String value) {
            for (DataType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return TEXT;
        }
    }

    // ==================== 来源模块 ====================

    /**
     * 来源模块枚举
     */
    public enum SourceModule {
        CHAT("chat", "聊天模块"),
        FEED("feed", "AI投喂模块"),
        CHARACTER("character", "角色管理模块"),
        SYSTEM("system", "系统模块"),
        ADMIN("admin", "管理后台");

        private final String value;
        private final String description;

        SourceModule(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static SourceModule fromValue(String value) {
            for (SourceModule module : values()) {
                if (module.value.equalsIgnoreCase(value)) {
                    return module;
                }
            }
            return SYSTEM;
        }
    }

    // ==================== 重要程度 ====================

    /**
     * 重要程度枚举
     */
    public enum ImportanceLevel {
        CRITICAL("critical", "关键数据", 4),
        HIGH("high", "重要数据", 3),
        MEDIUM("medium", "中等重要", 2),
        LOW("low", "一般数据", 1);

        private final String value;
        private final String description;
        private final int priority;

        ImportanceLevel(String value, String description, int priority) {
            this.value = value;
            this.description = description;
            this.priority = priority;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public int getPriority() {
            return priority;
        }

        public static ImportanceLevel fromValue(String value) {
            for (ImportanceLevel level : values()) {
                if (level.value.equalsIgnoreCase(value)) {
                    return level;
                }
            }
            return MEDIUM;
        }
    }

    // ==================== 访问权限 ====================

    /**
     * 访问权限枚举
     */
    public enum AccessPermission {
        PUBLIC("public", "公开数据", 0),
        PROTECTED("protected", "受保护数据", 1),
        PRIVATE("private", "私有数据", 2);

        private final String value;
        private final String description;
        private final int level;

        AccessPermission(String value, String description, int level) {
            this.value = value;
            this.description = description;
            this.level = level;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public int getLevel() {
            return level;
        }

        public static AccessPermission fromValue(String value) {
            for (AccessPermission permission : values()) {
                if (permission.value.equalsIgnoreCase(value)) {
                    return permission;
                }
            }
            return PRIVATE;
        }
    }

    // ==================== 数据状态 ====================

    /**
     * 数据状态枚举
     */
    public enum DataStatus {
        PENDING(0, "待处理"),
        PROCESSING(1, "处理中"),
        COMPLETED(2, "已完成"),
        ARCHIVED(3, "已归档"),
        DELETED(4, "已删除");

        private final int value;
        private final String description;

        DataStatus(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static DataStatus fromValue(int value) {
            for (DataStatus status : values()) {
                if (status.value == value) {
                    return status;
                }
            }
            return PENDING;
        }
    }

    // ==================== 支持的文件格式 ====================

    /**
     * 支持的文件格式列表
     */
    public static final List<String> SUPPORTED_FORMATS = Arrays.asList(
        "json", "xml", "csv", "txt", "yaml", "md",
        "pdf", "doc", "docx", "xls", "xlsx",
        "jpg", "jpeg", "png", "gif", "webp", "bmp",
        "mp3", "wav", "ogg",
        "mp4", "avi", "mov"
    );

    /**
     * 文本格式
     */
    public static final List<String> TEXT_FORMATS = Arrays.asList("txt", "md");

    /**
     * 结构化数据格式
     */
    public static final List<String> STRUCTURED_FORMATS = Arrays.asList("json", "xml", "csv", "yaml");

    /**
     * 图像格式
     */
    public static final List<String> IMAGE_FORMATS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "bmp");

    /**
     * 音频格式
     */
    public static final List<String> AUDIO_FORMATS = Arrays.asList("mp3", "wav", "ogg");

    /**
     * 视频格式
     */
    public static final List<String> VIDEO_FORMATS = Arrays.asList("mp4", "avi", "mov");

    /**
     * 文档格式
     */
    public static final List<String> DOCUMENT_FORMATS = Arrays.asList("pdf", "doc", "docx", "xls", "xlsx");

    // ==================== 存储路径配置 ====================

    /**
     * 数据池根目录
     */
    public static final String DATAPOOL_ROOT = "./datapool";

    /**
     * 各类型数据存储路径
     */
    public static final String TEXT_STORAGE_PATH = DATAPOOL_ROOT + "/text";
    public static final String STRUCTURED_STORAGE_PATH = DATAPOOL_ROOT + "/structured";
    public static final String IMAGE_STORAGE_PATH = DATAPOOL_ROOT + "/image";
    public static final String AUDIO_STORAGE_PATH = DATAPOOL_ROOT + "/audio";
    public static final String VIDEO_STORAGE_PATH = DATAPOOL_ROOT + "/video";
    public static final String DOCUMENT_STORAGE_PATH = DATAPOOL_ROOT + "/document";
    public static final String BACKUP_STORAGE_PATH = DATAPOOL_ROOT + "/backup";

    // ==================== 大小限制配置 ====================

    /**
     * 最大文件大小（字节）
     */
    public static final long MAX_TEXT_SIZE = 10 * 1024 * 1024; // 10MB
    public static final long MAX_STRUCTURED_SIZE = 50 * 1024 * 1024; // 50MB
    public static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final long MAX_AUDIO_SIZE = 100 * 1024 * 1024; // 100MB
    public static final long MAX_VIDEO_SIZE = 500 * 1024 * 1024; // 500MB
    public static final long MAX_DOCUMENT_SIZE = 50 * 1024 * 1024; // 50MB

    // ==================== 生命周期配置 ====================

    /**
     * 各类数据保留期限（天）
     */
    public static final int CRITICAL_RETENTION_DAYS = Integer.MAX_VALUE; // 永久
    public static final int HIGH_RETENTION_DAYS = 5 * 365; // 5年
    public static final int MEDIUM_RETENTION_DAYS = 2 * 365; // 2年
    public static final int LOW_RETENTION_DAYS = 365; // 1年

    /**
     * 归档后保留期限（天）
     */
    public static final int ARCHIVED_CRITICAL_RETENTION_DAYS = Integer.MAX_VALUE;
    public static final int ARCHIVED_HIGH_RETENTION_DAYS = 365; // 1年
    public static final int ARCHIVED_MEDIUM_RETENTION_DAYS = 180; // 6个月
    public static final int ARCHIVED_LOW_RETENTION_DAYS = 90; // 3个月

    /**
     * 软删除后恢复期限（天）
     */
    public static final int DELETED_RESTORE_DAYS = 30;

    // ==================== 备份配置 ====================

    /**
     * 增量备份间隔（小时）
     */
    public static final int INCREMENTAL_BACKUP_INTERVAL = 1;

    /**
     * 每日备份时间（小时，24小时制）
     */
    public static final int DAILY_BACKUP_HOUR = 2;

    /**
     * 每周备份时间（星期几，1-7）
     */
    public static final int WEEKLY_BACKUP_DAY = 7; // 周日

    /**
     * 每月备份时间（几号）
     */
    public static final int MONTHLY_BACKUP_DATE = 1;

    /**
     * 备份保留期限
     */
    public static final int INCREMENTAL_BACKUP_RETENTION_DAYS = 7;
    public static final int DAILY_BACKUP_RETENTION_DAYS = 30;
    public static final int WEEKLY_BACKUP_RETENTION_MONTHS = 3;
    public static final int MONTHLY_BACKUP_RETENTION_YEARS = 1;

    // ==================== 方法 ====================

    /**
     * 根据文件格式获取数据类型
     */
    public static DataType getDataTypeByFormat(String format) {
        if (format == null || format.isEmpty()) {
            return DataType.TEXT;
        }

        String lowerFormat = format.toLowerCase();

        if (STRUCTURED_FORMATS.contains(lowerFormat)) {
            return DataType.STRUCTURED;
        } else if (IMAGE_FORMATS.contains(lowerFormat)) {
            return DataType.IMAGE;
        } else if (AUDIO_FORMATS.contains(lowerFormat)) {
            return DataType.AUDIO;
        } else if (VIDEO_FORMATS.contains(lowerFormat)) {
            return DataType.VIDEO;
        } else if (DOCUMENT_FORMATS.contains(lowerFormat)) {
            return DataType.DOCUMENT;
        } else {
            return DataType.TEXT;
        }
    }

    /**
     * 根据数据类型获取存储路径
     */
    public static String getStoragePathByDataType(DataType dataType) {
        switch (dataType) {
            case TEXT:
                return TEXT_STORAGE_PATH;
            case STRUCTURED:
                return STRUCTURED_STORAGE_PATH;
            case IMAGE:
                return IMAGE_STORAGE_PATH;
            case AUDIO:
                return AUDIO_STORAGE_PATH;
            case VIDEO:
                return VIDEO_STORAGE_PATH;
            case DOCUMENT:
                return DOCUMENT_STORAGE_PATH;
            default:
                return TEXT_STORAGE_PATH;
        }
    }

    /**
     * 根据数据类型获取最大文件大小
     */
    public static long getMaxSizeByDataType(DataType dataType) {
        switch (dataType) {
            case TEXT:
                return MAX_TEXT_SIZE;
            case STRUCTURED:
                return MAX_STRUCTURED_SIZE;
            case IMAGE:
                return MAX_IMAGE_SIZE;
            case AUDIO:
                return MAX_AUDIO_SIZE;
            case VIDEO:
                return MAX_VIDEO_SIZE;
            case DOCUMENT:
                return MAX_DOCUMENT_SIZE;
            default:
                return MAX_TEXT_SIZE;
        }
    }

    /**
     * 根据重要程度获取保留期限（天）
     */
    public static int getRetentionDaysByImportance(ImportanceLevel importance) {
        switch (importance) {
            case CRITICAL:
                return CRITICAL_RETENTION_DAYS;
            case HIGH:
                return HIGH_RETENTION_DAYS;
            case MEDIUM:
                return MEDIUM_RETENTION_DAYS;
            case LOW:
                return LOW_RETENTION_DAYS;
            default:
                return MEDIUM_RETENTION_DAYS;
        }
    }

    /**
     * 根据重要程度获取归档后保留期限（天）
     */
    public static int getArchivedRetentionDaysByImportance(ImportanceLevel importance) {
        switch (importance) {
            case CRITICAL:
                return ARCHIVED_CRITICAL_RETENTION_DAYS;
            case HIGH:
                return ARCHIVED_HIGH_RETENTION_DAYS;
            case MEDIUM:
                return ARCHIVED_MEDIUM_RETENTION_DAYS;
            case LOW:
                return ARCHIVED_LOW_RETENTION_DAYS;
            default:
                return ARCHIVED_MEDIUM_RETENTION_DAYS;
        }
    }
}
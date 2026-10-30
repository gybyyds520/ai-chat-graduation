package com.example.aichat.controller;

import com.example.aichat.service.DataBackupService;
import com.example.aichat.dto.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/backup")
@PreAuthorize("hasRole('ADMIN')")
public class DataBackupController {

    private static final Logger logger = LoggerFactory.getLogger(DataBackupController.class);

    private final DataBackupService dataBackupService;

    public DataBackupController(DataBackupService dataBackupService) {
        this.dataBackupService = dataBackupService;
    }

    @PostMapping("/full")
    public R performFullBackup() {
        try {
            dataBackupService.fullBackup();
            return R.success("全量备份完成");
        } catch (Exception e) {
            logger.error("全量备份失败", e);
            return R.error("全量备份失败");
        }
    }

    @PostMapping("/incremental")
    public R performIncrementalBackup() {
        try {
            dataBackupService.incrementalBackup();
            return R.success("增量备份完成");
        } catch (Exception e) {
            logger.error("增量备份失败", e);
            return R.error("增量备份失败");
        }
    }

    @GetMapping("/list")
    public R listBackups() {
        try {
            List<String> backups = dataBackupService.listBackups();
            return R.success(backups);
        } catch (Exception e) {
            logger.error("获取备份列表失败", e);
            return R.error("获取备份列表失败");
        }
    }

    @PostMapping("/restore/{filename}")
    public R restoreBackup(@PathVariable String filename) {
        try {
            String result = dataBackupService.restoreFromBackup(filename);
            return R.success(result);
        } catch (Exception e) {
            logger.error("恢复备份失败", e);
            return R.error("恢复备份失败");
        }
    }

    @GetMapping("/stats")
    public R getBackupStats() {
        try {
            Map<String, Object> stats = dataBackupService.getBackupStats();
            return R.success(stats);
        } catch (Exception e) {
            logger.error("获取备份统计失败", e);
            return R.error("获取备份统计失败");
        }
    }
}
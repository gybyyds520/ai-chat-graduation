package com.example.aichat.controller;

import com.example.aichat.entity.UserDataRecord;
import com.example.aichat.service.DataPoolService;
import com.example.aichat.dto.R;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端数据池接口。
 *
 * 安全修复（P0）：本控制器承载全量用户数据的上传/查询/导出/删除，
 * 原实现未做任何管理员校验，任意登录用户都可读取/导出全部用户数据。
 * 现统一要求 ADMIN 角色；错误响应不再回显内部异常信息。
 */
@RestController
@RequestMapping("/api/admin/data-pool")
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
public class DataPoolController {

    private static final Logger logger = LoggerFactory.getLogger(DataPoolController.class);

    private final DataPoolService dataPoolService;
    private final ObjectMapper objectMapper;

    public DataPoolController(DataPoolService dataPoolService, ObjectMapper objectMapper) {
        this.dataPoolService = dataPoolService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/upload")
    public R uploadUserData(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.parseLong(request.get("userId").toString());
            List<Map<String, Object>> dataList = objectMapper.convertValue(
                request.get("data"), 
                new TypeReference<List<Map<String, Object>>>() {}
            );

            dataPoolService.processAndSaveUserData(userId, dataList);
            
            return R.success("数据上传成功");
        } catch (Exception e) {
            logger.error("数据上传失败", e);
            return R.error("数据上传失败");
        }
    }

    @GetMapping("/records")
    public R getUserDataRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String dataType,
            @RequestParam(required = false) String keyword) {
        try {
            Map<String, Object> result = dataPoolService.getUserDataRecords(page, size, userId, dataType, keyword);
            return R.success(result);
        } catch (Exception e) {
            logger.error("获取数据记录失败", e);
            return R.error("获取数据记录失败");
        }
    }

    @PostMapping("/records")
    public R addRecord(@RequestBody Map<String, Object> request) {
        try {
            UserDataRecord record = objectMapper.convertValue(request, UserDataRecord.class);
            dataPoolService.addRecord(record);
            return R.success("添加成功");
        } catch (Exception e) {
            logger.error("添加数据失败", e);
            return R.error("添加数据失败");
        }
    }

    @GetMapping("/records/{id}")
    public R getRecordById(@PathVariable Long id) {
        try {
            UserDataRecord record = dataPoolService.getRecordById(id);
            if (record == null) {
                return R.error("记录不存在");
            }
            return R.success(record);
        } catch (Exception e) {
            logger.error("获取记录失败", e);
            return R.error("获取记录失败");
        }
    }

    @PutMapping("/records/{id}")
    public R updateRecord(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            UserDataRecord record = objectMapper.convertValue(request, UserDataRecord.class);
            record.setId(id);
            dataPoolService.updateRecord(record);
            return R.success("更新成功");
        } catch (Exception e) {
            logger.error("更新数据失败", e);
            return R.error("更新数据失败");
        }
    }

    @GetMapping("/config")
    public R getConfig() {
        try {
            return R.success(dataPoolService.getDataPoolConfig());
        } catch (Exception e) {
            logger.error("获取配置失败", e);
            return R.error("获取配置失败");
        }
    }

    @PostMapping("/config")
    public R saveConfig(@RequestBody Map<String, Object> request) {
        try {
            Boolean autoSave = request.get("autoSaveEnabled") != null
                    ? Boolean.parseBoolean(request.get("autoSaveEnabled").toString()) : null;
            Integer retentionDays = request.get("retentionDays") != null
                    ? Integer.parseInt(request.get("retentionDays").toString()) : null;
            Integer maxRecordsPerUser = request.get("maxRecordsPerUser") != null
                    ? Integer.parseInt(request.get("maxRecordsPerUser").toString()) : null;
            dataPoolService.updateDataPoolConfig(autoSave, retentionDays, maxRecordsPerUser);
            return R.success("配置保存成功");
        } catch (Exception e) {
            logger.error("保存配置失败", e);
            return R.error("保存配置失败");
        }
    }

    @PostMapping("/import")
    public R importData(@RequestBody Map<String, Object> request) {
        try {
            Object userIdObj = request.get("userId");
            Long userId = userIdObj != null ? Long.parseLong(userIdObj.toString()) : 0L;
            List<Map<String, Object>> dataList = objectMapper.convertValue(
                request.get("data") != null ? request.get("data") : request.get("records"),
                new TypeReference<List<Map<String, Object>>>() {}
            );
            dataPoolService.processAndSaveUserData(userId, dataList);
            return R.success("导入成功");
        } catch (Exception e) {
            logger.error("导入数据失败", e);
            return R.error("导入数据失败");
        }
    }

    @GetMapping("/export")
    public R exportData() {
        try {
            List<UserDataRecord> records = dataPoolService.exportAllRecords();
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("records", records);
            result.put("total", records.size());
            return R.success(result);
        } catch (Exception e) {
            logger.error("导出数据失败", e);
            return R.error("导出数据失败");
        }
    }

    @GetMapping("/stats")
    public R getDataStats() {
        try {
            Map<String, Object> stats = dataPoolService.getDataStats();
            return R.success(stats);
        } catch (Exception e) {
            logger.error("获取数据统计失败", e);
            return R.error("获取数据统计失败");
        }
    }

    @GetMapping("/types")
    public R getDataTypes() {
        try {
            List<String> types = dataPoolService.getAllDataTypes();
            return R.success(types);
        } catch (Exception e) {
            logger.error("获取数据类型失败", e);
            return R.error("获取数据类型失败");
        }
    }

    @DeleteMapping("/records/{id}")
    public R deleteRecord(@PathVariable Long id) {
        try {
            dataPoolService.deleteRecord(id);
            return R.success("删除成功");
        } catch (Exception e) {
            logger.error("删除失败", e);
            return R.error("删除失败");
        }
    }

    @DeleteMapping("/records/batch")
    public R deleteBatchRecords(@RequestBody List<Long> ids) {
        try {
            dataPoolService.deleteBatchRecords(ids);
            return R.success("批量删除成功");
        } catch (Exception e) {
            logger.error("批量删除失败", e);
            return R.error("批量删除失败");
        }
    }

    @GetMapping("/analysis")
    public R analyzeUserData(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String dataType) {
        try {
            Map<String, Object> analysis = dataPoolService.analyzeUserData(userId, dataType);
            return R.success(analysis);
        } catch (Exception e) {
            logger.error("数据分析失败", e);
            return R.error("数据分析失败");
        }
    }

    @GetMapping("/auto-save/status")
    public R getAutoSaveStatus() {
        try {
            boolean enabled = dataPoolService.isAutoSaveEnabled();
            return R.success(Map.of("enabled", enabled));
        } catch (Exception e) {
            logger.error("获取自动保存状态失败", e);
            return R.error("获取自动保存状态失败");
        }
    }

    @PostMapping("/auto-save/toggle")
    public R toggleAutoSave(@RequestBody Map<String, Boolean> request) {
        try {
            Boolean enabled = request.get("enabled");
            if (enabled == null) {
                return R.error("参数错误");
            }
            dataPoolService.setAutoSaveEnabled(enabled);
            return R.success("操作成功");
        } catch (Exception e) {
            logger.error("操作失败", e);
            return R.error("操作失败");
        }
    }

    @GetMapping("/duplicates/check")
    public R checkDuplicates() {
        try {
            Map<String, Object> result = dataPoolService.checkDuplicates();
            return R.success(result);
        } catch (Exception e) {
            logger.error("检查重复数据失败", e);
            return R.error("检查重复数据失败");
        }
    }

    @PostMapping("/duplicates/remove")
    public R removeDuplicates() {
        try {
            Map<String, Object> result = dataPoolService.removeDuplicates();
            return R.success(result);
        } catch (Exception e) {
            logger.error("去重失败", e);
            return R.error("去重失败");
        }
    }
}

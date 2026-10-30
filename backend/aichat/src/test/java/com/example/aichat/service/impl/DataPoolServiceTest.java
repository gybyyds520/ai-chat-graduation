package com.example.aichat.service;

import com.example.aichat.entity.User;
import com.example.aichat.entity.UserDataRecord;
import com.example.aichat.mapper.UserDataRecordMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.DataPoolConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DataPoolService 单元测试（Mockito 隔离测试）。
 *
 * 覆盖点：
 *   1. getRecordById - 查询存在/不存在
 *   2. addRecord - 手动添加记录，设置默认值
 *   3. updateRecord - 更新记录
 *   4. deleteRecord - 删除记录
 *   5. getAllDataTypes - 查询所有数据类型
 *   6. getDataStats - 获取统计信息
 *   7. isAutoSaveEnabled / setAutoSaveEnabled - 自动保存开关
 *   8. checkDuplicates - 检查重复数据
 *   9. analyzeUserData - 分析用户数据
 */
@ExtendWith(MockitoExtension.class)
class DataPoolServiceTest {

    @Mock
    private UserDataRecordMapper userDataRecordMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private DataPoolConfigService configService;

    private ObjectMapper objectMapper;
    private DataPoolService dataPoolService;

    private UserDataRecord sampleRecord;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        dataPoolService = new DataPoolService(userDataRecordMapper, objectMapper, userMapper, configService);

        sampleRecord = new UserDataRecord();
        sampleRecord.setId(1L);
        sampleRecord.setUserId(42L);
        sampleRecord.setDataType("chat");
        sampleRecord.setContent("用户聊天记录");
        sampleRecord.setStatus("VALIDATED");
        sampleRecord.setIntegrityScore(0.8);
        sampleRecord.setCaptureTime(LocalDateTime.now());
    }

    @Test
    void getRecordById_存在应返回记录() {
        when(userDataRecordMapper.selectById(1L)).thenReturn(sampleRecord);

        UserDataRecord result = dataPoolService.getRecordById(1L);

        assertNotNull(result);
        assertEquals("chat", result.getDataType());
    }

    @Test
    void getRecordById_不存在应返回null() {
        when(userDataRecordMapper.selectById(999L)).thenReturn(null);

        UserDataRecord result = dataPoolService.getRecordById(999L);

        assertNull(result);
    }

    @Test
    void addRecord_应设置默认值并插入() {
        UserDataRecord record = new UserDataRecord();
        record.setUserId(42L);
        record.setDataType("search");
        record.setContent("搜索数据");

        UserDataRecord result = dataPoolService.addRecord(record);

        assertNotNull(result);
        assertEquals("VALIDATED", result.getStatus(), "默认状态应为VALIDATED");
        assertEquals(0.5, result.getIntegrityScore(), 0.001, "默认完整性分数应为0.5");
        assertNotNull(result.getCaptureTime(), "默认捕获时间应为当前时间");
        assertEquals("PENDING_BACKUP", result.getBackupStatus());
        assertNotNull(result.getGroupId());
        verify(userDataRecordMapper).insert(record);
    }

    @Test
    void updateRecord_应调用mapper更新() {
        sampleRecord.setContent("更新后的内容");

        dataPoolService.updateRecord(sampleRecord);

        verify(userDataRecordMapper).updateById(sampleRecord);
    }

    @Test
    void deleteRecord_应调用mapper删除() {
        dataPoolService.deleteRecord(1L);

        verify(userDataRecordMapper).deleteById(1L);
    }

    @Test
    void getAllDataTypes_应返回类型列表() {
        when(userDataRecordMapper.selectAllDataTypes()).thenReturn(List.of("chat", "search", "settings"));

        List<String> result = dataPoolService.getAllDataTypes();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("chat"));
    }

    @Test
    void getDataStats_应返回统计信息() {
        when(userDataRecordMapper.selectTotalCount()).thenReturn(100);
        when(userDataRecordMapper.selectBackedUpCount()).thenReturn(80);
        when(userDataRecordMapper.selectValidatedCount()).thenReturn(60);
        when(userDataRecordMapper.selectStatsByType()).thenReturn(List.of(
                Map.of("data_type", "chat", "count", 50)
        ));
        when(userDataRecordMapper.selectStatsByUser()).thenReturn(List.of(
                Map.of("user_id", 1, "count", 30),
                Map.of("user_id", 2, "count", 20)
        ));

        Map<String, Object> stats = dataPoolService.getDataStats();

        assertEquals(100, stats.get("totalRecords"));
        assertEquals(80, stats.get("backedUpRecords"));
        assertEquals(60, stats.get("validatedRecords"));
        assertEquals(2, stats.get("userCount"));
    }

    @Test
    void isAutoSaveEnabled_应委托DB配置服务() {
        when(configService.isAutoSaveEnabled()).thenReturn(true);
        assertTrue(dataPoolService.isAutoSaveEnabled());

        when(configService.isAutoSaveEnabled()).thenReturn(false);
        assertFalse(dataPoolService.isAutoSaveEnabled());
    }

    @Test
    void setAutoSaveEnabled_应委托DB配置服务持久化() {
        dataPoolService.setAutoSaveEnabled(true);
        verify(configService).setAutoSaveEnabled(true);

        dataPoolService.setAutoSaveEnabled(false);
        verify(configService).setAutoSaveEnabled(false);
    }

    // ============ autoSave 持久化 / isFreeUser 移除 / 生命周期（新增） ============

    @Test
    void processAndSaveUserData_autoSave关闭应跳过不写库() {
        when(configService.isAutoSaveEnabled()).thenReturn(false);

        Map<String, Object> item = new HashMap<>();
        item.put("dataType", "chat");
        item.put("content", "测试内容");
        dataPoolService.processAndSaveUserData(1L, List.of(item));

        verify(userDataRecordMapper, never()).insert(any(UserDataRecord.class));
    }

    @Test
    void processAndSaveUserData_付费用户也应采集_不再检查isFreeUser() {
        when(configService.isAutoSaveEnabled()).thenReturn(true);
        // 即使用户是非 FREE（付费）用户，也不应被 isFreeUser 拦截；
        // 该方法已不查询 User 表，故不 stub userMapper（严格模式下多余 stub 会失败）。
        when(userDataRecordMapper.selectByUniqueKey(any(), any(), any())).thenReturn(null);

        Map<String, Object> item = new HashMap<>();
        item.put("dataType", "chat");
        item.put("content", "付费用户的一条较长测试内容");
        dataPoolService.processAndSaveUserData(1L, List.of(item));

        // 无 NPE 且执行了插入（说明没有被 isFreeUser 拦截）
        verify(userDataRecordMapper).insert(any(UserDataRecord.class));
    }

    @Test
    void cleanupExpiredRecords_应按保留天数删除过期记录() {
        when(configService.getRetentionDays()).thenReturn(180);
        when(userDataRecordMapper.delete(any(Wrapper.class))).thenReturn(7);

        int deleted = dataPoolService.cleanupExpiredRecords();

        assertEquals(7, deleted);
        verify(configService).getRetentionDays();
        verify(userDataRecordMapper).delete(any(Wrapper.class));
    }

    @Test
    void getDataPoolConfig_应聚合返回DB配置() {
        when(configService.isAutoSaveEnabled()).thenReturn(true);
        when(configService.getRetentionDays()).thenReturn(180);
        when(configService.getMaxRecordsPerUser()).thenReturn(10000);

        Map<String, Object> cfg = dataPoolService.getDataPoolConfig();

        assertEquals(true, cfg.get("autoSaveEnabled"));
        assertEquals(180, cfg.get("retentionDays"));
        assertEquals(10000, cfg.get("maxRecordsPerUser"));
    }

    @Test
    void updateDataPoolConfig_应透传到配置服务() {
        dataPoolService.updateDataPoolConfig(false, 90, 5000);
        verify(configService).setAutoSaveEnabled(false);
        verify(configService).setRetentionDays(90);
        verify(configService).setMaxRecordsPerUser(5000);
    }

    @Test
    void checkDuplicates_应返回重复统计() {
        Map<String, Object> dupGroup = new HashMap<>();
        dupGroup.put("user_id", 42);
        dupGroup.put("data_type", "chat");
        dupGroup.put("content", "重复内容");
        dupGroup.put("count", 3);

        when(userDataRecordMapper.selectDuplicateRecords()).thenReturn(List.of(dupGroup));

        Map<String, Object> result = dataPoolService.checkDuplicates();

        assertEquals(1, result.get("duplicateGroups"));
        assertEquals(2L, result.get("totalDuplicateRecords"), "count=3意味着2条重复");
    }

    @Test
    void analyzeUserData_无用户过滤应分析全部数据() {
        when(userDataRecordMapper.selectList(null)).thenReturn(List.of(sampleRecord));

        Map<String, Object> analysis = dataPoolService.analyzeUserData(null, null);

        assertEquals(1, analysis.get("totalRecords"));
        assertNotNull(analysis.get("byType"));
        assertNotNull(analysis.get("avgIntegrityScore"));
    }

    @Test
    void exportAllRecords_应返回全部记录() {
        when(userDataRecordMapper.selectList(null)).thenReturn(List.of(sampleRecord));

        List<UserDataRecord> result = dataPoolService.exportAllRecords();

        assertEquals(1, result.size());
    }

    // ============ getUserDataRecords keyword（P2 修复回归） ============

    @Test
    @SuppressWarnings("unchecked")
    void getUserDataRecords_keyword非空_应追加content与tags模糊匹配() {
        when(userDataRecordMapper.selectPage(any(Page.class), any(Wrapper.class)))
                .thenReturn(new Page<>(1, 20));

        dataPoolService.getUserDataRecords(1, 20, null, null, "聊天");

        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(userDataRecordMapper).selectPage(any(Page.class), captor.capture());
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserDataRecord> wrapper =
                (com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserDataRecord>) captor.getValue();
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("content"), "WHERE 应包含 content 模糊条件: " + sqlSegment);
        assertTrue(sqlSegment.contains("tags"), "WHERE 应包含 tags 模糊条件: " + sqlSegment);
        // MyBatis-Plus 对 like 参数做 %kw% 包裹，校验参数值包含 keyword
        assertTrue(wrapper.getParamNameValuePairs().values().stream()
                        .anyMatch(v -> v != null && v.toString().contains("聊天")),
                "参数应携带 keyword 值");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUserDataRecords_keyword为null_不应追加模糊条件() {
        when(userDataRecordMapper.selectPage(any(Page.class), any(Wrapper.class)))
                .thenReturn(new Page<>(1, 20));

        dataPoolService.getUserDataRecords(1, 20, null, null, null);

        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(userDataRecordMapper).selectPage(any(Page.class), captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertFalse(sqlSegment.contains("content"), "keyword 为空时不应出现 content 条件: " + sqlSegment);
        assertFalse(sqlSegment.contains("tags"), "keyword 为空时不应出现 tags 条件: " + sqlSegment);
    }
}

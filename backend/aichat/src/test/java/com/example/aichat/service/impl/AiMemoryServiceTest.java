package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.entity.AiMemory;
import com.example.aichat.entity.AiMemorySetting;
import com.example.aichat.mapper.AiMemoryMapper;
import com.example.aichat.mapper.AiMemorySettingMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AI 记忆服务单元测试（Mock Mapper）。
 *
 * 覆盖：CRUD + 归属校验（IDOR）+ 分页过滤 + 统计 + 上下文注入召回 + 标记已用。
 */
@ExtendWith(MockitoExtension.class)
class AiMemoryServiceTest {

    @Mock
    private AiMemoryMapper aiMemoryMapper;

    @Mock
    private AiMemorySettingMapper aiMemorySettingMapper;

    @InjectMocks
    private AiMemoryServiceImpl service;

    private AiMemory memory(Long id, Long userId, boolean active) {
        AiMemory m = new AiMemory();
        m.setId(id);
        m.setUserId(userId);
        m.setContent("记忆" + id);
        m.setMemoryType("preference");
        m.setImportance(3);
        m.setIsActive(active);
        return m;
    }

    // ---------- createMemory ----------

    @Test
    void createMemory_正常_落库并回填() {
        AiMemory m = service.createMemory(1L, "喜欢喝咖啡", "manual", "preference", 4, "饮食");
        assertEquals("喜欢喝咖啡", m.getContent());
        assertEquals("preference", m.getMemoryType());
        assertEquals(4, m.getImportance());
        assertEquals("饮食", m.getCategory());
        assertTrue(m.getIsActive());
        assertNotNull(m.getCreatedAt());
        verify(aiMemoryMapper).insert(m);
    }

    @Test
    void createMemory_默认值_缺省填充() {
        AiMemory m = service.createMemory(1L, "   内容   ", null, null, null, null);
        assertEquals("manual", m.getSource());
        assertEquals("other", m.getMemoryType());
        assertEquals(3, m.getImportance());
        assertEquals("内容", m.getContent()); // trim
    }

    @Test
    void createMemory_重要性越界_被钳制() {
        assertEquals(5, service.createMemory(1L, "c", null, null, 99, null).getImportance());
        assertEquals(1, service.createMemory(1L, "c", null, null, 0, null).getImportance());
    }

    @Test
    void createMemory_内容为空_抛业务异常() {
        assertThrows(BusinessException.class,
                () -> service.createMemory(1L, "  ", null, null, null, null));
        verify(aiMemoryMapper, never()).insert(any(AiMemory.class));
    }

    // ---------- updateMemory / 归属 ----------

    @Test
    void updateMemory_正常_更新字段() {
        when(aiMemoryMapper.selectById(10L)).thenReturn(memory(10L, 1L, true));
        AiMemory m = service.updateMemory(1L, 10L, "新内容", "fact", 5, "工作", false);
        assertEquals("新内容", m.getContent());
        assertEquals("fact", m.getMemoryType());
        assertEquals(5, m.getImportance());
        assertFalse(m.getIsActive());
        verify(aiMemoryMapper).updateById(m);
    }

    @Test
    void updateMemory_他人记忆_拒绝IDOR() {
        when(aiMemoryMapper.selectById(10L)).thenReturn(memory(10L, 999L, true));
        assertThrows(BusinessException.class,
                () -> service.updateMemory(1L, 10L, "x", null, null, null, null));
        verify(aiMemoryMapper, never()).updateById(any(AiMemory.class));
    }

    @Test
    void updateMemory_记忆不存在_抛异常() {
        when(aiMemoryMapper.selectById(10L)).thenReturn(null);
        assertThrows(BusinessException.class,
                () -> service.updateMemory(1L, 10L, "x", null, null, null, null));
    }

    // ---------- deleteMemory ----------

    @Test
    void deleteMemory_正常_删除() {
        when(aiMemoryMapper.selectById(10L)).thenReturn(memory(10L, 1L, true));
        service.deleteMemory(1L, 10L);
        verify(aiMemoryMapper).deleteById(10L);
    }

    @Test
    void deleteMemory_他人记忆_拒绝IDOR() {
        when(aiMemoryMapper.selectById(10L)).thenReturn(memory(10L, 2L, true));
        assertThrows(BusinessException.class, () -> service.deleteMemory(1L, 10L));
        verify(aiMemoryMapper, never()).deleteById(anyLong());
    }

    // ---------- getMemory ----------

    @Test
    void getMemory_正常_返回() {
        when(aiMemoryMapper.selectById(10L)).thenReturn(memory(10L, 1L, true));
        assertSame(memory(10L, 1L, true).getClass(),
                service.getMemory(1L, 10L).getClass());
    }

    @Test
    void getMemory_他人记忆_拒绝IDOR() {
        when(aiMemoryMapper.selectById(10L)).thenReturn(memory(10L, 3L, true));
        assertThrows(BusinessException.class, () -> service.getMemory(1L, 10L));
    }

    // ---------- listMemories ----------

    @Test
    @SuppressWarnings("unchecked")
    void listMemories_分页_调用selectPage() {
        Page<AiMemory> page = new Page<>(1, 10);
        page.setRecords(List.of(memory(1L, 1L, true)));
        page.setTotal(1);
        when(aiMemoryMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

        IPage<AiMemory> result = service.listMemories(1L, 1, 10, null, null, null, null, null);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listMemories_过滤_封装查询条件() {
        when(aiMemoryMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(new Page<>());
        service.listMemories(1L, 1, 10, "preference", "饮食", true, "咖啡", "chat");

        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(aiMemoryMapper).selectPage(any(Page.class), captor.capture());
        String sql = captor.getValue().getExpression().getNormal().getSqlSegment();
        assertTrue(sql.contains("user_id"));
        assertTrue(sql.contains("memory_type"));
        assertTrue(sql.contains("category"));
        assertTrue(sql.contains("is_active"));
        assertTrue(sql.contains("source"));
        assertTrue(sql.contains("content"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listMemories_关键词为空_不追加LIKE() {
        when(aiMemoryMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(new Page<>());
        service.listMemories(1L, 1, 10, null, null, null, "   ", null);

        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(aiMemoryMapper).selectPage(any(Page.class), captor.capture());
        String sql = captor.getValue().getExpression().getNormal().getSqlSegment();
        assertFalse(sql.contains("content"));
    }

    // ---------- getMemoryStats ----------

    @Test
    void getMemoryStats_汇总总数与分布() {
        when(aiMemoryMapper.selectCount(any(QueryWrapper.class))).thenReturn(5L);
        Map<String, Object> row1 = new HashMap<>();
        row1.put("memoryType", "fact");
        row1.put("cnt", 3L);
        Map<String, Object> row2 = new HashMap<>();
        row2.put("memoryType", "preference");
        row2.put("cnt", 2L);
        when(aiMemoryMapper.selectMaps(any(QueryWrapper.class)))
                .thenReturn(Arrays.asList(row1, row2))
                .thenReturn(Arrays.asList());
        Map<String, Object> stats = service.getMemoryStats(1L);
        assertEquals(5L, stats.get("total"));
        @SuppressWarnings("unchecked")
        Map<String, Long> typeCounts = (Map<String, Long>) stats.get("typeCounts");
        assertEquals(3L, typeCounts.get("fact"));
        assertNotNull(stats.get("importanceDistribution"));
    }

    // ---------- toggleMemory ----------

    @Test
    void toggleMemory_取反生效状态() {
        when(aiMemoryMapper.selectById(10L)).thenReturn(memory(10L, 1L, true));
        AiMemory m = service.toggleMemory(1L, 10L);
        assertFalse(m.getIsActive());
        verify(aiMemoryMapper).updateById(m);
    }

    // ---------- getActiveMemoriesForContext ----------

    @Test
    @SuppressWarnings("unchecked")
    void getActiveMemoriesForContext_只查active并按重要性排序() {
        when(aiMemoryMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of(memory(1L, 1L, true), memory(2L, 1L, true)));
        List<AiMemory> list = service.getActiveMemoriesForContext(1L, 10);
        assertEquals(2, list.size());
        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(aiMemoryMapper).selectList(captor.capture());
        // 取 WHERE 段与 ORDER BY 段分别断言（MyBatis-Plus 3.5.8 order by 不在 normal 段）
        String whereSql = captor.getValue().getExpression().getNormal().getSqlSegment();
        String orderBySql = captor.getValue().getExpression().getOrderBy().getSqlSegment();
        assertTrue(whereSql.contains("is_active"));
        assertTrue(orderBySql.contains("importance"));
    }

    @Test
    void getActiveMemoriesForContext_空列表_返回空集合() {
        when(aiMemoryMapper.selectList(any(QueryWrapper.class))).thenReturn(null);
        assertTrue(service.getActiveMemoriesForContext(1L, 10).isEmpty());
    }

    // ---------- markMemoryUsed ----------

    @Test
    void markMemoryUsed_本人记忆_更新lastUsedAt() {
        AiMemory m = memory(5L, 1L, true);
        when(aiMemoryMapper.selectById(5L)).thenReturn(m);
        service.markMemoryUsed(1L, List.of(5L));
        assertNotNull(m.getLastUsedAt());
        verify(aiMemoryMapper).updateById(m);
    }

    @Test
    void markMemoryUsed_他人记忆_跳过不更新() {
        when(aiMemoryMapper.selectById(5L)).thenReturn(memory(5L, 999L, true));
        service.markMemoryUsed(1L, List.of(5L));
        verify(aiMemoryMapper, never()).updateById(any(AiMemory.class));
    }

    // ---------- extractMemoriesFromMessage ----------

    @Test
    void extractMemoriesFromMessage_提炼姓名与喜好_落库() {
        when(aiMemoryMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        service.extractMemoriesFromMessage(1L, "我叫小明，我喜欢喝咖啡，我住在北京");

        ArgumentCaptor<AiMemory> captor = ArgumentCaptor.forClass(AiMemory.class);
        verify(aiMemoryMapper, times(3)).insert(captor.capture());
        List<AiMemory> inserted = captor.getAllValues();
        assertEquals("我叫小明", inserted.get(0).getContent());
        assertEquals("fact", inserted.get(0).getMemoryType());
        assertEquals("chat", inserted.get(0).getSource());
        assertEquals("我喜欢喝咖啡", inserted.get(1).getContent());
        assertEquals("preference", inserted.get(1).getMemoryType());
        assertEquals("我住在北京", inserted.get(2).getContent());
        assertEquals("fact", inserted.get(2).getMemoryType());
    }

    @Test
    void extractMemoriesFromMessage_重复内容_跳过创建() {
        // selectCount 命中（精确或近似）→ 视为已存在，不再 insert
        when(aiMemoryMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        service.extractMemoriesFromMessage(1L, "我叫小明");
        verify(aiMemoryMapper, never()).insert(any(AiMemory.class));
    }

    @Test
    void extractMemoriesFromMessage_无匹配模式_不创建() {
        service.extractMemoriesFromMessage(1L, "今天天气不错，代码写完了");
        verify(aiMemoryMapper, never()).insert(any(AiMemory.class));
    }

    @Test
    void extractMemoriesFromMessage_空消息_直接返回() {
        service.extractMemoriesFromMessage(1L, "   ");
        service.extractMemoriesFromMessage(1L, null);
        verify(aiMemoryMapper, never()).insert(any(AiMemory.class));
        verify(aiMemoryMapper, never()).selectCount(any(QueryWrapper.class));
    }

    @Test
    void extractMemoriesFromMessage_年龄与生日_分别落库() {
        when(aiMemoryMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        service.extractMemoriesFromMessage(1L, "我今年25岁，我的生日是2000年5月20日");
        verify(aiMemoryMapper, times(2)).insert(any(AiMemory.class));
    }

    // ---------- M1 / A3：全局开关 ----------

    @Test
    void getMemorySetting_无记录_默认开启() {
        when(aiMemorySettingMapper.selectById(1L)).thenReturn(null);
        Map<String, Object> s = service.getMemorySetting(1L);
        assertEquals(Boolean.TRUE, s.get("enabled"));
    }

    @Test
    void updateMemorySetting_关闭_写入设置() {
        when(aiMemorySettingMapper.selectById(1L)).thenReturn(null);
        service.updateMemorySetting(1L, false);
        ArgumentCaptor<AiMemorySetting> captor = ArgumentCaptor.forClass(AiMemorySetting.class);
        verify(aiMemorySettingMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals(Boolean.FALSE, captor.getValue().getEnabled());
    }

    @Test
    void getActiveMemoriesForContext_开关关闭_不查记忆直接返回空() {
        AiMemorySetting off = new AiMemorySetting();
        off.setUserId(1L);
        off.setEnabled(false);
        when(aiMemorySettingMapper.selectById(1L)).thenReturn(off);

        List<AiMemory> list = service.getActiveMemoriesForContext(1L, 10);
        assertTrue(list.isEmpty(), "开关关闭后不应注入任何记忆");
        // 关键：关闭后不再查询 ai_memory，即删除/停用记忆都不会被引用
        verify(aiMemoryMapper, never()).selectList(any(QueryWrapper.class));
    }

    @Test
    void getActiveMemoriesForContext_开关开启_正常召回() {
        AiMemorySetting on = new AiMemorySetting();
        on.setUserId(1L);
        on.setEnabled(true);
        when(aiMemorySettingMapper.selectById(1L)).thenReturn(on);
        when(aiMemoryMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of(memory(1L, 1L, true)));

        List<AiMemory> list = service.getActiveMemoriesForContext(1L, 10);
        assertEquals(1, list.size());
    }

    // ---------- M1 / A3：钉选 ----------

    @Test
    void pinMessageToMemory_正常_以高重要性落库() {
        AiMemory pinned = service.pinMessageToMemory(1L, "我们上周三讨论了毕设选题", "pinned");
        assertEquals("我们上周三讨论了毕设选题", pinned.getContent());
        assertEquals(5, pinned.getImportance());
        assertEquals("pinned", pinned.getSource());
        assertEquals("钉选记忆", pinned.getCategory());
        verify(aiMemoryMapper).insert(pinned);
    }

    @Test
    void pinMessageToMemory_空内容_抛业务异常() {
        assertThrows(BusinessException.class, () -> service.pinMessageToMemory(1L, "  ", null));
        verify(aiMemoryMapper, never()).insert(any(AiMemory.class));
    }

    // ---------- 市场级升级：自动分类 ----------

    @Test
    void createMemory_未指定分类_按内容自动归类为偏好() {
        AiMemory m = service.createMemory(1L, "我讨厌香菜", "manual", "preference", 3, null);
        assertEquals("PREFERENCE", m.getCategory());
    }

    @Test
    void createMemory_未指定分类_默认事实() {
        AiMemory m = service.createMemory(1L, "用户今年25岁", "manual", "fact", 3, null);
        assertEquals("FACT", m.getCategory());
    }

    @Test
    void createMemory_显式分类_保留自由文本不覆盖() {
        AiMemory m = service.createMemory(1L, "我喜欢咖啡", "manual", "preference", 3, "饮食");
        assertEquals("饮食", m.getCategory());
    }

    // ---------- 市场级升级：引用计数 ----------

    @Test
    void markMemoryUsed_引用次数累加() {
        AiMemory m = memory(5L, 1L, true);
        m.setReferencedCount(2);
        when(aiMemoryMapper.selectById(5L)).thenReturn(m);
        service.markMemoryUsed(1L, List.of(5L));
        assertEquals(3, m.getReferencedCount());
        assertNotNull(m.getLastUsedAt());
    }

    // ---------- 市场级升级：批量删除 ----------

    @Test
    void batchDelete_全部本人记忆_逐条删除() {
        when(aiMemoryMapper.selectById(1L)).thenReturn(memory(1L, 1L, true));
        when(aiMemoryMapper.selectById(2L)).thenReturn(memory(2L, 1L, true));
        int deleted = service.batchDeleteMemories(1L, List.of(1L, 2L));
        assertEquals(2, deleted);
        verify(aiMemoryMapper).deleteById(1L);
        verify(aiMemoryMapper).deleteById(2L);
    }

    @Test
    void batchDelete_含他人记忆_跳过越权项() {
        when(aiMemoryMapper.selectById(1L)).thenReturn(memory(1L, 1L, true));
        when(aiMemoryMapper.selectById(99L)).thenReturn(memory(99L, 999L, true));
        int deleted = service.batchDeleteMemories(1L, List.of(1L, 99L));
        assertEquals(1, deleted);
        verify(aiMemoryMapper).deleteById(1L);
        verify(aiMemoryMapper, never()).deleteById(99L);
    }

    @Test
    void batchDelete_空列表_返回0() {
        assertEquals(0, service.batchDeleteMemories(1L, List.of()));
        assertEquals(0, service.batchDeleteMemories(1L, null));
    }

    // ---------- 市场级升级：导出 ----------

    @Test
    void exportAll_只返回生效记忆_按评分降序() {
        AiMemory high = memory(1L, 1L, true);
        high.setImportance(5);
        AiMemory low = memory(2L, 1L, true);
        low.setImportance(1);
        when(aiMemoryMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(low, high));
        List<AiMemory> out = service.exportAllMemories(1L);
        assertEquals(2, out.size());
        assertEquals(1L, out.get(0).getId()); // importance=5 排前面
    }

    // ---------- 市场级升级：主动引用（无向量客户端时降级关键词） ----------

    @Test
    void buildActiveReferenceContext_无记忆_返回空串() {
        when(aiMemoryMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of());
        assertEquals("", service.buildActiveReferenceContext(1L, "我喜欢什么", 3));
    }

    @Test
    void buildActiveReferenceContext_命中记忆_拼接并更新引用() {
        AiMemory m = memory(7L, 1L, true);
        m.setContent("用户喜欢喝咖啡");
        when(aiMemoryMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(m));
        when(aiMemoryMapper.selectById(7L)).thenReturn(m);
        String ctx = service.buildActiveReferenceContext(1L, "我喜欢喝什么", 3);
        assertTrue(ctx.contains("用户记得"), ctx);
        assertTrue(ctx.contains("喜欢喝咖啡"), ctx);
        verify(aiMemoryMapper, atLeastOnce()).updateById(any(AiMemory.class));
    }
}

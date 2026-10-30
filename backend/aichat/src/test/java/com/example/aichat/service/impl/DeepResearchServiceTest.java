package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.dto.search.DeepResearchReport;
import com.example.aichat.entity.SearchResult;
import com.example.aichat.service.DeepResearchService;
import com.example.aichat.service.WebSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * 深度研究服务离线单元测试（mock WebSearchService，不触网）。
 */
@ExtendWith(MockitoExtension.class)
class DeepResearchServiceTest {

    @Mock
    private WebSearchService webSearchService;

    @InjectMocks
    private DeepResearchServiceImpl service;

    @BeforeEach
    void setUp() {
        // mock 搜索：无论查询词都返回固定的若干结果
        lenient().when(webSearchService.search(anyString(), anyInt())).thenReturn(sampleResults());
    }

    private SearchResult result(String title, String url, String snippet, String source) {
        SearchResult r = new SearchResult();
        r.setTitle(title);
        r.setUrl(url);
        r.setSnippet(snippet);
        r.setSource(source);
        r.setRelevanceScore(0.9);
        return r;
    }

    private List<SearchResult> sampleResults() {
        List<SearchResult> list = new ArrayList<>();
        list.add(result("人工智能综述", "https://a.com/1",
                "人工智能是研究机器学习与深度学习的交叉学科，广泛应用于医疗与金融。", "baidu"));
        list.add(result("深度学习入门", "https://b.com/2",
                "深度学习基于神经网络，通过反向传播算法进行模型训练。", "bing"));
        list.add(result("神经网络原理", "https://c.com/3",
                "神经网络由神经元分层连接，具备强大的特征提取与表示学习能力。", "duck"));
        return list;
    }

    @Test
    void startResearch_返回有效researchId() {
        String id = service.startResearch(1L, "人工智能", 3);
        assertNotNull(id);
        assertFalse(id.isEmpty());
    }

    @Test
    void getResearchStatus_状态流转到完成() {
        String id = service.startResearch(1L, "人工智能", 3);
        Map<String, Object> status = service.getResearchStatus(1L, id);
        assertEquals("completed", status.get("status"));
        assertEquals(100, status.get("progress"));
        assertEquals(3, status.get("maxRounds"));
        assertNotNull(status.get("currentRound"));
        assertNotNull(status.get("createdAt"));
        assertNotNull(status.get("completedAt"));
    }

    @Test
    void getResearchResult_报告结构完整() {
        String id = service.startResearch(1L, "人工智能", 3);
        DeepResearchReport report = service.getResearchResult(1L, id);
        assertNotNull(report);
        assertEquals("人工智能", report.getQuery());
        assertNotNull(report.getSummary());
        assertFalse(report.getSummary().isEmpty());
        assertNotNull(report.getKeyFindings());
        assertFalse(report.getKeyFindings().isEmpty());
        assertNotNull(report.getSources());
        assertFalse(report.getSources().isEmpty());
        // 来源按 url 去重后应连续编号
        for (int i = 0; i < report.getSources().size(); i++) {
            assertEquals(i + 1, report.getSources().get(i).getIndex());
        }
        assertNotNull(report.getRoundDetails());
        assertFalse(report.getRoundDetails().isEmpty());
        assertNotNull(report.getCreatedAt());
        assertNotNull(report.getCompletedAt());
    }

    @Test
    void 多轮循环_maxRounds1_仅一轮() {
        String id = service.startResearch(1L, "人工智能", 1);
        DeepResearchReport report = service.getResearchResult(1L, id);
        // 只有第1轮原始查询
        assertEquals(1, report.getRoundDetails().size());
        assertEquals(1, report.getRoundDetails().get(0).getRound());
    }

    @Test
    void 多轮循环_maxRounds2_三轮详情() {
        String id = service.startResearch(1L, "人工智能", 2);
        DeepResearchReport report = service.getResearchResult(1L, id);
        // 第1轮 + 第2轮两个子查询 = 3 条轮次详情
        assertEquals(3, report.getRoundDetails().size());
        // topTitles 最多 3 条
        assertTrue(report.getRoundDetails().get(0).getTopTitles().size() <= 3);
    }

    @Test
    void 多轮循环_maxRounds3_五轮详情() {
        String id = service.startResearch(1L, "人工智能", 3);
        DeepResearchReport report = service.getResearchResult(1L, id);
        // 第1轮 + 第2轮2子查询 + 第3轮2子查询 = 5 条轮次详情
        assertEquals(5, report.getRoundDetails().size());
    }

    @Test
    void listResearches_分页() {
        service.startResearch(1L, "问题一", 1);
        service.startResearch(1L, "问题二", 1);
        service.startResearch(1L, "问题三", 1);
        List<DeepResearchReport> page1 = service.listResearches(1L, 1, 2);
        assertEquals(2, page1.size());
        List<DeepResearchReport> page2 = service.listResearches(1L, 1, 2);
        // 新的在前
        assertFalse(page1.isEmpty());
        List<DeepResearchReport> all = service.listResearches(1L, 1, 10);
        assertEquals(3, all.size());
        // 越界页码返回空
        assertTrue(service.listResearches(1L, 99, 2).isEmpty());
        assertNotNull(page2);
    }

    @Test
    void 非所有者访问被拒_IDOR() {
        String id = service.startResearch(1L, "私密研究", 3);
        // 用户2访问用户1的研究任务，应抛业务异常
        assertThrows(BusinessException.class, () -> service.getResearchResult(2L, id));
        assertThrows(BusinessException.class, () -> service.getResearchStatus(2L, id));
    }

    @Test
    void 空query处理_抛非法参数() {
        assertThrows(IllegalArgumentException.class, () -> service.startResearch(1L, "  ", 3));
        assertThrows(IllegalArgumentException.class, () -> service.startResearch(1L, "", 3));
    }

    // ============ A8: SSE ProgressListener 流式进度推送 ============

    @Test
    void startResearchWithListener_推送进度事件() {
        java.util.List<String> events = new java.util.ArrayList<>();
        java.util.List<Object> dataList = new java.util.ArrayList<>();

        DeepResearchService.ProgressListener listener = (eventName, data) -> {
            events.add(eventName);
            dataList.add(data);
        };

        String id = service.startResearchWithListener(1L, "人工智能", 3, listener);
        assertNotNull(id);

        // 应至少推送 start、多个 round、roundResult、integrating、complete 事件
        assertTrue(events.contains("start"), "应有 start 事件");
        assertTrue(events.contains("complete"), "应有 complete 事件");
        assertTrue(events.contains("round"), "应有 round 事件");
        assertTrue(events.contains("roundResult"), "应有 roundResult 事件");
        assertTrue(events.contains("integrating"), "应有 integrating 事件");

        // complete 事件的数据应是 DeepResearchReport
        int completeIdx = events.indexOf("complete");
        Object completeData = dataList.get(completeIdx);
        assertTrue(completeData instanceof DeepResearchReport);
    }

    @Test
    void startResearchWithListener_listener为null_不抛异常() {
        // listener 为 null 时应正常执行（向后兼容）
        String id = service.startResearchWithListener(1L, "测试问题", 2, null);
        assertNotNull(id);
        DeepResearchReport report = service.getResearchResult(1L, id);
        assertEquals("completed", service.getResearchStatus(1L, id).get("status"));
    }

    @Test
    void startResearchWithListener_listener抛异常_不影响研究() {
        // listener 抛异常时，研究流程不应中断
        DeepResearchService.ProgressListener badListener = (eventName, data) -> {
            throw new RuntimeException("SSE 客户端断开");
        };

        String id = service.startResearchWithListener(1L, "测试问题", 1, badListener);
        assertNotNull(id);
        // 研究应正常完成
        DeepResearchReport report = service.getResearchResult(1L, id);
        assertNotNull(report);
        assertEquals("completed", service.getResearchStatus(1L, id).get("status"));
    }
}

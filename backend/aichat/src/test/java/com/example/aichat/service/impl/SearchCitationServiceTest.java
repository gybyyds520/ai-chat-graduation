package com.example.aichat.service.impl;

import com.example.aichat.dto.search.CitedAnswer;
import com.example.aichat.dto.search.SearchResponse;
import com.example.aichat.dto.search.SourceCitation;
import com.example.aichat.entity.SearchResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 搜索结果引用标注服务离线单元测试（纯逻辑，不触网、不依赖 Spring）。
 */
class SearchCitationServiceTest {

    private final SearchCitationServiceImpl service = new SearchCitationServiceImpl();

    private SearchResult result(String title, String url, String snippet, String source) {
        SearchResult r = new SearchResult();
        r.setTitle(title);
        r.setUrl(url);
        r.setSnippet(snippet);
        r.setSource(source);
        r.setRelevanceScore(0.85);
        return r;
    }

    // ---------- buildCitations ----------

    @Test
    void buildCitations_正常映射且从1编号() {
        List<SearchResult> results = Arrays.asList(
                result("标题一", "https://a.com/1", "摘要一", "baidu"),
                result("标题二", "https://b.com/2", "摘要二", "bing"));
        List<SourceCitation> citations = service.buildCitations(results);
        assertEquals(2, citations.size());
        assertEquals(1, citations.get(0).getIndex());
        assertEquals(2, citations.get(1).getIndex());
        assertEquals("标题一", citations.get(0).getTitle());
        assertEquals("https://a.com/1", citations.get(0).getUrl());
        assertEquals("baidu", citations.get(0).getSource());
        assertEquals(0.85, citations.get(0).getRelevanceScore(), 0.001);
    }

    @Test
    void buildCitations_空列表返回空() {
        assertTrue(service.buildCitations(new ArrayList<>()).isEmpty());
    }

    @Test
    void buildCitations_null返回空() {
        assertTrue(service.buildCitations(null).isEmpty());
    }

    // ---------- annotateAnswerWithCitations ----------

    @Test
    void annotateAnswerWithCitations_命中句末插入引用() {
        SourceCitation c = SourceCitation.builder()
                .index(1).title("AI综述").url("https://a.com/1")
                .snippet("人工智能 机器学习 深度学习 神经网络").build();
        String answer = "人工智能在医疗领域有广泛应用。这是另一句无关的话。";
        String out = service.annotateAnswerWithCitations(answer, List.of(c));
        // 第一句命中，应插入 [1]
        assertTrue(out.contains("[1]"), "应插入引用序号: " + out);
        // 第二句无关不应再重复插入同一句
        assertTrue(out.startsWith("人工智能在医疗领域有广泛应用。[1]"));
    }

    @Test
    void annotateAnswerWithCitations_无匹配追加参考来源() {
        SourceCitation c = SourceCitation.builder()
                .index(1).title("AI综述").url("https://a.com/1")
                .snippet("人工智能 机器学习 深度学习").build();
        String answer = "今天天气很好我去公园散步骑车。";
        String out = service.annotateAnswerWithCitations(answer, List.of(c));
        assertTrue(out.contains("参考来源：[1]"), "无匹配应追加参考来源: " + out);
    }

    @Test
    void annotateAnswerWithCitations_空回答返回空() {
        SourceCitation c = SourceCitation.builder().index(1).snippet("人工智能").build();
        assertEquals("", service.annotateAnswerWithCitations("", List.of(c)));
    }

    @Test
    void annotateAnswerWithCitations_空引用原样返回() {
        String answer = "这是一段回答。";
        assertEquals(answer, service.annotateAnswerWithCitations(answer, new ArrayList<>()));
        assertEquals(answer, service.annotateAnswerWithCitations(answer, null));
    }

    // ---------- buildAnswerWithSources ----------

    @Test
    void buildAnswerWithSources_结构正确() {
        List<SearchResult> results = Arrays.asList(
                result("人工智能综述", "https://a.com/1", "人工智能 机器学习 深度学习", "baidu"),
                result("神经网络入门", "https://b.com/2", "神经网络 反向传播 深度学习", "bing"));
        CitedAnswer cited = service.buildAnswerWithSources("人工智能是研究机器学习的学科。", results);
        assertNotNull(cited.getAnswerWithCitations());
        assertEquals(2, cited.getCitations().size());
        // 回答与引用 snippet 高度重合，应插入引用
        assertTrue(cited.getAnswerWithCitations().contains("[1]")
                || cited.getAnswerWithCitations().contains("参考来源"), "应含引用标注: " + cited.getAnswerWithCitations());
    }

    // ---------- SearchResponse 结构 ----------

    @Test
    void searchResponse_结构含兼容字段与引用字段() {
        List<SearchResult> results = List.of(result("t", "https://a.com/1", "s", "baidu"));
        List<SourceCitation> citations = service.buildCitations(results);
        SearchResponse resp = SearchResponse.builder()
                .query("人工智能")
                .totalResults(results.size())
                .results(results)
                .citations(citations)
                .searchTime(123L)
                .build();
        assertEquals("人工智能", resp.getQuery());
        assertEquals(1, resp.getTotalResults());
        assertNotNull(resp.getResults());       // 兼容既有字段
        assertEquals(1, resp.getCitations().size());
        assertEquals(123L, resp.getSearchTime());
    }

    @Test
    void searchResponse_空结果仍可构造() {
        SearchResponse resp = SearchResponse.builder()
                .query("空")
                .totalResults(0)
                .results(new ArrayList<>())
                .citations(new ArrayList<>())
                .searchTime(0L)
                .build();
        assertNotNull(resp.getResults());
        assertTrue(resp.getCitations().isEmpty());
    }

    // ---------- A1: sources 字段 ----------

    @Test
    void toSourceMaps_应含titleUrlSnippet字段() {
        List<SourceCitation> citations = service.buildCitations(Arrays.asList(
                result("标题一", "https://a.com/1", "摘要一", "baidu"),
                result("标题二", "https://b.com/2", "摘要二", "bing")));
        List<java.util.Map<String, Object>> sources = service.toSourceMaps(citations);
        assertEquals(2, sources.size());
        // 字段格式正确：每个 source 含 index/title/url/snippet
        java.util.Map<String, Object> s0 = sources.get(0);
        assertEquals(1, s0.get("index"));
        assertEquals("标题一", s0.get("title"));
        assertEquals("https://a.com/1", s0.get("url"));
        assertEquals("摘要一", s0.get("snippet"));
        assertTrue(s0.containsKey("source"));
    }

    @Test
    void toSourceMaps_空结果返回空列表() {
        // 无搜索结果时 sources 为空，前端不渲染引用区
        assertTrue(service.toSourceMaps(service.buildCitations(new ArrayList<>())).isEmpty());
        assertTrue(service.toSourceMaps(null).isEmpty());
    }

    @Test
    void toSourceMaps_与citations一一对应() {
        List<SearchResult> results = Arrays.asList(
                result("t1", "https://a.com/1", "s1", "baidu"),
                result("t2", "https://b.com/2", "s2", "bing"),
                result("t3", "https://c.com/3", "s3", "google"));
        CitedAnswer cited = service.buildAnswerWithSources("任意回答文本。", results);
        List<java.util.Map<String, Object>> sources = service.toSourceMaps(cited.getCitations());
        // sources 条数与引用条数一致，序号连续
        assertEquals(cited.getCitations().size(), sources.size());
        assertEquals(1, sources.get(0).get("index"));
        assertEquals(3, sources.get(2).get("index"));
    }
}

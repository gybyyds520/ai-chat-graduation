package com.example.aichat.service.impl;

import com.example.aichat.dto.search.CitedAnswer;
import com.example.aichat.dto.search.SourceCitation;
import com.example.aichat.entity.SearchResult;
import com.example.aichat.service.SearchCitationService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 搜索结果引用标注服务默认实现。
 *
 * 纯离线逻辑：不依赖 LLM 与真实网络，关键词重叠基于中文 2-gram + 英文词切分。
 */
@Service
public class SearchCitationServiceImpl implements SearchCitationService {

    /**
     * 句子与引用 snippet 的关键词重叠率阈值，超过则在句末标注该引用。
     */
    private static final double OVERLAP_THRESHOLD = 0.2;

    /**
     * 句子切分正则：在句末标点后切分，保留标点。
     */
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[。！？；!?;\\.\\n])");

    /**
     * 中文（CJK）字符段。
     */
    private static final Pattern CJK_RUN = Pattern.compile("[\\u4e00-\\u9fff]+");

    /**
     * 英文/数字词段。
     */
    private static final Pattern WORD_RUN = Pattern.compile("[A-Za-z0-9]+");

    @Override
    public List<SourceCitation> buildCitations(List<SearchResult> results) {
        List<SourceCitation> citations = new ArrayList<>();
        if (results == null || results.isEmpty()) {
            return citations;
        }
        int index = 1;
        for (SearchResult r : results) {
            if (r == null) {
                continue;
            }
            citations.add(SourceCitation.builder()
                    .index(index++)
                    .title(r.getTitle())
                    .url(r.getUrl())
                    .snippet(r.getSnippet())
                    .source(r.getSource())
                    .publishTime(r.getPublishTime())
                    .relevanceScore(r.getRelevanceScore())
                    .build());
        }
        return citations;
    }

    @Override
    public String annotateAnswerWithCitations(String answer, List<SourceCitation> citations) {
        if (answer == null || answer.trim().isEmpty()) {
            return answer == null ? "" : answer;
        }
        if (citations == null || citations.isEmpty()) {
            return answer;
        }

        // 预计算每个 citation 的关键词集合
        List<Set<String>> citationKeywords = new ArrayList<>(citations.size());
        for (SourceCitation c : citations) {
            citationKeywords.add(extractKeywords(c.getSnippet()));
        }

        String[] sentences = SENTENCE_SPLIT.split(answer);
        StringBuilder annotated = new StringBuilder(answer.length() + 16 * citations.size());
        boolean anyCited = false;

        for (String sentence : sentences) {
            if (sentence == null || sentence.isEmpty()) {
                continue;
            }
            Set<String> sKeywords = extractKeywords(sentence);
            StringBuilder refs = new StringBuilder();
            if (!sKeywords.isEmpty()) {
                for (int i = 0; i < citations.size(); i++) {
                    double overlap = overlapRatio(sKeywords, citationKeywords.get(i));
                    if (overlap > OVERLAP_THRESHOLD) {
                        refs.append('[').append(citations.get(i).getIndex()).append(']');
                    }
                }
            }
            annotated.append(sentence);
            if (refs.length() > 0) {
                annotated.append(refs);
                anyCited = true;
            }
        }

        // 无任何句子命中：在末尾追加参考来源列表
        if (!anyCited) {
            annotated.append(answer.endsWith("\n") ? "" : "").append("参考来源：");
            for (int i = 0; i < citations.size(); i++) {
                if (i > 0) {
                    annotated.append(' ');
                }
                annotated.append('[').append(citations.get(i).getIndex()).append(']');
            }
        }

        return annotated.toString();
    }

    @Override
    public CitedAnswer buildAnswerWithSources(String answer, List<SearchResult> results) {
        List<SourceCitation> citations = buildCitations(results);
        String annotated = annotateAnswerWithCitations(answer, citations);
        return new CitedAnswer(annotated, citations);
    }

    @Override
    public List<Map<String, Object>> toSourceMaps(List<SourceCitation> citations) {
        List<Map<String, Object>> sources = new ArrayList<>();
        if (citations == null || citations.isEmpty()) {
            return sources;
        }
        for (SourceCitation c : citations) {
            if (c == null) {
                continue;
            }
            Map<String, Object> src = new LinkedHashMap<>();
            src.put("index", c.getIndex());
            src.put("title", c.getTitle());
            src.put("url", c.getUrl());
            src.put("snippet", c.getSnippet());
            src.put("source", c.getSource());
            sources.add(src);
        }
        return sources;
    }

    /**
     * 计算句子关键词集合相对引用关键词集合的重叠率。
     *
     * @param sentenceKw 句子关键词集合
     * @param citationKw 引用 snippet 关键词集合
     * @return 重叠率 [0,1]
     */
    private double overlapRatio(Set<String> sentenceKw, Set<String> citationKw) {
        if (sentenceKw.isEmpty() || citationKw.isEmpty()) {
            return 0.0;
        }
        int inter = 0;
        for (String kw : sentenceKw) {
            if (citationKw.contains(kw)) {
                inter++;
            }
        }
        return (double) inter / sentenceKw.size();
    }

    /**
     * 从文本提取关键词：中文按 2-gram 切分，英文/数字按词切分。
     */
    Set<String> extractKeywords(String text) {
        Set<String> keywords = new HashSet<>();
        if (text == null || text.isEmpty()) {
            return keywords;
        }
        // 中文 2-gram
        Matcher cjk = CJK_RUN.matcher(text);
        while (cjk.find()) {
            String run = cjk.group();
            for (int i = 0; i + 2 <= run.length(); i++) {
                keywords.add(run.substring(i, i + 2));
            }
        }
        // 英文/数字词（小写，长度>=2）
        Matcher word = WORD_RUN.matcher(text);
        while (word.find()) {
            String w = word.group().toLowerCase();
            if (w.length() >= 2) {
                keywords.add(w);
            }
        }
        return keywords;
    }
}

package com.example.aichat.service;

import java.util.List;
import java.util.Map;

public interface ExternalApiService {

    Map<String, Object> healthCheck();

    List<Map<String, Object>> getAvailableProviders();

    Map<String, Object> proxyRequest(String method, String url, Map<String, String> headers, Object body, int timeout);

    Map<String, Object> aiChat(String provider, String model, String prompt, String systemPrompt, boolean stream);

    Map<String, Object> aiCompletions(String provider, String model, String prompt, Integer maxTokens, Double temperature);

    List<Map<String, Object>> getProviderModels(String provider);

    Map<String, Object> aiEmbeddings(String provider, String model, List<String> texts);

    Map<String, Object> fetchUrl(String url, int timeout, boolean parse);

    byte[] downloadFile(String url);

    Map<String, Object> webSearch(String query, String engine, Integer maxResults);

    Map<String, Object> getSystemStatus();
}
package com.example.aichat.service;

import java.util.List;
import java.util.Map;

public interface ChatDataParserService {
    List<Map<String, Object>> parseTxt(String content);
    List<Map<String, Object>> parseJson(String content);
    List<Map<String, Object>> parse(String content, String format);
}
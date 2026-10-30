package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.entity.ToolCallLog;
import com.example.aichat.event.UserDataCaptureEvent;
import com.example.aichat.mapper.ToolCallLogMapper;
import com.example.aichat.service.ToolCallService;
import com.example.aichat.service.tool.Tool;
import com.example.aichat.service.tool.ToolExecutor;
import com.example.aichat.service.tool.ToolRegistry;
import com.example.aichat.service.tool.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ToolCallServiceImpl implements ToolCallService {

    private static final Logger logger = LoggerFactory.getLogger(ToolCallServiceImpl.class);

    private final ToolExecutor toolExecutor;
    private final ToolRegistry toolRegistry;
    private final ToolCallLogMapper toolCallLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToolCallServiceImpl(ToolExecutor toolExecutor,
                              ToolRegistry toolRegistry,
                              ToolCallLogMapper toolCallLogMapper) {
        this.toolExecutor = toolExecutor;
        this.toolRegistry = toolRegistry;
        this.toolCallLogMapper = toolCallLogMapper;
    }

    /** 数据池自动采集事件发布器。 */
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public ToolResult callTool(Long userId, String toolName, Map<String, Object> parameters) {
        long start = System.currentTimeMillis();
        ToolResult result = toolExecutor.execute(toolName, parameters);
        long duration = System.currentTimeMillis() - start;
        try {
            saveLog(userId, toolName, parameters, result, duration);
        } catch (Exception e) {
            // 日志失败不应影响工具返回
            logger.warn("记录工具调用日志失败: {}", e.getMessage());
        }

        // 数据池自动采集：工具调用名称 + 参数摘要（仅参数键，不记录参数值）
        if (eventPublisher != null && userId != null) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("toolName", toolName);
            if (parameters != null) {
                meta.put("paramKeys", new ArrayList<>(parameters.keySet()));
            }
            eventPublisher.publishEvent(new UserDataCaptureEvent(
                    userId, "tool_call", "tool_call:" + toolName, meta,
                    "tool:" + toolName + ":" + duration, LocalDateTime.now()));
        }
        return result;
    }

    private void saveLog(Long userId, String toolName, Map<String, Object> parameters,
                         ToolResult result, long durationMs) {
        ToolCallLog log = new ToolCallLog();
        log.setUserId(userId);
        log.setToolName(toolName);
        try {
            log.setParameters(parameters == null ? "{}" : objectMapper.writeValueAsString(parameters));
        } catch (Exception e) {
            log.setParameters("{}");
        }
        try {
            log.setResult(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.setResult("{}");
        }
        log.setSuccess(result.isSuccess());
        log.setDurationMs(durationMs);
        log.setCreatedAt(LocalDateTime.now());
        toolCallLogMapper.insert(log);
    }

    @Override
    public List<Map<String, Object>> listAvailableTools() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Tool tool : toolRegistry.listAvailableTools()) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("name", tool.getName());
            meta.put("description", tool.getDescription());
            meta.put("parameters", tool.getParameters());
            list.add(meta);
        }
        return list;
    }

    @Override
    public IPage<ToolCallLog> getToolCallHistory(Long userId, int page, int size) {
        Page<ToolCallLog> pageParam = new Page<>(page, size);
        QueryWrapper<ToolCallLog> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("created_at");
        return toolCallLogMapper.selectPage(pageParam, wrapper);
    }
}

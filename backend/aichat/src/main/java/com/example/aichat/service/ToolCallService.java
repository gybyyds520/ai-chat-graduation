package com.example.aichat.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.aichat.entity.ToolCallLog;
import com.example.aichat.service.tool.ToolResult;

import java.util.List;
import java.util.Map;

/**
 * 工具调用服务：执行工具调用、记录调用日志、查询可用工具与调用历史。
 */
public interface ToolCallService {

    /**
     * 执行一次工具调用并记录日志。
     */
    ToolResult callTool(Long userId, String toolName, Map<String, Object> parameters);

    /**
     * 列出所有可用工具（名称 / 描述 / 参数定义）。
     */
    List<Map<String, Object>> listAvailableTools();

    /**
     * 分页查询某用户的工具调用历史。
     */
    IPage<ToolCallLog> getToolCallHistory(Long userId, int page, int size);
}

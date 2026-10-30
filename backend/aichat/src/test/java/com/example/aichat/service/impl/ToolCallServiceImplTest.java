package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.aichat.entity.ToolCallLog;
import com.example.aichat.mapper.ToolCallLogMapper;
import com.example.aichat.service.tool.Tool;
import com.example.aichat.service.tool.ToolExecutor;
import com.example.aichat.service.tool.ToolRegistry;
import com.example.aichat.service.tool.ToolResult;
import com.example.aichat.service.tool.impl.CalculatorTool;
import com.example.aichat.service.tool.impl.TimeTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ToolCallService 测试：callTool 记录日志、listAvailableTools、历史分页。
 */
@ExtendWith(MockitoExtension.class)
class ToolCallServiceImplTest {

    @Mock
    private ToolCallLogMapper toolCallLogMapper;

    private ToolCallServiceImpl service;

    @BeforeEach
    void setUp() {
        ToolRegistry registry = new ToolRegistry(List.of(new TimeTool(), new CalculatorTool()));
        registry.init();
        ToolExecutor executor = new ToolExecutor(registry);
        service = new ToolCallServiceImpl(executor, registry, toolCallLogMapper);
    }

    @Test
    void callTool_应执行并记录日志() {
        ToolResult r = service.callTool(1L, "calculator", Map.of("expression", "1+1"));
        assertTrue(r.isSuccess());

        ArgumentCaptor<ToolCallLog> captor = ArgumentCaptor.forClass(ToolCallLog.class);
        verify(toolCallLogMapper).insert(captor.capture());
        ToolCallLog log = captor.getValue();
        assertEquals(1L, log.getUserId());
        assertEquals("calculator", log.getToolName());
        assertTrue(log.getSuccess());
        assertNotNull(log.getCreatedAt());
    }

    @Test
    void callTool_不存在工具也应记录失败日志() {
        ToolResult r = service.callTool(2L, "ghost", Map.of());
        assertFalse(r.isSuccess());
        verify(toolCallLogMapper).insert(any(ToolCallLog.class));
    }

    @Test
    void listAvailableTools_应返回名称描述参数() {
        List<Map<String, Object>> tools = service.listAvailableTools();
        assertEquals(2, tools.size());
        for (Map<String, Object> t : tools) {
            assertNotNull(t.get("name"));
            assertNotNull(t.get("description"));
            assertNotNull(t.get("parameters"));
        }
    }

    @Test
    void getToolCallHistory_应按用户分页查询() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ToolCallLog> empty =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        when(toolCallLogMapper.selectPage(any(), any())).thenReturn(empty);

        IPage<ToolCallLog> page = service.getToolCallHistory(7L, 1, 10);
        assertNotNull(page);
        verify(toolCallLogMapper).selectPage(any(), any());
    }
}

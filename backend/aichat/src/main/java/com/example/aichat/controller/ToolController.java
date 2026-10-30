package com.example.aichat.controller;

import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.service.ToolCallService;
import com.example.aichat.service.tool.ToolResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 工具调用接口（全部要求登录）。
 */
@RestController
@RequestMapping("/api/tool")
public class ToolController {

    private final ToolCallService toolCallService;

    public ToolController(ToolCallService toolCallService) {
        this.toolCallService = toolCallService;
    }

    /** 可用工具列表。 */
    @GetMapping("/list")
    public R list() {
        AuthContext.requireUserId();
        return R.success(toolCallService.listAvailableTools());
    }

    /** 执行工具调用。body: { toolName, parameters }。 */
    @PostMapping("/call")
    public R call(@RequestBody Map<String, Object> body) {
        Long userId = AuthContext.requireUserId();
        String toolName = body.get("toolName") == null ? null : body.get("toolName").toString();
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) body.get("parameters");
        ToolResult result = toolCallService.callTool(userId, toolName, parameters);
        return R.success(result);
    }

    /** 调用历史（分页）。 */
    @GetMapping("/history")
    public R history(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(defaultValue = "20") int size) {
        Long userId = AuthContext.requireUserId();
        return R.success(toolCallService.getToolCallHistory(userId, page, size));
    }
}

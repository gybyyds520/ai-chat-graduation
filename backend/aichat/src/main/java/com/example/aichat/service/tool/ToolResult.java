package com.example.aichat.service.tool;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 工具执行结果。
 */
@Data
@AllArgsConstructor
public class ToolResult {

    /** 是否执行成功。 */
    private boolean success;

    /** 成功时的结果数据。 */
    private Object data;

    /** 失败时的错误信息（不回显内部敏感细节）。 */
    private String error;

    public static ToolResult success(Object data) {
        return new ToolResult(true, data, null);
    }

    public static ToolResult failure(String error) {
        return new ToolResult(false, null, error);
    }
}

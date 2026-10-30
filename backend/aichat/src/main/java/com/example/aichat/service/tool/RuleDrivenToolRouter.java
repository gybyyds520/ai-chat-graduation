package com.example.aichat.service.tool;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则驱动的工具调用路由器（Ollama 不支持 Function Calling 时的降级方案）。
 *
 * 通过关键词正则检测用户输入，命中后构造对应的工具名与参数，
 * 供对话流程在拿到 LLM 回复前先执行工具、再回填结果。
 *
 * 支持：
 *   - 搜索 / 查一下 / 搜一下 -> web_search
 *   - 现在几点 / 现在时间 / 几点了 -> get_current_time
 *   - 计算 / 算一下 / 含数学表达式 -> calculator
 *   - 我的好友 / 我的朋友 / 好友列表 -> query_user_friends
 */
@Component
public class RuleDrivenToolRouter {

    private static final Pattern SEARCH_PATTERN =
            Pattern.compile("(搜索|搜一下|查一下|查找|百度一下|google一下)\\s*(.+)?");
    private static final Pattern TIME_PATTERN =
            Pattern.compile("(现在几点|现在时间|几点了|当前时间|现在日期|今天几号)");
    private static final Pattern CALC_PATTERN =
            Pattern.compile("(计算|算一下|帮我算|等于多少)\\s*(.+)?");
    private static final Pattern FRIENDS_PATTERN =
            Pattern.compile("(我的好友|我的朋友|好友列表|朋友列表|我有哪些朋友|我有多少好友)");
    /** 裸数学表达式（如 1+2*3），无需"计算"前缀也可触发。 */
    private static final Pattern BARE_MATH_PATTERN =
            Pattern.compile("^\\s*[\\d\\+\\-\\*\\/\\(\\)\\.\\s]+[\\+\\-\\*\\/][\\d\\+\\-\\*\\/\\(\\)\\.\\s]+\\s*$");

    /**
     * 尝试从用户输入中识别工具调用意图。
     *
     * @param userInput 用户原始消息
     * @param userId    当前用户 ID（用于需要用户上下文的工具）
     * @return 命中时返回 ToolInvocation；未命中返回 empty
     */
    public Optional<ToolInvocation> detect(String userInput, Long userId) {
        if (userInput == null || userInput.isBlank()) {
            return Optional.empty();
        }
        String text = userInput.trim();

        // 1. 好友列表
        if (FRIENDS_PATTERN.matcher(text).find()) {
            Map<String, Object> params = new HashMap<>();
            if (userId != null) {
                params.put("userId", userId);
            }
            return Optional.of(new ToolInvocation("query_user_friends", params));
        }

        // 2. 当前时间
        if (TIME_PATTERN.matcher(text).find()) {
            return Optional.of(new ToolInvocation("get_current_time", new HashMap<>()));
        }

        // 3. 网络搜索（必须在计算之前，避免"搜一下 1+1"被误判）
        Matcher sm = SEARCH_PATTERN.matcher(text);
        if (sm.find()) {
            String keyword = sm.group(2);
            if (keyword == null || keyword.isBlank()) {
                // 仅"搜索"二字，无后续关键词
                return Optional.empty();
            }
            Map<String, Object> params = new HashMap<>();
            params.put("query", keyword.trim());
            return Optional.of(new ToolInvocation("web_search", params));
        }

        // 4. 计算（带"计算"前缀 或 裸数学表达式）
        Matcher cm = CALC_PATTERN.matcher(text);
        String expr = null;
        if (cm.find()) {
            expr = cm.group(2);
        } else if (BARE_MATH_PATTERN.matcher(text).matches()) {
            expr = text;
        }
        if (expr != null && !expr.isBlank()) {
            Map<String, Object> params = new HashMap<>();
            params.put("expression", expr.trim());
            return Optional.of(new ToolInvocation("calculator", params));
        }

        return Optional.empty();
    }

    /** 一次规则匹配出的工具调用意图。 */
    public record ToolInvocation(String toolName, Map<String, Object> parameters) {
    }
}

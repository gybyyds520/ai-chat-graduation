package com.example.aichat.util;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 用户数据采集规则工具（纯静态、无 Spring 依赖，便于单元测试）。
 *
 * <p>集中维护：合法 dataType 枚举、隐私脱敏算法、完整性评分模型。
 * 监听器与规则校验测试均调用本类，保证规则口径唯一。</p>
 */
public final class UserDataCaptureRules {

    private UserDataCaptureRules() {
    }

    /** 单次采集内容最大长度，超出截断。 */
    public static final int MAX_CONTENT_LENGTH = 10000;

    /** 合法 dataType 白名单（snake_case，模块_行为）。 */
    private static final Set<String> ALLOWED_DATA_TYPES = new HashSet<>(Arrays.asList(
            "social_post",        // 社群帖子
            "social_comment",     // 评论
            "social_feed",        // 动态
            "private_chat",       // 私聊
            "group_chat",         // 群聊
            "ai_chat",            // AI 对话用户消息
            "ai_character",       // AI 角色创建
            "ai_elf_interaction", // 精灵互动
            "ai_memory",          // AI 记忆
            "knowledge_query",    // 知识库查询
            "tool_call",          // 工具调用
            "web_search"          // 网页搜索
    ));

    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    // 用数字环视而非 \b，避免与中文相邻时 Java \b 边界判断失效
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(?<![0-9])[0-9]{17}[0-9Xx](?![0-9])");

    /** 是否为合法 dataType。 */
    public static boolean isValidDataType(String dataType) {
        return dataType != null && ALLOWED_DATA_TYPES.contains(dataType);
    }

    /**
     * 隐私脱敏：手机号 -> ***；邮箱 -> ***@***；身份证号 -> **********。
     * 其他敏感信息（密码等）在采集接入点即不采集，不进入本方法。
     */
    public static String sanitize(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        // 顺序：先身份证（18 位），再手机号，再邮箱。
        // 避免手机号正则把身份证号中的一段误判为手机号、破坏 18 位身份证的整体匹配。
        String result = ID_CARD_PATTERN.matcher(content).replaceAll("**********");
        result = PHONE_PATTERN.matcher(result).replaceAll("***");
        result = EMAIL_PATTERN.matcher(result).replaceAll("***@***");
        return result;
    }

    /**
     * 内容超长截断。
     *
     * @return 截断后的内容；入参为 null 原样返回
     */
    public static String truncateIfNeeded(String content) {
        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            return content.substring(0, MAX_CONTENT_LENGTH);
        }
        return content;
    }

    /**
     * 完整性评分（0-100）。
     *
     * <ul>
     *   <li>内容非空 40 分</li>
     *   <li>metadata 完整（非空且不为 "{}"）20 分</li>
     *   <li>tags 非空 10 分</li>
     *   <li>内容长度合理（>=10 字符）10 分</li>
     *   <li>dataType 合法 10 分</li>
     *   <li>captureTime 非空 10 分</li>
     * </ul>
     */
    public static int score(String content, String metadata, String tags,
                            String dataType, LocalDateTime captureTime) {
        int total = 0;

        if (content != null && !content.isBlank()) {
            total += 40;
        }
        if (metadata != null && !metadata.isBlank() && !"{}".equals(metadata)) {
            total += 20;
        }
        if (tags != null && !tags.isBlank()) {
            total += 10;
        }
        if (content != null && content.length() >= 10) {
            total += 10;
        }
        if (isValidDataType(dataType)) {
            total += 10;
        }
        if (captureTime != null) {
            total += 10;
        }
        return total;
    }
}

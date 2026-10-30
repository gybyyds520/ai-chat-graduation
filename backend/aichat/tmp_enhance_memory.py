# -*- coding: utf-8 -*-
"""AI 记忆完善：搜索/自动提炼（后端 Service 接口+实现+Controller）"""
import io

BASE = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat\src\main\java\com\example\aichat'

# ---------- 1. AiMemoryService.java ----------
p = BASE + r'\service\AiMemoryService.java'
src = io.open(p, encoding='utf-8').read()
anchor = '    /** 标记记忆已被使用：批量更新 lastUsedAt 为当前时间（仅本人记忆）。 */'
addition = '''    /**
     * 从用户聊天消息中自动提炼长期记忆（规则式）并去重落库。
     * 匹配"我叫/我喜欢/我讨厌/我住在/我今年X岁/生日/职业"等模式，
     * 提炼内容写入 ai_memory（source=chat），相同或高度相似内容自动跳过。
     * 返回本次新增记忆条数。
     */
    int extractMemoriesFromMessage(Long userId, String message);

'''
assert anchor in src, 'anchor not found in AiMemoryService'
src = src.replace(anchor, addition + anchor, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(src)
print('AiMemoryService.java OK')

# ---------- 2. AiMemoryServiceImpl.java ----------
p = BASE + r'\service\impl\AiMemoryServiceImpl.java'
src = io.open(p, encoding='utf-8').read()

old_list = '''    @Override
    public IPage<AiMemory> listMemories(Long userId, int page, int size,
                                        String memoryType, String category, Boolean isActive) {
        int p = page < 1 ? 1 : page;
        int s = size < 1 ? 10 : Math.min(size, 100);
        Page<AiMemory> pageParam = new Page<>(p, s);
        QueryWrapper<AiMemory> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (memoryType != null && !memoryType.isBlank()) {
            wrapper.eq("memory_type", memoryType);
        }
        if (category != null && !category.isBlank()) {
            wrapper.eq("category", category);
        }
        if (isActive != null) {
            wrapper.eq("is_active", isActive);
        }
        wrapper.orderByDesc("is_active").orderByDesc("created_at");
        return aiMemoryMapper.selectPage(pageParam, wrapper);
    }'''
new_list = '''    @Override
    public IPage<AiMemory> listMemories(Long userId, int page, int size,
                                        String memoryType, String category, Boolean isActive,
                                        String keyword, String source) {
        int p = page < 1 ? 1 : page;
        int s = size < 1 ? 10 : Math.min(size, 100);
        Page<AiMemory> pageParam = new Page<>(p, s);
        QueryWrapper<AiMemory> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (memoryType != null && !memoryType.isBlank()) {
            wrapper.eq("memory_type", memoryType);
        }
        if (category != null && !category.isBlank()) {
            wrapper.eq("category", category);
        }
        if (isActive != null) {
            wrapper.eq("is_active", isActive);
        }
        if (source != null && !source.isBlank()) {
            wrapper.eq("source", source);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("content", keyword.trim());
        }
        wrapper.orderByDesc("is_active").orderByDesc("importance").orderByDesc("created_at");
        return aiMemoryMapper.selectPage(pageParam, wrapper);
    }'''
assert old_list in src, 'listMemories impl not found'
src = src.replace(old_list, new_list, 1)

extract_code = '''
    // ---------- 自动记忆提炼（规则式，聊天消息 → ai_memory） ----------

    private static final Pattern NAME_PATTERN = Pattern.compile("(?:我叫|我的名字叫|本人叫|本名)([^，。！？\\\\s、]{1,20})");
    private static final Pattern LIKE_PATTERN = Pattern.compile("(?:我喜欢|我爱|我最喜欢|我特别喜欢)([^，。！？\\\\s]{1,30})");
    private static final Pattern DISLIKE_PATTERN = Pattern.compile("(?:我不喜欢|我讨厌|我最讨厌)([^，。！？\\\\s]{1,30})");
    private static final Pattern LIVE_PATTERN = Pattern.compile("(?:我住在|我现居|我居住于|我的家在|我家在)([^，。！？\\\\s]{1,20})");
    private static final Pattern AGE_PATTERN = Pattern.compile("(?:我今年)?([1-9][0-9]{0,2})岁");
    private static final Pattern BIRTHDAY_PATTERN = Pattern.compile("(?:我的生日|我生日|生日是|出生于|出生在)([0-9]{4}[-/年][0-9]{1,2}[-/月][0-9]{1,2}日?)");
    private static final Pattern JOB_PATTERN = Pattern.compile("(?:我的职业是|我是一名|我是做|我从事|我的工作是)([^，。！？\\\\s]{1,20})");

    @Override
    public int extractMemoriesFromMessage(Long userId, String message) {
        if (message == null || message.isBlank()) {
            return 0;
        }
        String text = message.trim();
        int added = 0;
        // 事实/身份类
        added += extractIfMatched(userId, text, NAME_PATTERN, "我叫%s", "fact", 5, "身份");
        added += extractIfMatched(userId, text, BIRTHDAY_PATTERN, "生日：%s", "event", 5, "身份");
        added += extractIfMatched(userId, text, AGE_PATTERN, "年龄：%s岁", "fact", 4, "身份");
        added += extractIfMatched(userId, text, JOB_PATTERN, "职业：%s", "fact", 4, "身份");
        added += extractIfMatched(userId, text, LIVE_PATTERN, "居住地：%s", "fact", 4, "身份");
        // 偏好类
        added += extractIfMatched(userId, text, LIKE_PATTERN, "我喜欢%s", "preference", 4, "偏好");
        added += extractIfMatched(userId, text, DISLIKE_PATTERN, "我不喜欢%s", "preference", 3, "偏好");
        if (added > 0) {
            logger.info("[AI记忆] 用户 {} 自动提炼新增 {} 条记忆", userId, added);
        }
        return added;
    }

    private int extractIfMatched(Long userId, String text, Pattern pattern,
                                 String format, String memoryType, int importance, String category) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return 0;
        }
        String value = matcher.groupCount() >= 1 ? matcher.group(1) : matcher.group(0);
        if (value == null || value.isBlank()) {
            return 0;
        }
        String content = String.format(format, value.trim());
        if (hasSimilarMemory(userId, content)) {
            return 0;
        }
        createMemory(userId, content, "chat", memoryType, importance, category);
        return 1;
    }

    /** 去重：完全一致或前 12 字前缀相同的记忆视为重复，跳过。 */
    private boolean hasSimilarMemory(Long userId, String content) {
        QueryWrapper<AiMemory> exact = new QueryWrapper<>();
        exact.eq("user_id", userId).eq("content", content);
        Long exactCount = aiMemoryMapper.selectCount(exact);
        if (exactCount != null && exactCount > 0) {
            return true;
        }
        if (content.length() >= 12) {
            QueryWrapper<AiMemory> similar = new QueryWrapper<>();
            similar.eq("user_id", userId).likeRight("content", content.substring(0, 12));
            Long similarCount = aiMemoryMapper.selectCount(similar);
            if (similarCount != null && similarCount > 0) {
                return true;
            }
        }
        return false;
    }
}'''
marker = '    public void markMemoryUsed(Long userId, List<Long> memoryIds) {'
assert marker in src, 'markMemoryUsed not found'
idx = src.rfind(marker)
end_marker = '        }\n    }\n}'
assert end_marker in src[idx:], 'class end not found'
src = src[:idx] + src[idx:].replace(end_marker, '        }\n    }\n' + extract_code, 1)

# 确保 import Pattern/Matcher
if 'java.util.regex.Matcher' not in src:
    src = src.replace('import java.time.LocalDateTime;', 'import java.time.LocalDateTime;\nimport java.util.regex.Matcher;\nimport java.util.regex.Pattern;', 1)
io.open(p, 'w', encoding='utf-8', newline='').write(src)
print('AiMemoryServiceImpl.java OK')

# ---------- 3. AiMemoryController.java ----------
p = BASE + r'\controller\AiMemoryController.java'
src = io.open(p, encoding='utf-8').read()
old_ctrl = '''    @GetMapping("/list")
    public R list(@RequestParam(defaultValue = "1") int page,
                  @RequestParam(defaultValue = "10") int size,
                  @RequestParam(required = false) String memoryType,
                  @RequestParam(required = false) String category,
                  @RequestParam(required = false) Boolean isActive) {
        Long userId = AuthContext.requireUserId();
        IPage<AiMemory> result = aiMemoryService.listMemories(userId, page, size, memoryType, category, isActive);
        return R.success(result);
    }'''
new_ctrl = '''    @GetMapping("/list")
    public R list(@RequestParam(defaultValue = "1") int page,
                  @RequestParam(defaultValue = "10") int size,
                  @RequestParam(required = false) String memoryType,
                  @RequestParam(required = false) String category,
                  @RequestParam(required = false) Boolean isActive,
                  @RequestParam(required = false) String keyword,
                  @RequestParam(required = false) String source) {
        Long userId = AuthContext.requireUserId();
        IPage<AiMemory> result = aiMemoryService.listMemories(userId, page, size, memoryType, category, isActive, keyword, source);
        return R.success(result);
    }'''
assert old_ctrl in src, 'controller list not found'
src = src.replace(old_ctrl, new_ctrl, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(src)
print('AiMemoryController.java OK')

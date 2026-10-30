package com.example.aichat.util;

import com.example.aichat.entity.AiMemory;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 记忆上下文注入器单元测试（纯函数）。
 *
 * 覆盖：空列表返回空串、按类型描述、跳过空内容、正确包裹中文前缀。
 */
class MemoryContextInjectorTest {

    private AiMemory memory(Long id, String content, String type) {
        AiMemory m = new AiMemory();
        m.setId(id);
        m.setContent(content);
        m.setMemoryType(type);
        return m;
    }

    @Test
    void buildMemoryContextPrefix_空列表_返回空串() {
        assertEquals("", MemoryContextInjector.buildMemoryContextPrefix(null));
        assertEquals("", MemoryContextInjector.buildMemoryContextPrefix(Collections.emptyList()));
    }

    @Test
    void buildMemoryContextPrefix_正常记忆_生成中文前缀() {
        List<AiMemory> memories = Arrays.asList(
                memory(1L, "喜欢喝咖啡", "preference"),
                memory(2L, "生日是2000年5月20日", "event"),
                memory(3L, "性格开朗", "personality")
        );
        String prefix = MemoryContextInjector.buildMemoryContextPrefix(memories);
        assertTrue(prefix.contains("【关于用户的长期记忆】"));
        assertTrue(prefix.contains("[偏好] 喜欢喝咖啡"));
        assertTrue(prefix.contains("[事件] 生日是2000年5月20日"));
        assertTrue(prefix.contains("[性格] 性格开朗"));
        assertTrue(prefix.contains("请自然参考"));
    }

    @Test
    void buildMemoryContextPrefix_全为空内容_返回空串() {
        List<AiMemory> memories = Arrays.asList(
                memory(1L, null, "fact"),
                memory(2L, "   ", "preference")
        );
        assertEquals("", MemoryContextInjector.buildMemoryContextPrefix(memories));
    }

    @Test
    void buildMemoryContextPrefix_未知类型_归为其他() {
        List<AiMemory> memories = Collections.singletonList(memory(1L, "测试", "custom_type"));
        String prefix = MemoryContextInjector.buildMemoryContextPrefix(memories);
        assertTrue(prefix.contains("[其他] 测试"));
    }
}

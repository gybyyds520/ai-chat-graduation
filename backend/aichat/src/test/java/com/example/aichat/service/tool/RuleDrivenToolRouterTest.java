package com.example.aichat.service.tool;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RuleDrivenToolRouter 规则驱动工具意图识别测试。
 */
class RuleDrivenToolRouterTest {

    private final RuleDrivenToolRouter router = new RuleDrivenToolRouter();

    @Test
    void 搜索关键词应触发web_search() {
        Optional<RuleDrivenToolRouter.ToolInvocation> inv = router.detect("帮我搜索一下 今天天气", 1L);
        assertTrue(inv.isPresent());
        assertEquals("web_search", inv.get().toolName());
        assertTrue(inv.get().parameters().get("query").toString().contains("今天天气"));
    }

    @Test
    void 问时间应触发get_current_time() {
        assertTrue(router.detect("现在几点了", 1L).isPresent());
        assertEquals("get_current_time", router.detect("现在几点了", 1L).get().toolName());
        assertEquals("get_current_time", router.detect("现在时间是多少", 1L).get().toolName());
    }

    @Test
    void 计算表达式应触发calculator() {
        Optional<RuleDrivenToolRouter.ToolInvocation> inv = router.detect("帮我算一下 1+2*3", 1L);
        assertTrue(inv.isPresent());
        assertEquals("calculator", inv.get().toolName());
        assertTrue(inv.get().parameters().get("expression").toString().contains("1+2*3"));
    }

    @Test
    void 裸数学表达式应触发calculator() {
        Optional<RuleDrivenToolRouter.ToolInvocation> inv = router.detect("(2+3)*4", 1L);
        assertTrue(inv.isPresent());
        assertEquals("calculator", inv.get().toolName());
    }

    @Test
    void 问好友列表应触发query_user_friends() {
        Optional<RuleDrivenToolRouter.ToolInvocation> inv = router.detect("我的好友列表有谁", 42L);
        assertTrue(inv.isPresent());
        assertEquals("query_user_friends", inv.get().toolName());
        assertEquals(42L, inv.get().parameters().get("userId"));
    }

    @Test
    void 普通聊天不应触发任何工具() {
        assertTrue(router.detect("今天心情怎么样", 1L).isEmpty());
        assertTrue(router.detect("你好呀", 1L).isEmpty());
        assertTrue(router.detect("", 1L).isEmpty());
        assertTrue(router.detect(null, 1L).isEmpty());
    }
}

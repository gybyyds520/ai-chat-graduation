package com.example.aichat.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 记忆自动分类器纯函数测试（≥4 用例）。
 */
class MemoryClassifierTest {

    @Test
    void classify_含喜欢_归为偏好() {
        assertEquals(MemoryCategory.PREFERENCE, MemoryClassifier.classify("我喜欢喝咖啡"));
        assertEquals(MemoryCategory.PREFERENCE, MemoryClassifier.classify("他讨厌下雨天"));
        assertEquals(MemoryCategory.PREFERENCE, MemoryClassifier.classify("用户偏好深色模式"));
    }

    @Test
    void classify_含时间线词_归为事件() {
        assertEquals(MemoryCategory.EVENT, MemoryClassifier.classify("上次我们聊到毕设"));
        assertEquals(MemoryCategory.EVENT, MemoryClassifier.classify("昨天去了博物馆"));
        assertEquals(MemoryCategory.EVENT, MemoryClassifier.classify("我记得那天是周三"));
    }

    @Test
    void classify_含关系词_归为关系() {
        assertEquals(MemoryCategory.RELATIONSHIP, MemoryClassifier.classify("我的朋友小王在杭州"));
        assertEquals(MemoryCategory.RELATIONSHIP, MemoryClassifier.classify("家人周末聚餐"));
        assertEquals(MemoryCategory.RELATIONSHIP, MemoryClassifier.classify("同事下午开会"));
    }

    @Test
    void classify_其他_归为事实() {
        assertEquals(MemoryCategory.FACT, MemoryClassifier.classify("用户今年25岁"));
        assertEquals(MemoryCategory.FACT, MemoryClassifier.classify("住在福州市鼓楼区"));
    }

    @Test
    void classify_空内容_回退事实() {
        assertEquals(MemoryCategory.FACT, MemoryClassifier.classify(null));
        assertEquals(MemoryCategory.FACT, MemoryClassifier.classify("   "));
    }

    @Test
    void classify_偏好优先于事件() {
        // 同时命中偏好与事件关键词时，偏好优先级更高
        assertEquals(MemoryCategory.PREFERENCE,
                MemoryClassifier.classify("我记得我喜欢夏天"));
    }

    @Test
    void fromString_大小写不敏感_未知回退事实() {
        assertEquals(MemoryCategory.EVENT, MemoryCategory.fromString("event"));
        assertEquals(MemoryCategory.RELATIONSHIP, MemoryCategory.fromString("relationship"));
        assertEquals(MemoryCategory.FACT, MemoryCategory.fromString("unknown"));
        assertEquals(MemoryCategory.FACT, MemoryCategory.fromString(null));
    }
}

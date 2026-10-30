package com.example.aichat.service.impl;

import com.example.aichat.common.audit.SensitiveWordDictionary;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.SocialErrorCode;
import com.example.aichat.dto.audit.AuditResult;
import com.example.aichat.entity.ContentAuditLog;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.ContentAuditLogMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.ContentAuditService;
import com.example.aichat.service.MinorProtectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 内容审核端到端集成测试 — 验证"发帖带违规词 → 被拦截 → 抛 BUSINESS 异常"完整链路。
 *
 * 覆盖：
 * 1. 用户端发帖含敏感词 → audit 返回 reject → 调用方抛 CONTENT_VIOLATION；
 * 2. 正常内容 → audit 返回 pass → 不抛异常；
 * 3. emoji 插入字间绕过（如 色🍑情网站）→ 宽松匹配应拦截；
 * 4. emoji 替换字（如 色🍑网站）→ 记录已知限制（不保证拦截，文档化）；
 * 5. 审核日志在拦截时被记录（recordAuditLog 调用）；
 * 6. 私聊/群聊/评论 各内容类型均走同一审核入口。
 */
@ExtendWith(MockitoExtension.class)
class ContentAuditEndToEndTest {

    @Mock
    private ContentAuditLogMapper auditLogMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MinorProtectionService minorProtectionService;

    private ContentAuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new ContentAuditServiceImpl(auditLogMapper, new SensitiveWordDictionary(),
                userMapper, minorProtectionService);
    }

    // === 1. 用户端发帖含违规词 → 被拦截 ===

    @Test
    void 发帖含色情敏感词_应被拦截并抛异常() {
        String content = "快来这里有色情网站，资源丰富";
        AuditResult result = auditService.audit(content, "post", 1L);

        assertFalse(result.isPassed(), "含敏感词的帖子必须被拦截");
        assertTrue(result.getMatchedWords().contains("色情网站"));
        assertEquals("HIGH", result.getRiskLevel());

        // 模拟调用方（CommunityServiceImpl.createPost）的行为
        assertThrows(BusinessException.class, () -> {
            if (!result.isPassed()) {
                throw new BusinessException(SocialErrorCode.CONTENT_VIOLATION);
            }
        });
    }

    @Test
    void 评论含辱骂词_应被拦截() {
        String content = "你这个王八蛋，别说话了";
        AuditResult result = auditService.audit(content, "comment", 1L);
        assertFalse(result.isPassed());
        assertTrue(result.getMatchedWords().contains("王八蛋"));
    }

    @Test
    void 私聊含广告词_应被拦截() {
        String content = "想赚钱的加微信详聊";
        AuditResult result = auditService.audit(content, "private_chat", 1L);
        assertFalse(result.isPassed());
        assertTrue(result.getMatchedWords().contains("加微信"));
    }

    @Test
    void 群聊含敏感词_应被拦截() {
        String content = "这里有人散布反动宣传";
        AuditResult result = auditService.audit(content, "group_chat", 1L);
        assertFalse(result.isPassed());
        assertTrue(result.getMatchedWords().contains("反动宣传"));
    }

    // === 2. 正常内容 → 放行 ===

    @Test
    void 正常发帖内容_应放行() {
        String content = "今天天气真好，阳光明媚，适合出去散步";
        AuditResult result = auditService.audit(content, "post", 1L);
        assertTrue(result.isPassed());
        assertTrue(result.getMatchedWords().isEmpty());
    }

    // === 3. emoji 插入字间绕过 → 宽松匹配应拦截 ===

    @Test
    void emoji插入字间绕过_应被拦截() {
        // 宽松匹配允许字间插入 \p{S}（符号/emoji），所以 色🍑情网站 应被拦截
        String content = "色🍑情网站";
        AuditResult result = auditService.audit(content, "post", 1L);
        assertFalse(result.isPassed(),
                "emoji 插入字间属于符号插入，宽松匹配应拦截: " + result.getMatchedWords());
        assertTrue(result.getMatchedWords().contains("色情网站"));
    }

    @Test
    void emoji加标点组合绕过_应被拦截() {
        String content = "色·🍑·情·网·站";
        AuditResult result = auditService.audit(content, "post", 1L);
        assertFalse(result.isPassed(), "emoji+标点组合插入字间应被拦截");
    }

    // === 4. emoji 替换字 → 已知限制（文档化） ===

    @Test
    void emoji替换字_记录已知限制不崩溃() {
        // 用 emoji 替换"情"字：色🍑网站
        // 这属于语义替换绕过，当前宽松匹配无法识别（词表是精确字符匹配）。
        // 此用例仅验证不崩溃，不要求拦截——作为已知限制文档化。
        String content = "色🍑网站";
        assertDoesNotThrow(() -> auditService.audit(content, "post", 1L));
        // 不做断言是否通过——这是已知限制，未来可通过 ML 模型补充
    }

    // === 5. 拦截时审核日志被记录 ===

    @Test
    void 拦截内容时审核日志应被记录() {
        String content = "这里有色情网站";
        AuditResult result = auditService.audit(content, "post", 7L);
        auditService.recordAuditLog(7L, "post", 100L, content, result);

        // 验证 mapper.insert 被调用一次
        verify(auditLogMapper, times(1)).insert(any(ContentAuditLog.class));
    }

    @Test
    void 正常内容也记录审核日志() {
        String content = "今天天气不错";
        AuditResult result = auditService.audit(content, "post", 1L);
        auditService.recordAuditLog(1L, "post", null, content, result);
        verify(auditLogMapper, times(1)).insert(any(ContentAuditLog.class));
    }

    // === 6. 角色创建也走审核 ===

    @Test
    void 角色名称含敏感词_应被拦截() {
        String content = "这个角色的设定很好，但是描述里有色情网站";
        AuditResult result = auditService.audit(content, "ai_character", 1L);
        assertFalse(result.isPassed());
        assertTrue(result.getMatchedWords().contains("色情网站"));
    }
}

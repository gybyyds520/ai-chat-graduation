package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.common.audit.SensitiveWordDictionary;
import com.example.aichat.dto.audit.AuditResult;
import com.example.aichat.dto.audit.PageResult;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ContentAuditService 单元测试（Mockito 隔离 Mapper，使用真实内置词库）。
 *
 * 覆盖点：
 *   1. 正常内容通过；
 *   2-6. 各敏感类别命中并拦截（色情/暴力/政治敏感/辱骂/广告）；
 *   7. 敏感词在开头/中间/结尾均可匹配；
 *   8. 大小写不敏感；
 *   9. riskLevel 取最高等级；
 *  10. matchedWords 正确去重；
 *  11-12. 空/空白/null 内容通过；
 *  13. 超长内容不抛异常；
 *  14. 审核日志记录调用 mapper.insert；
 *  15. 分页查询返回结构；
 *  16. 统计返回结构。
 */
@ExtendWith(MockitoExtension.class)
class ContentAuditServiceTest {

    @Mock
    private ContentAuditLogMapper auditLogMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MinorProtectionService minorProtectionService;

    private ContentAuditService auditService;

    @BeforeEach
    void setUp() {
        // 使用真实内置词库，验证词表匹配逻辑；Mapper 仍 mock
        // 默认 userMapper.selectById 返回 null → isMinorUser=false，未成年加强词表不生效
        auditService = new ContentAuditServiceImpl(auditLogMapper, new SensitiveWordDictionary(),
                userMapper, minorProtectionService);
    }

    @Test
    void 正常内容应通过() {
        AuditResult r = auditService.audit("今天天气真好，一起去散步吧", "post", 1L);
        assertTrue(r.isPassed());
        assertTrue(r.getMatchedWords().isEmpty());
    }

    @Test
    void 色情类敏感词应拦截() {
        AuditResult r = auditService.audit("快来这里有色情网站", "post", 1L);
        assertFalse(r.isPassed());
        assertTrue(r.getMatchedWords().contains("色情网站"));
        assertEquals("HIGH", r.getRiskLevel());
    }

    @Test
    void 暴力类敏感词应拦截() {
        AuditResult r = auditService.audit("这段视频充满血腥暴力画面", "post", 1L);
        assertFalse(r.isPassed());
        assertTrue(r.getMatchedWords().contains("血腥暴力"));
        assertEquals("HIGH", r.getRiskLevel());
    }

    @Test
    void 政治敏感类敏感词应拦截() {
        AuditResult r = auditService.audit("发现有人散布反动宣传内容", "post", 1L);
        assertFalse(r.isPassed());
        assertTrue(r.getMatchedWords().contains("反动宣传"));
        assertEquals("HIGH", r.getRiskLevel());
    }

    @Test
    void 辱骂类敏感词应拦截() {
        AuditResult r = auditService.audit("你这个王八蛋", "comment", 1L);
        assertFalse(r.isPassed());
        assertTrue(r.getMatchedWords().contains("王八蛋"));
        assertEquals("MEDIUM", r.getRiskLevel());
    }

    @Test
    void 广告类敏感词应拦截() {
        AuditResult r = auditService.audit("想赚钱的加微信详聊", "private_chat", 1L);
        assertFalse(r.isPassed());
        assertTrue(r.getMatchedWords().contains("加微信"));
    }

    @Test
    void 敏感词在开头中间结尾均应命中() {
        assertTrue(auditService.audit("色情网站开头", "post", 1L).isPassed() == false);
        assertFalse(auditService.audit("开头 色情网站 结尾", "post", 1L).isPassed());
        assertFalse(auditService.audit("内容结尾色情网站", "post", 1L).isPassed());
    }

    @Test
    void 大小写不敏感应命中英文词() {
        AuditResult r = auditService.audit("点击VIAGRA试试", "post", 1L);
        assertFalse(r.isPassed());
        assertTrue(r.getMatchedWords().stream().anyMatch(w -> w.equalsIgnoreCase("viagra")));
        assertEquals("LOW", r.getRiskLevel());
    }

    @Test
    void 命中多类时风险等级取最高() {
        // 辱骂(MEDIUM) + 色情(HIGH) → HIGH
        AuditResult r = auditService.audit("你这个王八蛋快去色情网站", "post", 1L);
        assertFalse(r.isPassed());
        assertEquals("HIGH", r.getRiskLevel());
    }

    @Test
    void matchedWords应去重且正确() {
        AuditResult r = auditService.audit("色情网站色情网站", "post", 1L);
        assertEquals(1, r.getMatchedWords().size());
        assertTrue(r.getMatchedWords().contains("色情网站"));
    }

    @Test
    void 空白内容应通过() {
        assertTrue(auditService.audit("   ", "post", 1L).isPassed());
    }

    @Test
    void null内容应通过不抛异常() {
        assertTrue(auditService.audit(null, "post", 1L).isPassed());
    }

    @Test
    void 超长内容应正常处理不抛异常() {
        String longContent = "正常内容".repeat(500);
        AuditResult r = assertDoesNotThrow(() -> auditService.audit(longContent, "post", 1L));
        assertTrue(r.isPassed());
    }

    @Test
    void recordAuditLog应调用mapper插入() {
        AuditResult r = auditService.audit("这里有色情网站", "post", 7L);
        auditService.recordAuditLog(7L, "post", 100L, "这里有色情网站", r);
        verify(auditLogMapper).insert(any(ContentAuditLog.class));
    }

    @Test
    void getAuditLogs应返回分页结构() {
        Page<ContentAuditLog> page = new Page<>(1, 10);
        page.setTotal(25);
        page.setRecords(List.of(new ContentAuditLog()));
        when(auditLogMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<ContentAuditLog> result = auditService.getAuditLogs(1, 10, "HIGH", "post");

        assertEquals(25, result.getTotal());
        assertEquals(1, result.getRecords().size());
        verify(auditLogMapper).selectPage(any(Page.class), any());
    }

    @Test
    void getAuditStats应返回统计结构() {
        when(auditLogMapper.selectCount(any())).thenReturn(100L);

        Map<String, Object> stats = auditService.getAuditStats();

        assertEquals(100L, stats.get("total"));
        assertNotNull(stats.get("riskDistribution"));
        assertNotNull(stats.get("todayTotal"));
        assertNotNull(stats.get("blocked"));
    }

    // ===== Stage-1 P0 新增：词库扩库 + 未成年加强过滤 =====

    @Test
    void 内置词库应扩展至500以上() {
        SensitiveWordDictionary dict = new SensitiveWordDictionary();
        // 基线 23 → 本期扩库目标 500+
        assertTrue(dict.builtInSize() >= 500, "内置词库应≥500，实际=" + dict.builtInSize());
        assertFalse(dict.minorExtraEntries().isEmpty(), "未成年加强词表不应为空");
    }

    @Test
    void contains应O1命中已知敏感词与大小写不敏感() {
        SensitiveWordDictionary dict = new SensitiveWordDictionary();
        assertTrue(dict.containsWord("色情网站"));
        assertTrue(dict.containsWord("VIAGRA"));
        assertTrue(dict.containsWord("  王八蛋  "));
        assertFalse(dict.containsWord("今天天气很好"));
        assertFalse(dict.containsWord(null));
        assertFalse(dict.containsWord(""));
    }

    @Test
    void 成年用户未成年加强词表不应拦截() {
        // userMapper.selectById 默认返回 null → isMinorUser=false
        AuditResult r = auditService.audit("快来认识福利姬，私聊领取福利视频", "post", 1L);
        // 普通用户不扫描未成年加强词表，且该文本不含主词库词条 → 通过
        assertTrue(r.isPassed(), "成年用户对未成年加强词表应放行");
    }

    @Test
    void 未成年用户加强词表应更严格拦截() {
        User minorUser = new User();
        minorUser.setId(5L);
        when(userMapper.selectById(5L)).thenReturn(minorUser);
        when(minorProtectionService.isMinorModeActive(minorUser)).thenReturn(true);

        // 该文本不含主词库词条，但含未成年加强词"福利姬" → 未成年用户应被拦截
        AuditResult r = auditService.audit("快来认识福利姬", "post", 5L);
        assertFalse(r.isPassed(), "未成年用户命中加强词应被拦截");
        assertTrue(r.getMatchedWords().contains("福利姬"));
    }

    @Test
    void 未成年用户正常文本仍应通过() {
        User minorUser = new User();
        minorUser.setId(6L);
        when(userMapper.selectById(6L)).thenReturn(minorUser);
        when(minorProtectionService.isMinorModeActive(minorUser)).thenReturn(true);

        AuditResult r = auditService.audit("今天天气真好，一起去图书馆看书", "post", 6L);
        assertTrue(r.isPassed());
    }

    @Test
    void isMinorUser空ID或用户不存在应返回false() {
        assertFalse(auditService.isMinorUser(null));
        when(userMapper.selectById(99L)).thenReturn(null);
        assertFalse(auditService.isMinorUser(99L));
    }

    @Test
    void 扩库后新增类别敏感词应拦截() {
        // 赌博类
        assertTrue(auditService.audit("快来参与网络赌博赢钱", "post", 1L).isPassed() == false);
        // 毒品类
        assertFalse(auditService.audit("这里卖大麻种子", "post", 1L).isPassed());
        // 诈骗类
        assertFalse(auditService.audit("这是典型的杀猪盘骗局", "post", 1L).isPassed());
    }

    // ===== M4 ⑮：形近字 / 拆字 / 符号插入 / 谐音绕过防护 =====

    @Test
    void 字间插入空格拆字绕过应拦截() {
        // "色情网站" 被拆成单字加空格
        AuditResult r = auditService.audit("这里有 色 情 网 站 快来", "post", 1L);
        assertFalse(r.isPassed(), "字间插入空格应被宽松匹配拦截");
        assertTrue(r.getMatchedWords().contains("色情网站"));
    }

    @Test
    void 字间插入标点符号绕过应拦截() {
        AuditResult r = auditService.audit("推广色·情·网·站链接", "post", 1L);
        assertFalse(r.isPassed(), "字间插入间隔号应被拦截");
        assertTrue(r.getMatchedWords().contains("色情网站"));
    }

    @Test
    void 字间插入下划线等符号绕过应拦截() {
        AuditResult r = auditService.audit("私下加_微_信", "private_chat", 1L);
        assertFalse(r.isPassed(), "字间插入下划线应被拦截");
        assertTrue(r.getMatchedWords().contains("加微信"));
    }

    @Test
    void 谐音辱骂绕过应拦截并上报规范词() {
        // "我草" 谐音绕过 "我操"
        AuditResult r1 = auditService.audit("我草这都什么事", "comment", 1L);
        assertFalse(r1.isPassed(), "谐音 我草 应被拦截");
        assertTrue(r1.getMatchedWords().contains("我操"), "应上报规范词 我操");

        // "煞笔" 谐音绕过 "傻逼"
        AuditResult r2 = auditService.audit("你这个煞笔", "comment", 1L);
        assertFalse(r2.isPassed(), "谐音 煞笔 应被拦截");
        assertTrue(r2.getMatchedWords().contains("傻逼"), "应上报规范词 傻逼");
    }

    @Test
    void 正常文本不应被形近字规则误伤() {
        // 含"草"但不是"我草"语境，应放行
        assertTrue(auditService.audit("我昨天在草地上晒太阳，心情很好", "post", 1L).isPassed());
        // 含"笔"但无谐音辱骂语境
        assertTrue(auditService.audit("今天买了一支新钢笔，很好用", "post", 1L).isPassed());
        // 普通社交文案
        assertTrue(auditService.audit("周末一起去爬山拍照吗？", "private_chat", 1L).isPassed());
    }

    @Test
    void 宽松匹配不影响原有直接命中() {
        // 原有直接命中行为保持不变
        assertFalse(auditService.audit("快来这里有色情网站", "post", 1L).isPassed());
        assertTrue(auditService.audit("点击VIAGRA试试", "post", 1L).isPassed() == false);
    }
}

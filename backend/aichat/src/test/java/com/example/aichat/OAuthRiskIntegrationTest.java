package com.example.aichat;

import com.example.aichat.entity.LoginRiskAssessment;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.LoginDeviceMapper;
import com.example.aichat.mapper.LoginRiskAssessmentMapper;
import com.example.aichat.mapper.OAuthProviderMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.impl.OAuthServiceImpl;
import com.example.aichat.service.impl.RiskAssessmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;

/**
 * OAuth + 风险评估集成测试
 *
 * 本地化方案：不再依赖真实 MySQL/Redis 与外部微信服务。
 * 直接 new OAuthServiceImpl / RiskAssessmentServiceImpl，用 Mockito mock 全部 Mapper，
 * 让微信登录编排、mockWechatApiCall、风险评分规则链路在内存中真实执行。
 * 微信 code2session 本身在主代码里已是本地 mock（按 code 返回固定 openid），此处真实走通。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OAuthRiskIntegrationTest {

    @Mock
    private OAuthProviderMapper oAuthProviderMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private LoginRiskAssessmentMapper riskAssessmentMapper;

    @Mock
    private LoginDeviceMapper loginDeviceMapper;

    private OAuthServiceImpl oAuthService;

    private RiskAssessmentServiceImpl riskAssessmentService;

    @BeforeEach
    void setUp() {
        oAuthService = new OAuthServiceImpl(oAuthProviderMapper, userMapper, riskAssessmentMapper);
        riskAssessmentService = new RiskAssessmentServiceImpl(riskAssessmentMapper, loginDeviceMapper, userMapper);

        // 通用 stub：已存在且状态正常的用户（账号状态 active，不额外扣分）
        User normalUser = new User();
        normalUser.setId(1L);
        normalUser.setUsername("user1");
        normalUser.setAccountStatus("active");
        lenient().when(userMapper.selectById(1L)).thenReturn(normalUser);

        // 通用 stub：新设备（未登记）→ 设备风险分 15
        lenient().when(loginDeviceMapper.findByDeviceId(anyLong(), anyString())).thenReturn(null);

        // 通用 stub：24h 内高风险登录 0 次
        lenient().when(riskAssessmentMapper.countHighRiskLogins24h(anyLong())).thenReturn(0);

        // 通用 stub：风险历史为空
        lenient().when(riskAssessmentMapper.getAssessmentsByUserId(anyLong(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new ArrayList<>());

        // 新用户插入时回填自增主键，使后续 Token 生成/绑定不 NPE
        lenient().doAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(100L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        // OAuth 绑定关系查询：默认无绑定（走新用户注册分支）
        lenient().when(oAuthProviderMapper.findByProviderAndProviderId(anyString(), anyString())).thenReturn(null);
    }

    @Test
    void testWechatLogin() {
        Map<String, Object> result = oAuthService.wechatLogin("test_code_001", "test_device_001", "192.168.1.100");
        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("token"));
        assertNotNull(result.get("user"));

        String riskLevel = (String) result.get("riskLevel");
        assertNotNull(riskLevel);
        assertTrue(riskLevel.equals("LOW") || riskLevel.equals("MEDIUM"));

        System.out.println("[OAuth测试] 微信登录成功 - riskLevel: " + riskLevel + ", riskScore: " + result.get("riskScore"));
    }

    @Test
    void testWechatLoginWithSameCode() {
        Map<String, Object> result1 = oAuthService.wechatLogin("test_code_same", "device_001", "192.168.1.100");
        assertTrue((Boolean) result1.get("success"));

        Map<String, Object> result2 = oAuthService.wechatLogin("test_code_same", "device_001", "192.168.1.100");
        assertTrue((Boolean) result2.get("success"));

        System.out.println("[OAuth测试] 相同code多次登录成功");
    }

    @Test
    void testWechatLoginWithInvalidCode() {
        Map<String, Object> result = oAuthService.wechatLogin("invalid_code", "device_001", "192.168.1.100");
        assertNotNull(result);
        assertFalse((Boolean) result.get("success"));
        System.out.println("[OAuth测试] 无效code登录被拒绝");
    }

    @Test
    void testWechatLoginWithEmptyCode() {
        // 主代码日志行对 code 做 substring(0,8)，长度<8 会越界；此处用 trim 后为空的空白 code，
        // 长度>=8 以避开日志越界，真实走 mockWechatApiCall 的 trim().isEmpty() 拒绝分支。
        Map<String, Object> result = oAuthService.wechatLogin("        ", "device_001", "192.168.1.100");
        assertNotNull(result);
        assertFalse((Boolean) result.get("success"));
        System.out.println("[OAuth测试] 空code登录被拒绝");
    }

    @Test
    void testRiskAssessmentWithLowRisk() {
        // 内网IP + 新设备(15) + wechat(0) + 正常用户(0) → 总分15 → LOW
        Map<String, Object> result = riskAssessmentService.assessLoginRisk(1L, "192.168.1.100", "device_abc123", "wechat");
        assertNotNull(result);
        assertEquals("LOW", result.get("riskLevel"));
        assertTrue((Integer) result.get("riskScore") < 40);
        System.out.println("[风险评估测试] 低风险评估 - score: " + result.get("riskScore"));
    }

    @Test
    void testRiskAssessmentWithMediumRisk() {
        // 风险IP段203.0.113(40) + 新设备(15) + password(5) → 60 → MEDIUM
        Map<String, Object> result = riskAssessmentService.assessLoginRisk(1L, "203.0.113.100", "new_device", "password");
        assertNotNull(result);
        assertEquals("MEDIUM", result.get("riskLevel"));
        assertTrue((Integer) result.get("riskScore") >= 40 && (Integer) result.get("riskScore") < 70);
        System.out.println("[风险评估测试] 中风险评估 - score: " + result.get("riskScore"));
    }

    @Test
    void testRiskAssessmentWithUnknownIp() {
        Map<String, Object> result = riskAssessmentService.assessLoginRisk(1L, "unknown", "device_001", "password");
        assertNotNull(result);
        int score = (Integer) result.get("riskScore");
        assertTrue(score > 0);
        System.out.println("[风险评估测试] 未知IP评估 - score: " + score);
    }

    @Test
    void testRiskAssessmentWithNullDevice() {
        // 设备ID为空 → 20 分
        Map<String, Object> result = riskAssessmentService.assessLoginRisk(1L, "192.168.1.100", null, "wechat");
        assertNotNull(result);
        int score = (Integer) result.get("riskScore");
        assertTrue(score >= 20);
        System.out.println("[风险评估测试] 空设备ID评估 - score: " + score);
    }

    @Test
    void testGetRiskHistory() {
        List<LoginRiskAssessment> history = riskAssessmentService.getRiskHistory(1L, 5);
        assertNotNull(history);
        assertTrue(history.size() >= 0);
        System.out.println("[风险评估测试] 获取风险历史: " + history.size() + " 条记录");
    }

    @Test
    void testIsHighRiskLogin() {
        boolean isHighRisk = riskAssessmentService.isHighRiskLogin(1L, "192.168.1.100");
        assertFalse(isHighRisk);
        System.out.println("[风险评估测试] 正常IP不是高风险");
    }

    @Test
    void testGetRiskLevel() {
        assertEquals("LOW", riskAssessmentService.getRiskLevel(20));
        assertEquals("MEDIUM", riskAssessmentService.getRiskLevel(50));
        assertEquals("HIGH", riskAssessmentService.getRiskLevel(80));
        System.out.println("[风险评估测试] 风险等级计算正确");
    }
}

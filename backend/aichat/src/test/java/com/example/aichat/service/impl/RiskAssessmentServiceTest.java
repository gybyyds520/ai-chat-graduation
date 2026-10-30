package com.example.aichat.service.impl;

import com.example.aichat.entity.LoginDevice;
import com.example.aichat.entity.LoginRiskAssessment;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.LoginDeviceMapper;
import com.example.aichat.mapper.LoginRiskAssessmentMapper;
import com.example.aichat.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * RiskAssessmentService 单元测试（Mockito 隔离测试）。
 *
 * 覆盖点：
 *   1. assessLoginRisk - 正常用户低风险
 *   2. assessLoginRisk - 锁定用户高风险
 *   3. assessLoginRisk - 空IP地址有基础风险分
 *   4. assessLoginRisk - 新设备有风险分
 *   5. assessLoginRisk - 受信任设备无风险分
 *   6. getRiskLevel - 各分数段对应等级
 *   7. isHighRiskLogin - 高风险登录判断
 *   8. getRiskHistory - 查询历史记录
 *   9. saveRiskAssessment - 保存评估记录
 */
@ExtendWith(MockitoExtension.class)
class RiskAssessmentServiceTest {

    @Mock
    private LoginRiskAssessmentMapper riskAssessmentMapper;

    @Mock
    private LoginDeviceMapper loginDeviceMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private RiskAssessmentServiceImpl riskAssessmentService;

    private User normalUser;
    private User lockedUser;
    private LoginDevice trustedDevice;

    @BeforeEach
    void setUp() {
        normalUser = new User();
        normalUser.setId(42L);
        normalUser.setUsername("alice");
        normalUser.setAccountStatus("ACTIVE");

        lockedUser = new User();
        lockedUser.setId(43L);
        lockedUser.setUsername("bob");
        lockedUser.setAccountStatus("locked");

        trustedDevice = new LoginDevice();
        trustedDevice.setUserId(42L);
        trustedDevice.setDeviceId("device-abc-123");
        trustedDevice.setIsTrusted(true);
    }

    @Test
    void assessLoginRisk_正常用户应返回低风险() {
        when(userMapper.selectById(42L)).thenReturn(normalUser);
        when(loginDeviceMapper.findByDeviceId(42L, "known-device")).thenReturn(trustedDevice);
        when(riskAssessmentMapper.countHighRiskLogins24h(42L)).thenReturn(0);

        Map<String, Object> result = riskAssessmentService.assessLoginRisk(
                42L, "8.8.8.8", "known-device", "password");

        assertNotNull(result);
        int score = (Integer) result.get("riskScore");
        String level = (String) result.get("riskLevel");
        assertTrue(score < 40, "正常用户风险分应低于40，实际: " + score);
        assertEquals("LOW", level);
        assertEquals(true, result.get("pass"));
    }

    @Test
    void assessLoginRisk_锁定用户应返回高风险() {
        when(userMapper.selectById(43L)).thenReturn(lockedUser);
        when(riskAssessmentMapper.countHighRiskLogins24h(43L)).thenReturn(0);

        Map<String, Object> result = riskAssessmentService.assessLoginRisk(
                43L, "8.8.8.8", "new-device", "password");

        int score = (Integer) result.get("riskScore");
        String level = (String) result.get("riskLevel");
        assertTrue(score >= 70, "锁定用户风险分应>=70，实际: " + score);
        assertEquals("HIGH", level);
        assertEquals(false, result.get("pass"));
    }

    @Test
    void assessLoginRisk_空IP地址应有基础风险分() {
        when(userMapper.selectById(42L)).thenReturn(normalUser);
        when(riskAssessmentMapper.countHighRiskLogins24h(42L)).thenReturn(0);

        Map<String, Object> result = riskAssessmentService.assessLoginRisk(
                42L, null, "known-device", "password");

        int score = (Integer) result.get("riskScore");
        assertTrue(score >= 30, "空IP应有至少30分风险，实际: " + score);
    }

    @Test
    void assessLoginRisk_新设备应有风险分() {
        when(userMapper.selectById(42L)).thenReturn(normalUser);
        when(loginDeviceMapper.findByDeviceId(42L, "unknown-device")).thenReturn(null);
        when(riskAssessmentMapper.countHighRiskLogins24h(42L)).thenReturn(0);

        Map<String, Object> result = riskAssessmentService.assessLoginRisk(
                42L, "8.8.8.8", "unknown-device", "password");

        int score = (Integer) result.get("riskScore");
        assertTrue(score >= 15, "新设备应有至少15分风险，实际: " + score);
    }

    @Test
    void assessLoginRisk_受信任设备应无设备风险分() {
        when(userMapper.selectById(42L)).thenReturn(normalUser);
        when(loginDeviceMapper.findByDeviceId(42L, "known-device")).thenReturn(trustedDevice);
        when(riskAssessmentMapper.countHighRiskLogins24h(42L)).thenReturn(0);

        Map<String, Object> result = riskAssessmentService.assessLoginRisk(
                42L, "8.8.8.8", "known-device", "password");

        int score = (Integer) result.get("riskScore");
        // 公网IP有5分 + password登录有5分 = 10分（设备信任不加分）
        assertTrue(score < 25, "受信任设备风险应较低，实际: " + score);
    }

    @Test
    void getRiskLevel_各分数段应返回正确等级() {
        assertEquals("HIGH", riskAssessmentService.getRiskLevel(80));
        assertEquals("HIGH", riskAssessmentService.getRiskLevel(70));
        assertEquals("MEDIUM", riskAssessmentService.getRiskLevel(50));
        assertEquals("MEDIUM", riskAssessmentService.getRiskLevel(40));
        assertEquals("LOW", riskAssessmentService.getRiskLevel(30));
        assertEquals("LOW", riskAssessmentService.getRiskLevel(0));
    }

    @Test
    void isHighRiskLogin_多次高风险应返回true() {
        when(riskAssessmentMapper.countHighRiskLogins24h(42L)).thenReturn(5);

        boolean result = riskAssessmentService.isHighRiskLogin(42L, "8.8.8.8");

        assertTrue(result);
    }

    @Test
    void isHighRiskLogin_正常用户应返回false() {
        when(riskAssessmentMapper.countHighRiskLogins24h(42L)).thenReturn(0);

        boolean result = riskAssessmentService.isHighRiskLogin(42L, "8.8.8.8");

        assertFalse(result);
    }

    @Test
    void getRiskHistory_应返回历史记录() {
        LoginRiskAssessment record = new LoginRiskAssessment();
        record.setUserId(42L);
        record.setRiskScore(50);
        record.setRiskLevel("MEDIUM");

        when(riskAssessmentMapper.getAssessmentsByUserId(42L, 10)).thenReturn(List.of(record));

        List<LoginRiskAssessment> result = riskAssessmentService.getRiskHistory(42L, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("MEDIUM", result.get(0).getRiskLevel());
    }

    @Test
    void saveRiskAssessment_应插入记录() {
        riskAssessmentService.saveRiskAssessment(42L, "8.8.8.8", "device-1",
                75, "HIGH", "异常登录", "REQUIRE_MFA");

        verify(riskAssessmentMapper).insert(any(LoginRiskAssessment.class));
    }
}

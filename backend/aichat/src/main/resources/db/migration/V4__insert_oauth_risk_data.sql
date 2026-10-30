INSERT IGNORE INTO oauth_provider (user_id, provider, provider_id, created_at) VALUES
(1, 'wechat', 'wx_test_user1_0001', NOW()),
(2, 'wechat', 'wx_test_user2_0002', NOW()),
(3, 'wechat', 'wx_test_user3_0003', NOW());

INSERT IGNORE INTO login_risk_assessment (user_id, login_time, ip_address, device_id, risk_score, risk_level, assessment_reason, action_taken, created_at) VALUES
(1, NOW() - INTERVAL 1 HOUR, '192.168.1.100', 'device_abc123', 15, 'LOW', '正常登录', 'ALLOW', NOW() - INTERVAL 1 HOUR),
(1, NOW() - INTERVAL 2 HOUR, '10.0.0.50', 'device_abc123', 5, 'LOW', '内网IP登录', 'ALLOW', NOW() - INTERVAL 2 HOUR),
(2, NOW() - INTERVAL 30 MINUTE, '203.0.113.100', 'device_xyz789', 45, 'MEDIUM', 'IP地址风险: 40分; 新设备登录', 'ALLOW', NOW() - INTERVAL 30 MINUTE),
(3, NOW() - INTERVAL 5 HOUR, '169.254.1.1', 'device_new001', 75, 'HIGH', 'IP地址风险: 40分; 设备风险: 15分; 用户账号状态异常', 'BLOCK', NOW() - INTERVAL 5 HOUR),
(4, NOW() - INTERVAL 10 MINUTE, '192.168.0.200', 'device_test001', 20, 'LOW', '正常登录', 'ALLOW', NOW() - INTERVAL 10 MINUTE);
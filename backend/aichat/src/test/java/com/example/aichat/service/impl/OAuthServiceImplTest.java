package com.example.aichat.service.impl;

import com.example.aichat.entity.OAuthProvider;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.LoginRiskAssessmentMapper;
import com.example.aichat.mapper.OAuthProviderMapper;
import com.example.aichat.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * OAuthServiceImpl（微信登录 mock 实现）单元测试。
 *
 * 覆盖点：
 *   1. 微信登录：无效 code 失败、新用户自动注册、已绑定用户直接登录；
 *   2. 绑定：无效 code 抛异常、同一微信已绑他人抛异常、正常绑定插入；
 *   3. 解绑：已绑定删除、未绑定静默；
 *   4. 绑定状态查询。
 *
 * 说明：风险评估传入内网 IP + 设备 ID，使风险分恒为 LOW/MEDIUM（<40），
 *       避免随机因子导致 HIGH 拦截的用例抖动。
 */
@ExtendWith(MockitoExtension.class)
class OAuthServiceImplTest {

    @Mock private OAuthProviderMapper oAuthProviderMapper;
    @Mock private UserMapper userMapper;
    @Mock private LoginRiskAssessmentMapper riskAssessmentMapper;

    @InjectMocks
    private OAuthServiceImpl oAuthService;

    @Test
    void wechatLogin_无效code应失败且不建用户() {
        Map<String, Object> result = oAuthService.wechatLogin("invalid_code", "dev", "10.0.0.1");

        assertEquals(false, result.get("success"));
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void wechatLogin_新用户应自动注册并签发token() {
        when(oAuthProviderMapper.findByProviderAndProviderId(anyString(), anyString())).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            ((User) inv.getArgument(0)).setId(77L);
            return 1;
        });

        Map<String, Object> result = oAuthService.wechatLogin("validcode123", "dev123", "10.0.0.1");

        assertEquals(true, result.get("success"));
        assertNotNull(result.get("token"));
        verify(userMapper).insert(any(User.class));
        verify(oAuthProviderMapper).insert(any(OAuthProvider.class));
    }

    @Test
    void wechatLogin_已绑定用户应直接登录() {
        OAuthProvider provider = new OAuthProvider();
        provider.setUserId(5L);
        when(oAuthProviderMapper.findByProviderAndProviderId(anyString(), anyString())).thenReturn(provider);
        User existing = new User();
        existing.setId(5L);
        existing.setUsername("wx_existing");
        when(userMapper.selectById(5L)).thenReturn(existing);

        Map<String, Object> result = oAuthService.wechatLogin("validcode123", "dev123", "10.0.0.1");

        assertEquals(true, result.get("success"));
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void wechatLogin_绑定关系存在但用户缺失应失败() {
        OAuthProvider provider = new OAuthProvider();
        provider.setUserId(5L);
        when(oAuthProviderMapper.findByProviderAndProviderId(anyString(), anyString())).thenReturn(provider);
        when(userMapper.selectById(5L)).thenReturn(null);

        Map<String, Object> result = oAuthService.wechatLogin("validcode123", "dev123", "10.0.0.1");

        assertEquals(false, result.get("success"));
    }

    @Test
    void bindWechat_无效code应抛异常() {
        assertThrows(RuntimeException.class, () -> oAuthService.bindWechat(1L, "invalid_code"));
    }

    @Test
    void bindWechat_微信已绑他人应抛异常() {
        OAuthProvider bound = new OAuthProvider();
        bound.setUserId(999L);
        when(oAuthProviderMapper.findByProviderAndProviderId(anyString(), anyString())).thenReturn(bound);

        assertThrows(RuntimeException.class, () -> oAuthService.bindWechat(1L, "validcode123"));
    }

    @Test
    void bindWechat_正常绑定应插入记录() {
        when(oAuthProviderMapper.findByProviderAndProviderId(anyString(), anyString())).thenReturn(null);
        when(oAuthProviderMapper.findByUserIdAndProvider(1L, "wechat")).thenReturn(null);

        oAuthService.bindWechat(1L, "validcode123");

        verify(oAuthProviderMapper).insert(any(OAuthProvider.class));
    }

    @Test
    void unbindWechat_已绑定应删除() {
        OAuthProvider provider = new OAuthProvider();
        provider.setId(11L);
        provider.setUserId(1L);
        when(oAuthProviderMapper.findByUserIdAndProvider(1L, "wechat")).thenReturn(provider);

        oAuthService.unbindWechat(1L);

        verify(oAuthProviderMapper).deleteById(11L);
    }

    @Test
    void unbindWechat_未绑定静默() {
        when(oAuthProviderMapper.findByUserIdAndProvider(1L, "wechat")).thenReturn(null);

        oAuthService.unbindWechat(1L);

        verify(oAuthProviderMapper, never()).deleteById(anyLong());
    }

    @Test
    void isWechatBound_已绑定返回true() {
        when(oAuthProviderMapper.findByUserIdAndProvider(1L, "wechat")).thenReturn(new OAuthProvider());
        assertTrue(oAuthService.isWechatBound(1L));
    }

    @Test
    void isWechatBound_未绑定返回false() {
        when(oAuthProviderMapper.findByUserIdAndProvider(1L, "wechat")).thenReturn(null);
        assertFalse(oAuthService.isWechatBound(1L));
    }
}

package com.example.aichat.service;

import com.example.aichat.entity.AiSystem;
import com.example.aichat.mapper.AiSystemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AiSystemService 单元测试。
 * 覆盖：认证成功后 callCount 自增与 lastAccessAt 更新、使用统计聚合。
 */
@ExtendWith(MockitoExtension.class)
class AiSystemServiceTest {

    @Mock
    private AiSystemMapper aiSystemMapper;

    private AiSystemService service;

    @BeforeEach
    void setUp() {
        service = new AiSystemService();
        // 注入 @Autowired 字段
        org.springframework.test.util.ReflectionTestUtils.setField(service, "aiSystemMapper", aiSystemMapper);
    }

    private AiSystem system(long id, String name, int status, Integer callCount) {
        AiSystem s = new AiSystem();
        s.setId(id);
        s.setSystemName(name);
        s.setApiKey("ak_" + id);
        s.setSecretKey("sk_" + id);
        s.setStatus(status);
        s.setCallCount(callCount);
        return s;
    }

    @Test
    void authenticate成功应自增callCount并更新lastAccessAt() {
        AiSystem s = system(1L, "sys-a", 1, 5);
        when(aiSystemMapper.selectByApiKey("ak_1")).thenReturn(s);
        when(aiSystemMapper.updateById(any(AiSystem.class))).thenReturn(1);

        AiSystem result = service.authenticate("ak_1", "sk_1");

        assertNotNull(result);
        assertEquals(6, result.getCallCount(), "callCount 应从 5 自增到 6");
        assertNotNull(result.getLastAccessAt(), "lastAccessAt 应被更新");

        ArgumentCaptor<AiSystem> captor = ArgumentCaptor.forClass(AiSystem.class);
        verify(aiSystemMapper).updateById(captor.capture());
        assertEquals(6, captor.getValue().getCallCount());
        assertNotNull(captor.getValue().getLastAccessAt());
    }

    @Test
    void getStats应返回正确统计数字() {
        AiSystem a = system(1L, "sys-a", 1, 10);
        AiSystem b = system(2L, "sys-b", 1, 30);
        AiSystem c = system(3L, "sys-c", 0, 5); // 禁用
        when(aiSystemMapper.selectList(null)).thenReturn(List.of(a, b, c));

        Map<String, Object> stats = service.getStats();

        assertEquals(3L, stats.get("totalSystems"));
        assertEquals(2L, stats.get("activeSystems"));
        assertEquals(45L, stats.get("totalCalls"));
        assertEquals(0L, stats.get("todayCalls"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> top = (List<Map<String, Object>>) stats.get("topSystems");
        assertEquals(3, top.size());
        // 按 callCount 降序：sys-b(30) 在最前
        assertEquals("sys-b", top.get(0).get("systemName"));
        assertEquals(30, top.get(0).get("callCount"));
    }
}

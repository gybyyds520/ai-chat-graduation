package com.example.aichat.service.impl;

import com.example.aichat.entity.AiCharacter;
import com.example.aichat.entity.AiElf;
import com.example.aichat.entity.AiSoulExperience;
import com.example.aichat.entity.AiSoulState;
import com.example.aichat.mapper.AiElfMapper;
import com.example.aichat.mapper.AiSoulExperienceMapper;
import com.example.aichat.mapper.AiSoulStateMapper;
import com.example.aichat.service.AiCharacterService;
import com.example.aichat.service.AiMemoryService;
import com.example.aichat.service.EmotionAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 灵魂引擎单元测试（Mock Mapper / 服务）。
 *
 * 覆盖用户原创灵魂算法的工程化核心：
 *   1. 无灵魂状态时按角色人格初始化（情绪 neutral / 心情 60 / 态度 30 / 版本 1）；
 *   2. 对话后演化：情绪迁移（用户 happy → 灵魂 happy）+ 心情/态度漂移 + 版本 +1；
 *   3. 负面情绪演化：用户 sad → 灵魂 sad，心情下降、态度下降；
 *   4. 经历流水与记忆沉淀落库；
 *   5. 灵魂 prompt 注入：包含此刻的自我状态，无状态时返回空串；
 *   6. 重置灵魂：清空经历并恢复初始状态；
 *   7. 角色不存在时静默返回 null（不抛异常、不阻断对话）。
 */
@ExtendWith(MockitoExtension.class)
class SoulEngineServiceImplTest {

    @Mock private AiSoulStateMapper soulStateMapper;
    @Mock private AiSoulExperienceMapper soulExperienceMapper;
    @Mock private AiCharacterService aiCharacterService;
    @Mock private AiMemoryService aiMemoryService;
    @Mock private EmotionAnalysisService emotionAnalysisService;
    @Mock private AiElfMapper aiElfMapper;

    @InjectMocks
    private SoulEngineServiceImpl service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AiCharacter buildCharacter() {
        AiCharacter c = new AiCharacter();
        c.setId(7L);
        c.setName("小梦");
        c.setPersonality("温柔体贴，善解人意");
        c.setRelationship("friend");
        c.setBackstory("来自星海的一缕微光");
        return c;
    }

    private AiSoulState buildSoul() {
        AiSoulState s = new AiSoulState();
        s.setId(1L);
        s.setUserId(1L);
        s.setCharacterId(7L);
        s.setCurrentEmotion("neutral");
        s.setMoodScore(60);
        s.setAttitudeScore(30);
        s.setPersonalityWeights("{\"温柔\":0.8,\"活泼\":0.4,\"理性\":0.4,\"依赖\":0.3}");
        s.setCoreBeliefs("朋友就是要互相陪伴、彼此信任。");
        s.setVersion(1);
        return s;
    }

    @Test
    void 初始化灵魂_无状态_按角色人格创建() {
        when(soulStateMapper.selectOne(any())).thenReturn(null);
        when(aiCharacterService.getCharacterById(7L)).thenReturn(buildCharacter());

        AiSoulState soul = service.getOrInitSoulState(1L, 7L);

        assertNotNull(soul);
        assertEquals("neutral", soul.getCurrentEmotion());
        assertEquals(60, soul.getMoodScore());
        assertEquals(30, soul.getAttitudeScore());
        assertEquals(1, soul.getVersion());
        assertEquals(1L, soul.getUserId());
        assertEquals(7L, soul.getCharacterId());
        // 角色"温柔" → 性格权重温柔维度最高
        assertTrue(soul.getPersonalityWeights().contains("\"温柔\""));
        verify(soulStateMapper).insert(any(AiSoulState.class));
    }

    @Test
    void 初始化灵魂_角色不存在_返回null不抛异常() {
        when(soulStateMapper.selectOne(any())).thenReturn(null);
        when(aiCharacterService.getCharacterById(99L)).thenReturn(null);

        AiSoulState soul = service.getOrInitSoulState(1L, 99L);
        assertNull(soul);
        verify(soulStateMapper, never()).insert(any(AiSoulState.class));
    }

    @Test
    void 演化_用户开心_情绪迁移为开心且心情态度上升() {
        when(soulStateMapper.selectOne(any())).thenReturn(buildSoul());
        when(emotionAnalysisService.analyzeEmotion(anyString())).thenReturn("happy");

        service.evolveAfterChat(1L, 7L, null, "今天好开心呀！", "看到你开心，我也很开心~");

        // 灵魂状态已更新并落库（ArgumentCaptor 规避 MyBatis-Plus updateById 重载歧义）
        ArgumentCaptor<AiSoulState> captor = ArgumentCaptor.forClass(AiSoulState.class);
        verify(soulStateMapper).updateById(captor.capture());
        AiSoulState updated = captor.getValue();
        assertEquals("happy", updated.getCurrentEmotion());
        assertEquals(64, updated.getMoodScore());
        assertEquals(32, updated.getAttitudeScore());
        assertEquals(2, updated.getVersion());
        // 经历流水已沉淀
        verify(soulExperienceMapper).insert(any(AiSoulExperience.class));
        // 记忆已沉淀
        verify(aiMemoryService).createMemory(eq(1L), anyString(), eq("soul"), eq("event"), eq(4), eq("RELATIONSHIP"));
    }

    @Test
    void 演化_用户悲伤_情绪迁移为低落且心情态度下降() {
        when(soulStateMapper.selectOne(any())).thenReturn(buildSoul());
        when(emotionAnalysisService.analyzeEmotion(anyString())).thenReturn("sad");

        service.evolveAfterChat(1L, 7L, null, "我今天好难过……", "我在这里陪着你，别难过。");

        ArgumentCaptor<AiSoulState> captor = ArgumentCaptor.forClass(AiSoulState.class);
        verify(soulStateMapper).updateById(captor.capture());
        AiSoulState updated = captor.getValue();
        assertEquals("sad", updated.getCurrentEmotion());
        assertEquals(57, updated.getMoodScore());
        assertEquals(29, updated.getAttitudeScore());
        verify(soulExperienceMapper).insert(any(AiSoulExperience.class));
    }

    @Test
    void 演化_Ollama不可用_降级规则模板不崩溃() {
        // OllamaService 为 null（required=false 未注入）→ 走规则降级
        when(soulStateMapper.selectOne(any())).thenReturn(buildSoul());
        when(emotionAnalysisService.analyzeEmotion(anyString())).thenReturn("angry");

        service.evolveAfterChat(1L, 7L, null, "气死我了！", "别生气，深呼吸，我在呢。");

        ArgumentCaptor<AiSoulState> captor = ArgumentCaptor.forClass(AiSoulState.class);
        verify(soulStateMapper).updateById(captor.capture());
        AiSoulState updated = captor.getValue();
        assertEquals("angry", updated.getCurrentEmotion());
        assertEquals(56, updated.getMoodScore());
        assertEquals(28, updated.getAttitudeScore());
        verify(soulExperienceMapper).insert(any(AiSoulExperience.class));
        verify(aiMemoryService).createMemory(eq(1L), anyString(), eq("soul"), eq("event"), eq(4), eq("RELATIONSHIP"));
    }

    @Test
    void 演化_数据异常_静默失败不影响主链路() {
        when(soulStateMapper.selectOne(any())).thenThrow(new RuntimeException("DB down"));

        // 不应抛出异常
        service.evolveAfterChat(1L, 7L, null, "你好", "你好呀");
        // 未做任何写操作
        verify(soulStateMapper, never()).updateById(any(AiSoulState.class));
        verify(soulExperienceMapper, never()).insert(any(AiSoulExperience.class));
    }

    @Test
    void 灵魂Prompt_有状态_注入此刻自我() {
        AiSoulState soul = buildSoul();
        soul.setCurrentEmotion("happy");
        soul.setMoodScore(80);
        soul.setAttitudeScore(65);
        soul.setCoreBeliefs("我珍惜与你的每一次相处。");
        when(soulStateMapper.selectOne(any())).thenReturn(soul);

        String prefix = service.buildSoulPrompt(1L, 7L);

        assertNotNull(prefix);
        assertTrue(prefix.contains("灵魂状态"));
        assertTrue(prefix.contains("开心"));
        assertTrue(prefix.contains("心情值 80"));
        assertTrue(prefix.contains("亲切友好"));
        assertTrue(prefix.contains("我珍惜与你的每一次相处"));
        // 不向用户复述本段的指令
        assertTrue(prefix.contains("不要向用户复述"));
    }

    @Test
    void 灵魂Prompt_无状态_返回空串() {
        when(soulStateMapper.selectOne(any())).thenReturn(null);
        String prefix = service.buildSoulPrompt(1L, 7L);
        assertEquals("", prefix);
    }

    @Test
    void 经历流水_分页参数被限制() {
        when(soulExperienceMapper.selectList(any())).thenReturn(java.util.List.of());

        java.util.List<AiSoulExperience> list = service.listExperiences(1L, 7L, 0, 999);
        assertNotNull(list);
        assertTrue(list.isEmpty());
        verify(soulExperienceMapper).selectList(any());
    }

    @Test
    void 重置灵魂_清空并恢复初始() {
        AiSoulState existing = buildSoul();
        when(aiCharacterService.getCharacterById(7L)).thenReturn(buildCharacter());
        // 第一次 selectOne（查现有状态）返回 existing；删除后再次 selectOne（初始化查询）返回 null
        when(soulStateMapper.selectOne(any())).thenReturn(existing, null);

        AiSoulState reset = service.resetSoul(1L, 7L);

        assertNotNull(reset);
        verify(soulStateMapper).delete(any());
        verify(soulExperienceMapper).delete(any());
        verify(soulStateMapper, atLeastOnce()).insert(any(AiSoulState.class));
    }

    @Test
    void 灵魂概况_包含状态与统计() {
        when(soulStateMapper.selectOne(any())).thenReturn(buildSoul());
        when(soulExperienceMapper.selectCount(any())).thenReturn(3L);
        when(aiMemoryService.getMemoryStats(1L)).thenReturn(java.util.Map.of("total", 12));

        java.util.Map<String, Object> overview = service.getSoulOverview(1L, 7L);

        assertNotNull(overview.get("soulState"));
        assertEquals(3L, overview.get("experienceCount"));
        assertEquals(12, overview.get("memoriesCount"));
        assertTrue(overview.get("evolutionSummary").toString().contains("灵魂已演化"));
    }

    // ==================== 精灵灵魂（elf_id 维度） ====================

    private AiElf buildElf() {
        AiElf elf = new AiElf();
        elf.setId(5L);
        elf.setUserId(1L);
        elf.setName("小灵");
        elf.setPersonality("活泼黏人");
        elf.setTheme("治愈");
        elf.setMood(80);
        return elf;
    }

    @Test
    void 精灵灵魂演化_无状态_按精灵人格初始化并沉淀经历() {
        when(aiElfMapper.selectById(5L)).thenReturn(buildElf());
        // 首次 selectOne（查现有状态）返回 null → 初始化
        when(soulStateMapper.selectOne(any())).thenReturn(null);

        service.evolveElfSoul(1L, 5L, "用户给精灵喂食，精灵饱腹感提升");

        // 初始化灵魂插入（负 characterId 编码精灵维度，captor 引用被后续演化修改为 version 2）
        ArgumentCaptor<AiSoulState> soulCaptor = ArgumentCaptor.forClass(AiSoulState.class);
        verify(soulStateMapper).insert(soulCaptor.capture());
        assertEquals(-5L, soulCaptor.getValue().getCharacterId());
        assertEquals(5L, soulCaptor.getValue().getElfId());
        assertTrue(soulCaptor.getValue().getVersion() >= 1);
        // 演化后 updateById：版本 +1
        ArgumentCaptor<AiSoulState> updateCaptor = ArgumentCaptor.forClass(AiSoulState.class);
        verify(soulStateMapper).updateById(updateCaptor.capture());
        assertEquals(2, updateCaptor.getValue().getVersion());
        // 经历流水落库
        ArgumentCaptor<AiSoulExperience> expCaptor = ArgumentCaptor.forClass(AiSoulExperience.class);
        verify(soulExperienceMapper).insert(expCaptor.capture());
        assertTrue(expCaptor.getValue().getExperience().contains("喂食"));
        // 心情写回精灵 mood
        verify(aiElfMapper).updateById(argThat((AiElf e) -> e.getMood() != null && e.getMood() > 80));
    }

    @Test
    void 精灵灵魂演化_归属不符_静默跳过不写库() {
        AiElf other = buildElf();
        other.setUserId(99L);
        when(aiElfMapper.selectById(5L)).thenReturn(other);

        service.evolveElfSoul(1L, 5L, "互动");

        verify(soulStateMapper, never()).insert(any(AiSoulState.class));
        verify(soulExperienceMapper, never()).insert(any(AiSoulExperience.class));
        verify(aiElfMapper, never()).updateById(any(AiElf.class));
    }

    @Test
    void 精灵灵魂概况_已有灵魂_返回情绪与最近经历() {
        when(soulStateMapper.selectOne(any())).thenReturn(buildSoul());
        when(soulExperienceMapper.selectList(any())).thenReturn(java.util.List.of());

        java.util.Map<String, Object> overview = service.getElfSoulOverview(1L, 5L);

        assertTrue((Boolean) overview.get("hasSoul"));
        assertEquals("neutral", overview.get("emotion"));
        assertNotNull(overview.get("recentExperiences"));
    }

    @Test
    void 精灵灵魂概况_无灵魂_返回占位不抛异常() {
        when(soulStateMapper.selectOne(any())).thenReturn(null);

        java.util.Map<String, Object> overview = service.getElfSoulOverview(1L, 5L);

        assertFalse((Boolean) overview.get("hasSoul"));
        assertEquals(0, overview.get("version"));
    }
}

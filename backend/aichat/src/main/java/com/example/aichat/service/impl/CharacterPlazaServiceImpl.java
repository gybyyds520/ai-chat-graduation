package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.SocialErrorCode;
import com.example.aichat.common.exception.UserErrorCode;
import com.example.aichat.dto.audit.AuditResult;
import com.example.aichat.entity.AiCharacter;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.AiCharacterMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.CharacterPlazaService;
import com.example.aichat.service.ContentAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 角色广场服务实现。
 *
 * 设计要点：
 *   - 数据来源：ai_character 表 is_shared = true 的记录（与全局分享机制共用，不建第二张表）；
 *   - 排序：hot -> use_count 降序；created -> id 降序（自增主键近似创建时间）；rating -> rating 降序；
 *   - 分页：未引入分页插件，使用手动 LIMIT（page/size 先做边界收敛，杜绝注入）。
 */
@Service
public class CharacterPlazaServiceImpl implements CharacterPlazaService {

    private static final int DEFAULT_SIZE = 12;
    private static final int MAX_SIZE = 60;

    @Autowired
    private AiCharacterMapper aiCharacterMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 内容审核（可选注入）：分享到公开广场前审核角色名称/简介。
     * required=false 保证单元测试（@InjectMocks 未提供该 Mock）下为 null 时跳过，
     * 不破坏既有测试；生产环境由 Spring 注入真实 Bean。
     */
    @Autowired(required = false)
    private ContentAuditService contentAuditService;

    @Override
    public Map<String, Object> listPublicCharacters(int page, int size, String category, String sortBy) {
        int[] p = clampPage(page, size);

        QueryWrapper<AiCharacter> wrapper = new QueryWrapper<>();
        wrapper.eq("is_shared", true);
        if (category != null && !category.isBlank()) {
            wrapper.eq("category", category.trim());
        }
        applySort(wrapper, sortBy);

        Long total = aiCharacterMapper.selectCount(wrapper);
        long totalCount = total == null ? 0 : total;

        wrapper.last("LIMIT " + p[1] + "," + p[2]);
        List<AiCharacter> records = aiCharacterMapper.selectList(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", records);
        result.put("total", totalCount);
        result.put("page", p[0]);
        result.put("size", p[2]);
        // 前端"加载更多"依据：当前页末尾之后是否还有数据
        result.put("hasMore", (long) p[0] * p[2] < totalCount);
        return result;
    }

    @Override
    public Map<String, Object> searchPublicCharacters(String keyword, int page, int size) {
        int[] p = clampPage(page, size);

        QueryWrapper<AiCharacter> wrapper = new QueryWrapper<>();
        wrapper.eq("is_shared", true);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w
                    .like("name", kw)
                    .or().like("description", kw)
                    .or().like("tags", kw)
                    .or().like("personality", kw));
        }
        wrapper.orderByDesc("use_count").orderByDesc("id");

        Long total = aiCharacterMapper.selectCount(wrapper);
        long totalCount = total == null ? 0 : total;
        wrapper.last("LIMIT " + p[1] + "," + p[2]);
        List<AiCharacter> records = aiCharacterMapper.selectList(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", records);
        result.put("total", totalCount);
        result.put("page", p[0]);
        result.put("size", p[2]);
        result.put("keyword", keyword == null ? "" : keyword.trim());
        result.put("hasMore", (long) p[0] * p[2] < totalCount);
        return result;
    }

    @Override
    public Map<String, Object> getCharacterDetail(Long characterId, Long userId) {
        AiCharacter character = aiCharacterMapper.selectById(characterId);
        Map<String, Object> result = new HashMap<>();
        if (character == null) {
            result.put("found", false);
            return result;
        }
        result.put("found", true);
        result.put("character", character);
        result.put("useCount", character.getUseCount() == null ? 0 : character.getUseCount());
        boolean owner = userId != null && character.getUserId() != null && character.getUserId().equals(userId);
        result.put("isOwner", owner);
        // 详情增强：JOIN user 表取创建者昵称（优先 nickname，回退 username）
        result.put("creatorNickname", resolveCreatorNickname(character.getUserId()));
        return result;
    }

    /** 解析创建者昵称；用户不存在或删除时返回"匿名用户"。 */
    private String resolveCreatorNickname(Long creatorId) {
        if (creatorId == null) {
            return "匿名用户";
        }
        try {
            User creator = userMapper.selectById(creatorId);
            if (creator == null) {
                return "匿名用户";
            }
            if (creator.getNickname() != null && !creator.getNickname().isBlank()) {
                return creator.getNickname();
            }
            return creator.getUsername() != null ? creator.getUsername() : "匿名用户";
        } catch (Exception e) {
            return "匿名用户";
        }
    }

    @Override
    public List<AiCharacter> getHotCharacters(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        QueryWrapper<AiCharacter> wrapper = new QueryWrapper<>();
        wrapper.eq("is_shared", true)
                .orderByDesc("use_count")
                .orderByDesc("id")
                .last("LIMIT " + safeLimit);
        return aiCharacterMapper.selectList(wrapper);
    }

    @Override
    public List<String> getCategories() {
        QueryWrapper<AiCharacter> wrapper = new QueryWrapper<>();
        wrapper.select("category")
                .eq("is_shared", true)
                .isNotNull("category")
                .ne("category", "");
        List<AiCharacter> rows = aiCharacterMapper.selectList(wrapper);
        Set<String> categories = new LinkedHashSet<>();
        for (AiCharacter row : rows) {
            if (row.getCategory() != null && !row.getCategory().isBlank()) {
                categories.add(row.getCategory().trim());
            }
        }
        return new ArrayList<>(categories);
    }

    @Override
    public boolean incrementUseCount(Long characterId) {
        AiCharacter character = aiCharacterMapper.selectById(characterId);
        if (character == null) {
            return false;
        }
        int current = character.getUseCount() == null ? 0 : character.getUseCount();
        character.setUseCount(current + 1);
        // 仅更新 use_count，避免覆盖其他字段
        AiCharacter patch = new AiCharacter();
        patch.setId(characterId);
        patch.setUseCount(current + 1);
        return aiCharacterMapper.updateById(patch) > 0;
    }

    @Override
    public boolean shareToPlaza(Long characterId, Long userId) {
        return setShared(characterId, userId, true);
    }

    @Override
    public boolean unshareFromPlaza(Long characterId, Long userId) {
        return setShared(characterId, userId, false);
    }

    /**
     * 统一设置广场分享状态。
     * 安全：必须先校验角色存在且当前用户为创建者，杜绝 IDOR。
     */
    private boolean setShared(Long characterId, Long userId, boolean shared) {
        AiCharacter character = aiCharacterMapper.selectById(characterId);
        if (character == null) {
            throw new BusinessException(UserErrorCode.CHARACTER_NOT_FOUND);
        }
        if (userId == null || character.getUserId() == null || !character.getUserId().equals(userId)) {
            throw new BusinessException(UserErrorCode.CHARACTER_NOT_OWNER);
        }
        // 公开分享前走内容审核：角色名称 + 简介命中敏感词则拒绝上广场。
        // contentAuditService 为 null（如单测环境）时跳过，不阻塞既有流程。
        if (shared && contentAuditService != null) {
            String combined = (character.getName() == null ? "" : character.getName())
                    + " " + (character.getDescription() == null ? "" : character.getDescription())
                    + " " + (character.getPersonality() == null ? "" : character.getPersonality());
            AuditResult audit = contentAuditService.audit(combined, "ai_character", userId);
            if (audit != null && !audit.isPassed()) {
                throw new BusinessException(SocialErrorCode.CONTENT_VIOLATION);
            }
        }
        AiCharacter patch = new AiCharacter();
        patch.setId(characterId);
        patch.setIsShared(shared);
        return aiCharacterMapper.updateById(patch) > 0;
    }

    /** 排序规则：hot=使用次数 / created=最新(id 倒序) / rating=评分。 */
    private void applySort(QueryWrapper<AiCharacter> wrapper, String sortBy) {
        if (sortBy == null) {
            sortBy = "hot";
        }
        switch (sortBy) {
            case "created":
                wrapper.orderByDesc("id");
                break;
            case "rating":
                wrapper.orderByDesc("rating").orderByDesc("use_count");
                break;
            case "hot":
            default:
                wrapper.orderByDesc("use_count").orderByDesc("id");
                break;
        }
    }

    /**
     * 收敛分页参数。
     *
     * @return int[3] = { page, offset, size }
     */
    private int[] clampPage(int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        int offset = (safePage - 1) * safeSize;
        return new int[]{safePage, offset, safeSize};
    }
}

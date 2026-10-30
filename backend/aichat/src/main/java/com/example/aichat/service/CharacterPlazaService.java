package com.example.aichat.service;

import com.example.aichat.entity.AiCharacter;

import java.util.List;
import java.util.Map;

/**
 * 角色广场 / 发现页服务。
 *
 * 公开列表基于 ai_character.is_shared = true 的角色，复用既有分享机制（CharacterShareService），
 * 不重复建表；广场额外维护 use_count / rating / category / tags 等展示字段。
 */
public interface CharacterPlazaService {

    /**
     * 公开角色列表（isShared=true），支持分类筛选与排序。
     *
     * @param page    页码（从 1 开始）
     * @param size    每页大小
     * @param category 分类（可空，空表示全部）
     * @param sortBy  排序：hot=按使用次数 / created=最新 / rating=按评分
     */
    Map<String, Object> listPublicCharacters(int page, int size, String category, String sortBy);

    /** 搜索公开角色（按名称 / 描述 / 标签 / 性格模糊匹配）。 */
    Map<String, Object> searchPublicCharacters(String keyword, int page, int size);

    /**
     * 角色详情（公开信息 + 是否为所有者 + 使用次数）。
     *
     * @param characterId 角色 ID
     * @param userId     当前用户 ID（可空，匿名浏览时为 null）
     */
    Map<String, Object> getCharacterDetail(Long characterId, Long userId);

    /** 热门角色排行（按 useCount 降序）。 */
    List<AiCharacter> getHotCharacters(int limit);

    /** 角色分类列表（从 category 字段去重）。 */
    List<String> getCategories();

    /** 使用次数 +1（用户开始与角色对话时调用）。 */
    boolean incrementUseCount(Long characterId);

    /**
     * 将自建角色分享到广场（isShared = true）。
     *
     * @param characterId 角色 ID
     * @param userId       当前登录用户 ID（必须为角色创建者）
     * @return true 成功；角色不存在或非创建者时抛 BusinessException（404/403）
     */
    boolean shareToPlaza(Long characterId, Long userId);

    /**
     * 取消自建角色的广场分享（isShared = false）。
     *
     * @param characterId 角色 ID
     * @param userId      当前登录用户 ID（必须为角色创建者）
     * @return true 成功；角色不存在或非创建者时抛 BusinessException（404/403）
     */
    boolean unshareFromPlaza(Long characterId, Long userId);
}

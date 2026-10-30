package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.SocialErrorCode;
import com.example.aichat.dto.audit.AuditResult;
import com.example.aichat.entity.AiCharacter;
import com.example.aichat.event.UserDataCaptureEvent;
import com.example.aichat.mapper.AiCharacterMapper;
import com.example.aichat.mapper.CharacterShareMapper;
import com.example.aichat.service.AiCharacterService;
import com.example.aichat.service.ContentAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiCharacterServiceImpl implements AiCharacterService {

    @Autowired
    private AiCharacterMapper aiCharacterMapper;
    
    @Autowired
    private CharacterShareMapper characterShareMapper;

    @Autowired
    private ContentAuditService contentAuditService;

    /** 数据池自动采集事件发布器。 */
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public List<AiCharacter> getAllCharacters() {
        QueryWrapper<AiCharacter> wrapper = new QueryWrapper<>();
        wrapper.eq("is_system", true);
        return aiCharacterMapper.selectList(wrapper);
    }

    @Override
    public List<AiCharacter> getCharactersByUserId(Long userId) {
        List<Long> sharedCharacterIds = characterShareMapper.findSharedCharacterIdsByUserId(userId);
        
        QueryWrapper<AiCharacter> wrapper = new QueryWrapper<>();
        wrapper.and(w -> w
            .eq("user_id", userId)
            .eq("is_system", false)
        ).or(w -> w
            .eq("is_system", true)
        );
        
        List<AiCharacter> characters = aiCharacterMapper.selectList(wrapper);
        
        if (!sharedCharacterIds.isEmpty()) {
            QueryWrapper<AiCharacter> sharedWrapper = new QueryWrapper<>();
            sharedWrapper.in("id", sharedCharacterIds)
                        .eq("is_system", false);
            List<AiCharacter> sharedCharacters = aiCharacterMapper.selectList(sharedWrapper);
            characters.addAll(sharedCharacters);
        }
        
        return characters;
    }

    @Override
    public AiCharacter getCharacterById(Long id) {
        return aiCharacterMapper.selectById(id);
    }

    @Override
    public AiCharacter getCharacterByIdWithAccess(Long id, Long userId) {
        AiCharacter character = aiCharacterMapper.selectById(id);
        if (character == null) {
            return null;
        }
        
        if (Boolean.TRUE.equals(character.getIsSystem())) {
            return character;
        }
        
        if (character.getUserId() != null && character.getUserId().equals(userId)) {
            return character;
        }
        
        boolean hasShare = characterShareMapper.existsByCharacterIdAndUserId(id, userId);
        if (hasShare) {
            return character;
        }
        
        return null;
    }

    @Override
    @Transactional
    public AiCharacter createCharacter(AiCharacter character) {
        character.setCreateTime(LocalDateTime.now());
        
        if (character.getIsSystem() == null) {
            character.setIsSystem(false);
        }
        if (character.getIsShared() == null) {
            character.setIsShared(false);
        }
        
        if (character.getAvatar() != null && character.getAvatar().length() > 255) {
            character.setAvatar(character.getAvatar().substring(0, 255));
        }

        auditCharacterText(character, character.getUserId());

        aiCharacterMapper.insert(character);

        // 数据池自动采集：角色创建。仅记录名称/描述/标签，不记录完整 prompt 人设。
        if (eventPublisher != null && character.getUserId() != null) {
            StringBuilder sb = new StringBuilder();
            if (character.getName() != null) sb.append(character.getName());
            if (character.getDescription() != null) {
                if (sb.length() > 0) sb.append("：");
                sb.append(character.getDescription());
            }
            Map<String, Object> meta = new HashMap<>();
            meta.put("characterId", character.getId());
            if (character.getTags() != null) meta.put("tags", character.getTags());
            eventPublisher.publishEvent(new UserDataCaptureEvent(
                    character.getUserId(), "ai_character", sb.toString(), meta,
                    "char:" + character.getId(), LocalDateTime.now()));
        }
        return character;
    }

    @Override
    @Transactional
    public AiCharacter updateCharacter(AiCharacter character, Long userId) {
        AiCharacter existing = aiCharacterMapper.selectById(character.getId());
        if (existing == null) {
            return null;
        }
        
        if (Boolean.TRUE.equals(existing.getIsSystem())) {
            return null;
        }
        
        if (existing.getUserId() == null || !existing.getUserId().equals(userId)) {
            return null;
        }

        auditCharacterText(character, userId);

        aiCharacterMapper.updateById(character);
        return character;
    }

    @Override
    @Transactional
    public boolean deleteCharacter(Long id, Long userId) {
        AiCharacter character = aiCharacterMapper.selectById(id);
        if (character == null) {
            return false;
        }
        
        if (Boolean.TRUE.equals(character.getIsSystem())) {
            return false;
        }
        
        if (character.getUserId() == null || !character.getUserId().equals(userId)) {
            return false;
        }
        
        return aiCharacterMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean shareCharacter(Long id, boolean isShared) {
        AiCharacter character = aiCharacterMapper.selectById(id);
        if (character == null) {
            return false;
        }
        character.setIsShared(isShared);
        aiCharacterMapper.updateById(character);
        return true;
    }

    @Override
    public boolean isOwner(Long userId, Long characterId) {
        AiCharacter character = aiCharacterMapper.selectById(characterId);
        if (character == null) {
            return false;
        }
        return character.getUserId() != null && character.getUserId().equals(userId);
    }

    @Override
    public boolean canAccess(Long userId, Long characterId) {
        AiCharacter character = aiCharacterMapper.selectById(characterId);
        if (character == null) {
            return false;
        }
        
        if (Boolean.TRUE.equals(character.getIsSystem())) {
            return true;
        }
        
        if (character.getUserId() != null && character.getUserId().equals(userId)) {
            return true;
        }
        
        return characterShareMapper.existsByCharacterIdAndUserId(characterId, userId);
    }

    /**
     * 审核 AI 角色的人设相关文本（名称/提示词/性格/背景等）。
     * 命中敏感词则记录日志并抛 CONTENT_VIOLATION。
     */
    private void auditCharacterText(AiCharacter character, Long userId) {
        StringBuilder sb = new StringBuilder();
        append(sb, character.getName());
        append(sb, character.getPrompt());
        append(sb, character.getPersonality());
        append(sb, character.getBackstory());
        append(sb, character.getRelationship());

        AuditResult result = contentAuditService.audit(sb.toString(), "ai_character", userId);
        contentAuditService.recordAuditLog(userId, "ai_character", character.getId(), sb.toString(), result);
        if (!result.isPassed()) {
            throw new BusinessException(SocialErrorCode.CONTENT_VIOLATION);
        }
    }

    private static void append(StringBuilder sb, String s) {
        if (s != null && !s.isBlank()) {
            sb.append(s).append(' ');
        }
    }
}
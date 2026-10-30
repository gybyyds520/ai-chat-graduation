package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aichat.entity.AiCharacter;
import com.example.aichat.entity.CharacterShare;
import com.example.aichat.mapper.AiCharacterMapper;
import com.example.aichat.mapper.CharacterShareMapper;
import com.example.aichat.service.CharacterShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CharacterShareServiceImpl implements CharacterShareService {

    @Autowired
    private CharacterShareMapper characterShareMapper;

    @Autowired
    private AiCharacterMapper aiCharacterMapper;

    @Override
    @Transactional
    public CharacterShare shareWithUser(Long characterId, Long fromUserId, Long toUserId, String permissions) {
        AiCharacter character = aiCharacterMapper.selectById(characterId);
        if (character == null) {
            return null;
        }

        if (character.getUserId() == null) {
            return null;
        }

        if (!character.getUserId().equals(fromUserId)) {
            return null;
        }

        CharacterShare existing = characterShareMapper.selectOne(
            new QueryWrapper<CharacterShare>()
                .eq("character_id", characterId)
                .eq("shared_to_user_id", toUserId)
        );

        if (existing != null) {
            existing.setPermissions(permissions);
            existing.setStatus("ACTIVE");
            existing.setUpdatedAt(LocalDateTime.now());
            characterShareMapper.updateById(existing);
            return existing;
        }

        CharacterShare share = new CharacterShare();
        share.setCharacterId(characterId);
        share.setSharedByUserId(fromUserId);
        share.setSharedToUserId(toUserId);
        share.setPermissions(permissions != null ? permissions : "read");
        share.setStatus("ACTIVE");
        share.setCreatedAt(LocalDateTime.now());
        share.setUpdatedAt(LocalDateTime.now());

        characterShareMapper.insert(share);
        return share;
    }

    @Override
    @Transactional
    public boolean unshareWithUser(Long characterId, Long toUserId) {
        CharacterShare share = characterShareMapper.selectOne(
            new QueryWrapper<CharacterShare>()
                .eq("character_id", characterId)
                .eq("shared_to_user_id", toUserId)
                .eq("status", "ACTIVE")
        );

        if (share != null) {
            share.setStatus("REVOKED");
            share.setUpdatedAt(LocalDateTime.now());
            characterShareMapper.updateById(share);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean revokeAllShares(Long characterId) {
        List<CharacterShare> shares = characterShareMapper.findByCharacterId(characterId);
        for (CharacterShare share : shares) {
            share.setStatus("REVOKED");
            share.setUpdatedAt(LocalDateTime.now());
            characterShareMapper.updateById(share);
        }
        return !shares.isEmpty();
    }

    @Override
    public List<CharacterShare> getSharedToUsers(Long characterId) {
        return characterShareMapper.findByCharacterId(characterId);
    }

    @Override
    public List<CharacterShare> getSharedFromUsers(Long userId) {
        return characterShareMapper.findSharedByUser(userId);
    }

    @Override
    public List<CharacterShare> getSharedWithMe(Long userId) {
        return characterShareMapper.findSharedToUser(userId);
    }

    @Override
    public boolean hasAccess(Long userId, Long characterId) {
        AiCharacter character = aiCharacterMapper.selectById(characterId);
        if (character == null) {
            return false;
        }

        if (character.getIsSystem() != null && character.getIsSystem()) {
            return true;
        }

        if (character.getUserId().equals(userId)) {
            return true;
        }

        return characterShareMapper.existsByCharacterIdAndUserId(characterId, userId);
    }

    @Override
    public String getPermissions(Long userId, Long characterId) {
        AiCharacter character = aiCharacterMapper.selectById(characterId);
        if (character == null) {
            return null;
        }

        if (character.getIsSystem() != null && character.getIsSystem()) {
            return "manage";
        }

        if (character.getUserId().equals(userId)) {
            return "manage";
        }

        return characterShareMapper.findPermissionsByCharacterIdAndUserId(characterId, userId);
    }

    @Override
    public boolean hasPermission(Long userId, Long characterId, String permission) {
        String permissions = getPermissions(userId, characterId);
        if (permissions == null) {
            return false;
        }

        if ("manage".equals(permissions)) {
            return true;
        }

        if ("chat".equals(permissions) && "read".equals(permission)) {
            return true;
        }

        return permissions.equals(permission);
    }

    @Override
    public boolean isOwner(Long userId, Long characterId) {
        AiCharacter character = aiCharacterMapper.selectById(characterId);
        if (character == null) {
            return false;
        }
        return character.getUserId().equals(userId);
    }
}
package com.example.aichat.controller;

import com.example.aichat.common.audit.Audited;
import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.entity.AiCharacter;
import com.example.aichat.entity.CharacterShare;
import com.example.aichat.service.AiCharacterService;
import com.example.aichat.service.CharacterShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 角色管理接口。
 *
 * Phase 4 (P4-b) 声明式权限示范：
 *   - 只读接口（list/get）保持公开 permitAll，便于浏览；
 *   - 写操作（create/update/delete/share）叠加 @PreAuthorize("isAuthenticated()")，
 *     强制要求登录 token；
 *   - 所有写操作叠加 @Audited，自动记录操作日志；
 *   - createCharacter/shareWithUser 用 AuthContext 替代请求体中的 userId 参数。
 */
@RestController
@RequestMapping("/api/character")
public class AiCharacterController {

    @Autowired
    private AiCharacterService aiCharacterService;

    @Autowired
    private CharacterShareService characterShareService;

    @GetMapping("/list")
    public R getAllCharacters() {
        List<AiCharacter> characters = aiCharacterService.getAllCharacters();
        return R.success(characters);
    }

    @GetMapping("/list/user/{userId}")
    public R getCharactersByUserId(@PathVariable Long userId) {
        List<AiCharacter> characters = aiCharacterService.getCharactersByUserId(userId);
        return R.success(characters);
    }

    @GetMapping("/{id}")
    public R getCharacterById(@PathVariable Long id, @RequestParam Long userId) {
        AiCharacter character = aiCharacterService.getCharacterByIdWithAccess(id, userId);
        if (character != null) {
            return R.success(character);
        }
        return R.error("无权访问该角色");
    }

    /**
     * 创建角色 - Phase 4 示范：声明式权限 + AuthContext 替代 userId 参数。
     * 若请求体未提供 userId，则从 SecurityContext 自动取当前登录用户 ID。
     */
    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    @Audited(action = "CREATE_CHARACTER", resourceType = "AI_CHARACTER")
    public R createCharacter(@RequestBody AiCharacter character) {
        // 安全修复（P0）：创建者强制取当前登录用户，忽略请求体中的 userId
        character.setUserId(AuthContext.requireUserId());
        AiCharacter created = aiCharacterService.createCharacter(character);
        if (created != null) {
            return R.success(created);
        }
        return R.error("创建角色失败");
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Audited(action = "UPDATE_CHARACTER", resourceType = "AI_CHARACTER")
    public R updateCharacter(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        // 安全修复（P0）：仅本人可修改自己的角色
        Long clientUserId = request.get("userId") != null ? ((Number) request.get("userId")).longValue() : null;
        Long userId = AuthContext.requireSelf(clientUserId);

        AiCharacter character = new AiCharacter();
        character.setId(id);
        if (request.containsKey("name")) {
            character.setName((String) request.get("name"));
        }
        if (request.containsKey("avatar")) {
            character.setAvatar((String) request.get("avatar"));
        }
        if (request.containsKey("prompt")) {
            character.setPrompt((String) request.get("prompt"));
        }
        if (request.containsKey("model")) {
            character.setModel((String) request.get("model"));
        }
        // 角色字段丰富化（B组）：性格/背景/关系/语音/情感
        if (request.containsKey("personality")) {
            character.setPersonality((String) request.get("personality"));
        }
        if (request.containsKey("backstory")) {
            character.setBackstory((String) request.get("backstory"));
        }
        if (request.containsKey("relationship")) {
            character.setRelationship((String) request.get("relationship"));
        }
        if (request.containsKey("voiceStyle")) {
            character.setVoiceStyle((String) request.get("voiceStyle"));
        }
        if (request.containsKey("emotionBase")) {
            character.setEmotionBase((String) request.get("emotionBase"));
        }
        // 角色广场 / 丰富化新增字段
        if (request.containsKey("description")) {
            character.setDescription((String) request.get("description"));
        }
        if (request.containsKey("category")) {
            character.setCategory((String) request.get("category"));
        }
        if (request.containsKey("greeting")) {
            character.setGreeting((String) request.get("greeting"));
        }
        if (request.containsKey("exampleDialogue")) {
            character.setExampleDialogue((String) request.get("exampleDialogue"));
        }
        if (request.containsKey("tags")) {
            character.setTags((String) request.get("tags"));
        }
        if (request.containsKey("voiceId")) {
            character.setVoiceId((String) request.get("voiceId"));
        }
        if (request.containsKey("avatarStyle")) {
            character.setAvatarStyle((String) request.get("avatarStyle"));
        }

        AiCharacter updated = aiCharacterService.updateCharacter(character, userId);
        if (updated != null) {
            return R.success(updated);
        }
        return R.error("更新角色失败或无权操作");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Audited(action = "DELETE_CHARACTER", resourceType = "AI_CHARACTER")
    public R deleteCharacter(@PathVariable Long id, @RequestParam Long userId) {
        // 安全修复（P0）：仅本人可删除自己的角色
        Long current = AuthContext.requireSelf(userId);
        boolean deleted = aiCharacterService.deleteCharacter(id, current);
        if (deleted) {
            return R.success("删除成功");
        }
        return R.error("删除角色失败或无权操作");
    }

    @PutMapping("/{id}/share")
    @PreAuthorize("isAuthenticated()")
    @Audited(action = "SHARE_CHARACTER", resourceType = "AI_CHARACTER")
    public R shareCharacter(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        Boolean isShared = request.get("isShared");
        if (isShared == null) {
            return R.badRequest("请指定分享状态");
        }
        // 安全修复（P0 IDOR）：全局分享/取消分享必须校验当前用户为角色所有者，
        // 否则任意登录用户可通过枚举 id 篡改他人角色的公开分享状态。
        Long current = AuthContext.requireUserId();
        if (!aiCharacterService.isOwner(current, id)) {
            return R.error("无权分享该角色");
        }
        boolean success = aiCharacterService.shareCharacter(id, isShared);
        if (success) {
            return R.success(isShared ? "角色已全局分享" : "角色已取消全局分享");
        }
        return R.error("操作失败");
    }

    @PostMapping("/{id}/share-with-user")
    @PreAuthorize("isAuthenticated()")
    @Audited(action = "SHARE_WITH_USER", resourceType = "AI_CHARACTER")
    public R shareWithUser(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Long fromUserId = request.get("fromUserId") != null ? ((Number) request.get("fromUserId")).longValue() : null;
        Long toUserId = request.get("toUserId") != null ? ((Number) request.get("toUserId")).longValue() : null;
        String permissions = (String) request.getOrDefault("permissions", "read");

        // 安全修复（P0）：分享者强制取当前登录用户
        Long owner = AuthContext.requireSelf(fromUserId);
        if (toUserId == null) {
            return R.badRequest("被分享者用户ID不能为空");
        }

        CharacterShare share = characterShareService.shareWithUser(id, owner, toUserId, permissions);
        if (share != null) {
            return R.success(share);
        }
        return R.error("分享失败，可能无权分享该角色");
    }

    @DeleteMapping("/{id}/share-with-user/{toUserId}")
    @PreAuthorize("isAuthenticated()")
    @Audited(action = "UNSHARE_WITH_USER", resourceType = "AI_CHARACTER")
    public R unshareWithUser(@PathVariable Long id, @PathVariable Long toUserId) {
        // 安全修复（P0 IDOR）：取消分享必须校验当前用户为角色所有者，
        // 否则任意登录用户可撤销他人角色对第三方的分享。
        Long current = AuthContext.requireUserId();
        if (!aiCharacterService.isOwner(current, id)) {
            return R.error("无权操作该角色的分享");
        }
        boolean success = characterShareService.unshareWithUser(id, toUserId);
        if (success) {
            return R.success("已取消分享");
        }
        return R.error("取消分享失败");
    }

    @DeleteMapping("/{id}/revoke-all-shares")
    @PreAuthorize("isAuthenticated()")
    @Audited(action = "REVOKE_ALL_SHARES", resourceType = "AI_CHARACTER")
    public R revokeAllShares(@PathVariable Long id, @RequestParam Long userId) {
        // 安全修复（P0）：仅本人可撤销自己角色的分享
        Long current = AuthContext.requireSelf(userId);
        if (!aiCharacterService.isOwner(current, id)) {
            return R.error("无权操作");
        }
        boolean success = characterShareService.revokeAllShares(id);
        if (success) {
            return R.success("已取消所有分享");
        }
        return R.success("没有需要取消的分享");
    }

    @GetMapping("/{id}/shared-users")
    public R getSharedUsers(@PathVariable Long id, @RequestParam Long userId) {
        // 安全修复（P0）：仅本人可查看自己角色的分享名单
        Long current = AuthContext.requireSelf(userId);
        if (!aiCharacterService.isOwner(current, id)) {
            return R.error("无权查看");
        }
        List<CharacterShare> shares = characterShareService.getSharedToUsers(id);
        return R.success(shares);
    }

    @GetMapping("/shared-with-me")
    public R getSharedWithMe(@RequestParam Long userId) {
        Long current = AuthContext.requireSelf(userId);
        List<CharacterShare> shares = characterShareService.getSharedWithMe(current);
        return R.success(shares);
    }

    @GetMapping("/check-access")
    public R checkAccess(@RequestParam Long userId, @RequestParam Long characterId) {
        // 安全修复（P0）：只能查询当前用户自己的访问权限
        Long current = AuthContext.requireSelf(userId);
        boolean canAccess = aiCharacterService.canAccess(current, characterId);
        Map<String, Object> result = new HashMap<>();
        result.put("canAccess", canAccess);
        result.put("isOwner", aiCharacterService.isOwner(current, characterId));
        String permissions = characterShareService.getPermissions(current, characterId);
        result.put("permissions", permissions != null ? permissions : "none");
        return R.success(result);
    }

    @PostMapping("/upload-avatar")
    public R uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.badRequest("请选择要上传的图片");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.matches(".*\\.(jpg|jpeg|png|gif|webp)$")) {
            return R.badRequest("请上传有效的图片文件（支持JPG、PNG、GIF、WebP格式）");
        }

        String fileExtension = fileName.substring(fileName.lastIndexOf("."));
        try {
            File saved = com.example.aichat.util.FileUploadUtil.save(file, "character");

            Map<String, Object> result = new HashMap<>();
            result.put("id", saved.getName());
            result.put("fileName", fileName);
            result.put("filePath", com.example.aichat.util.FileUploadUtil.toUrl("character", saved));
            result.put("size", file.getSize());

            return R.success(result);
        } catch (IOException e) {
            return R.error("图片上传失败: " + e.getMessage());
        }
    }
}
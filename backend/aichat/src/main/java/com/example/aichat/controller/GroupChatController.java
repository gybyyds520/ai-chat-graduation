package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.GroupAiMember;
import com.example.aichat.entity.GroupChat;
import com.example.aichat.entity.GroupMember;
import com.example.aichat.entity.GroupMessage;
import com.example.aichat.service.GroupAiChatService;
import com.example.aichat.service.GroupChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/group")
public class GroupChatController {

    @Autowired
    private GroupChatService groupChatService;

    @Autowired
    private GroupAiChatService groupAiChatService;

    @PostMapping("/create")
    public R createGroup(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        String name = (String) request.get("name");
        
        if (name == null) {
            return R.error("参数错误");
        }
        
        // 安全修复（P0）：创建者强制取当前登录用户
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());
        String description = (String) request.get("description");
        
        GroupChat group = groupChatService.createGroup(userId, name, description);
        return R.success(group);
    }

    @GetMapping("/{groupId}")
    public R getGroup(@PathVariable Long groupId) {
        GroupChat group = groupChatService.getGroupById(groupId);
        if (group != null) {
            return R.success(group);
        }
        return R.error("群聊不存在");
    }

    @GetMapping("/my-groups")
    public R getUserGroups(@RequestParam Long userId) {
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        List<GroupChat> groups = groupChatService.getUserGroups(current);
        return R.success(groups);
    }

    @GetMapping("/public")
    public R getPublicGroups() {
        List<GroupChat> groups = groupChatService.getPublicGroups();
        return R.success(groups);
    }

    @PostMapping("/join")
    public R joinGroup(@RequestBody Map<String, Object> request) {
        Object groupIdObj = request.get("groupId");
        Object userIdObj = request.get("userId");
        
        if (groupIdObj == null) {
            return R.error("参数错误");
        }
        
        // 安全修复（P0）：加入/退出以当前登录用户为准
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());
        groupChatService.joinGroup(((Number) groupIdObj).longValue(), userId);
        return R.success("加入成功");
    }

    @PostMapping("/leave")
    public R leaveGroup(@RequestBody Map<String, Object> request) {
        Object groupIdObj = request.get("groupId");
        Object userIdObj = request.get("userId");
        
        if (groupIdObj == null) {
            return R.error("参数错误");
        }
        
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());
        groupChatService.leaveGroup(((Number) groupIdObj).longValue(), userId);
        return R.success("已退出");
    }

    @GetMapping("/members/{groupId}")
    public R getGroupMembers(@PathVariable Long groupId) {
        List<GroupMember> members = groupChatService.getGroupMembers(groupId);
        return R.success(members);
    }

    @PostMapping("/message")
    public R sendMessage(@RequestBody Map<String, Object> request) {
        Object groupIdObj = request.get("groupId");
        Object userIdObj = request.get("userId");
        String userName = (String) request.get("userName");
        String content = (String) request.get("content");
        
        if (groupIdObj == null || userName == null || content == null) {
            return R.error("参数错误");
        }
        
        // 安全修复（P0）：发件人强制取当前登录用户，防止冒充他人发言
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());
        groupChatService.sendMessage(
            ((Number) groupIdObj).longValue(),
            userId,
            userName,
            content
        );
        return R.success("发送成功");
    }

    @GetMapping("/messages/{groupId}")
    public R getGroupMessages(@PathVariable Long groupId, @RequestParam(defaultValue = "20") int limit) {
        // P1-4 修复：要求登录，且服务层校验操作者为群成员，防止遍历 groupId 读取他人群历史消息
        Long userId = com.example.aichat.common.security.AuthContext.requireUserId();
        List<GroupMessage> messages = groupChatService.getGroupMessages(groupId, userId, Math.min(Math.max(limit, 1), 200));
        return R.success(messages);
    }

    @DeleteMapping("/{groupId}")
    public R deleteGroup(@PathVariable Long groupId) {
        // P0 安全修复：删除群聊要求登录，且服务层校验操作者为群主
        Long userId = com.example.aichat.common.security.AuthContext.requireUserId();
        groupChatService.deleteGroup(groupId, userId);
        return R.success("删除成功");
    }

    @GetMapping("/recommend-friends")
    public R recommendFriends(@RequestParam Long userId) {
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        List<Map<String, Object>> recommendations = groupChatService.recommendFriends(current);
        return R.success(recommendations);
    }

    @PutMapping("/{groupId}/announcement")
    public R updateAnnouncement(@PathVariable Long groupId, @RequestBody Map<String, String> request) {
        String announcement = request.get("announcement");
        // P0 安全修复：要求登录，服务层校验操作者为群主
        Long userId = com.example.aichat.common.security.AuthContext.requireUserId();
        GroupChat group = groupChatService.updateAnnouncement(groupId, userId, announcement);
        return R.success(group);
    }

    @PutMapping("/{groupId}/settings")
    public R updateSettings(@PathVariable Long groupId, @RequestBody Map<String, String> request) {
        String settings = request.get("settings");
        // P0 安全修复：要求登录，服务层校验操作者为群主
        Long userId = com.example.aichat.common.security.AuthContext.requireUserId();
        GroupChat group = groupChatService.updateSettings(groupId, userId, settings);
        return R.success(group);
    }

    @GetMapping("/{groupId}/details")
    public R getGroupWithDetails(@PathVariable Long groupId) {
        GroupChat group = groupChatService.getGroupWithDetails(groupId);
        if (group != null) {
            return R.success(group);
        }
        return R.error("群聊不存在");
    }

    @GetMapping("/activity-ranking")
    public R getCommunityActivityRanking(@RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> ranking = groupChatService.getCommunityActivityRanking(limit);
        return R.success(ranking);
    }

    @PostMapping("/{groupId}/activity-score")
    public R updateActivityScore(@PathVariable Long groupId) {
        groupChatService.updateActivityScore(groupId);
        return R.success("活跃度已更新");
    }

    @PostMapping("/bot")
    public R createGroupBot(@RequestBody Map<String, Object> request) {
        Object groupIdObj = request.get("groupId");
        String botName = (String) request.get("botName");
        String description = (String) request.get("description");

        if (groupIdObj == null || botName == null) {
            return R.error("参数错误");
        }

        Long groupId = ((Number) groupIdObj).longValue();
        // P0 越权修复：仅群主可创建群机器人
        requireGroupOwner(groupId);

        Object bot = groupChatService.createGroupBot(
            groupId,
            botName,
            description
        );
        return R.success(bot);
    }

    @GetMapping("/bots/{groupId}")
    public R getGroupBots(@PathVariable Long groupId) {
        List<?> bots = groupChatService.getGroupBots(groupId);
        return R.success(bots);
    }

    @PutMapping("/bot/{botId}")
    public R updateGroupBot(@PathVariable Long botId, @RequestBody Map<String, Object> updates) {
        // P0 越权修复：先反查机器人所属群，再校验操作者为群主
        com.example.aichat.entity.GroupBot bot = groupChatService.getGroupBotById(botId);
        if (bot == null) {
            return R.error("群机器人不存在");
        }
        requireGroupOwner(bot.getGroupId());
        Object updated = groupChatService.updateGroupBot(botId, updates);
        return R.success(updated);
    }

    @DeleteMapping("/bot/{botId}")
    public R deleteGroupBot(@PathVariable Long botId) {
        // P0 越权修复：先反查机器人所属群，再校验操作者为群主
        com.example.aichat.entity.GroupBot bot = groupChatService.getGroupBotById(botId);
        if (bot == null) {
            return R.error("群机器人不存在");
        }
        requireGroupOwner(bot.getGroupId());
        groupChatService.deleteGroupBot(botId);
        return R.success("删除成功");
    }

    /**
     * P0 越权修复：校验当前登录用户为指定群的群主，否则抛 NOT_GROUP_OWNER（403）。
     */
    private void requireGroupOwner(Long groupId) {
        Long userId = com.example.aichat.common.security.AuthContext.requireUserId();
        com.example.aichat.entity.GroupChat group = groupChatService.getGroupById(groupId);
        if (group == null) {
            throw new com.example.aichat.common.exception.BusinessException(
                    com.example.aichat.common.exception.SocialErrorCode.GROUP_NOT_FOUND);
        }
        if (!group.getOwnerId().equals(userId)) {
            throw new com.example.aichat.common.exception.BusinessException(
                    com.example.aichat.common.exception.SocialErrorCode.NOT_GROUP_OWNER);
        }
    }

    @PostMapping("/bot/response")
    public R processBotResponse(@RequestBody Map<String, Object> request) {
        Object groupIdObj = request.get("groupId");
        String message = (String) request.get("message");
        
        if (groupIdObj == null || message == null) {
            return R.error("参数错误");
        }
        
        String response = groupChatService.processBotResponse(
            ((Number) groupIdObj).longValue(),
            message
        );
        return R.success(Map.of("response", response));
    }

    @GetMapping("/recommend-friends/advanced")
    public R recommendFriendsAdvanced(@RequestParam Long userId) {
        // P0 越权修复：只能查询自己的好友推荐
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        List<Map<String, Object>> recommendations = groupChatService.recommendFriendsAdvanced(current);
        return R.success(recommendations);
    }

    // ===== 多 AI 角色同场群聊 =====

    /** 添加 AI 角色到群（要求登录 + 群成员校验）。 */
    @PostMapping("/{groupId}/ai-members")
    public R addAiMember(@PathVariable Long groupId, @RequestBody Map<String, Object> request) {
        Object characterIdObj = request.get("characterId");
        if (characterIdObj == null) {
            return R.badRequest("characterId 不能为空");
        }
        // 安全：操作者强制取当前登录用户
        Long userId = com.example.aichat.common.security.AuthContext.requireUserId();
        groupChatService.addAiCharacterToGroup(groupId, userId, ((Number) characterIdObj).longValue());
        return R.success("已添加 AI 角色到群");
    }

    /** 移除群内 AI 角色（要求登录 + 群主校验）。 */
    @DeleteMapping("/{groupId}/ai-members/{characterId}")
    public R removeAiMember(@PathVariable Long groupId, @PathVariable Long characterId) {
        Long userId = com.example.aichat.common.security.AuthContext.requireUserId();
        groupChatService.removeAiCharacterFromGroup(groupId, userId, characterId);
        return R.success("已移除 AI 角色");
    }

    /** 列出群内激活的 AI 角色（要求登录）。 */
    @GetMapping("/{groupId}/ai-members")
    public R listAiMembers(@PathVariable Long groupId) {
        com.example.aichat.common.security.AuthContext.requireUserId();
        List<GroupAiMember> members = groupChatService.listAiCharactersInGroup(groupId);
        return R.success(members);
    }

    /** 触发一轮 AI 发言（要求登录 + 群成员校验）。 */
    @PostMapping("/{groupId}/ai-round")
    public R triggerAiRound(@PathVariable Long groupId, @RequestBody Map<String, Object> request) {
        String triggerMessage = (String) request.get("triggerMessage");
        Long userId = com.example.aichat.common.security.AuthContext.requireUserId();
        List<Map<String, Object>> replies = groupAiChatService.triggerAiRound(groupId, userId, triggerMessage);
        return R.success(replies);
    }
}
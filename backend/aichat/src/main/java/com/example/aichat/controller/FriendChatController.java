package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.CharacterShare;
import com.example.aichat.entity.FriendChatMessage;
import com.example.aichat.service.CharacterShareService;
import com.example.aichat.service.FriendChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/friend/chat")
public class FriendChatController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FriendChatController.class);

    @Autowired
    private FriendChatService friendChatService;

    @Autowired
    private CharacterShareService characterShareService;

    @Autowired
    private com.example.aichat.websocket.ChatWebSocketHandler chatWebSocketHandler;

    @PostMapping("/send")
    public R sendMessage(@RequestBody Map<String, Object> request) {
        // 安全地获取senderId，避免NullPointerException
        Object senderIdObj = request.get("senderId");
        Object receiverIdObj = request.get("receiverId");
        String content = (String) request.get("content");
        
        if (receiverIdObj == null) {
            return R.error("参数错误：用户ID不能为空");
        }
        
        // 安全修复（P0）：发件人强制取当前登录用户，忽略/拒绝客户端伪造的 senderId
        Long senderId = com.example.aichat.common.security.AuthContext.requireSelf(
                senderIdObj == null ? null : ((Number) senderIdObj).longValue());
        Long receiverId;
        try {
            receiverId = ((Number) receiverIdObj).longValue();
        } catch (ClassCastException | NumberFormatException e) {
            return R.error("参数错误：用户ID格式不正确");
        }
        
        if (receiverId <= 0) {
            return R.error("参数错误：用户ID必须为正数");
        }
        
        if (content == null || content.trim().isEmpty()) {
            return R.error("参数错误：消息内容不能为空");
        }
        
        FriendChatMessage message = friendChatService.sendMessage(senderId, receiverId, content);
        if (message != null) {
            return R.success(message);
        }
        return R.error("发送失败，可能不是好友关系");
    }

    @GetMapping("/history")
    public R getChatHistory(@RequestParam Long userId, @RequestParam Long friendId, 
                           @RequestParam(defaultValue = "20") int limit) {
        // 安全修复（P0）：只能查看自己的聊天记录
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        limit = Math.min(Math.max(limit, 1), 200);
        List<FriendChatMessage> messages = friendChatService.getChatHistory(current, friendId, limit);
        return R.success(messages);
    }

    @GetMapping("/unread")
    public R countUnreadMessages(@RequestParam Long userId) {
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        int count = friendChatService.countUnreadMessages(current);
        return R.success(count);
    }

    @PostMapping("/read")
    public R markAsRead(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        Long friendId = request.get("friendId");
        
        if (friendId == null) {
            return R.error("参数错误");
        }
        
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        friendChatService.markMessagesAsRead(current, friendId);
        return R.success("已标记为已读");
    }

    @GetMapping("/list")
    public R getRecentChats(@RequestParam Long userId) {
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        List<Map<String, Object>> chats = friendChatService.getRecentChats(current);
        return R.success(chats);
    }

    @PostMapping("/send/file")
    public R sendFileMessage(@RequestParam("file") MultipartFile file,
                             @RequestParam Long senderId,
                             @RequestParam Long receiverId) {
        if (file.isEmpty()) {
            return R.error("请选择要发送的文件");
        }

        // 安全修复（P0）：发件人强制取当前登录用户；统一走 FileUploadUtil 安全保存
        // （扩展名白名单 + 路径穿越防护）
        Long currentSender = com.example.aichat.common.security.AuthContext.requireSelf(senderId);

        try {
            File saved = com.example.aichat.util.FileUploadUtil.save(file, "chat");
            String filePath = com.example.aichat.util.FileUploadUtil.toUrl("chat", saved);
            FriendChatMessage message = friendChatService.sendFileMessage(
                    currentSender, receiverId, saved.getName(), filePath, file.getSize());

            if (message != null) {
                return R.success(message);
            }
            return R.error("发送失败，可能不是好友关系");
        } catch (IOException e) {
            return R.error("文件上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/share")
    public R shareToFriend(@RequestBody Map<String, Object> request) {
        Object senderIdObj = request.get("senderId");
        Object receiverIdObj = request.get("receiverId");
        Object dataObj = request.get("data");

        if (receiverIdObj == null || dataObj == null) {
            return R.error("参数错误");
        }

        // 安全修复（P0）：分享者强制取当前登录用户
        Long senderId = com.example.aichat.common.security.AuthContext.requireSelf(
                senderIdObj == null ? null : ((Number) senderIdObj).longValue());
        Long receiverId;
        try {
            receiverId = ((Number) receiverIdObj).longValue();
        } catch (ClassCastException | NullPointerException e) {
            return R.error("参数错误");
        }

        // 从 data 中提取角色信息，调用 CharacterShareService 创建分享记录
        // 这样接收方在查询角色列表时就能看到分享过来的角色
        String characterName = "AI角色";
        Long characterId = null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) dataObj;
            Object characterObj = data.get("character");
            if (characterObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> character = (Map<String, Object>) characterObj;
                Object idObj = character.get("id");
                if (idObj instanceof Number) {
                    characterId = ((Number) idObj).longValue();
                }
                Object nameObj = character.get("name");
                if (nameObj instanceof String) {
                    characterName = (String) nameObj;
                }
            }
        } catch (ClassCastException e) {
            // data 结构异常时退化为仅发送文本消息
        }

        if (characterId != null) {
            CharacterShare share = characterShareService.shareWithUser(
                characterId, senderId, receiverId, "chat");
            if (share == null) {
                return R.error("分享失败：角色不存在或无权分享");
            }
        }

        String shareContent = "收到一份AI角色分享：" + characterName + "（请在AI聊天中查看）";
        FriendChatMessage message = friendChatService.sendMessage(senderId, receiverId, shareContent);

        if (message != null) {
            return R.success(message);
        }
        return R.error("分享失败，可能不是好友关系");
    }

    /**
     * M3-A9：删除一条好友聊天消息（仅发送者本人可删，前端二次确认后调用）。
     */
    @DeleteMapping("/message/{messageId}")
    public R deleteMessage(@PathVariable Long messageId) {
        Long currentUserId = com.example.aichat.common.security.AuthContext.requireUserId();
        boolean ok = friendChatService.deleteMessage(messageId, currentUserId);
        if (!ok) {
            return R.unauthorized("消息不存在或无权删除");
        }
        return R.success(Map.of("deletedId", messageId));
    }

    /**
     * M1 ④：撤回本人好友聊天消息（发送后 2 分钟内）。
     * 撤回后双方均显示“消息已撤回”，并通过 WebSocket 实时通知接收方。
     */
    @PutMapping("/message/{messageId}/recall")
    public R recallMessage(@PathVariable Long messageId) {
        Long currentUserId = com.example.aichat.common.security.AuthContext.requireUserId();
        boolean ok = friendChatService.recallMessage(messageId, currentUserId, 2);
        if (!ok) {
            return R.badRequest("撤回失败：消息不存在、无权撤回或已超过 2 分钟");
        }
        // 通过 WebSocket 通知接收方刷新该消息状态
        try {
            com.example.aichat.websocket.ChatWebSocketHandler wsHandler = chatWebSocketHandler;
            if (wsHandler != null) {
                FriendChatMessage msg = friendChatService.getMessageById(messageId);
                if (msg != null && msg.getReceiverId() != null) {
                    java.util.Map<String, Object> payload = new java.util.HashMap<>();
                    payload.put("type", "message_recalled");
                    payload.put("messageId", messageId);
                    payload.put("senderId", currentUserId);
                    payload.put("content", "消息已撤回");
                    wsHandler.sendMessageToUser(msg.getReceiverId(),
                            new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload));
                }
            }
        } catch (Exception e) {
            logger.warn("推送撤回通知失败: {}", e.getMessage());
        }
        return R.success(Map.of("messageId", messageId, "content", "消息已撤回"));
    }

    @PostMapping("/import")
    public R importChatData(@RequestParam("file") MultipartFile file,
                            @RequestParam(required = false) String password,
                            @RequestParam Long userId,
                            @RequestParam(defaultValue = "false") boolean mergeChat) {        if (file.isEmpty()) {
            return R.error("请选择要导入的文件");
        }

        // 安全修复（P0）：导入仅允许操作当前登录用户自己的数据
        com.example.aichat.common.security.AuthContext.requireSelf(userId);

        try {
            byte[] fileBytes = file.getBytes();
            String content = new String(fileBytes);

            Map<String, Object> result = Map.of(
                "messages", List.of(),
                "character", Map.of("name", "导入的角色")
            );

            return R.success(result);
        } catch (IOException e) {
            return R.error("文件读取失败: " + e.getMessage());
        }
    }
}
package com.example.aichat.websocket;

import com.example.aichat.entity.FriendChatMessage;
import com.example.aichat.entity.GroupChatMessage;
import com.example.aichat.entity.User;
import com.example.aichat.entity.AiMemory;
import com.example.aichat.mapper.FriendChatMessageMapper;
import com.example.aichat.mapper.FriendshipMapper;
import com.example.aichat.mapper.GroupChatMemberMapper;
import com.example.aichat.mapper.GroupChatMessageMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.UserOnlineStatusService;
import com.example.aichat.service.AiMemoryService;
import com.example.aichat.service.ai.OllamaService;
import com.example.aichat.service.ChatMessageService;
import com.example.aichat.entity.ChatMessage;
import com.example.aichat.util.MemoryContextInjector;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final OllamaService ollamaService;
    private final ChatMessageService chatMessageService;
    private final FriendChatMessageMapper friendChatMessageMapper;
    private final GroupChatMessageMapper groupChatMessageMapper;
    private final GroupChatMemberMapper groupChatMemberMapper;
    private final FriendshipMapper friendshipMapper;
    private final UserMapper userMapper;
    private final UserOnlineStatusService userOnlineStatusService;
    private final AiMemoryService aiMemoryService;
    private final ObjectMapper objectMapper;
    private final com.example.aichat.service.SoulEngineService soulEngineService;

    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<Long, WebSocketSession> userIdToSession = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionIdToUserId = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(OllamaService ollamaService,
                                ChatMessageService chatMessageService,
                                FriendChatMessageMapper friendChatMessageMapper,
                                GroupChatMessageMapper groupChatMessageMapper,
                                GroupChatMemberMapper groupChatMemberMapper,
                                FriendshipMapper friendshipMapper,
                                UserMapper userMapper,
                                UserOnlineStatusService userOnlineStatusService,
                                AiMemoryService aiMemoryService,
                                com.example.aichat.service.SoulEngineService soulEngineService,
                                ObjectMapper objectMapper) {
        this.ollamaService = ollamaService;
        this.chatMessageService = chatMessageService;
        this.friendChatMessageMapper = friendChatMessageMapper;
        this.groupChatMessageMapper = groupChatMessageMapper;
        this.groupChatMemberMapper = groupChatMemberMapper;
        this.friendshipMapper = friendshipMapper;
        this.userMapper = userMapper;
        this.userOnlineStatusService = userOnlineStatusService;
        this.aiMemoryService = aiMemoryService;
        this.soulEngineService = soulEngineService;
        this.objectMapper = objectMapper;
    }

    /**
     * 安全加固：判断两个用户是否为已接受状态的好友（任一方向）。
     */
    private boolean isAcceptedFriend(Long userId, Long otherId) {
        try {
            com.example.aichat.entity.Friendship f1 = friendshipMapper.findByUserAndFriend(userId, otherId);
            if (f1 != null && "accepted".equalsIgnoreCase(f1.getStatus())) {
                return true;
            }
            com.example.aichat.entity.Friendship f2 = friendshipMapper.findByUserAndFriend(otherId, userId);
            return f2 != null && "accepted".equalsIgnoreCase(f2.getStatus());
        } catch (Exception e) {
            logger.warn("[WS_FRIEND_CHECK_FAILED] userId={}, otherId={}", userId, otherId, e);
            return false;
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        activeSessions.put(sessionId, session);

        // 安全修复（P0）：不再信任 URL 中的 userId 参数，改为校验握手携带的 token，
        // 从 token 中解析真实用户 ID，防止任意用户冒充他人收发消息。
        Long userId = getUserIdFromToken(session);
        if (userId == null) {
            // 兼容旧客户端：无 token 时拒绝建立有身份的连接（不注册在线状态）
            logger.warn("[WS_AUTH_FAILED] 无有效token，连接以匿名方式建立: sessionId={}", sessionId);
        } else {
            userIdToSession.put(userId, session);
            sessionIdToUserId.put(sessionId, userId);
            session.getAttributes().put("userId", userId);
            userOnlineStatusService.updateSessionId(userId, sessionId);

            logger.info("用户连接 - userId: {}, sessionId: {}", userId, sessionId);
        }

        // 修复：Map.of 不允许 null 值，当连接未携带 userId 时会抛 NullPointerException
        // 导致连接被关闭(1011)，前端发送按钮因 WebSocket 未连接而无法使用。
        // 改用 HashMap 允许 userId 为 null（未认证连接仍可建立，后续发消息时校验）。
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("type", "connected");
        response.put("sessionId", sessionId);
        response.put("userId", userId);
        response.put("message", userId != null ? "连接成功" : "未认证连接，仅可接收公开消息");
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    /**
     * 安全修复（P0）：仅从 WebSocket 握手 URL 的 token 参数解析并校验用户身份。
     * 不再回退到 URL 中的 userId 参数——该参数客户端可控，曾导致任意用户冒充他人收发消息。
     * 无 token 或 token 校验失败时返回 null，连接以匿名方式建立（不注册在线身份，无法收发消息）。
     */
    private Long getUserIdFromToken(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query == null || query.isEmpty()) {
                return null;
            }
            String token = null;
            for (String param : query.split("&")) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length != 2) continue;
                if ("token".equals(keyValue[0])) {
                    token = java.net.URLDecoder.decode(keyValue[1], java.nio.charset.StandardCharsets.UTF_8);
                }
            }

            // 必须有 token 且校验通过，以 token 内 userId 为准；否则一律视为未认证
            if (token != null && !token.isBlank()) {
                if (com.example.aichat.utils.TokenUtils.validateToken(token)) {
                    String userIdStr = com.example.aichat.utils.TokenUtils.getUserIdFromToken(token);
                    if (userIdStr != null) {
                        return Long.parseLong(userIdStr);
                    }
                }
                logger.warn("[WS_AUTH_INVALID] token校验失败");
            }
            return null;
        } catch (Exception e) {
            logger.warn("从URL token获取用户ID失败", e);
        }
        return null;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        logger.debug("收到消息: {}", payload);

        try {
            Map<String, Object> request = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
            String type = (String) request.get("type");
            
            Long userId = getUserIdFromSession(session);
            if (userId == null) {
                sendError(session, "未登录");
                return;
            }

            switch (type) {
                case "chat" -> handleChatMessage(session, request, userId);
                case "friend_message" -> handleFriendMessage(session, request, userId);
                case "group_message" -> handleGroupMessage(session, request, userId);
                case "typing" -> handleTyping(session, request, userId);
                case "read" -> handleRead(session, request, userId);
                case "notification_read" -> handleNotificationRead(session, request, userId);
                case "ping" -> {
                    Map<String, Object> pong = Map.of("type", "pong", "timestamp", System.currentTimeMillis());
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
                }
                default -> sendError(session, "未知消息类型: " + type);
            }
        } catch (Exception e) {
            logger.error("处理消息失败", e);
            sendError(session, "消息处理失败: " + e.getMessage());
        }
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        Object userIdObj = session.getAttributes().get("userId");
        if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        String userIdStr = (String) session.getAttributes().get("userId");
        if (userIdStr != null) {
            try {
                return Long.parseLong(userIdStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return sessionIdToUserId.get(session.getId());
    }

    private void handleChatMessage(WebSocketSession session, Map<String, Object> request, Long userId) throws IOException {
        String message = (String) request.get("message");
        Long characterId = Long.valueOf(request.get("characterId").toString());
        String sessionIdStr = (String) request.get("sessionId");

        // 安全加固：消息长度限制，防止超长消息消耗 AI token 与存储
        if (message == null || message.isBlank()) {
            sendError(session, "消息内容不能为空");
            return;
        }
        if (message.length() > 8000) {
            sendError(session, "单条消息不能超过8000个字符");
            return;
        }

        logger.info("处理AI聊天请求 - userId: {}, characterId: {}", userId, characterId);

        chatMessageService.saveMessage(sessionIdStr, userId, characterId, "user", message);

        String systemPrompt = buildSystemPrompt(characterId);

        // 注入灵魂状态（第五层，优先级最高）：此刻的自我
        // 灵魂算法：这一刻的自己 + 发生的经历 + 如何感受和理解 + 过去留下的记忆 = 下一刻的自己
        try {
            String soulPrefix = soulEngineService.buildSoulPrompt(userId, characterId);
            if (!soulPrefix.isEmpty()) {
                systemPrompt = soulPrefix + systemPrompt;
            }
        } catch (Exception soulEx) {
            logger.warn("注入灵魂状态失败，继续对话: {}", soulEx.getMessage());
        }

        // 注入用户长期记忆（ai_memory）到 systemPrompt 前缀（优先级最高）
        try {
            List<AiMemory> activeMemories = aiMemoryService.getActiveMemoriesForContext(userId, 10);
            if (activeMemories != null && !activeMemories.isEmpty()) {
                String aiMemoryPrefix = MemoryContextInjector.buildMemoryContextPrefix(activeMemories);
                if (!aiMemoryPrefix.isEmpty()) {
                    systemPrompt = aiMemoryPrefix + systemPrompt;
                }
                aiMemoryService.markMemoryUsed(userId,
                        activeMemories.stream().map(AiMemory::getId).toList());
            }
        } catch (Exception memEx) {
            logger.warn("注入AI记忆失败，继续对话: {}", memEx.getMessage());
        }

        // 从消息中自动提炼用户长期记忆（source=chat，自动去重）
        try {
            aiMemoryService.extractMemoriesFromMessage(userId, message);
        } catch (Exception memEx) {
            logger.warn("自动提炼AI记忆失败: {}", memEx.getMessage());
        }

        Map<String, Object> startMsg = Map.of(
            "type", "start",
            "message", ""
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(startMsg)));

        try {
            Flux<String> responseFlux = ollamaService.chatStream(null, message, systemPrompt);

            StringBuilder fullResponse = new StringBuilder();

            responseFlux.subscribe(
                chunk -> {
                    try {
                        fullResponse.append(chunk);

                        Map<String, Object> dataMsg = Map.of(
                            "type", "message",
                            "content", chunk,
                            "fullContent", fullResponse.toString()
                        );
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(dataMsg)));
                    } catch (IOException e) {
                        logger.error("发送消息失败", e);
                    }
                },
                error -> {
                    logger.error("AI响应错误", error);
                    try {
                        sendError(session, "AI响应错误: " + error.getMessage());
                    } catch (IOException e) {
                        logger.error("发送错误消息失败", e);
                    }
                },
                () -> {
                    logger.info("AI响应完成");
                    try {
                        if (fullResponse.length() > 0) {
                            chatMessageService.saveMessage(sessionIdStr, userId, characterId, "ai", fullResponse.toString());
                        }

                        // 灵魂演化：对话后异步演化下一刻的自己（失败绝不影响对话收尾）
                        try {
                            soulEngineService.evolveAfterChat(userId, characterId, null,
                                    message, fullResponse.toString());
                        } catch (Exception soulEx) {
                            logger.warn("灵魂演化失败，忽略: {}", soulEx.getMessage());
                        }

                        Map<String, Object> doneMsg = Map.of(
                            "type", "done",
                            "content", fullResponse.toString()
                        );
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(doneMsg)));
                    } catch (IOException e) {
                        logger.error("发送完成消息失败", e);
                    }
                }
            );
        } catch (Exception e) {
            logger.error("调用AI服务失败", e);
            sendError(session, "AI服务调用失败: " + e.getMessage());
        }
    }

    private void handleFriendMessage(WebSocketSession session, Map<String, Object> request, Long userId) throws IOException {
        Long receiverId = Long.valueOf(request.get("receiverId").toString());
        String content = (String) request.get("content");
        String messageType = request.get("messageType") != null ? (String) request.get("messageType") : "text";
        String extraData = request.get("extraData") != null ? request.get("extraData").toString() : null;

        // 安全修复（P0）：校验双方确为好友，防止向任意用户私发消息/骚扰
        if (content == null || content.isBlank()) {
            sendError(session, "消息内容不能为空");
            return;
        }
        if (content.length() > 8000) {
            sendError(session, "单条消息不能超过8000个字符");
            return;
        }
        boolean areFriends = isAcceptedFriend(userId, receiverId);
        if (!areFriends) {
            logger.warn("[WS_NOT_FRIEND] userId={} 尝试向非好友 {} 发消息", userId, receiverId);
            sendError(session, "你们还不是好友，无法发送消息");
            return;
        }

        logger.info("处理好友消息 - from: {}, to: {}, type: {}", userId, receiverId, messageType);

        User sender = userMapper.selectById(userId);
        User receiver = userMapper.selectById(receiverId);

        FriendChatMessage chatMessage = new FriendChatMessage();
        chatMessage.setSenderId(userId);
        chatMessage.setSenderName(sender != null ? sender.getNickname() : "未知用户");
        chatMessage.setReceiverId(receiverId);
        chatMessage.setReceiverName(receiver != null ? receiver.getNickname() : "未知用户");
        chatMessage.setContent(content);
        chatMessage.setMessageType(messageType);
        chatMessage.setIsRead(false);
        chatMessage.setExtraData(extraData);
        chatMessage.setCreateTime(LocalDateTime.now());

        friendChatMessageMapper.insert(chatMessage);

        // 最小修复：extraData 可能为 null，Map.of 不允许 null 值会抛 NPE，改用 HashMap。
        Map<String, Object> messageData = new java.util.HashMap<>();
        messageData.put("type", "friend_message");
        messageData.put("id", chatMessage.getId());
        messageData.put("senderId", userId);
        messageData.put("senderName", chatMessage.getSenderName());
        messageData.put("receiverId", receiverId);
        messageData.put("content", content);
        messageData.put("messageType", messageType);
        messageData.put("extraData", extraData);
        messageData.put("createTime", chatMessage.getCreateTime().toString());
        messageData.put("isRead", false);

        WebSocketSession receiverSession = userIdToSession.get(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(messageData)));
            logger.info("消息已发送给在线用户 - receiverId: {}", receiverId);
        } else {
            logger.info("接收者离线 - receiverId: {}", receiverId);
        }

        Map<String, Object> confirmMsg = Map.of(
            "type", "message_sent",
            "messageId", chatMessage.getId(),
            "status", "success"
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(confirmMsg)));
    }

    private void handleGroupMessage(WebSocketSession session, Map<String, Object> request, Long userId) throws IOException {
        Long groupId = Long.valueOf(request.get("groupId").toString());
        String content = (String) request.get("content");
        String messageType = request.get("messageType") != null ? (String) request.get("messageType") : "text";
        String extraData = request.get("extraData") != null ? request.get("extraData").toString() : null;

        // 安全修复（P0）：校验群成员身份，防止非成员向任意群组发消息
        if (content == null || content.isBlank()) {
            sendError(session, "消息内容不能为空");
            return;
        }
        if (content.length() > 8000) {
            sendError(session, "单条消息不能超过8000个字符");
            return;
        }
        Integer memberCount = groupChatMemberMapper.countMemberByGroupAndUser(groupId, userId);
        if (memberCount == null || memberCount <= 0) {
            logger.warn("[WS_NOT_GROUP_MEMBER] userId={} 尝试向群 {} 发消息", userId, groupId);
            sendError(session, "您不是该群成员，无法发送消息");
            return;
        }

        logger.info("处理群消息 - groupId: {}, senderId: {}, type: {}", groupId, userId, messageType);

        User sender = userMapper.selectById(userId);

        GroupChatMessage chatMessage = new GroupChatMessage();
        chatMessage.setGroupId(groupId);
        chatMessage.setSenderId(userId);
        chatMessage.setSenderName(sender != null ? sender.getNickname() : "未知用户");
        chatMessage.setContent(content);
        chatMessage.setMessageType(messageType);
        chatMessage.setIsRead(false);
        chatMessage.setExtraData(extraData);
        chatMessage.setCreateTime(LocalDateTime.now());

        groupChatMessageMapper.insert(chatMessage);

        // 最小修复：extraData 可能为 null，Map.of 不允许 null 值会抛 NPE，改用 HashMap。
        Map<String, Object> messageData = new java.util.HashMap<>();
        messageData.put("type", "group_message");
        messageData.put("id", chatMessage.getId());
        messageData.put("groupId", groupId);
        messageData.put("senderId", userId);
        messageData.put("senderName", chatMessage.getSenderName());
        messageData.put("content", content);
        messageData.put("messageType", messageType);
        messageData.put("extraData", extraData);
        messageData.put("createTime", chatMessage.getCreateTime().toString());
        messageData.put("isRead", false);

        List<com.example.aichat.entity.GroupChatMember> members = groupChatMemberMapper.getMembersByGroupId(groupId);
        for (com.example.aichat.entity.GroupChatMember member : members) {
            if (!member.getUserId().equals(userId)) {
                WebSocketSession memberSession = userIdToSession.get(member.getUserId());
                if (memberSession != null && memberSession.isOpen()) {
                    memberSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(messageData)));
                }
            }
        }

        Map<String, Object> confirmMsg = Map.of(
            "type", "message_sent",
            "messageId", chatMessage.getId(),
            "status", "success"
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(confirmMsg)));
    }

    private void handleTyping(WebSocketSession session, Map<String, Object> request, Long userId) throws IOException {
        Long targetId = Long.valueOf(request.get("targetId").toString());
        Boolean isTyping = request.get("isTyping") != null ? (Boolean) request.get("isTyping") : false;

        Map<String, Object> typingMsg = Map.of(
            "type", "typing",
            "userId", userId,
            "targetId", targetId,
            "isTyping", isTyping,
            "timestamp", System.currentTimeMillis()
        );

        WebSocketSession targetSession = userIdToSession.get(targetId);
        if (targetSession != null && targetSession.isOpen()) {
            targetSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(typingMsg)));
        }
    }

    private void handleRead(WebSocketSession session, Map<String, Object> request, Long userId) throws IOException {
        Long messageId = request.get("messageId") != null ? Long.valueOf(request.get("messageId").toString()) : null;
        Long senderId = request.get("senderId") != null ? Long.valueOf(request.get("senderId").toString()) : null;

        if (messageId != null) {
            FriendChatMessage message = friendChatMessageMapper.selectById(messageId);
            if (message != null && message.getReceiverId().equals(userId)) {
                message.setIsRead(true);
                message.setReadAt(LocalDateTime.now());
                friendChatMessageMapper.updateById(message);
            }
        } else if (senderId != null) {
            friendChatMessageMapper.update(new FriendChatMessage(),
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<FriendChatMessage>()
                    .eq("receiver_id", userId)
                    .eq("sender_id", senderId)
                    .eq("is_read", false)
                    .set("is_read", true)
                    .set("read_at", LocalDateTime.now())
            );
        }

        Map<String, Object> readMsg = Map.of(
            "type", "read_receipt",
            "userId", userId,
            "messageId", messageId,
            "senderId", senderId,
            "timestamp", System.currentTimeMillis()
        );

        if (senderId != null) {
            WebSocketSession senderSession = userIdToSession.get(senderId);
            if (senderSession != null && senderSession.isOpen()) {
                senderSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(readMsg)));
            }
        }
    }

    private void handleNotificationRead(WebSocketSession session, Map<String, Object> request, Long userId) throws IOException {
        Long notificationId = request.get("notificationId") != null ? Long.valueOf(request.get("notificationId").toString()) : null;
        
        Map<String, Object> response = Map.of(
            "type", "notification_read",
            "userId", userId,
            "notificationId", notificationId,
            "status", "success",
            "timestamp", System.currentTimeMillis()
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private String buildSystemPrompt(Long characterId) {
        return """
            你是一个专业、友好且高效的AI助手。
            
            ## 核心能力
            - 准确理解用户意图，提供有帮助的回答
            - 使用简洁清晰的语言表达
            - 在适当时候提供示例和详细解释
            
            ## 交互原则
            - 保持对话简洁有序
            - 适时询问以澄清不明确的问题
            - 承认知识局限，不知道的事情坦诚说明
            
            ## 回答风格
            - 优先使用结构化格式（列表、步骤等）
            - 复杂问题分步骤解释
            - 适当使用类比帮助理解
            """;
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        Map<String, Object> errorMsg = Map.of(
            "type", "error",
            "message", message
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMsg)));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        activeSessions.remove(sessionId);
        
        Long userId = sessionIdToUserId.remove(sessionId);
        if (userId != null) {
            userIdToSession.remove(userId);
            userOnlineStatusService.updateStatus(userId, "offline");
            
            logger.info("用户断开连接 - userId: {}, sessionId: {}", userId, sessionId);
        }
        
        logger.info("WebSocket连接已关闭: {}, 状态: {}", sessionId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket传输错误: {}", session.getId(), exception);
        activeSessions.remove(session.getId());
        
        Long userId = sessionIdToUserId.remove(session.getId());
        if (userId != null) {
            userIdToSession.remove(userId);
        }
    }

    public int getActiveConnectionCount() {
        return activeSessions.size();
    }

    public void broadcast(String message) {
        activeSessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                logger.error("广播消息失败", e);
            }
        });
    }

    public void broadcastToFriends(Long userId, String message) {
        activeSessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    Long targetUserId = getUserIdFromSession(session);
                    if (targetUserId != null && targetUserId != userId) {
                        session.sendMessage(new TextMessage(message));
                    }
                }
            } catch (IOException e) {
                logger.error("广播消息给好友失败", e);
            }
        });
    }

    public WebSocketSession getSessionByUserId(Long userId) {
        return userIdToSession.get(userId);
    }

    /**
     * M1 ②：当前在线（已认证）用户 ID 集合，供定时任务主动触达使用。
     */
    public java.util.Set<Long> getOnlineUserIds() {
        return java.util.Collections.unmodifiableSet(userIdToSession.keySet());
    }

    public void sendMessageToUser(Long userId, String message) throws IOException {
        WebSocketSession session = userIdToSession.get(userId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(message));
        }
    }
}
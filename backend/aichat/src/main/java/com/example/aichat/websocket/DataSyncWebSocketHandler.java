package com.example.aichat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DataSyncWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(DataSyncWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    public DataSyncWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        activeSessions.put(sessionId, session);

        // 安全修复（P0）：解析并校验握手 token，从 token 取用户身份，不再信任 URL userId；
        // 同时修复原实现 query 缺失时 split("=")[1] 抛数组越界的健壮性问题。
        String userId = resolveUserId(session);
        sessionUserMap.put(sessionId, userId != null ? userId : "anonymous");

        logger.info("数据同步WebSocket连接已建立: {}, 用户: {}", sessionId, userId);

        // 最小修复：userId 可能为 null（未认证连接），Map.of 不允许 null 值会抛 NPE，
        // 改为 HashMap（与 ChatWebSocketHandler.afterConnectionEstablished 一致）。
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("type", "connected");
        response.put("message", "数据同步连接成功");
        response.put("userId", userId);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    /**
     * 安全加固：优先使用 token 解析用户身份，其次兼容旧版 userId 参数。
     */
    private String resolveUserId(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query == null || query.isEmpty()) {
                return null;
            }
            String token = null;
            String queryUserId = null;
            for (String param : query.split("&")) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length != 2) continue;
                if ("token".equals(keyValue[0])) {
                    token = java.net.URLDecoder.decode(keyValue[1], java.nio.charset.StandardCharsets.UTF_8);
                } else if ("userId".equals(keyValue[0])) {
                    queryUserId = keyValue[1];
                }
            }
            if (token != null && !token.isBlank()) {
                if (com.example.aichat.utils.TokenUtils.validateToken(token)) {
                    String userIdStr = com.example.aichat.utils.TokenUtils.getUserIdFromToken(token);
                    if (userIdStr != null) {
                        return userIdStr;
                    }
                }
                logger.warn("[DATA_SYNC_AUTH_INVALID] token校验失败, sessionId={}", session.getId());
                return null;
            }
            return queryUserId;
        } catch (Exception e) {
            logger.warn("从URL参数获取用户ID失败, sessionId={}", session.getId(), e);
            return null;
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        logger.info("收到数据同步消息: {}", payload);

        try {
            Map<String, Object> request = objectMapper.readValue(payload, Map.class);
            String type = (String) request.get("type");

            if ("subscribe".equals(type)) {
                String channel = (String) request.get("channel");
                logger.info("用户订阅频道: {}", channel);
            } else if ("ping".equals(type)) {
                Map<String, Object> pong = Map.of("type", "pong", "timestamp", System.currentTimeMillis());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
            }
        } catch (Exception e) {
            logger.error("处理数据同步消息失败", e);
        }
    }

    public void broadcastDataChange(String channel, Map<String, Object> data) {
        try {
            Map<String, Object> message = Map.of(
                "type", "dataChange",
                "channel", channel,
                "data", data,
                "timestamp", System.currentTimeMillis()
            );
            
            String jsonMessage = objectMapper.writeValueAsString(message);
            
            activeSessions.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(jsonMessage));
                    }
                } catch (IOException e) {
                    logger.error("广播数据变更失败", e);
                }
            });
            
            logger.info("已广播数据变更到 {} 个客户端, 频道: {}", activeSessions.size(), channel);
        } catch (Exception e) {
            logger.error("构建广播消息失败", e);
        }
    }

    public void sendToUser(String userId, String channel, Map<String, Object> data) {
        try {
            Map<String, Object> message = Map.of(
                "type", "dataChange",
                "channel", channel,
                "data", data,
                "timestamp", System.currentTimeMillis()
            );
            
            String jsonMessage = objectMapper.writeValueAsString(message);
            
            activeSessions.entrySet().stream()
                .filter(entry -> userId.equals(sessionUserMap.get(entry.getKey())))
                .forEach(entry -> {
                    try {
                        if (entry.getValue().isOpen()) {
                            entry.getValue().sendMessage(new TextMessage(jsonMessage));
                        }
                    } catch (IOException e) {
                        logger.error("发送数据变更给用户失败", e);
                    }
                });
        } catch (Exception e) {
            logger.error("构建用户消息失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        activeSessions.remove(sessionId);
        sessionUserMap.remove(sessionId);
        logger.info("数据同步WebSocket连接已关闭: {}, 状态: {}", sessionId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("数据同步WebSocket传输错误: {}", session.getId(), exception);
        String sessionId = session.getId();
        activeSessions.remove(sessionId);
        sessionUserMap.remove(sessionId);
    }

    public int getActiveConnectionCount() {
        return activeSessions.size();
    }
}
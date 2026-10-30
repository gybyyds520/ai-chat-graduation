package com.example.aichat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebRTC 语音通话信令控制器（端点：/ws/voice/signaling）。
 *
 * 职责：只做信令中转，不碰媒体流。
 *  - join(room)：配对主叫/被叫；
 *  - offer / answer / ICE candidate：原样转发给同房间对端；
 *  - 媒体流由浏览器 WebRTC(P2P) 直接传输，服务器不经手音频字节。
 *
 * 免费/开源选型（永久免费，无付费云依赖）：
 *  - STT 语音识别：浏览器原生 Web Speech API（webkitSpeechRecognition，免费）；
 *    如需纯本地离线，可换 Vosk / whisper.cpp（均开源），二者均不产生调用费用。
 *  - TTS 语音合成：复用后端已接入的 Edge TTS（{@code EdgeTtsAdapter}，免费）。
 *  - 信令：本类自建，零成本。
 *
 * 前端骨架见 docs/webrtc-demo.html（getUserMedia + RTCPeerConnection）。
 */
@Component
public class WebRtcSignalingController extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebRtcSignalingController.class);

    private final ObjectMapper objectMapper;
    private final SignalingRoomManager roomManager = new SignalingRoomManager();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public WebRtcSignalingController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        Map<String, Object> resp = new HashMap<>();
        resp.put("type", "connected");
        resp.put("you", session.getId());
        send(session, resp);
        logger.info("[VOICE_SIGNAL] 连接建立 sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Map<String, Object> req;
        try {
            req = objectMapper.readValue(payload, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            sendError(session, "无法解析的信令消息");
            return;
        }

        String type = req.get("type") == null ? "" : req.get("type").toString();
        String sessionId = session.getId();

        switch (type) {
            case "join" -> handleJoin(session, req);
            case "offer", "answer", "candidate" -> relayToPeer(sessionId, req);
            case "leave" -> roomManager.leave(sessionId);
            case "ping" -> send(session, Map.of("type", "pong"));
            default -> sendError(session, "未知信令类型: " + type);
        }
    }

    private void handleJoin(WebSocketSession session, Map<String, Object> req) throws Exception {
        Object roomObj = req.get("room");
        if (roomObj == null || roomObj.toString().isBlank()) {
            sendError(session, "缺少 room 参数");
            return;
        }
        String roomId = roomObj.toString();
        SignalingRoomManager.JoinResult result = roomManager.join(roomId, session.getId());

        if (result.roomFull()) {
            sendError(session, "房间已满（最多两人通话）");
            return;
        }

        Map<String, Object> joined = new HashMap<>();
        joined.put("type", "joined");
        joined.put("room", roomId);
        joined.put("you", session.getId());
        joined.put("hasPeer", result.otherSessionId() != null);
        send(session, joined);

        // 通知房间里已存在的那一方：对端已加入，由其作为主叫创建 offer
        String peerId = result.otherSessionId();
        if (peerId != null) {
            WebSocketSession peer = sessions.get(peerId);
            if (peer != null && peer.isOpen()) {
                // 后加入者为被叫(false)；已在房间者为主叫(true)
                Map<String, Object> notifyExisting = new HashMap<>();
                notifyExisting.put("type", "peer-joined");
                notifyExisting.put("room", roomId);
                notifyExisting.put("initiator", true);
                send(peer, notifyExisting);

                Map<String, Object> notifyNew = new HashMap<>();
                notifyNew.put("type", "peer-joined");
                notifyNew.put("room", roomId);
                notifyNew.put("initiator", false);
                send(session, notifyNew);
            }
        }
    }

    /** 把 offer/answer/candidate 原样转发给同房间对端。 */
    private void relayToPeer(String fromSessionId, Map<String, Object> req) throws Exception {
        String peerId = roomManager.peerOf(fromSessionId);
        if (peerId == null) {
            WebSocketSession from = sessions.get(fromSessionId);
            if (from != null) {
                sendError(from, "通话尚未建立，无对端可转发");
            }
            return;
        }
        WebSocketSession peer = sessions.get(peerId);
        if (peer == null || !peer.isOpen()) {
            return;
        }
        req.put("from", fromSessionId);
        send(peer, req);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        roomManager.leave(session.getId());
        logger.info("[VOICE_SIGNAL] 连接关闭 sessionId={} status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("[VOICE_SIGNAL] 传输错误 sessionId={}", session.getId(), exception);
        sessions.remove(session.getId());
        roomManager.leave(session.getId());
    }

    private void send(WebSocketSession session, Object payload) throws Exception {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }

    private void sendError(WebSocketSession session, String message) throws Exception {
        send(session, Map.of("type", "error", "message", message));
    }

    /** 测试/运维用。 */
    public int activeSessions() {
        return sessions.size();
    }
}

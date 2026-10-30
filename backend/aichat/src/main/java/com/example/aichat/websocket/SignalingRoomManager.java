package com.example.aichat.websocket;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * WebRTC 信令房间管理器（纯内存、可单元测试，不依赖 WebSocketSession）。
 *
 * 设计目标：一个通话房间最多两个端（主叫 caller / 被叫 callee）。
 * 信令服务器本身不处理媒体，只做 offer / answer / ICE candidate 的中转，
 * 因此这里只维护"房间 -> 两个会话"的配对关系。
 *
 * 免费/开源说明：信令完全自建，无任何第三方服务费用。
 */
public class SignalingRoomManager {

    /** 一个房间最多容纳的端数（点对点语音 = 2）。 */
    public static final int MAX_PEERS_PER_ROOM = 2;

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> roomToSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionToRoom = new ConcurrentHashMap<>();

    /** join 的结果：房间里已存在的对端 sessionId（无则 null）；roomFull 表示房间已满。 */
    public record JoinResult(String otherSessionId, boolean roomFull) {}

    /**
     * 让指定会话加入房间。
     *
     * @param roomId    房间号（前端约定，如 call-10086）
     * @param sessionId WebSocket 会话 ID
     * @return 配对结果
     */
    public JoinResult join(String roomId, String sessionId) {
        ConcurrentLinkedQueue<String> room =
                roomToSessions.computeIfAbsent(roomId, k -> new ConcurrentLinkedQueue<>());

        synchronized (room) {
            // 同一会话重复 join：幂等，返回当前对端
            if (room.contains(sessionId)) {
                return new JoinResult(peerOf(room, sessionId), false);
            }
            if (room.size() >= MAX_PEERS_PER_ROOM) {
                return new JoinResult(null, true);
            }
            room.add(sessionId);
            sessionToRoom.put(sessionId, roomId);
            return new JoinResult(peerOf(room, sessionId), false);
        }
    }

    /** 会话离开房间（断开 / 主动 leave）。 */
    public void leave(String sessionId) {
        String roomId = sessionToRoom.remove(sessionId);
        if (roomId == null) {
            return;
        }
        ConcurrentLinkedQueue<String> room = roomToSessions.get(roomId);
        if (room == null) {
            return;
        }
        synchronized (room) {
            room.remove(sessionId);
            if (room.isEmpty()) {
                roomToSessions.remove(roomId);
            }
        }
    }

    /** 返回同一房间内对端的 sessionId；不存在则 null。 */
    public String peerOf(String sessionId) {
        String roomId = sessionToRoom.get(sessionId);
        if (roomId == null) {
            return null;
        }
        ConcurrentLinkedQueue<String> room = roomToSessions.get(roomId);
        return room == null ? null : peerOf(room, sessionId);
    }

    /** 该会话当前所在房间。 */
    public String roomOf(String sessionId) {
        return sessionToRoom.get(sessionId);
    }

    private String peerOf(ConcurrentLinkedQueue<String> room, String sessionId) {
        for (String s : room) {
            if (!s.equals(sessionId)) {
                return s;
            }
        }
        return null;
    }

    /** 测试/运维用：当前房间数。 */
    public int roomCount() {
        return roomToSessions.size();
    }
}

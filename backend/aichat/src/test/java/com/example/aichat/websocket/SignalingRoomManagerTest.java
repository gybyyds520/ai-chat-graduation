package com.example.aichat.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SignalingRoomManager 房间配对 / 转发寻址 单元测试。
 * 覆盖：双人配对、房间满拒绝、不同房间隔离、离开后释放、重复 join 幂等。
 */
class SignalingRoomManagerTest {

    private SignalingRoomManager manager;

    @BeforeEach
    void setUp() {
        manager = new SignalingRoomManager();
    }

    @Test
    void 两人加入同一房间应互相配对() {
        SignalingRoomManager.JoinResult first = manager.join("room-1", "A");
        assertNull(first.otherSessionId());
        assertFalse(first.roomFull());

        SignalingRoomManager.JoinResult second = manager.join("room-1", "B");
        assertEquals("A", second.otherSessionId());
        assertFalse(second.roomFull());

        // 互查对端
        assertEquals("B", manager.peerOf("A"));
        assertEquals("A", manager.peerOf("B"));
    }

    @Test
    void 第三人加入已满房间应被拒绝() {
        manager.join("room-1", "A");
        manager.join("room-1", "B");

        SignalingRoomManager.JoinResult third = manager.join("room-1", "C");
        assertTrue(third.roomFull());
        assertNull(third.otherSessionId());
        // C 不应被计入配对，A/B 的对端仍是彼此
        assertEquals("B", manager.peerOf("A"));
    }

    @Test
    void 不同房间互相隔离() {
        manager.join("room-1", "A");
        manager.join("room-2", "B");
        assertNull(manager.peerOf("A"));
        assertNull(manager.peerOf("B"));
    }

    @Test
    void 离开后应释放房间并可重新配对() {
        manager.join("room-1", "A");
        manager.join("room-1", "B");
        assertEquals("B", manager.peerOf("A"));

        manager.leave("A");
        assertNull(manager.peerOf("B"));
        assertNull(manager.peerOf("A"));

        // A 重新进来应与 B 重新配对
        SignalingRoomManager.JoinResult rejoin = manager.join("room-1", "A");
        assertEquals("B", rejoin.otherSessionId());
    }

    @Test
    void 重复join同一房间幂等且不破坏配对() {
        manager.join("room-1", "A");
        manager.join("room-1", "B");

        SignalingRoomManager.JoinResult again = manager.join("room-1", "A");
        assertFalse(again.roomFull());
        assertEquals("B", again.otherSessionId());
    }
}
